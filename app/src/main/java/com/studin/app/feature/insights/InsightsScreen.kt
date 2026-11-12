package com.studin.app.feature.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.LabeledProgress
import com.studin.app.core.designsystem.ProgressRing
import com.studin.app.core.designsystem.SectionHeader
import com.studin.app.core.designsystem.StatTile
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.ui.navigation.BottomBarPadding
import com.studin.app.ui.navigation.Routes
import java.time.LocalDate

@Composable
fun InsightsScreen(onNavigate: (String) -> Unit, vm: InsightsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = BottomBarPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.padding(vertical = 4.dp)) {
                Text("Insights", style = MaterialTheme.typography.headlineMedium)
                Text("Evidence of progress – computed on your phone.", style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted)
            }
        }
        item { WeekHero(s) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Sessions", "${s.weekSessions}", Icons.Rounded.Schedule, Modifier.weight(1f), StudinColors.Cyan, caption = "this week")
                StatTile("Completed", "${(s.completionRate * 100).toInt()}%", Icons.Rounded.CheckCircle, Modifier.weight(1f), StudinColors.Mint)
                StatTile("Blocked", "${s.blockedAttempts}", Icons.Rounded.Block, Modifier.weight(1f), StudinColors.Coral, caption = "attempts")
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionHeader("Last 7 days")
                WeekChart(s.week, s.goalMinutes)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionHeader("12-week consistency")
                Heatmap(s.heatmap, s.heatmapStartDay, s.goalMinutes)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Less", style = MaterialTheme.typography.labelSmall, color = Studin.extras.muted)
                    Spacer(Modifier.width(6.dp))
                    listOf(0.08f, 0.3f, 0.55f, 0.8f, 1f).forEach {
                        Box(Modifier.padding(horizontal = 2.dp).size(12.dp).clip(RoundedCornerShape(3.dp)).background(heatColor(it)))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("More", style = MaterialTheme.typography.labelSmall, color = Studin.extras.muted)
                    Spacer(Modifier.weight(1f))
                    if (s.streaksEnabled) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = StudinColors.Star, modifier = Modifier.size(16.dp))
                        Text(" ${s.currentStreak} now · best ${s.bestStreak}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(Modifier.weight(1f), onClick = { onNavigate(Routes.quiz()) }) {
                    Text("Recall accuracy", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    ProgressRing(s.reviewAccuracy ?: 0f, Modifier.align(Alignment.CenterHorizontally), size = 96.dp, stroke = 10.dp) {
                        Text(s.reviewAccuracy?.let { "${(it * 100).toInt()}%" } ?: "—", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${s.reviewsThisWeek} answers in 7 days", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                }
                GlassCard(Modifier.weight(1f), onClick = { onNavigate(Routes.review()) }) {
                    Text("Review backlog", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    IconBubble(Icons.Rounded.Style, if (s.dueCards > 30) StudinColors.Star else StudinColors.Cyan, 56.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("${s.dueCards}", style = MaterialTheme.typography.headlineMedium)
                    Text(if (s.dueCards == 0) "All caught up" else "cards due – tap to review", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionHeader("Subject mastery")
                if (s.subjects.isEmpty()) Text("Add syllabus topics in Study to see mastery.", color = Studin.extras.muted)
                s.subjects.forEachIndexed { i, sub ->
                    LabeledProgress(
                        sub.subject, sub.mastery, "${(sub.mastery * 100).toInt()}% · ${sub.minutes / 60}h",
                        Modifier.padding(vertical = 6.dp),
                        color = listOf(StudinColors.Mint, StudinColors.Cyan, StudinColors.Violet, StudinColors.Star, StudinColors.Teal, StudinColors.Coral)[i % 6],
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Mastery grows with focused minutes on a topic and correct recall.", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Shield, StudinColors.Violet)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Focus Guard this week", style = MaterialTheme.typography.titleMedium)
                        Text("${s.guardDecisions} titles checked on-device · ${s.guardBlocks} gated", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                    }
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Info, StudinColors.Cyan, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("How we measure", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Focus minutes are time inside StudIN focus sessions – a signal of effort, not proof of learning.",
                    "Recall accuracy counts your own flashcard and quiz answers; easy decks inflate it.",
                    "Blocked attempts and app usage depend on Android's Usage Access / Accessibility data, which can be approximate or delayed on some phones.",
                    "Everything is calculated locally. Nothing is uploaded.",
                ).forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun WeekHero(s: InsightsState) {
    val hours = s.weekFocusMinutes / 60f
    val delta = s.weekFocusMinutes - s.lastWeekFocusMinutes
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Studin.extras.heroGradient).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Focused this week", style = MaterialTheme.typography.titleMedium, color = StudinColors.Navy950.copy(alpha = 0.75f))
                Text("%.1f h".format(hours), style = MaterialTheme.typography.displaySmall, color = StudinColors.Navy950)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (delta >= 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown, null, tint = StudinColors.Navy950, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (s.lastWeekFocusMinutes == 0 && delta >= 0) "First week tracked – great start"
                        else "${if (delta >= 0) "+" else ""}$delta min vs last week",
                        style = MaterialTheme.typography.labelLarge, color = StudinColors.Navy950.copy(alpha = 0.8f),
                    )
                }
            }
            Icon(Icons.Rounded.EmojiEvents, null, tint = StudinColors.Navy950.copy(alpha = 0.8f), modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun WeekChart(days: List<DayBar>, goal: Int) {
    if (days.isEmpty()) return
    val anim = remember { Animatable(0f) }
    LaunchedEffect(days) { anim.snapTo(0f); anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val max = (days.maxOf { it.minutes }).coerceAtLeast(goal).coerceAtLeast(30).toFloat()
    val muted = Studin.extras.muted
    val desc = days.joinToString { "${it.label} ${it.minutes} minutes" }
    Column(Modifier.semantics { contentDescription = "Focus minutes per day: $desc" }) {
        Box(Modifier.fillMaxWidth().height(160.dp)) {
            Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                val slot = size.width / days.size
                val barW = slot * 0.52f
                days.forEachIndexed { i, d ->
                    val h = size.height * (d.minutes / max) * anim.value
                    val x = i * slot + (slot - barW) / 2
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.05f), topLeft = Offset(x, 0f), size = Size(barW, size.height),
                        cornerRadius = CornerRadius(barW / 2, barW / 2),
                    )
                    if (h > 0f) drawRoundRect(
                        brush = Brush.verticalGradient(
                            if (d.isToday) listOf(StudinColors.Mint, StudinColors.Teal) else listOf(StudinColors.Cyan, StudinColors.Violet.copy(alpha = 0.7f)),
                            startY = size.height - h, endY = size.height,
                        ),
                        topLeft = Offset(x, size.height - h), size = Size(barW, h), cornerRadius = CornerRadius(barW / 2, barW / 2),
                    )
                }
                val gy = size.height * (1 - goal / max)
                drawLine(
                    StudinColors.Star.copy(alpha = 0.8f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEach {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${it.minutes}", style = MaterialTheme.typography.labelSmall, color = if (it.isToday) MaterialTheme.colorScheme.primary else muted)
                    Text(it.label, style = MaterialTheme.typography.labelSmall, color = if (it.isToday) MaterialTheme.colorScheme.primary else muted, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Dashed line = daily goal ($goal min)", style = MaterialTheme.typography.labelSmall, color = muted)
    }
}

private fun heatColor(f: Float): Color =
    if (f <= 0f) Color.White.copy(alpha = 0.06f) else StudinColors.Mint.copy(alpha = (0.18f + 0.82f * f.coerceIn(0f, 1f)))

@Composable
private fun Heatmap(days: List<Int>, startDay: Long, goal: Int) {
    if (days.isEmpty()) return
    // Columns are weeks, rows Mon..Sun.
    val firstDow = LocalDate.ofEpochDay(startDay).dayOfWeek.value - 1 // 0 = Monday
    val cells = List(firstDow) { -1 } + days
    val weeks = cells.chunked(7)
    val anim = remember { Animatable(0f) }
    LaunchedEffect(days) { anim.snapTo(0f); anim.animateTo(1f, tween(1000)) }
    val activeDays = days.count { it > 0 }
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(weeks.size / 7f).semantics { contentDescription = "Studied on $activeDays of the last ${days.size} days" }
    ) {
        val gap = 3.dp.toPx()
        val cell = (size.width - gap * (weeks.size - 1)) / weeks.size
        weeks.forEachIndexed { w, week ->
            week.forEachIndexed { d, minutes ->
                if (minutes < 0) return@forEachIndexed
                val order = (w * 7 + d).toFloat() / cells.size
                val visible = anim.value >= order
                val f = if (minutes == 0) 0f else (minutes.toFloat() / goal).coerceIn(0.1f, 1f)
                drawRoundRect(
                    if (visible) heatColor(f) else Color.Transparent,
                    topLeft = Offset(w * (cell + gap), d * (cell + gap)), size = Size(cell, cell), cornerRadius = CornerRadius(cell * 0.25f),
                )
            }
        }
    }
}
