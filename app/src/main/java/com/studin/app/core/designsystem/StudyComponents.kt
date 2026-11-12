package com.studin.app.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studin.app.R

// ---------------------------------------------------------------- Brand

/** The StudIN "rising sun book" mark (vector, crisp at every size). */
@Composable
fun StudINLogo(modifier: Modifier = Modifier, size: Dp = 40.dp, contentDescription: String? = "StudIN") {
    Image(painterResource(R.drawable.ic_studin_mark), contentDescription = contentDescription, modifier = modifier.size(size))
}

/** Mark + wordmark ("Stud" forest, "IN" orange). */
@Composable
fun StudINWordmark(modifier: Modifier = Modifier, markSize: Dp = 32.dp) {
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "StudIN" }, verticalAlignment = Alignment.CenterVertically) {
        StudINLogo(size = markSize, contentDescription = null)
        Spacer(Modifier.width(Spacing.xs))
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.primary)); append("Stud"); pop()
                pushStyle(androidx.compose.ui.text.SpanStyle(color = StudINTheme.colors.accent)); append("IN"); pop()
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        )
    }
}

/**
 * Vector illustration on a soft theme-coloured backdrop. The art itself has no background so it
 * works in light and dark themes. Decorative by default ([contentDescription] = null).
 */
@Composable
fun StudINIllustration(@DrawableRes res: Int, contentDescription: String?, modifier: Modifier = Modifier, height: Dp = 200.dp, backdrop: Boolean = true) {
    val backdropColor = StudINTheme.colors.surfaceSecondary
    Box(
        modifier.fillMaxWidth().height(height).then(
            if (backdrop) Modifier.drawBehind {
                val d = size.minDimension * 0.92f
                drawCircle(backdropColor, radius = d / 2, center = Offset(size.width / 2, size.height / 2))
            } else Modifier
        ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(res), contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize().aspectRatio(4f / 3f, matchHeightConstraintsFirst = true),
        )
    }
}

// ---------------------------------------------------------------- Home

/** Compact forest hero: exam, days left and today's focus progress (gradient permitted, §1.4). */
@Composable
fun StudINExamCountdownCard(
    examName: String,
    daysLeft: Long?,
    focusedMinutesToday: Int,
    goalMinutes: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = StudINTheme.colors
    val progress = if (goalMinutes > 0) focusedMinutesToday / goalMinutes.toFloat() else 0f
    val daysText = when {
        daysLeft == null -> "Set your exam date"
        daysLeft < 0 -> "Exam date passed"
        daysLeft == 0L -> "Exam day – you've got this"
        else -> "D-$daysLeft"
    }
    Surface(
        modifier = modifier.fillMaxWidth().clip(StudINShapes.card).then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = "$examName. ${daysLeft?.let { if (it >= 0) "$it days left" else "Exam date passed" } ?: "No exam date set"}. $focusedMinutesToday of $goalMinutes minutes focused today" },
        shape = StudINShapes.card, color = Color.Transparent,
    ) {
        Column(Modifier.background(c.heroGradient).padding(Spacing.card)) {
            Text(examName, style = MaterialTheme.typography.labelLarge, color = c.onHero.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(Spacing.xxs))
            Text(daysText, style = if (daysLeft != null && daysLeft > 0) StudINType.heroValue else MaterialTheme.typography.titleLarge, color = c.onHero)
            Spacer(Modifier.height(Spacing.sm))
            StudINLinearProgress(progress, color = c.accent, trackColor = c.onHero.copy(alpha = 0.18f), height = 6.dp)
            Spacer(Modifier.height(Spacing.xs))
            Row {
                Text("$focusedMinutesToday of $goalMinutes min focused today", style = MaterialTheme.typography.bodySmall, color = c.onHero.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                Text("${(progress * 100).toInt().coerceIn(0, 999)}%", style = MaterialTheme.typography.labelMedium, color = c.onHero)
            }
        }
    }
}

