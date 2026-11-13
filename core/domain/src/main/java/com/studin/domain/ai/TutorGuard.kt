package com.studin.domain.ai

import com.studin.domain.classifier.TextNormalizer

/**
 * App-side control layer around the small on-device language model (Gemma 3 1B).
 *
 * A 1B model is good at explaining and summarising but drifts, over-answers and invents facts
 * when left unconstrained. StudIN therefore decides *in code* what the model is allowed to do:
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

    /** Short follow-ups ("give an example", "why?", "explain it more") reuse the previous question. */
    fun isFollowUp(question: String): Boolean {
        val q = TextNormalizer.normalize(question)
        val words = q.split(' ').filter { it.isNotBlank() }
        if (words.isEmpty() || words.size > 12) return false
        val cues = listOf("it", "this", "that", "they", "them", "its", "more", "again", "example", "examples", "why", "how", "elaborate", "explain more", "continue", "simpler", "shorter", "hindi", "detail", "also", "another")
        return words.size <= 3 || words.any { it in cues }
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
        maxWords: Int = if (intent == Intent.SUMMARIZE) 150 else 120,
        maxChars: Int = 2_000,
    ): String {
        val rules = if (intent == Intent.SUMMARIZE) {
            """You are StudIN, a careful study assistant${examName?.let { " for a $it student" } ?: ""}.
Summarise the student's notes below.
Rules:
- Use only information that is in the notes. Do not add new facts.
- Give at most 6 short bullet points, then one line starting "Key terms:" with up to 5 terms.
- Stay under $maxWords words. No introduction and no closing remarks."""
        } else {
            """You are StudIN, a careful study tutor${examName?.let { " for a $it student" } ?: ""}.
Rules:
- Answer only the question below. Do not add extra sections, tasks or follow-up questions.
- Stay under $maxWords words. Use simple English (Hinglish is fine if the student writes Hinglish).
- Use short bullet points for lists. For code, give one short code block with a one-line explanation.
- Only state facts you are sure of. Never invent numbers, names, dates, formulas or sources.
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
    private val specialTokens = Regex("<(start_of_turn|end_of_turn|eos|bos|pad)>|</?s>")
    /** The model starting a new conversational turn on its own = everything after is invented. */
    private val roleEcho = Regex("(?m)^\\s*(Student|User|Question|Q|Human|Tutor|Assistant|StudIN|Model)\\s*:")
    private val leadingRole = Regex("^\\s*(model\\s*\\n|Tutor\\s*:|Assistant\\s*:|StudIN\\s*:|Answer\\s*:)\\s*", RegexOption.IGNORE_CASE)

    fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { it.isNotBlank() }

    /** Cleaned text for display while streaming (cheap, idempotent). */
    fun clean(raw: String): String {
        var t = raw.replace(specialTokens, "").replace("\r", "")
        t = t.replace(leadingRole, "")
        // Cut at a role label that appears after the first line (a fabricated next turn).
        roleEcho.findAll(t).firstOrNull { it.range.first > 0 }?.let { t = t.substring(0, it.range.first) }
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
