package com.studin.domain.focus

import com.studin.domain.model.FocusState

/**
 * Immutable snapshot of a focus session. All durations are measured against a
 * monotonic clock (SystemClock.elapsedRealtime on Android) so wall-clock changes
 * cannot corrupt a session.
 */
data class FocusSnapshot(
    val state: FocusState = FocusState.IDLE,
    val sessionId: Long = 0L,
    val plannedMs: Long = 0L,
    /** Focused time accumulated before the current ACTIVE stretch. */
    val accumulatedMs: Long = 0L,
    /** Monotonic anchor of the current ACTIVE stretch, or -1 when not active. */
    val resumedAtElapsed: Long = -1L,
    val startWallTime: Long = 0L,
    val pauseCount: Int = 0,
    val topicId: Long? = null,
    val label: String = "",
    val endReason: String? = null,
) {
    fun focusedMs(nowElapsed: Long): Long =
        if (state == FocusState.ACTIVE && resumedAtElapsed >= 0)
            accumulatedMs + (nowElapsed - resumedAtElapsed).coerceAtLeast(0)
        else accumulatedMs

    fun remainingMs(nowElapsed: Long): Long = (plannedMs - focusedMs(nowElapsed)).coerceAtLeast(0)

    fun progress(nowElapsed: Long): Float =
        if (plannedMs <= 0) 0f else (focusedMs(nowElapsed).toFloat() / plannedMs).coerceIn(0f, 1f)
}

sealed interface FocusEvent {
    data class Prepare(val sessionId: Long, val plannedMs: Long, val wallTime: Long, val topicId: Long?, val label: String) : FocusEvent
    data class Start(val nowElapsed: Long) : FocusEvent
    data class Pause(val nowElapsed: Long) : FocusEvent
    data class Resume(val nowElapsed: Long) : FocusEvent
    data class Tick(val nowElapsed: Long) : FocusEvent
    data class Abort(val nowElapsed: Long, val reason: String) : FocusEvent
    data class Finish(val nowElapsed: Long) : FocusEvent
    data object Reset : FocusEvent
}

class IllegalFocusTransition(from: FocusState, event: FocusEvent) :
    IllegalStateException("Cannot apply ${event::class.simpleName} in state $from")

/** Pure reducer – no Android dependencies, fully unit-testable. */
object FocusStateMachine {

    const val MAX_PAUSES = 3

    fun reduce(s: FocusSnapshot, e: FocusEvent): FocusSnapshot = when (e) {
        is FocusEvent.Prepare -> {
            require(e.plannedMs > 0) { "plannedMs must be positive" }
            if (s.state.isRunning) throw IllegalFocusTransition(s.state, e)
            FocusSnapshot(
                state = FocusState.PREPARING, sessionId = e.sessionId, plannedMs = e.plannedMs,
                startWallTime = e.wallTime, topicId = e.topicId, label = e.label,
            )
        }
        is FocusEvent.Start -> {
            if (s.state != FocusState.PREPARING) throw IllegalFocusTransition(s.state, e)
            s.copy(state = FocusState.ACTIVE, resumedAtElapsed = e.nowElapsed)
        }
        is FocusEvent.Pause -> {
            if (s.state != FocusState.ACTIVE) throw IllegalFocusTransition(s.state, e)
            s.copy(
                state = FocusState.PAUSED,
                accumulatedMs = s.focusedMs(e.nowElapsed),
                resumedAtElapsed = -1,
                pauseCount = s.pauseCount + 1,
            )
        }
        is FocusEvent.Resume -> {
            if (s.state != FocusState.PAUSED) throw IllegalFocusTransition(s.state, e)
            s.copy(state = FocusState.ACTIVE, resumedAtElapsed = e.nowElapsed)
        }
        is FocusEvent.Tick -> {
            if (s.state == FocusState.ACTIVE && s.remainingMs(e.nowElapsed) == 0L) {
                s.copy(state = FocusState.COMPLETED, accumulatedMs = s.plannedMs, resumedAtElapsed = -1)
            } else s
        }
        is FocusEvent.Finish -> {
            if (!s.state.isRunning) throw IllegalFocusTransition(s.state, e)
            s.copy(state = FocusState.COMPLETED, accumulatedMs = s.focusedMs(e.nowElapsed), resumedAtElapsed = -1)
        }
        is FocusEvent.Abort -> {
            if (!s.state.isRunning) throw IllegalFocusTransition(s.state, e)
            s.copy(
                state = FocusState.ABORTED, accumulatedMs = s.focusedMs(e.nowElapsed),
                resumedAtElapsed = -1, endReason = e.reason,
            )
        }
        FocusEvent.Reset -> FocusSnapshot()
    }

    /**
     * Reconcile a persisted snapshot after process death or reboot.
     * If the monotonic clock went backwards (reboot), the active stretch cannot be
     * trusted, so the session is paused with the time accumulated before the stretch.
     */
    fun reconcile(s: FocusSnapshot, nowElapsed: Long, rebooted: Boolean): FocusSnapshot {
        if (s.state != FocusState.ACTIVE) return s
        if (rebooted || nowElapsed < s.resumedAtElapsed) {
            return s.copy(state = FocusState.PAUSED, resumedAtElapsed = -1, endReason = "Device restarted – session paused")
        }
        return reduce(s, FocusEvent.Tick(nowElapsed))
    }
}