/** A single study task (subject · chapter · duration · state · action). */
@Composable
fun StudINStudyTaskCard(
    title: String,
    subject: String,
    minutes: Int,
    modifier: Modifier = Modifier,
    kindLabel: String? = null,
    reason: String? = null,
    done: Boolean = false,
    icon: ImageVector = Icons.Rounded.Schedule,
    accent: Color = MaterialTheme.colorScheme.primary,
    actionLabel: String = "Start",
    onAction: (() -> Unit)? = null,
) {
    StudINCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = Spacing.card, vertical = Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINIconTile(if (done) Icons.Rounded.CheckCircle else icon, if (done) StudINTheme.colors.success else accent, 40.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(subject.takeIf { it.isNotBlank() }, "$minutes min", kindLabel).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                )
                if (reason != null) Text(reason, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (onAction != null && !done) {
                Spacer(Modifier.width(Spacing.xs))
                StudINIconButton(Icons.Rounded.PlayArrow, "$actionLabel: $title", onAction, tonal = true, tint = MaterialTheme.colorScheme.primary)
            } else if (done) {
                StudINBadge("Done", container = StudINTheme.colors.successContainer, content = StudINTheme.colors.success)
            }
        }
    }
}

// ---------------------------------------------------------------- Focus

/**
 * Circular focus timer. The ring scales with available width (min 200dp, max 300dp) so it stays
 * readable on compact phones and doesn't balloon on tablets.
 */
