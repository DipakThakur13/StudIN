package com.studin.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.db.StudinDatabase
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.AppClock
import com.studin.app.core.data.repo.FocusRepository
import com.studin.app.core.data.repo.ProfileRepository
import com.studin.app.core.data.repo.StudyRepository
import com.studin.domain.model.FocusState
import com.studin.domain.stats.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class DayBar(val label: String, val minutes: Int, val isToday: Boolean)
data class SubjectMastery(val subject: String, val mastery: Float, val topics: Int, val minutes: Int)

data class InsightsState(
    val loaded: Boolean = false,
    val weekFocusMinutes: Int = 0,
    val lastWeekFocusMinutes: Int = 0,
    val weekSessions: Int = 0,
    val completionRate: Float = 0f,
    val blockedAttempts: Int = 0,
    val week: List<DayBar> = emptyList(),
    val goalMinutes: Int = 120,
    /** 84 days, oldest first; minutes per day. */
    val heatmap: List<Int> = emptyList(),
    val heatmapStartDay: Long = 0,
    val reviewAccuracy: Float? = null,
    val reviewsThisWeek: Int = 0,
    val dueCards: Int = 0,
    val subjects: List<SubjectMastery> = emptyList(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val streaksEnabled: Boolean = true,
    val guardDecisions: Int = 0,
    val guardBlocks: Int = 0,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    focus: FocusRepository,
    study: StudyRepository,
    profiles: ProfileRepository,
    prefs: UserPreferences,
    db: StudinDatabase,
    private val clock: AppClock,
) : ViewModel() {
    private val today = clock.today()
    private val start84 = today - 83

    val state: StateFlow<InsightsState> = combine(
        focus.observeSessionsSince(clock.startOfDay(start84)),
        db.reviewDao().observeSince(clock.startOfDay(today - 6)),
        study.observeTopics(),
        combine(study.observeDueCount(), focus.observeDecisions(500)) { a, b -> a to b },
        combine(profiles.observe(), prefs.settings) { p, s -> p to s },
    ) { sessions, reviews, topics, (due, decisions), (profile, settings) ->
        val byDay = sessions.groupBy { clock.dayOf(it.startWallTime) }.mapValues { (_, v) -> (v.sumOf { it.focusedMs } / 60_000L).toInt() }
        val weekStart = today - 6
        val week = sessions.filter { clock.dayOf(it.startWallTime) >= weekStart }
        val lastWeek = sessions.filter { clock.dayOf(it.startWallTime) in (today - 13)..(today - 7) }
        val finished = week.filter { it.status == FocusState.COMPLETED.name || it.status == FocusState.ABORTED.name }
        val completed = finished.count { it.status == FocusState.COMPLETED.name }
        val activeDays = byDay.filterValues { it >= 25 }.keys
        val weekFrom = clock.startOfDay(weekStart)
        InsightsState(
            loaded = true,
            weekFocusMinutes = week.sumOf { it.focusedMs / 60_000L }.toInt(),
            lastWeekFocusMinutes = lastWeek.sumOf { it.focusedMs / 60_000L }.toInt(),
            weekSessions = week.size,
            completionRate = if (finished.isEmpty()) 0f else completed.toFloat() / finished.size,
            blockedAttempts = week.sumOf { it.blockedAttempts },
            week = (6 downTo 0).map { back ->
                val d = today - back
                DayBar(LocalDate.ofEpochDay(d).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), byDay[d] ?: 0, back == 0)
            },
            goalMinutes = profile?.dailyGoalMinutes ?: 120,
            heatmap = (start84..today).map { byDay[it] ?: 0 },
            heatmapStartDay = start84,
            reviewAccuracy = if (reviews.isEmpty()) null else reviews.count { it.correct }.toFloat() / reviews.size,
            reviewsThisWeek = reviews.size,
            dueCards = due,
            subjects = topics.groupBy { it.subject }.map { (subject, list) ->
                SubjectMastery(subject, list.map { it.mastery }.average().toFloat(), list.size, list.sumOf { it.minutesStudied })
            }.sortedByDescending { it.mastery },
            currentStreak = StreakCalculator.currentStreak(activeDays, today),
            bestStreak = StreakCalculator.bestStreak(activeDays),
            streaksEnabled = settings.streaksEnabled,
            guardDecisions = decisions.count { it.timestamp >= weekFrom },
            guardBlocks = decisions.count { it.timestamp >= weekFrom && it.label == "BLOCK" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsState())
}
