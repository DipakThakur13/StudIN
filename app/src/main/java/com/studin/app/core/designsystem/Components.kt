package com.studin.app.core.designsystem

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studin.app.R

/** Full-screen brand background with slowly drifting glow orbs. */
@Composable
fun GradientBackground(modifier: Modifier = Modifier, animate: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val ex = Studin.extras
    val t = rememberInfiniteTransition(label = "orbs")
    val drift by if (animate) t.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    else remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    Box(modifier.fillMaxSize().background(ex.backgroundGradient)) {
        Canvas(Modifier.fillMaxSize().blur(80.dp).alpha(if (ex.isDark) 0.55f else 0.35f)) {
            val w = size.width; val h = size.height
            drawCircle(Brush.radialGradient(listOf(StudinColors.Mint.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(w * (0.15f + 0.2f * drift), h * 0.12f), radius = w * 0.6f),
                radius = w * 0.6f, center = Offset(w * (0.15f + 0.2f * drift), h * 0.12f))
            drawCircle(Brush.radialGradient(listOf(StudinColors.Violet.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(w * (0.9f - 0.2f * drift), h * 0.55f), radius = w * 0.55f),
                radius = w * 0.55f, center = Offset(w * (0.9f - 0.2f * drift), h * 0.55f))
            drawCircle(Brush.radialGradient(listOf(StudinColors.Cyan.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(w * 0.3f, h * (0.95f - 0.1f * drift)), radius = w * 0.5f),
                radius = w * 0.5f, center = Offset(w * 0.3f, h * (0.95f - 0.1f * drift)))
        }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        ) { content() }
    }
}

/** Frosted glass card. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ex = Studin.extras
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "press")
    val border = if (highlight) BorderStroke(1.2.dp, Brush.linearGradient(listOf(StudinColors.Mint.copy(alpha = 0.9f), StudinColors.Cyan.copy(alpha = 0.4f))))
    else BorderStroke(1.dp, ex.glassBorder)
    Surface(
        modifier = modifier.scale(scale).then(
            if (onClick != null) Modifier.clip(shape).clickable(interaction, null, role = Role.Button, onClick = onClick) else Modifier
        ),
        shape = shape,
        color = ex.glass,
        border = border,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(contentPadding).animateContentSize(), content = content)
    }
}

/** Animated circular progress with gradient sweep and glow. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    stroke: Dp = 16.dp,
    colors: List<Color> = listOf(StudinColors.Mint, StudinColors.Cyan, StudinColors.Violet, StudinColors.Mint),
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    glow: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (glow) {
            Canvas(Modifier.fillMaxSize().blur(18.dp).alpha(0.6f)) {
                val s = stroke.toPx()
                drawArc(Brush.sweepGradient(colors), -90f, 360f * animated, false,
                    topLeft = Offset(s, s), size = androidx.compose.ui.geometry.Size(this.size.width - 2 * s, this.size.height - 2 * s),
                    style = Stroke(s * 1.6f, cap = StrokeCap.Round))
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - s, this.size.height - s)
            drawArc(trackColor, 0f, 360f, false, topLeft = Offset(s / 2, s / 2), size = arcSize, style = Stroke(s))
            drawArc(Brush.sweepGradient(colors), -90f, 360f * animated, false, topLeft = Offset(s / 2, s / 2), size = arcSize,
                style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Primary call-to-action with brand gradient. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    brush: Brush = Studin.extras.heroGradient,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "btn")
    Box(
        modifier
            .scale(scale)
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) brush else Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.3f))))
            .clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) { Icon(icon, null, tint = StudinColors.Navy950); Spacer(Modifier.width(8.dp)) }
            Text(text, color = StudinColors.Navy950, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Secondary outlined pill button. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(
        onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp),
        color = Color.Transparent, border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(8.dp))
        }
    }
}

/** Compact metric tile with icon bubble. */
@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    caption: String? = null,
) {
    GlassCard(modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" }, contentPadding = 14.dp) {
        IconBubble(icon, accent, 36.dp)
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Studin.extras.muted, maxLines = 1)
        if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = accent, maxLines = 1)
    }
}

@Composable
fun IconBubble(icon: ImageVector, accent: Color, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = accent, modifier = Modifier.size(size * 0.55f)) }
}

/** Small selectable pill. */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, modifier = modifier, shape = CircleShape,
        color = if (selected) c.primary else Studin.extras.glass,
        border = BorderStroke(1.dp, if (selected) c.primary else Studin.extras.glassBorder),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, Modifier.size(16.dp), tint = if (selected) c.onPrimary else c.onSurface); Spacer(Modifier.width(6.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) c.onPrimary else c.onSurface)
        }
    }
}

/** Tiny status label, e.g. "AI", "Beta", "Granted". */
@Composable
fun Badge(text: String, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), modifier = modifier.clip(CircleShape).background(color.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconBubble(icon, MaterialTheme.colorScheme.primary, 72.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted, textAlign = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(18.dp)); action() }
    }
}

@Composable
fun GradientProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 8.dp, color: Color? = null) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))) {
        Box(
            Modifier.fillMaxWidth(p).height(height).clip(CircleShape)
                .background(if (color != null) Brush.linearGradient(listOf(color, color)) else Studin.extras.heroGradient)
        )
    }
}

/** App logo image with a soft pulsing glow. */
@Composable
fun StudinLogo(modifier: Modifier = Modifier, size: Dp = 120.dp, pulse: Boolean = true) {
    val t = rememberInfiniteTransition(label = "logo")
    val glow by if (pulse) t.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "glow")
    else remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    Box(modifier.size(size * 1.25f), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).alpha(glow).blur(28.dp)
                .drawBehind { drawCircle(Brush.radialGradient(listOf(StudinColors.Mint, Color.Transparent))) }
        )
        Image(painterResource(R.drawable.studin_app_icon), contentDescription = "StudIN logo", modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.22f)))
    }
}

/** Row helper for two equally weighted children. */
@Composable
fun TwoColumn(modifier: Modifier = Modifier, spacing: Dp = 12.dp, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing), content = content)
}

@Composable
fun LabeledProgress(label: String, value: Float, trailing: String, modifier: Modifier = Modifier, color: Color? = null) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = Studin.extras.muted)
        }
        Spacer(Modifier.height(6.dp))
        GradientProgressBar(value, color = color)
    }
}

@Suppress("unused")
private fun Modifier.glassBorder(color: Color, shape: Shape) = this.border(1.dp, color, shape)

@Composable
fun LinearLoading(modifier: Modifier = Modifier) {
    LinearProgressIndicator(modifier.fillMaxWidth().clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = Studin.extras.glass)
}
