package com.studin.app.ai

import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.AppClock
import com.studin.app.core.data.repo.ProfileRepository
import com.studin.app.core.data.repo.StudyRepository
import com.studin.domain.ai.LiteAi
import com.studin.domain.classifier.ClassificationResult
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

data class ContentCheck(val result: ClassificationResult, val engine: AiEngine, val latencyMs: Long)

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
            val out = runCatching { llm.generate(prompt, temperature = 0.3f).lastOrNull() }.getOrNull()
            val cards = out?.let { LiteAi.parseQaBlocks(it, max) }.orEmpty()
            if (cards.size >= 2) return CardGenResult(cards, AiEngine.LLM, keywords)
        }
        return CardGenResult(LiteAi.notesToCards(clean, max), AiEngine.LITE, keywords)
    }

    // ------------------------------------------------------------- Tutor chat
    /** Streams the accumulated reply text. */
    fun chat(question: String, history: List<ChatTurn>): Flow<Pair<String, AiEngine>> = flow {
        val q = question.trim().take(2_000)
        val context = retrieveContext(q)
        if (llmEnabled()) {
            val profile = profiles.get()
            val hist = history.takeLast(4).joinToString("\n") { (if (it.fromUser) "Student: " else "Tutor: ") + it.text.take(300) }
            val prompt = buildString {
                append("You are StudIN, a friendly, concise study tutor for an Indian ")
                append(profile?.exam?.displayName ?: "competitive exam").append(" student. ")
                append("Explain step by step, use simple English (Hinglish is fine if the student uses it), keep answers under 180 words. ")
                append("If unsure, say so. Never invent facts.\n")
                if (context.isNotEmpty()) append("Student's own notes:\n").append(context.joinToString("\n") { "- " + it }).append('\n')
                if (hist.isNotEmpty()) append(hist).append('\n')
                append("Student: ").append(q).append("\nTutor:")
            }
            var produced = false
            emitAll(flow {
                llm.generate(prompt).collect { produced = true; emit(it.trim() to AiEngine.LLM) }
            }.catch { if (!produced) emitAll(liteStream(q, context)) })
        } else emitAll(liteStream(q, context))
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
            listOf("plan", "what should i study", "kya padhu", "schedule", "today").any { it in lower } -> planAnswer()
            listOf("tired", "bored", "motivat", "stress", "can't focus", "cant focus", "distract", "anxious").any { it in lower } ->
                "That's normal – focus is a skill, not a personality trait. Try this:\n\n" +
                    "• Start a 15-minute session on the easiest topic – momentum first.\n" +
                    "• Put your phone's distracting apps on Block in Focus → Rules.\n" +
                    "• After the session, do 5 quick flashcards: active recall beats re-reading.\n" +
                    "• Sleep and water matter more than one extra hour.\n\nYou've got this. 💪"
            context.isNotEmpty() -> "From your own cards and syllabus:\n\n" + context.joinToString("\n") { "• $it" } +
                "\n\nWant more depth? Import an on-device model in AI Tutor → Model for full explanations."
            else -> "I couldn't find this in your cards or syllabus yet.\n\n" +
                "• Add notes in Study → Generate cards and I'll turn them into flashcards.\n" +
                "• Import an on-device model (AI Tutor → Model) for open-ended explanations – it runs fully offline.\n\n" +
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

    // ------------------------------------------------------------- Content check
    /** Tier 0/1 classifier with optional Tier-2 LLM for UNKNOWN titles. */
    suspend fun checkContent(title: String, allowLlm: Boolean = true): ContentCheck {
        val start = System.nanoTime()
        val r = classifiers.classifier.classify(title)
        if (r.verdict != Verdict.UNKNOWN || !allowLlm || !llmEnabled()) {
            return ContentCheck(r, AiEngine.LITE, (System.nanoTime() - start) / 1_000_000)
        }
        val prompt = "Classify this video title for a student during a study session. Reply with one word, STUDY or DISTRACTION.\nTitle: \"${title.take(200)}\"\nAnswer:"
        val out = runCatching { llm.generate(prompt, temperature = 0f).lastOrNull() }.getOrNull()?.uppercase().orEmpty()
        val upgraded = when {
            "DISTRACTION" in out -> r.copy(verdict = Verdict.WARN, confidence = 0.7, tier = 2, reason = "On-device model: likely distraction (advisory)")
            "STUDY" in out -> r.copy(verdict = Verdict.ALLOW, confidence = 0.7, tier = 2, reason = "On-device model: likely study content")
            else -> r
        }
        return ContentCheck(upgraded, if (upgraded.tier == 2) AiEngine.LLM else AiEngine.LITE, (System.nanoTime() - start) / 1_000_000)
    }
}
