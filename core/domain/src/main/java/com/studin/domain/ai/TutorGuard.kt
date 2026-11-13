package com.studin.domain.ai

import com.studin.domain.classifier.TextNormalizer

/**
 * App-side control layer around the on-device language model (Qwen2.5 1.5B / Qwen3 4B, or the
 * older Gemma 3 1B).
 *
 * Small models are good at explaining and summarising but drift, over-answer and invent facts
 * (names, dates, links) when left unconstrained. StudIN therefore decides *in code* what the
 * model is allowed to do:
 *  1. [route] answers identity / greeting / planning / quiz / wellbeing / off-topic / safety
 *     messages deterministically – the model never sees them;
 *  2. [buildPrompt] gives the model one clean instruction turn whose rules are never truncated;
 *  3. [ReplyGuard] strips role echoes, cuts repetition loops and caps length at a sentence end.
 */
object TutorGuard {

    enum class Intent { SAFETY, GREETING, THANKS, IDENTITY, PLAN, QUIZ, WELLBEING, OFF_TOPIC, SUMMARIZE, STUDY }

    /** Questions longer than this are treated as notes to summarise. */
    const val SUMMARY_MIN_CHARS = 350

    private val safety = listOf(
        "kill myself", "killing myself", "suicide", "suicidal", "end my life", "end it all", "want to die",
        "wanna die", "self harm", "self-harm", "hurt myself", "harm myself", "no reason to live", "better off dead",
        "marna chahta", "marna chahti", "mar jana chahta", "mar jaana", "jeena nahi chahta", "jeena nahi chahti",
        "आत्महत्या", "मरना चाहता", "मरना चाहती",
    )
    private val greetings = setOf(
        "hi", "hii", "hiii", "hello", "hey", "heya", "hola", "namaste", "namaskar", "yo", "sup",
        "good morning", "good afternoon", "good evening", "gm", "hi there", "hello there", "hey there",
    )
    private val thanks = listOf("thanks", "thank you", "thankyou", "thx", "ty", "dhanyavad", "dhanyawad", "shukriya", "great thanks", "ok thanks", "okay thanks")
    private val identity = listOf(
        "who are you", "what are you", "your name", "who made you", "who created you", "who built you",
        "are you human", "are you a bot", "are you chatgpt", "are you gemini", "are you real", "what can you do",
        "introduce yourself", "tum kaun ho", "aap kaun ho", "kaun ho", "what model are you", "which model",
    )
    private val plan = listOf(
        "what should i study", "what to study", "study plan", "revision plan", "plan my", "plan for today",
        "make a plan", "timetable", "time table", "schedule for today", "kya padhu", "kya padhna", "aaj kya",
    )
    private val quiz = listOf("quiz me", "test me", "ask me questions", "ask me a question", "give me mcq", "give me mcqs", "mcq test", "practice questions")
    private val wellbeing = listOf(
        "tired", "bored", "motivat", "stressed", "stress", "can't focus", "cant focus", "cannot focus", "distracted",
        "anxious", "anxiety", "nervous", "exam fear", "procrastinat", "lazy", "overwhelmed", "demotivated", "burnout", "burn out",
    )
    private val summarizeVerbs = listOf("summarise", "summarize", "summary of", "short notes", "key points", "tl;dr", "tldr")

    /**
     * Decide who answers. [isDistraction] lets the caller plug in the syllabus-aware
     * content classifier (true only for clearly off-syllabus requests, e.g. game tips).
     */
    fun route(question: String, isDistraction: (String) -> Boolean = { false }): Intent {
        val raw = question.trim()
        val q = raw.lowercase()
        val norm = TextNormalizer.normalize(raw)
        val words = norm.split(' ').filter { it.isNotBlank() }
        if (safety.any { it in q }) return Intent.SAFETY
        if (raw.length >= SUMMARY_MIN_CHARS) return Intent.SUMMARIZE
        if (words.size <= 4 && (norm in greetings || greetings.any { norm.startsWith("$it ") && words.size <= 3 })) return Intent.GREETING
        if (words.size <= 5 && thanks.any { norm == it || norm.startsWith("$it ") || norm.endsWith(" $it") }) return Intent.THANKS
        if (identity.any { it in q }) return Intent.IDENTITY
        if (quiz.any { it in q }) return Intent.QUIZ
        if (plan.any { it in q }) return Intent.PLAN
        if (summarizeVerbs.any { q.startsWith(it) || q.contains("$it:") || q.contains("$it these") || q.contains("$it this") }) return Intent.SUMMARIZE
        if (wellbeing.any { it in q } && words.size <= 25) return Intent.WELLBEING
        if (isDistraction(raw)) return Intent.OFF_TOPIC
        return Intent.STUDY
    }

    private val followPronouns = setOf("it", "this", "that", "they", "them", "its", "these", "those", "he", "she", "his", "her")
    private val followWords = setOf(
        "more", "again", "example", "examples", "elaborate", "continue", "simpler", "simply", "shorter", "detail",
        "detailed", "another", "also", "else", "hindi", "hinglish",
    )
    private val followShort = setOf("why", "how", "really", "so")

