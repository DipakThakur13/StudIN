package com.studin.app.feature.insights

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Time filters supported by Insights. Day counts include today. */
enum class InsightsPeriod(val label: String, val days: Int, val description: String) {
    WEEK("7D", 7, "last 7 days"),
    MONTH("30D", 30, "last 30 days"),
    QUARTER("12W", 84, "last 12 weeks"),
}

/** One stored focus session reduced to what Insights needs. */
data class SessionStat(
    val day: Long,
    val focusedMinutes: Int,
    val completed: Boolean,
    val aborted: Boolean,
    val blockedAttempts: Int,
)

data class PeriodSummary(
    val focusMinutes: Int,
    val previousFocusMinutes: Int,
    val sessions: Int,
    val completedSessions: Int,
    /** Completed / (completed + ended early). Null when nothing finished. */
    val completionRate: Float?,
    val blockedAttempts: Int,
    val activeDays: Int,
    val daysInPeriod: Int,
    val goalMetDays: Int,
) {
    val consistency: Float get() = if (daysInPeriod == 0) 0f else activeDays.toFloat() / daysInPeriod
}

/** A chart bar covering [startDay]..[endDay] (inclusive epoch days). */
data class ChartPoint(
    val label: String,
    val minutes: Int,
    val startDay: Long,
    val endDay: Long,
    /** Full spoken label, e.g. "Tuesday 12 November" or "12 to 18 November". */
    val accessibilityLabel: String,
    /** Short visible detail, e.g. "Tue 12 Nov" or "12–18 Nov". */
    val detailLabel: String,
    val isCurrent: Boolean,
)

data class ChartData(val points: List<ChartPoint>, val goalLine: Int?, val bucketDays: Int, val caption: String)

/** Heatmap cell; [minutes] is null for days in the future (rendered empty). */
data class HeatCell(val day: Long, val minutes: Int?, val level: Int)

object InsightsMath {

    /** A day counts as "active" when at least this many focus minutes were recorded. */
    const val ACTIVE_DAY_MINUTES = 1

    fun minutesByDay(sessions: List<SessionStat>): Map<Long, Int> =
        sessions.groupBy { it.day }.mapValues { (_, v) -> v.sumOf { it.focusedMinutes } }

    fun range(period: InsightsPeriod, today: Long): LongRange = (today - period.days + 1)..today
    private fun previousRange(period: InsightsPeriod, today: Long): LongRange = (today - 2 * period.days + 1)..(today - period.days)

    fun summarize(sessions: List<SessionStat>, today: Long, period: InsightsPeriod, dailyGoal: Int): PeriodSummary {
        val r = range(period, today)
        val inPeriod = sessions.filter { it.day in r }
        val prev = previousRange(period, today)
        val byDay = minutesByDay(inPeriod)
        val completed = inPeriod.count { it.completed }
        val finished = inPeriod.count { it.completed || it.aborted }
        return PeriodSummary(
            focusMinutes = inPeriod.sumOf { it.focusedMinutes },
            previousFocusMinutes = sessions.filter { it.day in prev }.sumOf { it.focusedMinutes },
            sessions = inPeriod.size,
            completedSessions = completed,
            completionRate = if (finished == 0) null else completed.toFloat() / finished,
            blockedAttempts = inPeriod.sumOf { it.blockedAttempts },
            activeDays = byDay.count { it.value >= ACTIVE_DAY_MINUTES },
            daysInPeriod = period.days,
            goalMetDays = if (dailyGoal <= 0) 0 else byDay.count { it.value >= dailyGoal },
        )
    }

    /**
     * Bars for the selected period:
     *  - 7D: one bar per day (weekday labels), goal line = daily goal.
     *  - 30D: five 6-day blocks (exactly covers 30 days), goal line = 6 × daily goal.
     *  - 12W: twelve 7-day blocks ending today, goal line = 7 × daily goal.
     */
    fun chart(byDay: Map<Long, Int>, today: Long, period: InsightsPeriod, dailyGoal: Int, locale: Locale = Locale.getDefault()): ChartData {
        val bucket = when (period) { InsightsPeriod.WEEK -> 1; InsightsPeriod.MONTH -> 6; InsightsPeriod.QUARTER -> 7 }
        val count = period.days / bucket
        val points = (count - 1 downTo 0).map { back ->
            val end = today - back * bucket
            val start = end - bucket + 1
            val minutes = (start..end).sumOf { byDay[it] ?: 0 }
            val s = LocalDate.ofEpochDay(start); val e = LocalDate.ofEpochDay(end)
            if (bucket == 1) {
                ChartPoint(
                    label = s.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    minutes = minutes, startDay = start, endDay = end,
                    accessibilityLabel = "${s.dayOfWeek.getDisplayName(TextStyle.FULL, locale)} ${s.dayOfMonth} ${s.month.getDisplayName(TextStyle.FULL, locale)}",
                    detailLabel = "${s.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)} ${s.dayOfMonth} ${s.month.getDisplayName(TextStyle.SHORT, locale)}",
                    isCurrent = back == 0,
                )
            } else {
                ChartPoint(
                    label = "${s.dayOfMonth} ${s.month.getDisplayName(TextStyle.SHORT, locale)}",
                    minutes = minutes, startDay = start, endDay = end,
                    accessibilityLabel = "${s.dayOfMonth} ${s.month.getDisplayName(TextStyle.FULL, locale)} to ${e.dayOfMonth} ${e.month.getDisplayName(TextStyle.FULL, locale)}",
                    detailLabel = shortRange(s, e, locale),
                    isCurrent = back == 0,
                )
            }
        }
        val caption = when (period) {
            InsightsPeriod.WEEK -> "Minutes per day · dashed line = daily goal"
            InsightsPeriod.MONTH -> "Minutes per 6-day block · dashed line = goal for 6 days"
            InsightsPeriod.QUARTER -> "Minutes per week · dashed line = weekly goal"
        }
        return ChartData(points, if (dailyGoal > 0) dailyGoal * bucket else null, bucket, caption)
    }

