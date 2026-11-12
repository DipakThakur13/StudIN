package com.studin.app.feature.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.data.db.FocusSessionEntity
import com.studin.app.core.data.db.SyllabusTopicEntity
import com.studin.app.core.designsystem.Motion
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINBadge
import com.studin.app.core.designsystem.StudINBottomSheet
import com.studin.app.core.designsystem.StudINButtonTone
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINDialog
import com.studin.app.core.designsystem.StudINFocusTimer
import com.studin.app.core.designsystem.StudINIconButton
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINListItem
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINRow
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINSecondaryButton
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINShapes
import com.studin.app.core.designsystem.StudINStatCard
import com.studin.app.core.designsystem.StudINTextButton
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.motionDuration
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.app.feature.home.HomeUi
import com.studin.app.ui.navigation.Routes
import com.studin.domain.focus.FocusSnapshot
import com.studin.domain.model.FocusState
import java.text.DateFormat
import java.util.Date

private enum class FocusMode { SETUP, RUNNING, DONE }

@Composable
fun FocusScreen(onNavigate: (String) -> Unit, vm: FocusViewModel = hiltViewModel()) {
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val setup by vm.setup.collectAsStateWithLifecycle()
    val topics by vm.topics.collectAsStateWithLifecycle()
    val sessions by vm.todaySessions.collectAsStateWithLifecycle()
    val guardMissing by vm.guardMissing.collectAsStateWithLifecycle()

    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    var topicSheet by rememberSaveable { mutableStateOf(false) }
    var customSheet by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshPermissions() }

    val mode = when {
        snap.state.isRunning -> FocusMode.RUNNING
        snap.state == FocusState.COMPLETED || snap.state == FocusState.ABORTED -> FocusMode.DONE
        else -> FocusMode.SETUP
    }
    val stateLabel = FocusUi.stateLabel(snap.state)

    StudINScaffold(
        title = "Focus",
        subtitle = "Protect your study time — on your terms",
        actions = { StudINIconButton(Icons.Rounded.Tune, "App rules", { onNavigate(Routes.RULES) }) },
    ) { padding ->
        LazyColumn(
            Modifier.studinContentWidth().fillMaxSize(),
            contentPadding = screenContentPadding(padding, top = Spacing.xxs),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (guardMissing && mode == FocusMode.SETUP) {
                item(key = "guard") { GuardBanner(onFix = { onNavigate(Routes.PERMISSIONS) }) }
            }
            // The finished state leads with the completion card (no stale countdown).
            if (mode != FocusMode.DONE) item(key = "timer") {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = Spacing.xs)
                        // Announce state changes (not every tick) to screen readers.
                        .semantics { liveRegion = LiveRegionMode.Polite; stateDescription = stateLabel },
                ) {
                    StudINFocusTimer(
                        progress = FocusUi.ringProgress(snap, now),
                        remainingText = FocusUi.formatTimer(FocusUi.displayRemainingMs(snap, now, setup.minutes)),
                        stateLabel = stateLabel,
                        topic = when (mode) {
                            FocusMode.SETUP -> setup.topic?.title
                            else -> snap.label.takeIf { it.isNotBlank() }
                        },
                        active = snap.state == FocusState.ACTIVE,
                    )
                }
            }
            item(key = "controls") {
                val inMs = motionDuration(Motion.card)
                val outMs = motionDuration(Motion.small)
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = { fadeIn(tween(inMs)) togetherWith fadeOut(tween(outMs)) },
                    label = "focusMode",
                ) { m ->
                    when (m) {
                        FocusMode.SETUP -> SetupControls(
                            minutes = setup.minutes,
                            topic = setup.topic,
                            onMinutes = vm::setMinutes,
                            onCustom = { customSheet = true },
                            onPickTopic = { topicSheet = true },
                            onClearTopic = { vm.setTopic(null) },
                            onStart = vm::start,
                        )
                        FocusMode.RUNNING -> RunningControls(snap, onPause = vm::pause, onResume = vm::resume, onFinish = vm::finish, onEnd = { confirmEnd = true })
                        FocusMode.DONE -> CompletionCard(
                            snap = snap,
                            topic = snap.topicId?.let { id -> topics.firstOrNull { it.id == id } },
                            onBreak = vm::dismiss,
                            onNext = vm::start,
                            nextMinutes = setup.minutes,
                            onReview = { vm.dismiss(); onNavigate(Routes.review()) },
                        )
                    }
                }
            }
            if (mode != FocusMode.RUNNING) {
                item(key = "todayHeader") {
                    Spacer(Modifier.height(Spacing.xs))
                    StudINSectionHeader("Today")
                    TodayStats(sessions)
                }
                if (sessions.isNotEmpty()) item(key = "todayList") { SessionList(sessions) }
            }
        }
    }

    if (confirmEnd) {
        StudINDialog(
            title = "End session?",
            onDismiss = { confirmEnd = false },
            confirmLabel = "End session",
            onConfirm = { confirmEnd = false; vm.stop() },
            dismissLabel = "Keep going",
            destructive = true,
            text = { Text("Your focused minutes so far are saved. You can start again any time.", style = MaterialTheme.typography.bodyMedium) },
        )
    }
    if (topicSheet) {
        TopicSheet(topics, selectedId = setup.topic?.id, onPick = { vm.setTopic(it); topicSheet = false }, onDismiss = { topicSheet = false })
    }
    if (customSheet) {
        CustomDurationSheet(setup.minutes, onConfirm = { vm.setMinutes(it); customSheet = false }, onDismiss = { customSheet = false })
    }
}