@Composable
fun StudINFocusTimer(
    progress: Float,
    remainingText: String,
    stateLabel: String,
    modifier: Modifier = Modifier,
    topic: String? = null,
    active: Boolean = false,
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val ring = (maxWidth * 0.72f).coerceIn(200.dp, 300.dp)
        StudINProgressRing(
            progress, size = ring, stroke = if (ring > 240.dp) 14.dp else 12.dp,
            color = if (active) StudINTheme.colors.accent else MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$stateLabel. $remainingText remaining" + (topic?.let { ". Topic $it" } ?: "") },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Spacing.xl)) {
                Text(stateLabel, style = MaterialTheme.typography.labelLarge, color = StudINTheme.colors.textSecondary, textAlign = TextAlign.Center)
                Text(remainingText, style = StudINType.timer, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                if (topic != null) {
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(topic, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Insights

data class ChartBar(val label: String, val value: Int, val accessibilityLabel: String = label, val highlight: Boolean = false)

/**
 * Accessible bar chart with zero baseline, y-axis labels, optional goal line and selection.
 * Scale = max(goal, max value) rounded up, so bars are never exaggerated.
 */
@Composable
fun StudINInsightChart(
    bars: List<ChartBar>,
    modifier: Modifier = Modifier,
    goal: Int? = null,
    unit: String = "min",
    selectedIndex: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
    chartHeight: Dp = 160.dp,
) {
    val c = StudINTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val maxValue = maxOf(goal ?: 0, bars.maxOfOrNull { it.value } ?: 0, 1)
    val step = niceStep(maxValue)
    val top = ((maxValue + step - 1) / step) * step
    val grid = c.border
    val goalColor = c.accent
    val anim by animateFloatAsState(1f, tween(motionDuration(Motion.screen)), label = "bars")
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(chartHeight)) {
            // Y axis labels
            Column(Modifier.width(36.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                listOf(top, top / 2, 0).forEach { Text("$it", style = MaterialTheme.typography.labelSmall, color = c.textSecondary) }
            }
            Spacer(Modifier.width(Spacing.xs))
            Box(Modifier.weight(1f).fillMaxSize()) {
                Canvas(Modifier.fillMaxSize()) {
                    val h = size.height
                    listOf(0f, 0.5f, 1f).forEach { f -> drawLine(grid, Offset(0f, h * f), Offset(size.width, h * f), strokeWidth = 1.dp.toPx()) }
                    if (goal != null && goal > 0) {
                        val y = h - h * (goal / top.toFloat())
                        drawLine(goalColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                    }
                }
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    bars.forEachIndexed { i, b ->
                        val selected = selectedIndex == i
                        val frac = (b.value / top.toFloat()) * anim
                        Box(
                            Modifier.weight(1f).fillMaxSize()
                                .then(if (onSelect != null) Modifier.clickable(role = Role.Button) { onSelect(i) } else Modifier)
                                .semantics { contentDescription = "${b.accessibilityLabel}: ${b.value} $unit"; this.selected = selected },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Canvas(Modifier.fillMaxWidth(0.56f).fillMaxSize()) {
                                val bh = size.height * frac.coerceIn(0f, 1f)
                                val color = when { selected -> goalColor; b.highlight -> primary; b.value == 0 -> grid; else -> primary.copy(alpha = 0.75f) }
                                drawRoundRect(color, topLeft = Offset(0f, size.height - maxOf(bh, 3.dp.toPx())), size = Size(size.width, maxOf(bh, 3.dp.toPx())), cornerRadius = CornerRadius(6.dp.toPx()))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(Modifier.fillMaxWidth().padding(start = 44.dp)) {
            bars.forEachIndexed { i, b ->
                Text(b.label, style = MaterialTheme.typography.labelSmall, color = if (selectedIndex == i) MaterialTheme.colorScheme.onSurface else c.textSecondary,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

/** 1-2-5 step so axis labels are round numbers. */
fun niceStep(max: Int): Int {
    val raw = maxOf(1, max / 2)
    var mag = 1
    while (mag * 10 <= raw) mag *= 10
    return listOf(1, 2, 5, 10).map { it * mag }.first { it >= raw }
}

// ---------------------------------------------------------------- Flashcard

/** Flashcard with accessible reveal; flip is a restrained 3D rotation (instant with reduced motion). */
@Composable
fun StudINFlashcard(front: String, back: String, revealed: Boolean, onFlip: () -> Unit, modifier: Modifier = Modifier, label: String = "Question") {
    val rotation by animateFloatAsState(if (revealed) 180f else 0f, tween(motionDuration(Motion.screen)), label = "flip")
    val showBack = rotation > 90f
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 220.dp)
            .graphicsLayer { rotationY = rotation; cameraDistance = 14f * density }
            .clip(StudINShapes.card)
            .clickable(role = Role.Button, onClickLabel = if (revealed) "Show question" else "Reveal answer", onClick = onFlip)
            .semantics { stateDescription = if (revealed) "Answer shown" else "Answer hidden" },
        shape = StudINShapes.card, color = if (showBack) StudINTheme.colors.surfaceSecondary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, StudINTheme.colors.border),
    ) {
        Column(
            Modifier.graphicsLayer { rotationY = if (showBack) 180f else 0f }.padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StudINBadge(if (showBack) "Answer" else label, container = if (showBack) StudINTheme.colors.successContainer else StudINTheme.colors.surfaceSecondary,
                content = if (showBack) StudINTheme.colors.success else StudINTheme.colors.textSecondary)
            Spacer(Modifier.height(Spacing.lg))
            Text(if (showBack) back else front, style = if (showBack) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal) else MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(Spacing.lg))
            Text(if (showBack) "Tap to see the question" else "Tap to reveal the answer", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
        }
    }
}

// ---------------------------------------------------------------- Permissions

/** Explains one permission: what, why, status (icon + text, not colour only) and action. */
@Composable
fun StudINPermissionExplanation(
    icon: ImageVector,
    title: String,
    why: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    footnote: String? = null,
) {
    val c = StudINTheme.colors
    StudINCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINIconTile(icon, if (granted) c.success else MaterialTheme.colorScheme.primary, 40.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            StudINBadge(
                if (granted) "Allowed" else "Off",
                container = if (granted) c.successContainer else c.surfaceSecondary,
                content = if (granted) c.success else c.textSecondary,
                icon = if (granted) Icons.Rounded.CheckCircle else null,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(why, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        if (footnote != null) { Spacer(Modifier.height(Spacing.xxs)); Text(footnote, style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }
        Spacer(Modifier.height(Spacing.sm))
        if (granted) StudINOutlinedButton(actionLabel, onAction, Modifier.fillMaxWidth()) else StudINPrimaryButton(actionLabel, onAction, Modifier.fillMaxWidth())
    }
}

@Suppress("unused")
private fun Modifier.selectable(onClick: () -> Unit) = this.semantics { onClick { onClick(); true } }
