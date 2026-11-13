package com.studin.app.ai

import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.AppClock
import com.studin.app.core.data.repo.ProfileRepository
import com.studin.app.core.data.repo.StudyRepository
import com.studin.domain.ai.LiteAi
import com.studin.domain.ai.ReplyGuard
import com.studin.domain.ai.TutorGuard
import com.studin.domain.importer.ParsedCard
import com.studin.domain.model.Verdict
import com.studin.domain.plan.PlannerTopic
import com.studin.domain.plan.StudyPlanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import javax.inject.Inject
import javax.inject.Singleton

enum class AiEngine(val label: String) { LLM("On-device LLM"), LITE("StudIN Lite AI") }

data class ChatTurn(val fromUser: Boolean, val text: String)

data class CardGenResult(val cards: List<ParsedCard>, val engine: AiEngine, val keywords: List<String>)

/**
 * Facade over the two on-device AI tiers. Uses the optional LLM when the student
 * has imported a model and enabled it; otherwise (or on any failure) falls back
 * to the deterministic Lite engine so features always work offline.
 */
@Singleton
class StudyAssistant @Inject constructor(
    private val llm: OnDeviceLlm,
    private val classifiers: ClassifierProvider,
    private val study: StudyRepository,
    private val profiles: ProfileRepository,
    private val prefs: UserPreferences,
    private val clock: AppClock,
) {
    val llmState: StateFlow<LlmState> get() = llm.state

    private suspend fun llmEnabled(): Boolean =
        prefs.current().aiUseLlm && (llm.state.value is LlmState.Ready || llm.state.value is LlmState.Installed)

    // ------------------------------------------------------------- Flashcards
    suspend fun generateCards(notes: String, max: Int = 15): CardGenResult {
        val clean = notes.take(12_000)
        val keywords = LiteAi.keywords(clean, 8).map { it.first }
        if (llmEnabled()) {
            val prompt = """
                You are a strict exam tutor. Create up to $max flashcards from the notes below.
                Use ONLY facts in the notes. Format exactly:
                Q: <question>
                A: <short answer>

                NOTES:
                ${clean.take(2_400)}
            """.trimIndent()
            val out = runCatching { llm.generate(prompt, temperature = 0.2f).lastOrNull()?.let { ReplyGuard.clean(it.text) } }.getOrNull()
            val cards = out?.let { LiteAi.parseQaBlocks(it, max) }.orEmpty()
            if (cards.size >= 2) return CardGenResult(cards, AiEngine.LLM, keywords)
        }
        return CardGenResult(LiteAi.notesToCards(clean, max), AiEngine.LITE, keywords)
    }

    // ------------------------------------------------------------- Tutor chat
    /** Streams the accumulated reply text. */
    fun chat(question: String, history: List<ChatTurn>): Flow<Pair<String, AiEngine>> = flow {
        val q = question.trim().take(4_000)
        // 1) The app decides who answers. The model never sees identity, greetings, planning,
        //    quiz, wellbeing, off-topic or safety messages.
        val intent = TutorGuard.route(q) { classifiers.classifier.classify(it).verdict == Verdict.BLOCK }
        when (intent) {
            TutorGuard.Intent.SAFETY, TutorGuard.Intent.GREETING, TutorGuard.Intent.THANKS, TutorGuard.Intent.IDENTITY,
            TutorGuard.Intent.QUIZ, TutorGuard.Intent.WELLBEING, TutorGuard.Intent.OFF_TOPIC ->
                return@flow emitAll(stream(cannedAnswer(intent), AiEngine.LITE))
            TutorGuard.Intent.PLAN -> return@flow emitAll(stream(planAnswer(), AiEngine.LITE))
            TutorGuard.Intent.SUMMARIZE, TutorGuard.Intent.STUDY -> Unit
        }
        val context = if (intent == TutorGuard.Intent.STUDY) retrieveContext(q) else emptyList()
        if (!llmEnabled()) {
            return@flow emitAll(if (intent == TutorGuard.Intent.SUMMARIZE) stream(liteSummary(q), AiEngine.LITE) else liteStream(q, context))
        }

        // 2) One clean instruction turn: rules first (never truncated), relevant notes, and the
        //    previous exchange only for genuine follow-ups (prevents answers bleeding across).
        val previous = history.windowed(2).lastOrNull { it[0].fromUser && !it[1].fromUser }
            ?.takeIf { TutorGuard.isFollowUp(q) }?.let { TutorGuard.Turn(it[0].text, it[1].text) }
        val maxWords = if (intent == TutorGuard.Intent.SUMMARIZE) 150 else 120
        val prompt = TutorGuard.buildPrompt(q, intent, profiles.get()?.exam?.displayName, context, previous, maxWords)

        // 3) Guarded generation: cleaned while streaming, stopped at the length cap or on loops.
        var produced = false
        emitAll(flow {
            llm.generate(
                prompt,
                temperature = if (intent == TutorGuard.Intent.SUMMARIZE) 0.2f else 0.3f,
                stopWhen = { ReplyGuard.shouldStop(ReplyGuard.clean(it), maxWords) },
            ).collect { chunk ->
                val text = if (chunk.done) ReplyGuard.finish(chunk.text, truncated = chunk.stoppedEarly) else ReplyGuard.clean(chunk.text)
                if (text.isNotBlank()) { produced = true; emit(text to AiEngine.LLM) }
            }
        }.catch { if (!produced) emitAll(liteStream(q, context)) else throw it })
    }

    /** Fixed, honest replies for messages the model shouldn't improvise. */
    private suspend fun cannedAnswer(intent: TutorGuard.Intent): String = when (intent) {
        TutorGuard.Intent.SAFETY ->
            "I'm really sorry you're feeling this way. You don't have to handle it alone.\n\n" +
                "• Please talk to someone you trust right now — a parent, friend, teacher or counsellor.\n" +
                "• Tele-MANAS (Government of India, free, 24×7): call 14416 or 1-800-891-4416.\n" +
                "• If you are in immediate danger, call 112.\n\n" +
                "Exams matter, but you matter more. When you feel ready, I can help you plan in small, manageable steps."
        TutorGuard.Intent.GREETING -> {
            val name = profiles.get()?.name?.takeIf { it.isNotBlank() && it != "Student" }
            "Hi${name?.let { " $it" } ?: ""}! What are we studying today?\n\n" +
                "• Ask me to explain a topic\n• Paste notes and I'll summarise them\n• Ask \"what should I study today?\" for a plan"
        }
        TutorGuard.Intent.THANKS -> "You're welcome! Small, steady sessions add up — keep going."
        TutorGuard.Intent.IDENTITY ->
            "I'm StudIN's AI Tutor — a study assistant that runs entirely on this phone.\n\n" +
                "• I explain concepts, summarise your notes and help turn them into flashcards.\n" +
                "• I plan your revision from your syllabus and due cards.\n" +
                (if (llm.state.value is LlmState.Ready || llm.state.value is LlmState.Installed) "• Fuller explanations come from Google's Gemma 3 1B model, running offline.\n"
                else "• Download the AI Tutor model (Gemma 3 1B) for fuller explanations — it then works offline.\n") +
                "\nI can make mistakes, so please double-check important facts with your textbook."
        TutorGuard.Intent.QUIZ ->
            "Let's test you with real questions from your own flashcards:\n\n" +
                "• Tap Quiz me in Quick actions, or open Study → Quiz.\n" +
                "• Wrong answers bring that card back sooner, so every mistake helps."
        TutorGuard.Intent.WELLBEING ->
            "That's normal — focus is a skill, not a personality trait. Try this:\n\n" +
                "• Start a 15-minute session on the easiest topic – momentum first.\n" +
                "• Put distracting apps on Block in Focus → Rules.\n" +
                "• After the session, do 5 quick flashcards: active recall beats re-reading.\n" +
                "• Sleep, water and a short walk matter more than one extra hour."
        TutorGuard.Intent.OFF_TOPIC ->
            "That sounds like a break-time topic, so I'll skip it. I'm here for your studies — ask me to explain a concept, " +
                "summarise your notes or plan your revision. Need a break? Take a proper 5 minutes away from the screen."
        else -> ""
    }

    /** Lite (no model) summary of pasted notes, with any "summarise:" prefix removed. */
    private fun liteSummary(q: String): String {
        val text = q.replace(Regex("^(please\\s+)?(summari[sz]e|summary of|short notes|key points)[^:\\n]*[:\\n]?", RegexOption.IGNORE_CASE), "").trim()
        val points = LiteAi.summarize(text, 4)
        if (points.isEmpty()) return "Paste a few sentences of notes after \"Summarise:\" and I'll pull out the key points."
        val kw = LiteAi.keywords(text, 5).joinToString { it.first }
        return "Key points:\n\n" + points.joinToString("\n") { "• $it" } + (if (kw.isNotBlank()) "\n\nKey terms: $kw" else "")
    }

    /** Stream a fixed answer word by word for a consistent feel. */
    private fun stream(answer: String, engine: AiEngine): Flow<Pair<String, AiEngine>> = flow {
        val words = answer.split(" ")
        val sb = StringBuilder()
        for (w in words) {
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(w)
            emit(sb.toString() to engine)
            delay(8)
        }
    }

    private suspend fun retrieveContext(q: String): List<String> {
        val docs = study.quizPool(limit = 300).map { LiteAi.Doc(it.id, it.question, it.answer, "card") } +
            study.topics().map { LiteAi.Doc(-it.id, it.title, it.subject + " " + it.aliases, "topic") }
        return LiteAi.retrieve(q, docs, 4).map { h -> if (h.doc.kind == "card") "${h.doc.title} → ${h.doc.body}" else "Topic: ${h.doc.title} (${h.doc.body.substringBefore(' ')})" }
    }

    private fun liteStream(q: String, context: List<String>): Flow<Pair<String, AiEngine>> = flow {
        val answer = liteAnswer(q, context)
        // Stream word by word for a natural feel.
        val words = answer.split(" ")
        val sb = StringBuilder()
        for (w in words) {
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(w)
            emit(sb.toString() to AiEngine.LITE)
            delay(12)
        }
    }

    private suspend fun liteAnswer(q: String, context: List<String>): String {
        val lower = q.lowercase()
        return when {
            q.length > 350 -> {
                val summary = LiteAi.summarize(q, 4)
                val kw = LiteAi.keywords(q, 6).joinToString { it.first }
                "Here's a quick summary of your text:\n\n" + summary.joinToString("\n") { "• $it" } +
                    "\n\nKey terms: $kw\n\nTip: open Study → Generate cards to turn this into flashcards."
            }
            listOf("study plan", "what should i study", "kya padhu", "schedule").any { it in lower } -> planAnswer()
            listOf("tired", "bored", "motivat", "stress", "can't focus", "cant focus", "distract", "anxious").any { it in lower } ->
                "That's normal – focus is a skill, not a personality trait. Try this:\n\n" +
                    "• Start a 15-minute session on the easiest topic – momentum first.\n" +
                    "• Put your phone's distracting apps on Block in Focus → Rules.\n" +
                    "• After the session, do 5 quick flashcards: active recall beats re-reading.\n" +
                    "• Sleep and water matter more than one extra hour.\n\nYou've got this. 💪"
            context.isNotEmpty() -> "From your own cards and syllabus:\n\n" + context.joinToString("\n") { "• $it" } +
                "\n\nWant more depth? Tap Download AI Tutor on the AI Tutor screen to get Gemma 3 1B for full explanations."
            else -> "I couldn't find this in your cards or syllabus yet.\n\n" +
                "• Add notes in Study → Generate cards and I'll turn them into flashcards.\n" +
                "• Download the full AI Tutor (Gemma 3 1B) on the AI Tutor screen for open-ended explanations – it then runs fully offline.\n\n" +
                "Tip: ask me \"what should I study today?\" for a plan."
        }
    }

    private suspend fun planAnswer(): String {
        val profile = profiles.get()
        val topics = study.topics().map { PlannerTopic(it.id, it.subject, it.title, it.priority, it.mastery, it.minutesStudied, it.lastStudiedDay) }
        val blocks = StudyPlanner.planToday(topics, clock.today(), profile?.examDay, profile?.dailyGoalMinutes ?: 120, study.dueCount(), false)
        if (blocks.isEmpty()) return "Add a few syllabus topics in Study and I'll build your plan."
        return "Here's an advisory plan for today:\n\n" + blocks.joinToString("\n") { "• ${it.minutes} min – ${it.title} (${it.why})" } +
            "\n\nStart the first block from the Focus tab. Adjust freely – it's your plan."
    }
}
