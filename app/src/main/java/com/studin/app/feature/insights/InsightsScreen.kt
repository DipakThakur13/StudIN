package com.studin.app.feature.insights

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.R
import com.studin.app.core.designsystem.ChartBar
import com.studin.app.core.designsystem.Motion
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINEmptyState
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINInsightChart
import com.studin.app.core.designsystem.StudINLinearProgress
import com.studin.app.core.designsystem.StudINLoadingState
import com.studin.app.core.designsystem.StudINRow
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINSegmentedTabs
import com.studin.app.core.designsystem.StudINStatCard
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.motionDuration
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.app.ui.navigation.Routes
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(onNavigate: (String) -> Unit, vm: InsightsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    StudINScaffold(title = "Insights", subtitle = "Evidence of progress — computed on your phone") { padding ->
        LazyColumn(
            Modifier.fillMaxSize().studinContentWidth(),
            contentPadding = screenContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "filter") {
                StudINSegmentedTabs(
                    options = InsightsPeriod.entries.map { it.label },
                    selectedIndex = s.period.ordinal,
                    onSelect = { vm.setPeriod(InsightsPeriod.entries[it]) },
                    modifier = Modifier.semantics { contentDescription = "Time period: ${s.period.description}" },
                )
            }
            when {
                !s.loaded -> item(key = "loading") { StudINLoadingState(message = "Crunching your numbers…") }
                !s.hasAnySessions -> item(key = "empty") {
                    StudINEmptyState(
                        title = "No focus sessions yet",
                        message = "Insights are built only from what you actually do in StudIN. Finish a focus session and your charts appear here.",
                        illustration = R.drawable.ill_insights,
                        actionLabel = "Start a focus session",
                        onAction = { onNavigate(Routes.FOCUS) },
                    )
                }
                else -> {
                    item(key = "summary") { SummaryGrid(s) }
                    item(key = "chart") { FocusChartCard(s) }
                    if (s.observations.isNotEmpty()) item(key = "obs") { ObservationsCard(s.observations) }
                    item(key = "heat") { HeatmapCard(s) }
                }
            }
            if (s.loaded) {
                item(key = "recall") { RecallRow(s) }
                if (s.subjects.isNotEmpty()) item(key = "subjects") { SubjectsCard(s.subjects) }
                item(key = "how") { HowWeMeasure() }
            }
        }
    }
}

// ------------------------------------------------------------------ Summary

@Composable
private fun SummaryGrid(s: InsightsState) {
    val sum = s.summary
    val c = StudINTheme.colors
    val delta = sum.focusMinutes - sum.previousFocusMinutes
    val trend = when {
        sum.previousFocusMinutes == 0 && sum.focusMinutes == 0 -> null
        sum.previousFocusMinutes == 0 -> "New this period"
        delta == 0 -> "Same as before"
        else -> "${if (delta > 0) "+" else "−"}${InsightsMath.formatCompactMinutes(abs(delta))} vs prev."
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StudINRow {
            StudINStatCard("Focus time", InsightsMath.formatHours(sum.focusMinutes), Icons.Rounded.Timer, Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.primary, supporting = trend)
            StudINStatCard(
                "Sessions completed", "${sum.completedSessions}", Icons.Rounded.CheckCircle, Modifier.weight(1f), accent = c.success,
                supporting = sum.completionRate?.let { "${(it * 100).roundToInt()}% finished" } ?: "of ${sum.sessions} started",
            )
        }
        StudINRow {
            StudINStatCard("Distractions blocked", "${sum.blockedAttempts}", Icons.Rounded.Block, Modifier.weight(1f), accent = c.accent,
                supporting = if (sum.blockedAttempts == 0) "None needed" else "Break screens")
            StudINStatCard(
                "Study consistency", "${sum.activeDays}/${sum.daysInPeriod} days", Icons.Rounded.CalendarMonth, Modifier.weight(1f), accent = c.ai,
                supporting = "${(sum.consistency * 100).roundToInt()}% of days",
            )
        }
    }
}

// ------------------------------------------------------------------ Chart

