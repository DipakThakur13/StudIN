package com.studin.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.R
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINBadge
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINEmptyState
import com.studin.app.core.designsystem.StudINExamCountdownCard
import com.studin.app.core.designsystem.StudINIconButton
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINLinearProgress
import com.studin.app.core.designsystem.StudINLoadingState
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINRow
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINStatCard
import com.studin.app.core.designsystem.StudINStudyTaskCard
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.StudINType
import com.studin.app.core.designsystem.StudINWordmark
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.app.ui.navigation.Routes
import com.studin.domain.focus.FocusSnapshot
import com.studin.domain.model.FocusState
import com.studin.domain.plan.PlanBlock
import java.time.LocalTime

@Composable
fun HomeScreen(onNavigate: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    var showAllPlan by rememberSaveable { mutableStateOf(false) }

    val featured = HomeUi.pickContinue(s.plan)
    val rest = HomeUi.remainingPlan(s.plan, featured)
    val shownRest = if (showAllPlan) rest else rest.take(HomeUi.PLAN_PREVIEW)

    val openBlock: (PlanBlock) -> Unit = { block ->
        if (block.kind == PlanBlock.Kind.RECALL) onNavigate(Routes.review())
        else { vm.startBlock(block); onNavigate(Routes.FOCUS) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeTopBar(s.name, onSettings = { onNavigate(Routes.SETTINGS) }) },
    ) { padding ->
        when {
            !s.loaded -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { StudINLoadingState() }
            s.exam == null && !s.hasTopics -> Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.screen), contentAlignment = Alignment.Center) {
                StudINEmptyState(
                    title = "Welcome to StudIN",
                    message = "Add your exam and syllabus to get a daily plan, focus sessions and flashcards.",
                    illustration = R.drawable.ill_welcome,
                    actionLabel = "Set up my study plan",
                    onAction = { onNavigate(Routes.STUDY) },
                )
            }
            else -> LazyColumn(
                Modifier.studinContentWidth().fillMaxSize(),
                contentPadding = screenContentPadding(padding, top = Spacing.xxs),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                item(key = "greeting") { Greeting(s.name) }
                item(key = "countdown") {
                    StudINExamCountdownCard(
                        examName = s.exam?.displayName ?: "Your exam",
                        daysLeft = s.daysToExam,
                        focusedMinutesToday = s.todayMinutes,
                        goalMinutes = s.goalMinutes,
                        onClick = { onNavigate(Routes.SETTINGS) },
                    )
                }
                item(key = "continueHeader") {
                    Spacer(Modifier.height(Spacing.sm))
                    StudINSectionHeader(if (snap.state.isRunning) "Your session" else "Continue studying")
                }
                item(key = "continue") {
                    when {
                        snap.state.isRunning -> RunningSessionCard(
                            snap, now,
                            onOpen = { onNavigate(Routes.FOCUS) },
                            onResume = { vm.resume(); onNavigate(Routes.FOCUS) },
                        )
                        featured != null -> ContinueCard(featured, featured.topicId?.let { s.topicProgress[it] }, s.dueCards, onStart = { openBlock(featured) })
                        !s.hasTopics -> SetupCard(onSetup = { onNavigate(Routes.STUDY) })
                        else -> GoalReachedCard(onFocus = { onNavigate(Routes.FOCUS) })
                    }
                }
                item(key = "progressHeader") {
                    Spacer(Modifier.height(Spacing.sm))
                    StudINSectionHeader("Today's progress")
                }
                item(key = "progress") { ProgressGrid(s, onReview = { onNavigate(Routes.review()) }) }
                if (rest.isNotEmpty()) {
                    item(key = "planHeader") {
                        Spacer(Modifier.height(Spacing.sm))
                        StudINSectionHeader(
                            "Today's plan",
                            supporting = "Advisory – adjust it to your day",
                            action = if (rest.size > HomeUi.PLAN_PREVIEW) (if (showAllPlan) "Show less" else "View all") else null,
                            onAction = { showAllPlan = !showAllPlan },
                        )
                    }
                    items(shownRest, key = { b -> "plan_${b.kind}_${b.topicId}_${b.title}" }) { block ->
                        StudINStudyTaskCard(
                            title = block.title,
                            subject = block.subject,
                            minutes = block.minutes,
                            kindLabel = HomeUi.kindLabel(block.kind),
                            done = block.topicId?.let { s.topicProgress[it]?.studiedToday } == true,
                            icon = iconFor(block.kind),
                            accent = if (block.kind == PlanBlock.Kind.RECALL) StudINTheme.colors.accent else MaterialTheme.colorScheme.primary,
                            actionLabel = if (block.kind == PlanBlock.Kind.RECALL) "Review" else "Start",
                            onAction = { openBlock(block) },
                        )
                    }
                }
            }
        }
    }
}

private fun iconFor(kind: PlanBlock.Kind): ImageVector = when (kind) {
    PlanBlock.Kind.RECALL -> Icons.Rounded.Style
    PlanBlock.Kind.REVISE -> Icons.Rounded.Replay
    PlanBlock.Kind.RECOVERY -> Icons.Rounded.SelfImprovement
    PlanBlock.Kind.LEARN -> Icons.AutoMirrored.Rounded.MenuBook
}

