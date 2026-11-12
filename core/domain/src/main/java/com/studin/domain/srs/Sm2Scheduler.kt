package com.studin.domain.srs

import com.studin.domain.model.Grade
import kotlin.math.max
import kotlin.math.roundToLong

data class CardSchedule(
    val repetitions: Int = 0,
    val intervalDays: Double = 0.0,
    val easeFactor: Double = 2.5,
    val lapses: Int = 0,
    val dueAt: Long = 0L,
)

/**
 * SM-2 baseline scheduler (roadmap §5) with Anki-style 4-button grading.
 * Deterministic: pure function of (schedule, grade, now).
 */
object Sm2Scheduler {
    const val DAY_MS = 86_400_000L
    private const val MIN_EASE = 1.3
    private const val LEARNING_STEP_MS = 10 * 60_000L

    fun review(s: CardSchedule, grade: Grade, now: Long): CardSchedule {
        val q = when (grade) { Grade.AGAIN -> 1; Grade.HARD -> 3; Grade.GOOD -> 4; Grade.EASY -> 5 }
        var ease = s.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        ease = max(MIN_EASE, ease)
        if (grade == Grade.AGAIN) {
            return CardSchedule(
                repetitions = 0, intervalDays = 0.0, easeFactor = max(MIN_EASE, s.easeFactor - 0.2),
                lapses = s.lapses + 1, dueAt = now + LEARNING_STEP_MS,
            )
        }
        val reps = s.repetitions + 1
        val interval = when {
            reps == 1 -> when (grade) { Grade.HARD -> 0.5; Grade.EASY -> 3.0; else -> 1.0 }
            reps == 2 -> when (grade) { Grade.HARD -> 3.0; Grade.EASY -> 8.0; else -> 6.0 }
            else -> {
                val base = max(1.0, s.intervalDays)
                when (grade) {
                    Grade.HARD -> base * 1.2
                    Grade.EASY -> base * ease * 1.3
                    else -> base * ease
                }
            }
        }.coerceAtMost(365.0)
        return CardSchedule(reps, interval, ease, s.lapses, now + (interval * DAY_MS).roundToLong())
    }

    /** Human friendly preview such as "10m", "1d", "2.3mo". */
    fun formatInterval(s: CardSchedule, grade: Grade, now: Long): String {
        val next = review(s, grade, now)
        val ms = next.dueAt - now
        val minutes = ms / 60_000.0
        return when {
            minutes < 60 -> "${minutes.roundToLong()}m"
            minutes < 60 * 24 -> "${(minutes / 60).roundToLong()}h"
            minutes < 60 * 24 * 30 -> "${(minutes / 1440).roundToLong()}d"
            else -> String.format(java.util.Locale.US, "%.1fmo", minutes / 43_200)
        }
    }
}