@Composable
private fun FocusChartCard(s: InsightsState) {
    val chart = s.chart
    var selected by rememberSaveable(s.period) { mutableStateOf<Int?>(null) }
    val sel = selected?.let { chart.points.getOrNull(it) }
    val bars = chart.points.map { p ->
        val label = if (s.period == InsightsPeriod.QUARTER) "${LocalDate.ofEpochDay(p.startDay).dayOfMonth}" else p.label
        ChartBar(label, p.minutes, p.accessibilityLabel, highlight = p.isCurrent)
    }
    StudINCard(Modifier.fillMaxWidth().animateContentSize(tween(motionDuration(Motion.card)))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Focus time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Text(
                    if (sel != null) "${sel.detailLabel} · ${InsightsMath.formatMinutes(sel.minutes)}"
                    else "${InsightsMath.formatMinutes(s.summary.focusMinutes)} in the ${s.period.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sel != null) StudINTheme.colors.accent else StudINTheme.colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        StudINInsightChart(
            bars, goal = chart.goalLine, unit = "min", selectedIndex = selected,
            onSelect = { i -> selected = if (selected == i) null else i },
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(chart.caption + if (s.period == InsightsPeriod.QUARTER) " · labels = week start day" else "",
            style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
        if (s.period != InsightsPeriod.WEEK && chart.points.isNotEmpty()) {
            Text(
                "${InsightsMath.shortRange(LocalDate.ofEpochDay(chart.points.first().startDay), LocalDate.ofEpochDay(chart.points.last().endDay))} · tap a bar for details",
                style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
            )
        } else Text("Tap a bar for details", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
    }
}

// ------------------------------------------------------------------ Observations

@Composable
private fun ObservationsCard(items: List<String>) {
    StudINCard(Modifier.fillMaxWidth(), containerColor = StudINTheme.colors.surfaceSecondary, border = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb, null, tint = StudINTheme.colors.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text("Observations", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        }
        Spacer(Modifier.height(Spacing.xs))
        items.forEach {
            Row(Modifier.padding(vertical = Spacing.xxs)) {
                Box(Modifier.padding(top = 7.dp).size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                Spacer(Modifier.width(Spacing.xs))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ------------------------------------------------------------------ Heatmap

@Composable
private fun levelColor(level: Int) = when (level) {
    0 -> StudINTheme.colors.focusTrack
    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun HeatmapCard(s: InsightsState) {
    var selectedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val locale = Locale.getDefault()
    val cell = s.heatmap.flatten().firstOrNull { it.day == selectedDay }
    StudINCard(Modifier.fillMaxWidth()) {
        Text("12-week consistency", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(
            when {
                cell?.minutes != null -> {
                    val d = LocalDate.ofEpochDay(cell.day)
                    "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)} ${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, locale)} · ${InsightsMath.formatMinutes(cell.minutes)}"
                }
                s.streaksEnabled -> "Current streak ${s.currentStreak} days · best ${s.bestStreak} (days with 25+ min)"
                else -> "Each square is one day, Monday to Sunday"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (cell?.minutes != null) StudINTheme.colors.accent else StudINTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.sm))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val labelW = 20.dp
            val gap = 4.dp
            val weeks = s.heatmap.size.coerceAtLeast(1)
            val size: Dp = min(26.dp, (maxWidth - labelW - gap * (weeks - 1)) / weeks)
            Column {
                // Month labels above the first week of each month.
                Row(Modifier.padding(start = labelW)) {
                    s.heatmap.forEachIndexed { w, week ->
                        val monday = LocalDate.ofEpochDay(week.first().day)
                        val prev = s.heatmap.getOrNull(w - 1)?.first()?.day?.let { LocalDate.ofEpochDay(it) }
                        Box(Modifier.width(size)) {
                            if (prev == null || prev.month != monday.month) {
                                Text(monday.month.getDisplayName(TextStyle.SHORT, locale), style = MaterialTheme.typography.labelSmall,
                                    color = StudINTheme.colors.textSecondary, maxLines = 1, softWrap = false,
                                    modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true))
                            }
                        }
                        if (w < weeks - 1) Spacer(Modifier.width(gap))
                    }
                }
                Spacer(Modifier.height(Spacing.xxs))
                (0 until 7).forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (d % 2 == 0) java.time.DayOfWeek.of(d + 1).getDisplayName(TextStyle.NARROW, locale) else "",
                            style = MaterialTheme.typography.labelSmall, color = StudINTheme.colors.textSecondary,
                            modifier = Modifier.width(labelW), maxLines = 1,
                        )
                        s.heatmap.forEachIndexed { w, week ->
                            val c = week[d]
                            val date = LocalDate.ofEpochDay(c.day)
                            val isSel = selectedDay == c.day
                            Box(
                                Modifier.size(size).then(
                                    if (c.minutes == null) Modifier
                                    else Modifier
                                        .background(levelColor(c.level), RoundedCornerShape(4.dp))
                                        .then(if (isSel) Modifier.border(2.dp, StudINTheme.colors.accent, RoundedCornerShape(4.dp)) else Modifier)
                                        .clickable(role = Role.Button) { selectedDay = if (isSel) null else c.day }
                                        .semantics {
                                            contentDescription = "${date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)} ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, locale)}: ${c.minutes} minutes"
                                            selected = isSel
                                        }
                                ),
                            )
                            if (w < weeks - 1) Spacer(Modifier.width(gap))
                        }
                    }
                    if (d < 6) Spacer(Modifier.height(gap))
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Legend: fainter squares mean less focus time, a full-colour square means you met your daily goal" },
        ) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = StudINTheme.colors.textSecondary)
            Spacer(Modifier.width(Spacing.xxs))
            (0..4).forEach { l ->
                Box(Modifier.padding(horizontal = 2.dp).size(12.dp).background(levelColor(l), RoundedCornerShape(3.dp)))
            }
            Spacer(Modifier.width(Spacing.xxs))
            Text("More · full colour = goal met", style = MaterialTheme.typography.labelSmall, color = StudINTheme.colors.textSecondary)
        }
    }
}

// ------------------------------------------------------------------ Recall + subjects

@Composable
private fun RecallRow(s: InsightsState) {
    val c = StudINTheme.colors
    StudINRow {
        StudINStatCard(
            "Recall accuracy", s.reviewAccuracy?.let { "${(it * 100).roundToInt()}%" } ?: "—", Icons.Rounded.Psychology, Modifier.weight(1f),
            accent = c.ai, supporting = if (s.reviewsInPeriod == 0) "No answers yet" else "${s.reviewsInPeriod} answers",
        )
        StudINStatCard(
            "Review backlog", "${s.dueCards}", Icons.Rounded.Style, Modifier.weight(1f),
            accent = if (s.dueCards > 0) c.accent else c.success, supporting = if (s.dueCards == 0) "All caught up" else "cards due now",
        )
    }
}

@Composable
private fun SubjectsCard(subjects: List<SubjectMastery>) {
    StudINCard(Modifier.fillMaxWidth()) {
        StudINSectionHeader("Subject mastery", supporting = "Estimated from focus time and quiz results")
        subjects.forEachIndexed { i, m ->
            if (i > 0) HorizontalDivider(color = StudINTheme.colors.border, modifier = Modifier.padding(vertical = Spacing.xs))
            val pct = (m.mastery * 100).roundToInt()
            Column(Modifier.semantics(mergeDescendants = true) { contentDescription = "${m.subject}: $pct percent mastery, ${m.topics} topics" }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.subject, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${m.topics} topics · ${InsightsMath.formatMinutes(m.minutes)} studied", style = MaterialTheme.typography.bodySmall,
                            color = StudINTheme.colors.textSecondary)
                    }
                    Text("$pct%", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(Spacing.xxs + 2.dp))
                StudINLinearProgress(m.mastery, height = 6.dp)
            }
        }
    }
}

@Composable
private fun HowWeMeasure() {
    var open by rememberSaveable { mutableStateOf(false) }
    StudINCard(
        Modifier.fillMaxWidth().animateContentSize(tween(motionDuration(Motion.card))),
        onClick = { open = !open }, containerColor = StudINTheme.colors.surfaceSecondary, border = null,
        contentPadding = PaddingValues(horizontal = Spacing.card, vertical = Spacing.sm),
    ) {
        Row(
            Modifier.semantics { stateDescription = if (open) "Expanded" else "Collapsed" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudINIconTile(Icons.Rounded.Info, StudINTheme.colors.textSecondary, 32.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text("How we measure", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
        }
        if (open) {
            Spacer(Modifier.height(Spacing.xs))
            listOf(
                "Focus time = minutes inside StudIN focus sessions. It shows effort, not proof of learning.",
                "A day counts towards consistency when it has any recorded focus time; streaks need 25+ minutes.",
                "Completion rate = sessions finished on time ÷ sessions that ended (finished or stopped early).",
                "Recall accuracy counts flashcard reviews and quiz answers in the selected period.",
                "Mastery is an estimate from focus minutes and quiz results – treat it as a guide.",
                "Everything is computed on this phone. Nothing is uploaded.",
            ).forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
