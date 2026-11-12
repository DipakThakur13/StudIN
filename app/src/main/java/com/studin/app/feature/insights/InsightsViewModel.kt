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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SubjectMastery(val subject: String, val mastery: Float, val topics: Int, val minutes: Int)

data class InsightsState(
    val loaded: Boolean = false,
    val period: InsightsPeriod = InsightsPeriod.WEEK,
    val hasAnySessions: Boolean = false,
    val summary: PeriodSummary = PeriodSummary(0, 0, 0, 0, null, 0, 0, InsightsPeriod.WEEK.days, 0),
    val chart: ChartData = ChartData(emptyList(), null, 1, ""),
    val goalMinutes: Int = 120,
    /** 12 Monday-first weeks × 7 days. */
    val heatmap: List<List<HeatCell>> = emptyList(),
    val reviewAccuracy: Float? = null,
    val reviewsInPeriod: Int = 0,
    val dueCards: Int = 0,
    val subjects: List<SubjectMastery> = emptyList(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val streaksEnabled: Boolean = true,
    val observations: List<String> = emptyList(),
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

    /** Two full 12-week windows so the 12W filter can compare with the previous period. */
    private val loadFrom = today - 2 * InsightsPeriod.QUARTER.days + 1

    private val _period = MutableStateFlow(InsightsPeriod.WEEK)
    val period: StateFlow<InsightsPeriod> = _period.asStateFlow()
    fun setPeriod(p: InsightsPeriod) { _period.value = p }

    val state: StateFlow<InsightsState> = combine(
        focus.observeSessionsSince(clock.startOfDay(loadFrom)),
        db.reviewDao().observeSince(clock.startOfDay(today - InsightsPeriod.QUARTER.days + 1)),
        combine(study.observeTopics(), study.observeDueCount()) { a, b -> a to b },
        combine(profiles.observe(), prefs.settings) { p, s -> p to s },
        _period,
    ) { sessions, reviews, (topics, due), (profile, settings), period ->
        val stats = sessions.map {
            SessionStat(
                day = clock.dayOf(it.startWallTime),
                focusedMinutes = (it.focusedMs / 60_000L).toInt(),
                completed = it.status == FocusState.COMPLETED.name,
                aborted = it.status == FocusState.ABORTED.name,
                blockedAttempts = it.blockedAttempts,
            )
        }
        val goal = profile?.dailyGoalMinutes ?: 120
        val byDay = InsightsMath.minutesByDay(stats)
        val range = InsightsMath.range(period, today)
        val periodReviews = reviews.filter { clock.dayOf(it.reviewedAt) in range }
        val streakDays = byDay.filterValues { it >= 25 }.keys
        InsightsState(
            loaded = true,
            period = period,
            hasAnySessions = stats.isNotEmpty(),
            summary = InsightsMath.summarize(stats, today, period, goal),
            chart = InsightsMath.chart(byDay, today, period, goal),
            goalMinutes = goal,
            heatmap = InsightsMath.heatmap(byDay, today, goal),
            reviewAccuracy = if (periodReviews.isEmpty()) null else periodReviews.count { it.correct }.toFloat() / periodReviews.size,
            reviewsInPeriod = periodReviews.size,
            dueCards = due,
            subjects = topics.groupBy { it.subject }.map { (subject, list) ->
                SubjectMastery(subject, list.map { it.mastery }.average().toFloat(), list.size, list.sumOf { it.minutesStudied })
            }.sortedByDescending { it.mastery },
            currentStreak = StreakCalculator.currentStreak(streakDays, today),
            bestStreak = StreakCalculator.bestStreak(streakDays),
            streaksEnabled = settings.streaksEnabled,
            observations = InsightsMath.observations(stats, today, period, goal),
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsState())
}
