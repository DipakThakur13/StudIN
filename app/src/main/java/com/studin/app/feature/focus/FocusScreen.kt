package com.studin.app.feature.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Topic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.data.db.FocusSessionEntity
import com.studin.app.core.data.db.SyllabusTopicEntity
import com.studin.app.core.designsystem.Badge
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.MonoNumbers
import com.studin.app.core.designsystem.Pill
import com.studin.app.core.designsystem.ProgressRing
import com.studin.app.core.designsystem.SectionHeader
import com.studin.app.core.designsystem.StatTile
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.core.designsystem.TwoColumn
import com.studin.app.platform.focus.FocusOutcome
import com.studin.app.ui.navigation.BottomBarPadding
import com.studin.app.ui.navigation.Routes
import com.studin.domain.focus.FocusSnapshot
import com.studin.domain.model.FocusState
import java.text.DateFormat
import java.util.Date

private val PRESETS = listOf(15, 25, 50, 90)

@Composable
fun FocusScreen(onNavigate: (String) -> Unit, vm: FocusViewModel = hiltViewModel()) {
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val setup by vm.setup.collectAsStateWithLifecycle()
    val topics by vm.topics.collectAsStateWithLifecycle()
    val sessions by vm.todaySessions.collectAsStateWithLifecycle()
    val guardMissing by vm.guardMissing.collectAsStateWithLifecycle()

    var celebrate by remember { mutableStateOf<FocusOutcome.Completed?>(null) }
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    var topicSheet by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshPermissions() }
    LaunchedEffect(Unit) {
        vm.outcomes.collect { if (it is FocusOutcome.Completed) celebrate = it }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Focus", style = MaterialTheme.typography.headlineLarge)
                    Text("Protect your study time – on your terms", style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted)
                }
                FilledTonalIconButton(onClick = { onNavigate(Routes.RULES) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Tune, contentDescription = "App rules")
                }
            }
            Spacer(Modifier.height(14.dp))

            AnimatedVisibility(guardMissing) {
                GlassCard(Modifier.fillMaxWidth().padding(bottom = 14.dp), onClick = { onNavigate(Routes.PERMISSIONS) }, contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.Shield, Studin.extras.warning)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Timer works now – blocking needs a permission", style = MaterialTheme.typography.titleSmall)
                            Text("Grant Usage access or Focus Guard so StudIN can pause distracting apps.", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                        }
                        Icon(Icons.Rounded.ChevronRight, null)
                    }
                }
            }

            TimerHero(snap, now, setup.minutes)
            Spacer(Modifier.height(18.dp))

            AnimatedContent(
                targetState = when {
                    snap.state.isRunning -> 1
                    snap.state == FocusState.COMPLETED || snap.state == FocusState.ABORTED -> 2
                    else -> 0
                },
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
                label = "controls",
            ) { mode ->
                when (mode) {
                    0 -> SetupControls(
                        minutes = setup.minutes, topic = setup.topic,
                        onMinutes = vm::setMinutes, onPickTopic = { topicSheet = true },
                        onClearTopic = { vm.setTopic(null) }, onStart = vm::start,
                    )
                    1 -> RunningControls(snap, onPause = vm::pause, onResume = vm::resume, onFinish = vm::finish, onStop = { confirmStop = true })
                    else -> SummaryCard(snap, onDone = vm::dismiss)
                }
            }

            Spacer(Modifier.height(22.dp))
            TodaySection(sessions)
            Spacer(Modifier.height(14.dp))
            GlassCard(Modifier.fillMaxWidth(), onClick = { onNavigate(Routes.RULES) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Block, StudinColors.Coral)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("App rules", style = MaterialTheme.typography.titleMedium)
                        Text("Choose Allow, Smart (syllabus-aware) or Block per app. Emergency apps always work.",
                            style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                    }
                    Icon(Icons.Rounded.ChevronRight, null)
                }
            }
            Spacer(Modifier.height(BottomBarPadding))
        }

        celebrate?.let { c ->
            ConfettiBurst(trigger = c.sessionId, onDone = { celebrate = null })
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("End this session?") },
            text = { Text("Your focused minutes so far are still saved. You can always start again.") },
            confirmButton = { TextButton(onClick = { confirmStop = false; vm.stop() }) { Text("End session") } },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Keep going") } },
        )
    }

    if (topicSheet) {
        TopicSheet(topics, onPick = { vm.setTopic(it); topicSheet = false }, onDismiss = { topicSheet = false })
    }
}

