@file:Suppress("DEPRECATION")

package com.studin.app.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * V1 component API kept only so screens compile during the V2 migration.
 * Each shim renders with V2 tokens. Delete once no screen references them.
 */

@Deprecated("Screens get their background from the scaffold") @Composable
fun GradientBackground(modifier: Modifier = Modifier, @Suppress("UNUSED_PARAMETER") animate: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), content = content)
}

@Deprecated("Use StudINCard") @Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") shape: Shape = StudINShapes.card,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Spacing.card,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) = StudINCard(modifier, onClick = onClick, contentPadding = PaddingValues(contentPadding),
    border = BorderStroke(1.dp, if (highlight) MaterialTheme.colorScheme.primary else StudINTheme.colors.border), content = content)

@Deprecated("Use StudINProgressRing") @Composable
fun ProgressRing(
    progress: Float, modifier: Modifier = Modifier, size: Dp = 120.dp, stroke: Dp = 10.dp,
    @Suppress("UNUSED_PARAMETER") colors: List<Color> = emptyList(), trackColor: Color = StudINTheme.colors.focusTrack,
    @Suppress("UNUSED_PARAMETER") glow: Boolean = false, content: @Composable BoxScope.() -> Unit = {},
) = StudINProgressRing(progress, modifier, size, stroke, StudINTheme.colors.accent, trackColor, content)

@Deprecated("Use StudINPrimaryButton") @Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, @Suppress("UNUSED_PARAMETER") brush: Brush? = null) =
    StudINPrimaryButton(text, onClick, modifier, icon, enabled)

@Deprecated("Use StudINOutlinedButton") @Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = MaterialTheme.colorScheme.primary) =
    StudINOutlinedButton(text, onClick, modifier, icon, contentColor = color)

@Deprecated("Use StudINSectionHeader") @Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) =
    StudINSectionHeader(title, modifier, action, onAction)

@Deprecated("Use StudINStatCard") @Composable
fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary, caption: String? = null) =
    StudINStatCard(label, value, icon, modifier, accent, caption)

@Deprecated("Use StudINIconTile") @Composable
fun IconBubble(icon: ImageVector, accent: Color, size: Dp = 40.dp) = StudINIconTile(icon, accent, size)

@Deprecated("Use StudINFilterChip") @Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) =
    StudINFilterChip(text, selected, onClick, modifier, icon)

@Deprecated("Use StudINBadge") @Composable
fun Badge(text: String, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) =
    StudINBadge(text, modifier, container = color.copy(alpha = 0.12f), content = color)

@Deprecated("Use StudINEmptyState") @Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier) {
        StudINEmptyState(title, message, icon = icon)
        if (action != null) action()
    }
}

@Deprecated("Use StudINLinearProgress") @Composable
fun GradientProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 8.dp, color: Color? = null) =
    StudINLinearProgress(progress, modifier, color ?: MaterialTheme.colorScheme.primary, height = height)

@Deprecated("Use StudINLogo") @Composable
fun StudinLogo(modifier: Modifier = Modifier, size: Dp = 120.dp, @Suppress("UNUSED_PARAMETER") pulse: Boolean = false) = StudINLogo(modifier, size)

@Deprecated("Use StudINRow") @Composable
fun TwoColumn(modifier: Modifier = Modifier, spacing: Dp = Spacing.sm, content: @Composable RowScope.() -> Unit) = StudINRow(modifier, spacing, content)

@Deprecated("Use StudINLinearProgress with a label") @Composable
fun LabeledProgress(label: String, value: Float, trailing: String, modifier: Modifier = Modifier, color: Color? = null) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = StudINTheme.colors.textSecondary)
        }
        Spacer(Modifier.height(6.dp))
        StudINLinearProgress(value, color = color ?: MaterialTheme.colorScheme.primary, label = "$label $trailing")
    }
}

@Deprecated("Use StudINLoadingState") @Composable
fun LinearLoading(modifier: Modifier = Modifier) = StudINLinearProgress(0.5f, modifier)
