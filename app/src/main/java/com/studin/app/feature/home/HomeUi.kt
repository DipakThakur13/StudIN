package com.studin.app.feature.home

import com.studin.domain.plan.PlanBlock

/** Pure presentation helpers for Home (unit-tested in HomeUiTest). */
object HomeUi {

    fun greetingFor(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Studying late"
    }

    /** Up to two initials for the profile button; "S" (student) when no name is set. */
    fun initialsOf(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "S"
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
    }

    /**
     * The block shown in "Continue studying": the first syllabus block (learn/revise) in plan
     * order; falls back to the first block of any kind (e.g. a recall or recovery block).
     */
    fun pickContinue(plan: List<PlanBlock>): PlanBlock? =
        plan.firstOrNull { it.topicId != null && (it.kind == PlanBlock.Kind.LEARN || it.kind == PlanBlock.Kind.REVISE) }
            ?: plan.firstOrNull()

    /** Plan list without the featured block, so nothing is shown twice. */
    fun remainingPlan(plan: List<PlanBlock>, featured: PlanBlock?): List<PlanBlock> {
        if (featured == null) return plan
        val idx = plan.indexOf(featured)
        return if (idx < 0) plan else plan.filterIndexed { i, _ -> i != idx }
    }

    fun kindLabel(kind: PlanBlock.Kind): String = when (kind) {
        PlanBlock.Kind.LEARN -> "Learn"
        PlanBlock.Kind.REVISE -> "Revise"
        PlanBlock.Kind.RECALL -> "Flashcards"
        PlanBlock.Kind.RECOVERY -> "Warm-up"
    }

    /** "12:05" or "1:02:05" for a remaining duration (rounded up to whole seconds). */
    fun formatClock(ms: Long): String {
        val total = (ms.coerceAtLeast(0) + 999) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    /** "25 min" / "2 h" / "1 h 40 min". */
    fun formatMinutes(min: Int): String = when {
        min < 60 -> "$min min"
        min % 60 == 0 -> "${min / 60} h"
        else -> "${min / 60} h ${min % 60} min"
    }

    /** Number of plan rows shown before "View all". */
    const val PLAN_PREVIEW = 3
}