@Composable
private fun TimerHero(snap: FocusSnapshot, now: Long, setupMinutes: Int) {
    val running = snap.state.isRunning
    val remaining = if (running || snap.state == FocusState.COMPLETED || snap.state == FocusState.ABORTED) snap.remainingMs(now) else setupMinutes * 60_000L
    val progress = if (running) snap.progress(now) else if (snap.state == FocusState.COMPLETED) 1f else 0f
    val pulse = rememberInfiniteTransition(label = "pulse")
    val breathe by pulse.animateFloat(1f, if (snap.state == FocusState.ACTIVE) 1.025f else 1f, infiniteRepeatable(tween(2_000), RepeatMode.Reverse), label = "b")
    val totalSec = (remaining + 999) / 1000
    val timeText = "%02d:%02d".format(totalSec / 60, totalSec % 60)
    val stateLabel = when (snap.state) {
        FocusState.IDLE -> "Ready when you are"
        FocusState.PREPARING -> "Getting ready…"
        FocusState.ACTIVE -> "Focusing"
        FocusState.PAUSED -> "Paused"
        FocusState.COMPLETED -> "Session complete"
        FocusState.ABORTED -> "Session ended"
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ProgressRing(progress, Modifier.scale(breathe), size = 264.dp, stroke = 18.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    timeText,
                    style = MonoNumbers.copy(fontSize = 56.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "${totalSec / 60} minutes ${totalSec % 60} seconds remaining" },
                )
                Spacer(Modifier.height(4.dp))
                Badge(stateLabel, color = if (snap.state == FocusState.PAUSED) Studin.extras.warning else MaterialTheme.colorScheme.primary)
                if (running && snap.label.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(snap.label, style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.width(180.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SetupControls(
    minutes: Int, topic: SyllabusTopicEntity?, onMinutes: (Int) -> Unit,
    onPickTopic: () -> Unit, onClearTopic: () -> Unit, onStart: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PRESETS.forEach { p -> Pill("$p min", selected = minutes == p, onClick = { onMinutes(p) }, icon = Icons.Rounded.Timer) }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Custom", style = MaterialTheme.typography.labelLarge, color = Studin.extras.muted)
            Slider(
                value = minutes.toFloat(), onValueChange = { onMinutes((it / 5).toInt() * 5) }, valueRange = 5f..180f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).semantics { contentDescription = "Session length $minutes minutes" },
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
            )
            Text("$minutes m", style = MonoNumbers)
        }
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth(), onClick = onPickTopic, contentPadding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.Topic, StudinColors.Cyan)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(topic?.title ?: "Pick a syllabus topic (optional)", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(topic?.let { "${it.subject} · minutes count towards mastery" } ?: "Track progress per chapter",
                        style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                }
                if (topic != null) TextButton(onClick = onClearTopic) { Text("Clear") } else Icon(Icons.Rounded.ChevronRight, null)
            }
        }
        Spacer(Modifier.height(16.dp))
        GradientButton("Start $minutes-minute focus", onStart, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
        Spacer(Modifier.height(8.dp))
        Text("You can pause or end any time. Emergency calls and settings are never blocked.",
            style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RunningControls(snap: FocusSnapshot, onPause: () -> Unit, onResume: () -> Unit, onFinish: () -> Unit, onStop: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (snap.state == FocusState.PAUSED) GradientButton("Resume", onResume, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
        else GradientButton("Pause", onPause, Modifier.fillMaxWidth(), icon = Icons.Rounded.Pause)
        Spacer(Modifier.height(10.dp))
        TwoColumn {
            GhostButton("Finish early", onFinish, Modifier.weight(1f), icon = Icons.Rounded.Flag)
            GhostButton("End", onStop, Modifier.weight(1f), icon = Icons.Rounded.Stop, color = StudinColors.Coral)
        }
        if (snap.pauseCount > 0) {
            Spacer(Modifier.height(8.dp))
            Text("Paused ${snap.pauseCount}× – breaks are part of studying well.", style = MaterialTheme.typography.bodySmall,
                color = Studin.extras.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SummaryCard(snap: FocusSnapshot, onDone: () -> Unit) {
    val minutes = (snap.accumulatedMs / 60_000L).toInt()
    val completed = snap.state == FocusState.COMPLETED
    GlassCard(Modifier.fillMaxWidth(), highlight = completed) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(if (completed) Icons.Rounded.EmojiEvents else Icons.Rounded.CheckCircle, if (completed) Studin.extras.star else StudinColors.Cyan, 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (completed) "Brilliant – $minutes minutes of deep focus!" else "$minutes focused minutes saved",
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    if (completed) "Lock it in: review a few flashcards while it's fresh." else (snap.endReason ?: "Every minute counts."),
                    style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        GradientButton("New session", onDone, Modifier.fillMaxWidth())
    }
}

@Composable
private fun TodaySection(sessions: List<FocusSessionEntity>) {
    val totalMin = sessions.sumOf { it.focusedMs } / 60_000L
    val done = sessions.count { it.status == FocusState.COMPLETED.name }
    val blocked = sessions.sumOf { it.blockedAttempts }
    SectionHeader("Today")
    TwoColumn {
        StatTile("Focused", "${totalMin}m", Icons.Rounded.Timer, Modifier.weight(1f))
        StatTile("Completed", "$done", Icons.Rounded.CheckCircle, Modifier.weight(1f), accent = StudinColors.Cyan)
        StatTile("Distractions", "$blocked", Icons.Rounded.Shield, Modifier.weight(1f), accent = StudinColors.Violet)
    }
    if (sessions.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
            val fmt = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
            sessions.sortedByDescending { it.startWallTime }.take(6).forEachIndexed { i, s ->
                if (i > 0) Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (label, color) = when (s.status) {
                        FocusState.COMPLETED.name -> "Done" to StudinColors.Mint
                        FocusState.ABORTED.name -> "Ended" to StudinColors.Coral
                        else -> "Live" to StudinColors.Star
                    }
                    Text(fmt.format(Date(s.startWallTime)), style = MonoNumbers, modifier = Modifier.width(76.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${s.focusedMs / 60_000}/${s.plannedMinutes} min" + if (s.blockedAttempts > 0) " · ${s.blockedAttempts} blocked" else "",
                            style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                    }
                    Badge(label, color)
                }
            }
        }
    }
}

@Composable
private fun TopicSheet(topics: List<SyllabusTopicEntity>, onPick: (SyllabusTopicEntity) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Text("Choose a topic", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        if (topics.isEmpty()) {
            Text("No syllabus yet – set your exam in onboarding or add topics in Study.",
                modifier = Modifier.padding(20.dp), color = Studin.extras.muted)
        }
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
            topics.groupBy { it.subject }.forEach { (subject, list) ->
                item(key = "h_$subject") {
                    Text(subject.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp))
                }
                items(list, key = { it.id }) { t ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                            .then(Modifier)
                            .padding(vertical = 2.dp),
                    ) {
                        GlassCard(Modifier.fillMaxWidth(), onClick = { onPick(t) }, contentPadding = 12.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.bodyLarge)
                                    Text("${(t.mastery * 100).toInt()}% mastery · ${t.minutesStudied} min", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                                }
                                Badge(when (t.priority) { 3 -> "High"; 2 -> "Med"; else -> "Low" },
                                    color = when (t.priority) { 3 -> StudinColors.Coral; 2 -> StudinColors.Star; else -> StudinColors.Cyan })
                            }
                        }
                    }
                }
            }
        }
    }
}
