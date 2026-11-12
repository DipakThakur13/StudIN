package com.studin.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.designsystem.Badge
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.ProgressRing
import com.studin.app.core.designsystem.SectionHeader
import com.studin.app.core.designsystem.StatTile
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.ui.navigation.BottomBarPadding
import com.studin.app.ui.navigation.Routes
import com.studin.domain.model.FocusState
import com.studin.domain.plan.PlanBlock
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun HomeScreen(onNavigate: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = BottomBarPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "top") { Enter(0) { TopRow(s.name) { onNavigate(Routes.SETTINGS) } } }
        item(key = "hero") { Enter(1) { HeroCard(s) } }
        item(key = "session") {
            AnimatedVisibility(snap.state.isRunning, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                val rem = snap.remainingMs(now)
                GlassCard(Modifier.fillMaxWidth(), highlight = true, onClick = { onNavigate(Routes.FOCUS) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(snap.progress(now), size = 52.dp, stroke = 6.dp, glow = false) {
                            Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (snap.state == FocusState.PAUSED) "Session paused" else "Focus session running", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${snap.label.ifBlank { "Deep focus" }} · ${formatClock(rem)} left",
                                style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item(key = "today") { Enter(2) { TodayCard(s) } }
        item(key = "tiles") {
            Enter(3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (s.streaksEnabled) StatTile("Day streak", "${s.streak}", Icons.Rounded.LocalFireDepartment, Modifier.weight(1f), StudinColors.Star,
                        caption = if (s.streak == 0) "Start today" else "Keep it warm")
                    StatTile("Cards due", "${s.dueCards}", Icons.Rounded.Style, Modifier.weight(1f), StudinColors.Cyan,
                        caption = if (s.dueCards > 0) "Review now" else "All caught up")
                    StatTile("Total cards", "${s.totalCards}", Icons.Rounded.MenuBook, Modifier.weight(1f), StudinColors.Violet)
                }
            }
        }
        item(key = "planHeader") { Enter(4) { SectionHeader("Today's plan", action = "Ask AI", onAction = { onNavigate(Routes.AI) }) } }
        if (s.loaded && s.plan.isEmpty()) {
            item(key = "planEmpty") {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Goal reached for today 🎉", style = MaterialTheme.typography.titleMedium)
                    Text("Rest well – recovery is part of learning.", color = Studin.extras.muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        itemsIndexed(s.plan, key = { i, b -> "plan$i${b.title}" }) { i, block ->
            Enter(5 + i) {
                PlanRow(block) {
                    if (block.kind == PlanBlock.Kind.RECALL) onNavigate(Routes.review())
                    else { vm.startBlock(block); onNavigate(Routes.FOCUS) }
                }
            }
        }
        item(key = "qaHeader") { SectionHeader("Quick actions") }
        item(key = "qa") {
            val actions = listOf(
                QuickAction("Start focus", Icons.Rounded.Bolt, StudinColors.Mint, Routes.FOCUS),
                QuickAction("Review", Icons.Rounded.Replay, StudinColors.Cyan, Routes.review()),
                QuickAction("Quiz", Icons.Rounded.Quiz, StudinColors.Violet, Routes.quiz()),
                QuickAction("Scan notes", Icons.Rounded.DocumentScanner, StudinColors.Star, Routes.generate()),
                QuickAction("Ask AI", Icons.Rounded.AutoAwesome, StudinColors.Teal, Routes.AI),
                QuickAction("Rules", Icons.Rounded.Shield, StudinColors.Coral, Routes.RULES),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                actions.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { a ->
                            GlassCard(Modifier.weight(1f), onClick = { onNavigate(a.route) }, contentPadding = 14.dp) {
                                IconBubble(a.icon, a.color, 40.dp)
                                Spacer(Modifier.height(10.dp))
                                Text(a.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class QuickAction(val label: String, val icon: ImageVector, val color: Color, val route: String)

@Composable
private fun Enter(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60L * index.coerceAtMost(8)); shown = true }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(420), label = "a")
    val offset by animateFloatAsState(if (shown) 0f else 40f, tween(480), label = "o")
    Box(Modifier.graphicsLayer { this.alpha = alpha; translationY = offset }) { content() }
}

@Composable
private fun TopRow(name: String, onSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greet = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..21 -> "Good evening"; else -> "Late night study" }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greet + ",", style = MaterialTheme.typography.bodyLarge, color = Studin.extras.muted)
            Text(name.ifBlank { "Student" }, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalIconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
    }
}

@Composable
private fun HeroCard(s: HomeState) {
    val progress = if (s.goalMinutes > 0) s.todayMinutes.toFloat() / s.goalMinutes else 0f
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Studin.extras.heroGradient).padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.exam?.displayName ?: "Your exam", style = MaterialTheme.typography.titleMedium, color = StudinColors.Navy950.copy(alpha = 0.8f))
                    Text(
                        when {
                            s.daysToExam == null -> "Set exam date"
                            s.daysToExam!! < 0 -> "Exam done!"
                            s.daysToExam == 0L -> "Exam day 🍀"
                            else -> "D-${s.daysToExam}"
                        },
                        style = MaterialTheme.typography.displaySmall, color = StudinColors.Navy950, fontWeight = FontWeight.Black,
                    )
                }
                Box(Modifier.size(64.dp).clip(CircleShape).background(StudinColors.Navy950.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.EventAvailable, null, tint = StudinColors.Navy950, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "hero")
            Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(StudinColors.Navy950.copy(alpha = 0.15f))) {
                Box(Modifier.fillMaxWidth(p).height(10.dp).clip(CircleShape).background(StudinColors.Navy950.copy(alpha = 0.75f)))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${s.todayMinutes} of ${s.goalMinutes} min focused today",
                style = MaterialTheme.typography.labelLarge, color = StudinColors.Navy950.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun TodayCard(s: HomeState) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val progress = if (s.goalMinutes > 0) s.todayMinutes.toFloat() / s.goalMinutes else 0f
            ProgressRing(progress, size = 120.dp, stroke = 12.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${s.todayMinutes}", style = MaterialTheme.typography.headlineMedium)
                    Text("min", style = MaterialTheme.typography.labelMedium, color = Studin.extras.muted)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Today's focus", style = MaterialTheme.typography.titleMedium)
                Text("${(progress * 100).toInt().coerceAtMost(999)}% of goal · ${s.todaySessions} sessions",
                    style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted)
                Spacer(Modifier.height(10.dp))
                MiniWeek(s.weekMinutes, s.goalMinutes)
            }
        }
    }
}

@Composable
private fun MiniWeek(minutes: List<Int>, goal: Int) {
    val max = (minutes.maxOrNull() ?: 0).coerceAtLeast(goal).coerceAtLeast(1)
    Row(Modifier.height(44.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        minutes.forEachIndexed { i, m ->
            val h by animateFloatAsState(m.toFloat() / max, tween(700 + i * 60), label = "w")
            Box(
                Modifier.width(12.dp).fillMaxHeight(h.coerceAtLeast(0.06f)).clip(RoundedCornerShape(4.dp))
                    .background(
                        if (i == minutes.lastIndex) Studin.extras.heroGradient
                        else androidx.compose.ui.graphics.Brush.verticalGradient(listOf(StudinColors.Cyan.copy(alpha = 0.7f), StudinColors.Violet.copy(alpha = 0.5f)))
                    )
            )
        }
    }
}

@Composable
private fun PlanRow(block: PlanBlock, onStart: () -> Unit) {
    val (icon, color) = when (block.kind) {
        PlanBlock.Kind.LEARN -> Icons.Rounded.MenuBook to StudinColors.Mint
        PlanBlock.Kind.REVISE -> Icons.Rounded.Replay to StudinColors.Cyan
        PlanBlock.Kind.RECALL -> Icons.Rounded.Style to StudinColors.Violet
        PlanBlock.Kind.RECOVERY -> Icons.Rounded.Healing to StudinColors.Star
    }
    GlassCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon, color, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(block.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    Badge("${block.minutes}m", color)
                }
                Text(block.why, style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onStart) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Studin.extras.heroGradient), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Start ${block.title}", tint = StudinColors.Navy950)
                }
            }
        }
    }
}

internal fun formatClock(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

