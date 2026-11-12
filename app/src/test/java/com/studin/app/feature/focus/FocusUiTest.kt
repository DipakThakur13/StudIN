package com.studin.app.feature.focus

import com.studin.app.platform.gate.breakActionLabel
import com.studin.domain.focus.FocusEvent
import com.studin.domain.focus.FocusSnapshot
import com.studin.domain.focus.FocusStateMachine
import com.studin.domain.model.FocusState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusUiTest {
    private fun active(plannedMin: Int, startAt: Long): FocusSnapshot {
        val p = FocusStateMachine.reduce(FocusSnapshot(), FocusEvent.Prepare(1, plannedMin * 60_000L, 0, null, "Topic"))
        return FocusStateMachine.reduce(p, FocusEvent.Start(startAt))
    }

    @Test fun timerFormatting() {
        assertEquals("25:00", FocusUi.formatTimer(25 * 60_000L))
        assertEquals("24:59", FocusUi.formatTimer(25 * 60_000L - 1_000))
        assertEquals("00:01", FocusUi.formatTimer(400)) // rounds up: never 00:00 while time is left
        assertEquals("00:00", FocusUi.formatTimer(0))
        assertEquals("2:00:00", FocusUi.formatTimer(120 * 60_000L))
    }

    @Test fun presetsAndCustom() {
        FocusUi.PRESETS.forEach { assertFalse(FocusUi.isCustom(it)) }
        assertTrue(FocusUi.isCustom(35))
        assertEquals(listOf(15, 25, 50, 90), FocusUi.PRESETS)
    }

    @Test fun sliderSnapsToFiveMinuteStepsWithinRange() {
        assertEquals(5, FocusUi.snapMinutes(0f))
        assertEquals(5, FocusUi.snapMinutes(6f))
        assertEquals(10, FocusUi.snapMinutes(8f))
        assertEquals(45, FocusUi.snapMinutes(44.9f))
        assertEquals(180, FocusUi.snapMinutes(500f))
    }

    @Test fun idleShowsPlannedDurationAndFullRing() {
        val idle = FocusSnapshot()
        assertEquals(50 * 60_000L, FocusUi.displayRemainingMs(idle, now = 999, setupMinutes = 50))
        assertEquals(1f, FocusUi.ringProgress(idle, 0), 0f)
    }

    @Test fun ringDrainsWithRemainingTime() {
        val s = active(20, startAt = 1_000)
        assertEquals(1f, FocusUi.ringProgress(s, 1_000), 0.0001f)
        assertEquals(0.5f, FocusUi.ringProgress(s, 1_000 + 10 * 60_000L), 0.0001f)
        assertEquals(10 * 60_000L, FocusUi.displayRemainingMs(s, 1_000 + 10 * 60_000L, setupMinutes = 99))
        val paused = FocusStateMachine.reduce(s, FocusEvent.Pause(1_000 + 5 * 60_000L))
        // Time stands still while paused.
        assertEquals(FocusUi.ringProgress(paused, 1_000 + 5 * 60_000L), FocusUi.ringProgress(paused, 1_000 + 50 * 60_000L), 0f)
    }

    @Test fun stateLabelsAreDistinct() {
        val labels = FocusState.entries.map { FocusUi.stateLabel(it) }
        assertEquals(labels.size, labels.toSet().size)
        assertEquals("Paused", FocusUi.stateLabel(FocusState.PAUSED))
    }

    @Test fun masteryPercentIsClamped() {
        assertEquals(0, FocusUi.masteryPercent(-1.0))
        assertEquals(33, FocusUi.masteryPercent(0.333))
        assertEquals(100, FocusUi.masteryPercent(2.0))
    }

    @Test fun gateBreakLabel() {
        assertEquals("Answer 1 question for a 5-min break", breakActionLabel(1, 5))
        assertEquals("Answer 3 questions for a 10-min break", breakActionLabel(3, 10))
        assertEquals("Take a short pause for a 5-min break", breakActionLabel(0, 5))
    }
}
