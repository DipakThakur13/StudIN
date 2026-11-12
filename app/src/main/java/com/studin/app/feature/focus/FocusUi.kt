package com.studin.app.feature.focus

import com.studin.domain.focus.FocusSnapshot
import com.studin.domain.model.FocusState

/** Pure presentation helpers for the Focus screen (unit-tested in FocusUiTest). */
object FocusUi {
    val PRESETS = listOf(15, 25, 50, 90)
    const val MIN_MINUTES = 5
    const val MAX_MINUTES = 180

    /** True when [minutes] is not one of the presets (the "Custom" option is selected). */
    fun isCustom(minutes: Int) = minutes !in PRESETS

    /** Snap a slider value to 5-minute steps inside the allowed range. */
    fun snapMinutes(raw: Float): Int = (Math.round(raw / 5f) * 5).coerceIn(MIN_MINUTES, MAX_MINUTES)

    fun stateLabel(state: FocusState): String = when (state) {
        FocusState.IDLE -> "Ready when you are"
        FocusState.PREPARING -> "Getting ready"
        FocusState.ACTIVE -> "Focusing"
        FocusState.PAUSED -> "Paused"
        FocusState.COMPLETED -> "Session complete"
        FocusState.ABORTED -> "Session ended"
    }

    /** "mm:ss" (or "h:mm:ss"), rounding up so the display never shows 00:00 while time is left. */
    fun formatTimer(ms: Long): String {
        val total = (ms.coerceAtLeast(0) + 999) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    /** Remaining time shown on the ring: the planned duration while idle. */
    fun displayRemainingMs(snap: FocusSnapshot, now: Long, setupMinutes: Int): Long = when (snap.state) {
        FocusState.IDLE -> setupMinutes * 60_000L
        else -> snap.remainingMs(now)
    }

    /**
     * Ring fill = fraction of time remaining (full when ready, drains while focusing),
     * so the ring and the countdown always tell the same story.
     */
    fun ringProgress(snap: FocusSnapshot, now: Long): Float = when (snap.state) {
        FocusState.IDLE, FocusState.PREPARING -> 1f
        FocusState.COMPLETED -> 0f
        else -> if (snap.plannedMs <= 0) 0f else (snap.remainingMs(now).toFloat() / snap.plannedMs).coerceIn(0f, 1f)
    }

    fun startLabel(minutes: Int) = "Start $minutes-minute focus"

    /** Mastery shown after a session, as a whole percentage. */
    fun masteryPercent(mastery: Double): Int = (mastery.coerceIn(0.0, 1.0) * 100).toInt()
}
