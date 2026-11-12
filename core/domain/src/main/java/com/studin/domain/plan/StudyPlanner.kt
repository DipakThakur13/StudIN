package com.studin.domain.plan

import kotlin.math.max

data class PlannerTopic(
    val id: Long,
    val subject: String,
    val title: String,
    /** 1 = low, 2 = medium, 3 = high. */
    val priority: Int,
    /** 0..1 estimated mastery. */
    val mastery: Double,
    val minutesStudied: Int,
    val lastStudiedDay: Long?,
)

data class PlanBlock(
    val topicId: Long?,
    val title: String,
    val subject: String,
    val minutes: Int,
    val kind: Kind,
    val why: String,
) {
    enum class Kind { LEARN, REVISE, RECALL, RECOVERY }
}

/**
 * Advisory daily planner (roadmap §5). Ranks topics by priority, low mastery,
 * staleness and exam proximity. Produces focus blocks that fit the daily budget,
 * plus a recall block when cards are due, and a gentle recovery plan after misses.
 */
object StudyPlanner {
    fun score(t: PlannerTopic, today: Long, daysToExam: Long?): Double {
        val stale = t.lastStudiedDay?.let { (today - it).coerceIn(0, 30).toDouble() / 30.0 } ?: 1.0
        val urgency = when {
            daysToExam == null -> 1.0
            daysToExam < 30 -> 1.6
            daysToExam < 90 -> 1.3
            else -> 1.0
        }
        return (t.priority * 1.2 + (1 - t.mastery) * 2.0 + stale * 1.5) * urgency
    }

    fun planToday(
        topics: List<PlannerTopic>,
        today: Long,
        examDay: Long?,
        dailyMinutes: Int,
        dueCards: Int,
        missedYesterday: Boolean,
    ): List<PlanBlock> {
        val daysToExam = examDay?.let { it - today }
        val blocks = mutableListOf<PlanBlock>()
        var budget = max(25, dailyMinutes)
        if (missedYesterday) {
            blocks += PlanBlock(null, "Easy restart", "Recovery", 15, PlanBlock.Kind.RECOVERY,
                "Missed a day? Start small – a 15 minute win rebuilds momentum.")
            budget -= 15
        }
        if (dueCards > 0) {
            val m = (dueCards / 2).coerceIn(10, 30)
            blocks += PlanBlock(null, "Review $dueCards due cards", "Flashcards", m, PlanBlock.Kind.RECALL,
                "Spaced repetition: reviewing on time beats re-reading.")
            budget -= m
        }
        val ranked = topics.sortedByDescending { score(it, today, daysToExam) }
        for (t in ranked) {
            if (budget < 20) break
            val minutes = if (budget >= 50 && t.mastery < 0.5) 50 else 25
            val kind = if (t.mastery >= 0.6) PlanBlock.Kind.REVISE else PlanBlock.Kind.LEARN
            val why = buildString {
                append(if (t.priority >= 3) "High weightage" else if (t.priority == 2) "Medium weightage" else "Low weightage")
                append(" · ")
                append(if (t.mastery < 0.3) "new for you" else if (t.mastery < 0.6) "needs practice" else "keep it fresh")
                if (t.lastStudiedDay == null) append(" · not started")
            }
            blocks += PlanBlock(t.id, t.title, t.subject, minutes, kind, why)
            budget -= minutes
        }
        return blocks
    }
}