@Composable
private fun GuardBanner(onFix: () -> Unit) {
    StudINCard(Modifier.fillMaxWidth(), containerColor = StudINTheme.colors.warningContainer, border = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINIconTile(Icons.Rounded.Shield, StudINTheme.colors.warning, 36.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text("Timer works — app blocking is off", style = MaterialTheme.typography.titleSmall)
                Text("Allow Usage access or Focus Guard to gate distracting apps.", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
            }
            StudINTextButton("Fix", onFix)
        }
    }
}

/**
 * One adaptive duration control: four presets + Custom as five equal-width options, so the
 * 90-minute preset can never wrap onto its own row.
 */
@Composable
private fun DurationSelector(minutes: Int, onMinutes: (Int) -> Unit, onCustom: () -> Unit) {
    val custom = FocusUi.isCustom(minutes)
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FocusUi.PRESETS.forEach { p ->
            DurationOption(value = "$p", unit = "min", selected = minutes == p, description = "$p minutes", modifier = Modifier.weight(1f)) { onMinutes(p) }
        }
        DurationOption(
            value = if (custom) "$minutes" else "",
            unit = "custom",
            selected = custom,
            description = if (custom) "Custom, $minutes minutes" else "Custom duration",
            modifier = Modifier.weight(1f),
            icon = if (custom) null else Icons.Rounded.Tune,
            onClick = onCustom,
        )
    }
}