@Composable
private fun HomeTopBar(name: String, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 64.dp).padding(start = Spacing.screen, end = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StudINWordmark(Modifier.weight(1f), markSize = 32.dp)
        StudINIconButton(Icons.Rounded.NotificationsNone, "Reminders and notifications", onSettings)
        Spacer(Modifier.width(Spacing.xxs))
        Surface(
            onClick = onSettings,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp).semantics { contentDescription = "Profile and settings" },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(HomeUi.initialsOf(name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun Greeting(name: String) {
    val greet = HomeUi.greetingFor(LocalTime.now().hour)
    Column(Modifier.fillMaxWidth().padding(bottom = Spacing.xxs).semantics(mergeDescendants = true) { heading() }) {
        Text("$greet,", style = MaterialTheme.typography.bodyLarge, color = StudINTheme.colors.textSecondary)
        Text(name.ifBlank { "Student" }, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** The single most prominent action on Home: the next syllabus block. */
@Composable
private fun ContinueCard(block: PlanBlock, progress: TopicProgress?, dueCards: Int, onStart: () -> Unit) {
    val isRecall = block.kind == PlanBlock.Kind.RECALL
    StudINCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            StudINIconTile(iconFor(block.kind), if (isRecall) StudINTheme.colors.accent else MaterialTheme.colorScheme.primary, 44.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(block.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(block.subject.takeIf { it.isNotBlank() }, HomeUi.formatMinutes(block.minutes), HomeUi.kindLabel(block.kind)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(block.why, style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary)
        if (progress != null) {
            Spacer(Modifier.height(Spacing.sm))
            val pct = (progress.mastery.coerceIn(0.0, 1.0) * 100).toInt()
            Row {
                Text("Mastery", style = MaterialTheme.typography.labelMedium, color = StudINTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                Text("$pct% · ${HomeUi.formatMinutes(progress.minutesStudied)} studied", style = MaterialTheme.typography.labelMedium, color = StudINTheme.colors.textSecondary)
            }
            Spacer(Modifier.height(Spacing.xxs + 2.dp))
            StudINLinearProgress(progress.mastery.toFloat(), label = "Mastery $pct percent")
        }
        Spacer(Modifier.height(Spacing.md))
        StudINPrimaryButton(
            text = if (isRecall) "Review $dueCards cards" else "Start ${block.minutes}-min session",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Rounded.PlayArrow,
        )
    }
}

@Composable
private fun RunningSessionCard(snap: FocusSnapshot, now: Long, onOpen: () -> Unit, onResume: () -> Unit) {
    val paused = snap.state == FocusState.PAUSED
    val remaining = HomeUi.formatClock(snap.remainingMs(now))
    StudINCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINIconTile(if (paused) Icons.Rounded.Pause else Icons.Rounded.Timer, StudINTheme.colors.accent, 44.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(snap.label.ifBlank { "Deep focus" }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (paused) "Paused" else "Focusing now", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(remaining, style = StudINType.statValue)
                Text("left", style = MaterialTheme.typography.labelSmall, color = StudINTheme.colors.textSecondary)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        StudINLinearProgress(snap.progress(now), color = StudINTheme.colors.accent, label = "Session progress")
        Spacer(Modifier.height(Spacing.md))
        if (paused) StudINPrimaryButton("Resume session", onResume, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
        else StudINOutlinedButton("Open focus timer", onOpen, Modifier.fillMaxWidth(), icon = Icons.Rounded.Timer)
    }
}

@Composable
private fun SetupCard(onSetup: () -> Unit) {
    StudINCard(Modifier.fillMaxWidth()) {
        Text("Add your syllabus", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.xxs))
        Text("StudIN builds today's plan from your topics and their weightage.", style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary)
        Spacer(Modifier.height(Spacing.md))
        StudINPrimaryButton("Set up syllabus", onSetup, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.MenuBook)
    }
}

@Composable
private fun GoalReachedCard(onFocus: () -> Unit) {
    StudINCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINIconTile(Icons.Rounded.CheckCircle, StudINTheme.colors.success, 44.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text("Today's goal is done", style = MaterialTheme.typography.titleMedium)
                Text("Rest well – recovery is part of learning.", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
            }
            StudINBadge("Done", container = StudINTheme.colors.successContainer, content = StudINTheme.colors.success, icon = Icons.Rounded.CheckCircle)
        }
        Spacer(Modifier.height(Spacing.md))
        StudINOutlinedButton("Start an extra session", onFocus, Modifier.fillMaxWidth(), icon = Icons.Rounded.Timer)
    }
}

@Composable
private fun ProgressGrid(s: HomeState, onReview: () -> Unit) {
    val c = StudINTheme.colors
    // Each row takes the height of its tallest card so the grid stays aligned.
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StudINRow(Modifier.height(IntrinsicSize.Min)) {
            val cell = Modifier.weight(1f).fillMaxHeight()
            StudINStatCard("Focused today", HomeUi.formatMinutes(s.todayMinutes), Icons.Rounded.Timer, cell, accent = c.accent,
                supporting = "Goal ${HomeUi.formatMinutes(s.goalMinutes)}")
            StudINStatCard("Sessions completed", "${s.completedToday}", Icons.Rounded.CheckCircle, cell, accent = c.success,
                supporting = if (s.todaySessions > s.completedToday) "${s.todaySessions} started" else null)
        }
        StudINRow(Modifier.height(IntrinsicSize.Min)) {
            val cell = Modifier.weight(1f).fillMaxHeight()
            StudINStatCard("Revision cards due", "${s.dueCards}", Icons.Rounded.Style, cell, accent = MaterialTheme.colorScheme.primary,
                supporting = if (s.dueCards > 0) "Review now" else "All caught up", onClick = if (s.dueCards > 0) onReview else null)
            if (s.streaksEnabled) StudINStatCard("Day streak", if (s.streak == 1) "1 day" else "${s.streak} days", Icons.Rounded.LocalFireDepartment, cell,
                accent = c.warning, supporting = if (s.streak == 0) "Start today" else null)
            else StudINStatCard("Sessions this week", "${s.weekSessions}", Icons.Rounded.CalendarMonth, cell, accent = c.warning)
        }
    }
}
