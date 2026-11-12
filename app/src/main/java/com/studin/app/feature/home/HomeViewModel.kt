package com.studin.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.AppClock
import com.studin.app.core.data.repo.FocusRepository
import com.studin.app.core.data.repo.ProfileRepository
import com.studin.app.core.data.repo.StudyRepository
import com.studin.app.core.data.repo.exam
import com.studin.app.platform.focus.FocusController
import com.studin.domain.focus.FocusSnapshot
import com.studin.domain.model.ExamType
import com.studin.domain.model.FocusState
import com.studin.domain.plan.PlanBlock
import com.studin.domain.plan.PlannerTopic
import com.studin.domain.plan.StudyPlanner
import com.studin.domain.stats.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Progress of a syllabus topic, for the "Continue studying" card and plan completion state. */
data class TopicProgress(val mastery: Double, val minutesStudied: Int, val studiedToday: Boolean)

data class HomeState(
    val loaded: Boolean = false,
    val name: String = "",
    val exam: ExamType? = null,
    val daysToExam: Long? = null,
    val goalMinutes: Int = 120,
    val todayMinutes: Int = 0,
    val todaySessions: Int = 0,
    val completedToday: Int = 0,
    val weekSessions: Int = 0,
    val streak: Int = 0,
    val streaksEnabled: Boolean = true,
    val dueCards: Int = 0,
    val totalCards: Int = 0,
    val hasTopics: Boolean = false,
    val plan: List<PlanBlock> = emptyList(),
    val topicProgress: Map<Long, TopicProgress> = emptyMap(),
    val weekMinutes: List<Int> = List(7) { 0 },
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    profiles: ProfileRepository,
    study: StudyRepository,
    focus: FocusRepository,
    prefs: UserPreferences,
    private val controller: FocusController,
    private val clock: AppClock,
) : ViewModel() {

    val snapshot: StateFlow<FocusSnapshot> = controller.snapshot
    val now: StateFlow<Long> = controller.now

    private val today = clock.today()
    private val sessionsFlow = focus.observeSessionsSince(clock.startOfDay(today - 60))

    val state: StateFlow<HomeState> = combine(
        profiles.observe(), study.observeTopics(), sessionsFlow,
        combine(study.observeDueCount(), study.observeTotalCards()) { a, b -> a to b },
        prefs.settings,
    ) { profile, topics, sessions, (due, total), settings ->
        val byDay = sessions.groupBy { clock.dayOf(it.startWallTime) }.mapValues { (_, v) -> (v.sumOf { it.focusedMs } / 60_000L).toInt() }
        val todayMin = byDay[today] ?: 0
        val activeDays = byDay.filterValues { it >= 25 }.keys
        val goal = profile?.dailyGoalMinutes ?: 120
        val missedYesterday = sessions.isNotEmpty() && (byDay[today - 1] ?: 0) == 0 && todayMin == 0
        val planner = topics.map { PlannerTopic(it.id, it.subject, it.title, it.priority, it.mastery, it.minutesStudied, it.lastStudiedDay) }
        val todays = sessions.filter { clock.dayOf(it.startWallTime) == today }
        HomeState(
            loaded = true,
            name = profile?.name.orEmpty(),
            exam = profile?.exam(),
            daysToExam = profile?.examDay?.let { it - today },
            goalMinutes = goal,
            todayMinutes = todayMin,
            todaySessions = todays.size,
            completedToday = todays.count { it.status == FocusState.COMPLETED.name },
            weekSessions = sessions.count { clock.dayOf(it.startWallTime) > today - 7 },
            streak = StreakCalculator.currentStreak(activeDays, today),
            streaksEnabled = settings.streaksEnabled,
            dueCards = due,
            totalCards = total,
            hasTopics = topics.isNotEmpty(),
            plan = StudyPlanner.planToday(planner, today, profile?.examDay, (goal - todayMin).coerceAtLeast(25), due, missedYesterday).take(6),
            topicProgress = topics.associate { it.id to TopicProgress(it.mastery, it.minutesStudied, it.lastStudiedDay == today) },
            weekMinutes = (6 downTo 0).map { byDay[today - it] ?: 0 },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun startBlock(block: PlanBlock) = controller.start(block.minutes, block.topicId, block.title)
    fun resume() = controller.resume()
}
