package com.studin.app.platform.focus

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.studin.app.core.data.db.FocusSessionEntity
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.AppClock
import com.studin.app.core.data.repo.FocusRepository
import com.studin.app.core.data.repo.StudyRepository
import com.studin.app.core.di.AppScope
import com.studin.domain.focus.FocusEvent
import com.studin.domain.focus.FocusSnapshot
import com.studin.domain.focus.FocusStateMachine
import com.studin.domain.focus.IllegalFocusTransition
import com.studin.domain.model.FocusState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** One-off outcomes the UI can celebrate or explain. */
sealed interface FocusOutcome {
    data class Completed(val sessionId: Long, val focusedMinutes: Int, val label: String) : FocusOutcome
    data class Ended(val sessionId: Long, val focusedMinutes: Int, val reason: String) : FocusOutcome
}

/**
 * Single source of truth for the running focus session. Applies the pure
 * [FocusStateMachine], persists every transition (process death / reboot safe),
 * mirrors it into Room and drives the foreground [FocusService].
 */
@Singleton
class FocusController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
    private val focusRepo: FocusRepository,
    private val studyRepo: StudyRepository,
    private val clock: AppClock,
    @AppScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val _snapshot = MutableStateFlow(FocusSnapshot())
    val snapshot: StateFlow<FocusSnapshot> = _snapshot.asStateFlow()

    /** Monotonic "now" that ticks every second while a session is running. */
    private val _now = MutableStateFlow(SystemClock.elapsedRealtime())
    val now: StateFlow<Long> = _now.asStateFlow()

    private val _outcomes = MutableSharedFlow<FocusOutcome>(extraBufferCapacity = 4)
    val outcomes: SharedFlow<FocusOutcome> = _outcomes.asSharedFlow()

    private var ticker: Job? = null
    @Volatile private var restored = false

    val isSessionActive: Boolean get() = _snapshot.value.state == FocusState.ACTIVE

    /** Restore after process start or reboot. Idempotent. */
    fun restore() {
        if (restored) return
        restored = true
        scope.launch {
            mutex.withLock {
                val (saved, savedBoot) = prefs.loadSnapshot()
                val rebooted = savedBoot != -1 && savedBoot != bootCount()
                val reconciled = FocusStateMachine.reconcile(saved, SystemClock.elapsedRealtime(), rebooted)
                _snapshot.value = reconciled
                persistLocked(reconciled)
                if (reconciled.state == FocusState.COMPLETED && saved.state == FocusState.ACTIVE) finalizeLocked(reconciled)
                if (reconciled.state.isRunning) { FocusService.start(context); startTicker() }
            }
        }
    }

    fun start(minutes: Int, topicId: Long?, label: String) = scope.launch {
        mutex.withLock {
            if (_snapshot.value.state.isRunning) return@withLock
            val planned = minutes.coerceIn(1, 240) * 60_000L
            val nowE = SystemClock.elapsedRealtime(); val wall = clock.now()
            val id = focusRepo.insertSession(
                FocusSessionEntity(topicId = topicId, label = label.take(80), plannedMinutes = minutes, startElapsed = nowE,
                    startWallTime = wall, status = FocusState.ACTIVE.name)
            )
            var s = FocusStateMachine.reduce(FocusSnapshot(), FocusEvent.Prepare(id, planned, wall, topicId, label.take(80)))
            s = FocusStateMachine.reduce(s, FocusEvent.Start(nowE))
            applyLocked(s)
            prefs.setPassUntil(0)
            FocusService.start(context)
            startTicker()
        }
    }

    fun pause() = transition { FocusEvent.Pause(it) }
    fun resume() = transition { FocusEvent.Resume(it) }
    fun finishEarly() = transition { FocusEvent.Finish(it) }
    fun abort(reason: String) = transition { FocusEvent.Abort(it, reason) }

    /** Clear a finished session so the timer returns to IDLE. */
    fun dismiss() = scope.launch {
        mutex.withLock { if (!_snapshot.value.state.isRunning) applyLocked(FocusSnapshot()) }
    }

    /** Temporary, user-earned exception for blocked apps (session exception layer). */
    fun grantPass(minutes: Int) = scope.launch { prefs.setPassUntil(clock.now() + minutes * 60_000L) }

    suspend fun isPassActive(): Boolean = prefs.current().passUntil > clock.now()

    fun recordBlockedAttempt() = scope.launch {
        val id = _snapshot.value.sessionId
        if (id > 0 && _snapshot.value.state.isRunning) focusRepo.incrementBlocked(id)
    }

    private fun transition(event: (Long) -> FocusEvent) = scope.launch {
        mutex.withLock {
            val before = _snapshot.value
            val after = try { FocusStateMachine.reduce(before, event(SystemClock.elapsedRealtime())) } catch (_: IllegalFocusTransition) { return@withLock }
            applyLocked(after)
            if (after.state == FocusState.COMPLETED || after.state == FocusState.ABORTED) finalizeLocked(after)
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val nowE = SystemClock.elapsedRealtime()
                _now.value = nowE
                mutex.withLock {
                    val s = _snapshot.value
                    if (!s.state.isRunning) return@launch
                    val next = FocusStateMachine.reduce(s, FocusEvent.Tick(nowE))
                    if (next.state != s.state) { applyLocked(next); if (next.state == FocusState.COMPLETED) finalizeLocked(next) }
                }
                delay(1_000L - (SystemClock.elapsedRealtime() % 1_000L).coerceAtMost(999))
            }
        }
    }

    private suspend fun applyLocked(s: FocusSnapshot) {
        _snapshot.value = s
        _now.value = SystemClock.elapsedRealtime()
        persistLocked(s)
        if (s.state.isRunning) focusRepo.session(s.sessionId)?.let { row ->
            focusRepo.updateSession(row.copy(status = s.state.name, focusedMs = s.focusedMs(_now.value), pauses = s.pauseCount))
        }
    }

    private suspend fun persistLocked(s: FocusSnapshot) = prefs.saveSnapshot(s, bootCount())

    private suspend fun finalizeLocked(s: FocusSnapshot) {
        val minutes = (s.accumulatedMs / 60_000L).toInt()
        focusRepo.session(s.sessionId)?.let { row ->
            focusRepo.updateSession(row.copy(status = s.state.name, focusedMs = s.accumulatedMs, endWallTime = clock.now(), pauses = s.pauseCount, interruptedReason = s.endReason))
        }
        if (s.topicId != null && minutes > 0) studyRepo.recordTopicStudy(s.topicId!!, minutes)
        prefs.setPassUntil(0)
        _outcomes.tryEmit(
            if (s.state == FocusState.COMPLETED) FocusOutcome.Completed(s.sessionId, minutes, s.label)
            else FocusOutcome.Ended(s.sessionId, minutes, s.endReason ?: "Ended")
        )
    }

    private fun bootCount(): Int = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(-1)
}
