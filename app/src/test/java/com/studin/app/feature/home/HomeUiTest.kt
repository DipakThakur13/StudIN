package com.studin.app.feature.home

import com.studin.domain.plan.PlanBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeUiTest {
    private fun block(title: String, kind: PlanBlock.Kind, topicId: Long? = null, minutes: Int = 25) =
        PlanBlock(topicId, title, "Physics", minutes, kind, "why")

    @Test fun greetingCoversTheWholeDay() {
        assertEquals("Good morning", HomeUi.greetingFor(5))
        assertEquals("Good morning", HomeUi.greetingFor(11))
        assertEquals("Good afternoon", HomeUi.greetingFor(12))
        assertEquals("Good evening", HomeUi.greetingFor(21))
        assertEquals("Studying late", HomeUi.greetingFor(23))
        assertEquals("Studying late", HomeUi.greetingFor(2))
    }

    @Test fun initials() {
        assertEquals("AS", HomeUi.initialsOf("Aarav Sharma"))
        assertEquals("AK", HomeUi.initialsOf("  aarav  kumar  singh khan "))
        assertEquals("A", HomeUi.initialsOf("aarav"))
        assertEquals("S", HomeUi.initialsOf(""))
        assertEquals("S", HomeUi.initialsOf("   "))
        assertEquals("अ", HomeUi.initialsOf("अर्जुन"))
    }

    @Test fun continuePrefersTheFirstSyllabusBlock() {
        val plan = listOf(
            block("Easy restart", PlanBlock.Kind.RECOVERY),
            block("Review 12 due cards", PlanBlock.Kind.RECALL),
            block("Kinematics", PlanBlock.Kind.LEARN, topicId = 7),
            block("Optics", PlanBlock.Kind.REVISE, topicId = 9),
        )
        assertEquals("Kinematics", HomeUi.pickContinue(plan)?.title)
        assertEquals(listOf("Easy restart", "Review 12 due cards", "Optics"), HomeUi.remainingPlan(plan, HomeUi.pickContinue(plan)).map { it.title })
    }

    @Test fun continueFallsBackToFirstBlockAndHandlesEmpty() {
        val recallOnly = listOf(block("Review 3 due cards", PlanBlock.Kind.RECALL))
        assertEquals("Review 3 due cards", HomeUi.pickContinue(recallOnly)?.title)
        assertEquals(emptyList<PlanBlock>(), HomeUi.remainingPlan(recallOnly, HomeUi.pickContinue(recallOnly)))
        assertNull(HomeUi.pickContinue(emptyList()))
        assertEquals(emptyList<PlanBlock>(), HomeUi.remainingPlan(emptyList(), null))
    }

    @Test fun remainingPlanRemovesOnlyOneOccurrence() {
        val a = block("Same", PlanBlock.Kind.LEARN, topicId = 1)
        assertEquals(1, HomeUi.remainingPlan(listOf(a, a), a).size)
    }

    @Test fun clockFormatting() {
        assertEquals("25:00", HomeUi.formatClock(25 * 60_000L))
        assertEquals("00:01", HomeUi.formatClock(1))
        assertEquals("00:00", HomeUi.formatClock(0))
        assertEquals("00:00", HomeUi.formatClock(-5_000))
        assertEquals("1:30:00", HomeUi.formatClock(90 * 60_000L))
    }

    @Test fun minutesFormatting() {
        assertEquals("0 min", HomeUi.formatMinutes(0))
        assertEquals("45 min", HomeUi.formatMinutes(45))
        assertEquals("4 h", HomeUi.formatMinutes(240))
        assertEquals("1 h 40 min", HomeUi.formatMinutes(100))
    }

    @Test fun kindLabels() {
        assertEquals("Flashcards", HomeUi.kindLabel(PlanBlock.Kind.RECALL))
        assertEquals("Warm-up", HomeUi.kindLabel(PlanBlock.Kind.RECOVERY))
    }
}