    fun shortRange(s: LocalDate, e: LocalDate, locale: Locale = Locale.getDefault()): String =
        if (s.month == e.month) "${s.dayOfMonth}–${e.dayOfMonth} ${e.month.getDisplayName(TextStyle.SHORT, locale)}"
        else "${s.dayOfMonth} ${s.month.getDisplayName(TextStyle.SHORT, locale)} – ${e.dayOfMonth} ${e.month.getDisplayName(TextStyle.SHORT, locale)}"

    /** Intensity 0–4 relative to the daily goal (0 = nothing, 4 = goal met). */
    fun level(minutes: Int, dailyGoal: Int): Int {
        if (minutes <= 0) return 0
        val goal = dailyGoal.coerceAtLeast(1)
        return when {
            minutes >= goal -> 4
            minutes * 2 >= goal -> 3
            minutes * 4 >= goal -> 2
            else -> 1
        }
    }

    /**
     * 12 Monday-first week columns; the last column is the current week. Each inner list is one
     * week, Monday..Sunday. Future days have null minutes.
     */
    fun heatmap(byDay: Map<Long, Int>, today: Long, dailyGoal: Int, weeks: Int = 12): List<List<HeatCell>> {
        val todayDate = LocalDate.ofEpochDay(today)
        val thisMonday = todayDate.with(DayOfWeek.MONDAY).toEpochDay()
        val firstMonday = thisMonday - (weeks - 1) * 7L
        return (0 until weeks).map { w ->
            (0 until 7).map { d ->
                val day = firstMonday + w * 7L + d
                if (day > today) HeatCell(day, null, 0)
                else { val m = byDay[day] ?: 0; HeatCell(day, m, level(m, dailyGoal)) }
            }
        }
    }

    /**
     * Up to three short observations derived only from stored data. Returns an empty list when
     * there isn't enough data to say anything honest.
     */
    fun observations(sessions: List<SessionStat>, today: Long, period: InsightsPeriod, dailyGoal: Int, locale: Locale = Locale.getDefault()): List<String> {
        if (sessions.size < 2) return emptyList()
        val out = mutableListOf<String>()
        val summary = summarize(sessions, today, period, dailyGoal)

        // 1. Trend vs the previous period of equal length.
        val cur = summary.focusMinutes; val prev = summary.previousFocusMinutes
        when {
            prev > 0 && cur > 0 -> {
                val pct = ((cur - prev) * 100f / prev).roundToInt()
                out += when {
                    pct >= 5 -> "Focus time is up $pct% compared with the previous ${period.days} days."
                    pct <= -5 -> "Focus time is down ${-pct}% compared with the previous ${period.days} days."
                    else -> "Focus time is steady compared with the previous ${period.days} days."
                }
            }
            prev > 0 && cur == 0 -> out += "No focus sessions in the ${period.description} – the previous ${period.days} days had ${formatMinutes(prev)}."
        }

        // 2. Most consistent weekday over the last 12 weeks.
        val window = (today - 83)..today
        val activeDays = minutesByDay(sessions.filter { it.day in window }).filter { it.value >= ACTIVE_DAY_MINUTES }.keys
        if (activeDays.size >= 4) {
            val byWeekday = activeDays.groupingBy { LocalDate.ofEpochDay(it).dayOfWeek }.eachCount()
            val best = byWeekday.maxByOrNull { it.value }
            if (best != null && best.value >= 2 && byWeekday.values.count { it == best.value } == 1) {
                val weeksSeen = ((today - (activeDays.minOrNull() ?: today)) / 7 + 1).coerceIn(1, 12)
                out += "${best.key.getDisplayName(TextStyle.FULL, locale)} is your most consistent study day (${best.value} of the last $weeksSeen weeks)."
            }
        }

        // 3. Progress toward the daily goal in this period.
        if (dailyGoal > 0 && summary.sessions > 0) {
            out += "You met your ${formatMinutes(dailyGoal)} daily goal on ${summary.goalMetDays} of ${summary.daysInPeriod} days."
        }
        return out.take(3)
    }

    fun formatMinutes(min: Int): String = when {
        min < 60 -> "$min min"
        min % 60 == 0 -> "${min / 60} h"
        else -> "${min / 60} h ${min % 60} min"
    }

    /** Very short duration for tight labels: "45m", "1h 30m", "2h". */
    fun formatCompactMinutes(min: Int): String = when {
        min < 60 -> "${min}m"
        min % 60 == 0 -> "${min / 60}h"
        else -> "${min / 60}h ${min % 60}m"
    }

    /** Compact hours value for stat cards: "4.4 h", "45 min". */
    fun formatHours(min: Int): String = if (min < 60) "$min min" else String.format(Locale.US, "%.1f h", min / 60f)
}
