package com.studin.domain.stats

/** Streak helpers. Streaks are opt-in and forgiving (one "freeze" day per week). */
object StreakCalculator {
    /** @param activeDays epoch days with >= goal minutes; @return current streak length. */
    fun currentStreak(activeDays: Set<Long>, today: Long, allowFreeze: Boolean = true): Int {
        var day = if (today in activeDays) today else today - 1
        var streak = 0
        var freezesLeft = if (allowFreeze) 1 else 0
        var sinceFreeze = 0
        while (true) {
            if (day in activeDays) {
                streak++; sinceFreeze++
                if (sinceFreeze >= 7 && allowFreeze) { freezesLeft = 1; sinceFreeze = 0 }
            } else if (freezesLeft > 0 && streak > 0 && (day - 1) in activeDays) {
                freezesLeft--
            } else break
            day--
            if (streak > 3650) break
        }
        return streak
    }

    fun bestStreak(activeDays: Set<Long>): Int {
        if (activeDays.isEmpty()) return 0
        val sorted = activeDays.sorted()
        var best = 1; var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
            best = maxOf(best, run)
        }
        return best
    }
}
