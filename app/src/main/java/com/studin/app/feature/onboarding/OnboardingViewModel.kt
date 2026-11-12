package com.studin.app.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.Profile
import com.studin.app.core.data.repo.ProfileRepository
import com.studin.app.platform.notifications.ReminderScheduler
import com.studin.domain.model.ExamType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val name: String = "",
    val exam: ExamType? = null,
    val examDay: Long? = null,
    val dailyGoalMinutes: Int = 120,
    val streaksEnabled: Boolean = true,
    val saving: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profiles: ProfileRepository,
    private val prefs: UserPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setName(v: String) = _state.update { it.copy(name = v.take(40)) }
    fun setExam(v: ExamType) = _state.update { it.copy(exam = v) }
    fun setExamDay(v: Long?) = _state.update { it.copy(examDay = v) }
    fun setGoal(v: Int) = _state.update { it.copy(dailyGoalMinutes = v) }
    fun setStreaks(v: Boolean) = _state.update { it.copy(streaksEnabled = v) }

    fun finish(onDone: () -> Unit) {
        val s = _state.value
        val exam = s.exam ?: return
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            profiles.save(Profile(s.name.trim().ifEmpty { "Student" }, exam, s.examDay, s.dailyGoalMinutes))
            prefs.setStreaks(s.streaksEnabled)
            prefs.setOnboardingDone(true)
            val settings = prefs.current()
            ReminderScheduler.schedule(context, settings.reminderHour, settings.remindersEnabled)
            onDone()
        }
    }
}