    /**
     * Follow-ups ("give an example", "why?", "explain it more") reuse the previous question.
     * A short *new* topic ("linux os", "binary search") is not a follow-up: it needs a cue word.
     */
    fun isFollowUp(question: String): Boolean {
        val q = TextNormalizer.normalize(question)
        val words = q.split(' ').filter { it.isNotBlank() }
        if (words.isEmpty() || words.size > 12) return false
        if (words.size <= 2 && words.any { it in followShort }) return true // "why?", "but why", "how so"
        return words.any { it in followPronouns || it in followWords } || q.startsWith("what about ") || q.startsWith("and ")
    }

    private val codeCue = Regex(
        "\\b(code|coding|program|programs|programme|function|syntax|implement|implementation|script|snippet|pseudocode|" +
            "python|java|javascript|kotlin|sql|html|css|print|loop|array|recursion)\\b|\\bc\\+\\+|\\bc program",
        RegexOption.IGNORE_CASE,
    )

    /** The student wants code: the answer gets a bigger budget so the code block isn't cut. */
    fun wantsCode(question: String): Boolean = codeCue.containsMatchIn(question)

    /** Word budget for an answer. */
    fun maxWordsFor(question: String, intent: Intent): Int = when {
        intent == Intent.SUMMARIZE -> 150
        wantsCode(question) -> 220
        else -> 120
    }

    data class Turn(val question: String, val answer: String)

    /**
     * One instruction turn for the model. The rules come first and are never cut; when the
     * prompt is too long, the follow-up context is dropped first, then notes, then the question
     * is shortened.
     */
    fun buildPrompt(
        question: String,
        intent: Intent,
        examName: String?,
        notes: List<String> = emptyList(),
        previous: Turn? = null,
        maxWords: Int = maxWordsFor(question, intent),
        maxChars: Int = 2_000,
    ): String {
        val rules = if (intent == Intent.SUMMARIZE) {
            """You are StudIN, a careful study assistant${examName?.let { " for a $it student" } ?: ""}.
Summarise the student's notes below.
Rules:
- Use only information that is in the notes. Do not add new facts.
- Give at most 6 short bullet points, then one line starting "Key terms:" with up to 5 terms.
- Do not include website links or URLs.
- Stay under $maxWords words. No introduction and no closing remarks."""
        } else {
            """You are StudIN, a careful study tutor${examName?.let { " for a $it student" } ?: ""}.
Rules:
- Answer only the question below. Do not add extra sections, tasks or follow-up questions.
- Stay under $maxWords words. Use simple English (Hinglish is fine if the student writes Hinglish).
- Use short bullet points for lists. If the student asks for code, give one complete code block in a fenced ``` block, then a one-line explanation.
- Only state facts you are sure of. Never invent numbers, names, dates, formulas or sources.
- Do not include website links or URLs.
- If you are not sure, say "I'm not sure about this — please check your textbook." """.trimEnd()
        }
        fun assemble(ctxNotes: List<String>, prev: Turn?, q: String) = buildString {
            append(rules).append("\n\n")
            if (ctxNotes.isNotEmpty() && intent != Intent.SUMMARIZE) {
                append("The student's own flashcards (use only if relevant):\n")
                ctxNotes.forEach { append("- ").append(it.take(160)).append('\n') }
                append('\n')
            }
            if (prev != null && intent != Intent.SUMMARIZE) {
                append("Previous question: ").append(prev.question.take(200)).append('\n')
                append("Your previous answer began: ").append(prev.answer.take(220).replace('\n', ' ')).append("\n\n")
            }
            append(if (intent == Intent.SUMMARIZE) "Notes:\n" else "Question: ").append(q)
        }
        var notesNow = notes.take(3)
        var prevNow = previous
        var q = question.trim()
        var p = assemble(notesNow, prevNow, q)
        if (p.length > maxChars) { prevNow = null; p = assemble(notesNow, prevNow, q) }
        while (p.length > maxChars && notesNow.isNotEmpty()) { notesNow = notesNow.dropLast(1); p = assemble(notesNow, prevNow, q) }
        if (p.length > maxChars) {
            val room = (maxChars - assemble(notesNow, prevNow, "").length - 2).coerceAtLeast(200)
            val head = q.take(room)
            q = head.substringBeforeLast(' ', head).trimEnd() + " …"
            p = assemble(notesNow, prevNow, q)
        }
        return p
    }
}

