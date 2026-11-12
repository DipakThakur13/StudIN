package com.studin.app.feature.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

class InsightsMathTest {
    /** Thursday 12 Nov 2026. */
    private val today = LocalDate.of(2026, 11, 12).toEpochDay()

    private fun s(daysAgo: Int, min: Int, completed: Boolean = true, aborted: Boolean = false, blocked: Int = 0) =
        SessionStat(today - daysAgo, min, completed, aborted, blocked)

    @Test fun summaryOnlyCountsSessionsInsidePeriod() {
        val sessions = listOf(s(0, 25), s(6, 50, blocked = 2), s(7, 90), s(13, 30), s(20, 60))
        val week = InsightsMath.summarize(sessions, today, InsightsPeriod.WEEK, dailyGoal = 50)
        assertEquals(75, week.focusMinutes)
        assertEquals(120, week.previousFocusMinutes)
        assertEquals(2, week.sessions)
        assertEquals(2, week.blockedAttempts)
        assertEquals(2, week.activeDays)
        assertEquals(7, week.daysInPeriod)
        assertEquals(1, week.goalMetDays)
        assertEquals(2f / 7f, week.consistency, 1e-6f)
    }

    @Test fun completionRateIgnoresRunningSessionsAndNullWhenNothingFinished() {
        val running = SessionStat(today, 10, completed = false, aborted = false, blockedAttempts = 0)
        assertNull(InsightsMath.summarize(listOf(running), today, InsightsPeriod.WEEK, 60).completionRate)
        val mixed = listOf(s(0, 25), s(1, 25), s(2, 10, completed = false, aborted = true), running)
        assertEquals(2f / 3f, InsightsMath.summarize(mixed, today, InsightsPeriod.WEEK, 60).completionRate!!, 1e-6f)
    }

    @Test fun weekChartHasSevenDailyBarsEndingToday() {
        val byDay = mapOf(today to 30, today - 1 to 45, today - 7 to 999)
        val c = InsightsMath.chart(byDay, today, InsightsPeriod.WEEK, dailyGoal = 120, locale = Locale.UK)
        assertEquals(7, c.points.size)
        assertEquals(listOf("Fri", "Sat", "Sun", "Mon", "Tue", "Wed", "Thu"), c.points.map { it.label })
        assertEquals(30, c.points.last().minutes)
        assertEquals(45, c.points[5].minutes)
        assertTrue(c.points.last().isCurrent)
        assertEquals(120, c.goalLine)
        assertEquals("Thu 12 Nov", c.points.last().detailLabel)
        assertEquals("Thursday 12 November", c.points.last().accessibilityLabel)
    }

    @Test fun monthChartUsesFiveSixDayBlocksCoveringExactly30Days() {
        val byDay = (0 until 40).associate { (today - it) to 10 }
        val c = InsightsMath.chart(byDay, today, InsightsPeriod.MONTH, dailyGoal = 60, locale = Locale.UK)
        assertEquals(5, c.points.size)
        assertTrue(c.points.all { it.minutes == 60 })
        assertEquals(today - 29, c.points.first().startDay)
        assertEquals(today, c.points.last().endDay)
        assertEquals(360, c.goalLine)
        assertEquals(6, c.bucketDays)
        // Sum of bars == focus minutes in the period (no double counting, nothing dropped).
        assertEquals(300, c.points.sumOf { it.minutes })
    }

    @Test fun quarterChartHasTwelveWeeklyTotals() {
        val byDay = (0 until 84).associate { (today - it) to 5 }
        val c = InsightsMath.chart(byDay, today, InsightsPeriod.QUARTER, dailyGoal = 30, locale = Locale.UK)
        assertEquals(12, c.points.size)
        assertTrue(c.points.all { it.minutes == 35 })
        assertEquals(210, c.goalLine)
        assertEquals("6–12 Nov", c.points.last().detailLabel)
    }

    @Test fun zeroGoalMeansNoGoalLine() {
        assertNull(InsightsMath.chart(emptyMap(), today, InsightsPeriod.WEEK, 0).goalLine)
    }

    @Test fun heatmapIsMondayFirstAndHidesFutureDays() {
        val byDay = mapOf(today to 120, today - 1 to 30)
        val grid = InsightsMath.heatmap(byDay, today, dailyGoal = 120)
        assertEquals(12, grid.size)
        assertTrue(grid.all { it.size == 7 })
        grid.forEach { week -> assertEquals(DayOfWeek.MONDAY, LocalDate.ofEpochDay(week.first().day).dayOfWeek) }
        val last = grid.last()
        val todayCell = last.first { it.day == today }
        assertEquals(120, todayCell.minutes); assertEquals(4, todayCell.level)
        assertEquals(2, last.first { it.day == today - 1 }.level) // 30 of 120 = 25% of goal → level 2
        assertTrue(last.filter { it.day > today }.all { it.minutes == null })
        // Consecutive days, no gaps.
        val days = grid.flatten().map { it.day }
        assertEquals((days.first()..days.last()).toList(), days)
    }

    @Test fun levelsAreRelativeToGoal() {
        assertEquals(0, InsightsMath.level(0, 100))
        assertEquals(1, InsightsMath.level(10, 100))
        assertEquals(2, InsightsMath.level(25, 100))
        assertEquals(3, InsightsMath.level(50, 100))
        assertEquals(4, InsightsMath.level(100, 100))
        assertEquals(4, InsightsMath.level(5, 0)) // degenerate goal never divides by zero
    }

    @Test fun observationsNeedRealData() {
        assertTrue(InsightsMath.observations(emptyList(), today, InsightsPeriod.WEEK, 60).isEmpty())
        assertTrue(InsightsMath.observations(listOf(s(0, 30)), today, InsightsPeriod.WEEK, 60).isEmpty())
    }

    @Test fun observationsReportTrendWeekdayAndGoal() {
        // Every Monday for 6 weeks + a couple of other days.
        val mondays = (0 until 6).map { w -> val d = LocalDate.ofEpochDay(today).with(DayOfWeek.MONDAY).toEpochDay() - 7 * w; SessionStat(d, 60, true, false, 0) }
        val sessions = mondays + s(0, 120) + s(8, 20)
        val obs = InsightsMath.observations(sessions, today, InsightsPeriod.WEEK, dailyGoal = 60, locale = Locale.UK)
        assertTrue(obs.toString(), obs.any { it.startsWith("Focus time is up") })
        assertTrue(obs.toString(), obs.any { it.startsWith("Monday is your most consistent study day") })
        assertTrue(obs.toString(), obs.any { it == "You met your 1 h daily goal on 2 of 7 days." })
        assertTrue(obs.size <= 3)
    }

    @Test fun observationReportsDropToZero() {
        val obs = InsightsMath.observations(listOf(s(8, 40), s(9, 20)), today, InsightsPeriod.WEEK, 60)
        assertTrue(obs.toString(), obs.first().startsWith("No focus sessions in the last 7 days"))
    }

    @Test fun durationFormatting() {
        assertEquals("45 min", InsightsMath.formatMinutes(45))
        assertEquals("2 h", InsightsMath.formatMinutes(120))
        assertEquals("1 h 30 min", InsightsMath.formatMinutes(90))
        assertEquals("1h 30m", InsightsMath.formatCompactMinutes(90))
        assertEquals("4.4 h", InsightsMath.formatHours(265))
        assertEquals("50 min", InsightsMath.formatHours(50))
    }
}