@Composable
private fun DurationOption(
    value: String,
    unit: String,
    selected: Boolean,
    description: String,
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        shape = StudINShapes.button,
        color = if (selected) scheme.primary else scheme.surface,
        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, StudINTheme.colors.border),
    ) {
        Column(Modifier.padding(vertical = Spacing.xs), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            else Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(unit, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                color = if (selected) scheme.onPrimary.copy(alpha = 0.85f) else StudINTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun SetupControls(
    minutes: Int,
    topic: SyllabusTopicEntity?,
    onMinutes: (Int) -> Unit,
    onCustom: () -> Unit,
    onPickTopic: () -> Unit,
    onClearTopic: () -> Unit,
    onStart: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Column {
            Text("Duration", style = MaterialTheme.typography.labelLarge, color = StudINTheme.colors.textSecondary)
            Spacer(Modifier.height(Spacing.xs))
            DurationSelector(minutes, onMinutes, onCustom)
        }
        StudINCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            StudINListItem(
                title = topic?.title ?: "Syllabus topic (optional)",
                supporting = topic?.let { "${it.subject} · minutes count towards mastery" } ?: "Track progress per chapter",
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                onClick = onPickTopic,
                trailing = {
                    if (topic != null) StudINIconButton(Icons.Rounded.Close, "Clear topic", onClearTopic)
                    else Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = StudINTheme.colors.textSecondary)
                },
            )
        }
        Column {
            StudINPrimaryButton(FocusUi.startLabel(minutes), onStart, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow, tone = StudINButtonTone.Accent)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Pause or end any time. Emergency calls and Settings are never blocked.",
                style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RunningControls(snap: FocusSnapshot, onPause: () -> Unit, onResume: () -> Unit, onFinish: () -> Unit, onEnd: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (snap.state == FocusState.PAUSED) StudINPrimaryButton("Resume", onResume, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow, tone = StudINButtonTone.Accent)
        else StudINPrimaryButton("Pause", onPause, Modifier.fillMaxWidth(), icon = Icons.Rounded.Pause)
        StudINRow {
            StudINOutlinedButton("End", onEnd, Modifier.weight(1f), icon = Icons.Rounded.Stop, contentColor = MaterialTheme.colorScheme.error)
            StudINOutlinedButton("Finish early", onFinish, Modifier.weight(1f), icon = Icons.Rounded.Flag)
        }
        StudINCard(Modifier.fillMaxWidth(), containerColor = StudINTheme.colors.surfaceSecondary, border = null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    if (snap.pauseCount > 0) "Paused ${snap.pauseCount}× — breaks are part of studying well. Emergency calls and Settings are never blocked."
                    else "Emergency calls and Settings are never blocked.",
                    style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                )
            }
        }
    }
}

