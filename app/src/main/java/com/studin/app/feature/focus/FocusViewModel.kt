package com.studin.app.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.db.FocusSessionEntity
import com.studin.app.core.data.db.SyllabusTopicEntity
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.AppClock
import com.studin.app.core.data.repo.FocusRepository
import com.studin.app.core.data.repo.StudyRepository
import com.studin.app.platform.focus.FocusController
import com.studin.app.platform.focus.FocusOutcome
import com.studin.app.platform.permissions.PermissionChecker
import com.studin.domain.focus.FocusSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusSetup(
    val minutes: Int = 25,
    val topic: SyllabusTopicEntity? = null,
    val setupLoaded: Boolean = false,
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val controller: FocusController,
    private val permissions: PermissionChecker,
    private val prefs: UserPreferences,
    study: StudyRepository,
    focusRepo: FocusRepository,
    clock: AppClock,
) : ViewModel() {
    val snapshot: StateFlow<FocusSnapshot> = controller.snapshot
    val now: StateFlow<Long> = controller.now
    val outcomes: SharedFlow<FocusOutcome> = controller.outcomes

    val topics: StateFlow<List<SyllabusTopicEntity>> =
        study.observeTopics().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todaySessions: StateFlow<List<FocusSessionEntity>> =
        focusRepo.observeSessionsSince(clock.startOfDay(clock.today()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _setup = MutableStateFlow(FocusSetup())
    val setup: StateFlow<FocusSetup> = _setup.asStateFlow()

    /** True when neither Usage Access nor Focus Guard is available (no blocking possible). */
    private val _guardMissing = MutableStateFlow(false)
    val guardMissing: StateFlow<Boolean> = _guardMissing.asStateFlow()

    init {
        viewModelScope.launch {
            val m = prefs.current().defaultFocusMinutes
            _setup.update { if (it.setupLoaded) it else it.copy(minutes = m, setupLoaded = true) }
        }
        refreshPermissions()
    }

    fun refreshPermissions() {
        _guardMissing.value = !permissions.hasUsageAccess() && !permissions.isAccessibilityEnabled()
    }

    fun setMinutes(m: Int) = _setup.update { it.copy(minutes = m.coerceIn(5, 180)) }
    fun setTopic(t: SyllabusTopicEntity?) = _setup.update { it.copy(topic = t) }

    fun start() {
        val s = _setup.value
        val label = s.topic?.title ?: "Deep work"
        controller.start(s.minutes, s.topic?.id, label)
        viewModelScope.launch { prefs.setDefaultFocusMinutes(s.minutes) }
    }

    fun pause() = controller.pause()
    fun resume() = controller.resume()
    fun finish() = controller.finishEarly()
    fun stop() = controller.abort("Stopped by you")
    fun dismiss() = controller.dismiss()
}