/** Cleans and bounds model output, both while streaming and at the end. */
object ReplyGuard {
    private val specialTokens = Regex("<(start_of_turn|end_of_turn|eos|bos|pad)>|</?s>|<\\|[a-z_]{2,20}\\|>")
    /** Qwen3 reasoning blocks: StudIN switches thinking off; this is the backstop. */
    private val thinkBlock = Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL)
    private val url = Regex("(https?://|www\\.)\\S+", RegexOption.IGNORE_CASE)
    private val mdLink = Regex("\\[([^\\]]{1,80})]\\((https?://|www\\.)[^)]*\\)?", RegexOption.IGNORE_CASE)
    private val emptyBullet = Regex("^\\s*([-*•]|\\d{1,2}[.)])\\s*$")
    /** The model starting a new conversational turn on its own = everything after is invented. */
    private val roleEcho = Regex("(?m)^\\s*(Student|User|Question|Q|Human|Tutor|Assistant|StudIN|Model)\\s*:")
    private val leadingRole = Regex("^\\s*(model\\s*\\n|Tutor\\s*:|Assistant\\s*:|StudIN\\s*:|Answer\\s*:)\\s*", RegexOption.IGNORE_CASE)

    fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { it.isNotBlank() }

    /**
     * Cleaned text for display while streaming (cheap, idempotent). [qaFormat] keeps "Q:"/"A:"
     * lines (flashcard generation) instead of treating them as an invented next turn.
     */
    fun clean(raw: String, qaFormat: Boolean = false): String {
        var t = raw.replace(thinkBlock, "").replace(specialTokens, "").replace("\r", "")
        // An unfinished reasoning block: nothing after it is an answer yet.
        t.indexOf("<think>").takeIf { it >= 0 }?.let { t = t.substring(0, it) }
        t = t.replace("</think>", "")
        t = t.replace(leadingRole, "")
        // Cut at a role label that appears after the first line (a fabricated next turn).
        if (!qaFormat) roleEcho.findAll(t).firstOrNull { it.range.first > 0 }?.let { t = t.substring(0, it.range.first) }
        t = stripLinks(t)
        t = t.split('\n').filterNot { emptyBullet.matches(it) }.joinToString("\n")
        t = dedupeLines(t)
        t = cutLoop(t)
        return t.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    /** True once generation should be stopped: length cap reached at a sentence end, or a loop. */
    fun shouldStop(text: String, maxWords: Int): Boolean {
        val words = wordCount(text)
        if (words >= (maxWords * 1.3).toInt()) return true
        if (words >= maxWords && text.trimEnd().lastOrNull()?.let { it in ".!?।\n" } == true && !insideCodeBlock(text)) return true
        if (cutLoop(text).length < text.length - 40) return true
        if (roleEcho.findAll(text).any { it.range.first > 0 }) return true
        return false
    }

    /** Final tidy-up: if output was cut mid-sentence, end at the last complete sentence. */
    fun finish(raw: String, truncated: Boolean): String {
        var t = clean(raw)
        if (insideCodeBlock(t)) t += "\n```"
        if (truncated) {
            val last = t.trimEnd().lastOrNull()
            if (last != null && last !in ".!?।`)\"") {
                val cut = Regex("[.!?।](\\s|$)").findAll(t).lastOrNull()?.range?.first
                t = if (cut != null && cut > t.length * 0.5) t.substring(0, cut + 1) else "$t…"
            }
        }
        return t.trim()
    }

    private fun insideCodeBlock(t: String) = Regex("```").findAll(t).count() % 2 == 1

    /**
     * Small models invent plausible-looking links. Markdown links keep their text; any sentence
     * that still carries a URL is dropped. Code blocks are left alone.
     */
    private fun stripLinks(t: String): String {
        if (!url.containsMatchIn(t)) return t
        var inCode = false
        return t.split('\n').mapNotNull { line ->
            if (line.trimStart().startsWith("```")) { inCode = !inCode; return@mapNotNull line }
            if (inCode || !url.containsMatchIn(line)) return@mapNotNull line
            val linked = line.replace(mdLink) { m -> if (url.containsMatchIn(m.groupValues[1])) "" else m.groupValues[1] }
            if (!url.containsMatchIn(linked)) return@mapNotNull linked
            val kept = linked.split(Regex("(?<=[.!?])\\s+")).filterNot { url.containsMatchIn(it) }.joinToString(" ")
            kept.takeIf { it.isNotBlank() && !emptyBullet.matches(it) && it.trim().length > 2 }
        }.joinToString("\n")
    }

    private fun dedupeLines(t: String): String {
        val out = ArrayList<String>()
        for (line in t.split('\n')) {
            val key = line.trim().lowercase()
            if (key.length >= 12 && out.isNotEmpty() && out.last().trim().lowercase() == key) continue
            out += line
        }
        return out.joinToString("\n")
    }

    /** Detect a sentence repeated 3+ times (generation loop) and keep text up to its 2nd occurrence. */
    private fun cutLoop(t: String): String {
        val sentences = Regex("[^.!?\\n]{20,}[.!?\\n]").findAll(t).toList()
        val seen = HashMap<String, Int>()
        for (m in sentences) {
            val key = m.value.trim().lowercase()
            val n = (seen[key] ?: 0) + 1
            seen[key] = n
            if (n >= 3) {
                val second = sentences.filter { it.value.trim().lowercase() == key }[1]
                return t.substring(0, second.range.first).trimEnd()
            }
        }
        return t
    }
}