/** Restrained success state: a single scale/fade-in check (380ms) – no particles. */
@Composable
private fun CompletionCard(
    snap: FocusSnapshot,
    topic: SyllabusTopicEntity?,
    nextMinutes: Int,
    onBreak: () -> Unit,
    onNext: () -> Unit,
    onReview: () -> Unit,
) {
    val completed = snap.state == FocusState.COMPLETED
    val minutes = (snap.accumulatedMs / 60_000L).toInt()
    val appear = remember(snap.sessionId) { Animatable(0f) }
    val duration = motionDuration(Motion.success)
    LaunchedEffect(snap.sessionId) { appear.animateTo(1f, tween(duration)) }
    val c = StudINTheme.colors

    StudINCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.graphicsLayer { alpha = appear.value; scaleX = 0.8f + 0.2f * appear.value; scaleY = 0.8f + 0.2f * appear.value }) {
                StudINIconTile(if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.Flag, if (completed) c.success else MaterialTheme.colorScheme.primary, 48.dp)
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(if (completed) "Session complete" else "Session ended", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (completed) "${HomeUi.formatMinutes(minutes)} of focused study" else "${HomeUi.formatMinutes(minutes)} focused · ${snap.endReason ?: "Ended"}",
                    style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
                )
            }
        }
        if (topic != null) {
            Spacer(Modifier.height(Spacing.md))
            StudINCard(Modifier.fillMaxWidth(), containerColor = c.surfaceSecondary, border = null, contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.sm)) {
                Text(topic.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${topic.subject} · ${HomeUi.formatMinutes(topic.minutesStudied)} studied · ${FocusUi.masteryPercent(topic.mastery)}% mastery",
                    style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text("What next?", style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
        Spacer(Modifier.height(Spacing.xs))
        StudINPrimaryButton("Start next $nextMinutes-min block", onNext, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
        Spacer(Modifier.height(Spacing.xs))
        StudINRow {
            StudINSecondaryButton("Take a break", onBreak, Modifier.weight(1f), icon = Icons.Rounded.Timer)
            StudINOutlinedButton("Review cards", onReview, Modifier.weight(1f), icon = Icons.Rounded.Style)
        }
    }
}

@Composable
private fun TodayStats(sessions: List<FocusSessionEntity>) {
    val totalMin = (sessions.sumOf { it.focusedMs } / 60_000L).toInt()
    val done = sessions.count { it.status == FocusState.COMPLETED.name }
    val blocked = sessions.sumOf { it.blockedAttempts }
    StudINRow(Modifier.height(IntrinsicSize.Min)) {
        val cell = Modifier.weight(1f).fillMaxHeight()
        StudINStatCard("Focused", HomeUi.formatMinutes(totalMin), Icons.Rounded.Timer, cell, accent = StudINTheme.colors.accent)
        StudINStatCard("Completed", "$done", Icons.Rounded.CheckCircle, cell, accent = StudINTheme.colors.success)
        StudINStatCard("Blocked", "$blocked", Icons.Rounded.Shield, cell, accent = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SessionList(sessions: List<FocusSessionEntity>) {
    val fmt = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    val c = StudINTheme.colors
    StudINCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.xxs)) {
        sessions.sortedByDescending { it.startWallTime }.take(6).forEach { s ->
            val (label, container, content) = when (s.status) {
                FocusState.COMPLETED.name -> Triple("Done", c.successContainer, c.success)
                FocusState.ABORTED.name -> Triple("Ended", c.surfaceSecondary, c.textSecondary)
                else -> Triple("Live", c.accentContainer, c.onAccentContainer)
            }
            StudINListItem(
                title = s.label.ifBlank { "Deep work" },
                supporting = "${fmt.format(Date(s.startWallTime))} · ${s.focusedMs / 60_000}/${s.plannedMinutes} min" +
                    if (s.blockedAttempts > 0) " · ${s.blockedAttempts} blocked" else "",
                trailing = { StudINBadge(label, container = container, content = content) },
            )
        }
    }
}

@Composable
private fun TopicSheet(topics: List<SyllabusTopicEntity>, selectedId: Long?, onPick: (SyllabusTopicEntity) -> Unit, onDismiss: () -> Unit) {
    StudINBottomSheet(onDismiss = onDismiss, title = "Choose a topic") {
        if (topics.isEmpty()) {
            Text("No syllabus yet — add topics in Study.", style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary)
            return@StudINBottomSheet
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            topics.groupBy { it.subject }.forEach { (subject, list) ->
                item(key = "h_$subject") {
                    Text(subject, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xxs))
                }
                items(list, key = { it.id }) { t ->
                    StudINListItem(
                        title = t.title,
                        supporting = "${FocusUi.masteryPercent(t.mastery)}% mastery · ${HomeUi.formatMinutes(t.minutesStudied)}",
                        onClick = { onPick(t) },
                        modifier = Modifier.semantics { if (t.id == selectedId) stateDescription = "Selected" },
                        trailing = {
                            StudINBadge(
                                when (t.priority) { 3 -> "High"; 2 -> "Medium"; else -> "Low" },
                                container = if (t.priority >= 3) StudINTheme.colors.accentContainer else StudINTheme.colors.surfaceSecondary,
                                content = if (t.priority >= 3) StudINTheme.colors.onAccentContainer else StudINTheme.colors.textSecondary,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomDurationSheet(initial: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableIntStateOf(initial.coerceIn(FocusUi.MIN_MINUTES, FocusUi.MAX_MINUTES)) }
    StudINBottomSheet(onDismiss = onDismiss, title = "Custom duration") {
        Text(HomeUi.formatMinutes(value), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Spacer(Modifier.height(Spacing.xs))
        Slider(
            value = value.toFloat(),
            onValueChange = { value = FocusUi.snapMinutes(it) },
            valueRange = FocusUi.MIN_MINUTES.toFloat()..FocusUi.MAX_MINUTES.toFloat(),
            steps = (FocusUi.MAX_MINUTES - FocusUi.MIN_MINUTES) / 5 - 1,
            modifier = Modifier.semantics { contentDescription = "Session length" },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = StudINTheme.colors.focusTrack,
                activeTickColor = MaterialTheme.colorScheme.primary,
                inactiveTickColor = StudINTheme.colors.focusTrack,
            ),
        )
        Row {
            Text("${FocusUi.MIN_MINUTES} min", style = MaterialTheme.typography.labelSmall, color = StudINTheme.colors.textSecondary, modifier = Modifier.weight(1f))
            Text("3 h", style = MaterialTheme.typography.labelSmall, color = StudINTheme.colors.textSecondary)
        }
        Spacer(Modifier.height(Spacing.lg))
        StudINPrimaryButton("Use ${HomeUi.formatMinutes(value)}", { onConfirm(value) }, Modifier.fillMaxWidth())
    }
}
