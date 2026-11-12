package com.studin.app.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------- Layout

/**
 * Standard screen frame: StudIN top bar + Material 3 Scaffold. Window insets are applied once
 * here (status bar via the top bar, navigation bar/IME via Scaffold) – the app-level navigation
 * scaffold consumes its own bar's insets before content is laid out.
 */
@Composable
fun StudINScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { StudINTopAppBar(title, subtitle = subtitle, onBack = onBack, actions = actions) },
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        content = content,
    )
}

/**
 * Content-height top bar (never clips at large font scales). Top-level screens use a 28sp title;
 * screens with [onBack] use a 22sp title next to the back button.
 */
@Composable
fun StudINTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 64.dp)
            .padding(start = if (onBack != null) Spacing.xxs else Spacing.screen, end = Spacing.xxs, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(Spacing.xxs))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = if (onBack == null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize * 0.92f),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Lazy-list padding for screen content: 20dp sides, scaffold insets added once. */
@Composable
fun screenContentPadding(scaffold: PaddingValues, top: Dp = Spacing.xs, bottom: Dp = Spacing.xxl): PaddingValues {
    val ld = androidx.compose.ui.platform.LocalLayoutDirection.current
    return PaddingValues(
        start = scaffold.calculateStartPadding(ld) + Spacing.screen,
        end = scaffold.calculateEndPadding(ld) + Spacing.screen,
        top = scaffold.calculateTopPadding() + top,
        bottom = scaffold.calculateBottomPadding() + bottom,
    )
}

/** Centres content and caps its width on tablets / landscape (no stretched phone layouts). */
fun Modifier.studinContentWidth(): Modifier = this.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = Spacing.maxContentWidth)

// ---------------------------------------------------------------- Surfaces

/** Flat card: 16dp radius, 1dp border, no elevation, content-driven height. */
@Composable
fun StudINCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = BorderStroke(1.dp, StudINTheme.colors.border),
    contentPadding: PaddingValues = PaddingValues(Spacing.card),
    enabled: Boolean = true,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Surface(
            onClick = onClick, modifier = modifier, enabled = enabled, shape = StudINShapes.card,
            color = containerColor, contentColor = contentColor, border = border,
        ) { Column(Modifier.padding(contentPadding), content = content) }
    } else {
        Surface(modifier = modifier, shape = StudINShapes.card, color = containerColor, contentColor = contentColor, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

// ---------------------------------------------------------------- Buttons

enum class StudINButtonTone { Primary, Accent }

/** The one primary action of a section. Forest by default, Accent (orange) for focus actions. */
@Composable
fun StudINPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    tone: StudINButtonTone = StudINButtonTone.Primary,
) {
    val colors = if (tone == StudINButtonTone.Accent) ButtonDefaults.buttonColors(
        containerColor = StudINTheme.colors.accent, contentColor = StudINTheme.colors.onAccent,
    ) else ButtonDefaults.buttonColors()
    Button(
        onClick = onClick, modifier = modifier.heightIn(min = Spacing.touchTarget), enabled = enabled && !loading,
        shape = StudINShapes.button, colors = colors, contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) { ButtonContent(text, icon, loading) }
}

@Composable
fun StudINSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, loading: Boolean = false) {
    FilledTonalButton(
        onClick = onClick, modifier = modifier.heightIn(min = Spacing.touchTarget), enabled = enabled && !loading, shape = StudINShapes.button,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) { ButtonContent(text, icon, loading) }
}

@Composable
fun StudINOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, contentColor: Color = MaterialTheme.colorScheme.primary) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.heightIn(min = Spacing.touchTarget), enabled = enabled, shape = StudINShapes.button,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) { ButtonContent(text, icon, false) }
}

@Composable
fun StudINTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = Spacing.touchTarget), enabled = enabled, shape = StudINShapes.button) {
        ButtonContent(text, icon, false)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, loading: Boolean) {
    if (loading) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.material3.LocalContentColor.current)
        Spacer(Modifier.width(Spacing.xs))
    } else if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.xs))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 2, textAlign = TextAlign.Center)
}

/** 48dp icon button. [tonal] gives it a container for standalone placement. */
@Composable
fun StudINIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, tonal: Boolean = false, enabled: Boolean = true, tint: Color? = null) {
    if (tonal) {
        FilledTonalIconButton(onClick = onClick, modifier = modifier.size(Spacing.touchTarget), enabled = enabled, shape = StudINShapes.button,
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = StudINTheme.colors.surfaceSecondary)) {
            Icon(icon, contentDescription, tint = tint ?: MaterialTheme.colorScheme.onSurface)
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier, enabled = enabled) { Icon(icon, contentDescription, tint = tint ?: androidx.compose.material3.LocalContentColor.current) }
    }
}

// ---------------------------------------------------------------- Text & headers

@Composable
fun StudINSectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null, supporting: String? = null) {
    Row(modifier.fillMaxWidth().padding(bottom = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
        }
        if (action != null && onAction != null) StudINTextButton(action, onAction)
    }
}

/** Small status label with 8dp radius. Always pair colour with text (non-colour-only status). */
@Composable
fun StudINBadge(text: String, modifier: Modifier = Modifier, container: Color = StudINTheme.colors.surfaceSecondary, content: Color = MaterialTheme.colorScheme.onSurface, icon: ImageVector? = null) {
    Row(
        modifier.background(container, StudINShapes.badge).padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(14.dp), tint = content); Spacer(Modifier.width(Spacing.xxs)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}

// ---------------------------------------------------------------- Chips & segmented

/** Selectable chip (filter / option). Selected = forest container. */
@Composable
fun StudINFilterChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    FilterChip(
        selected = selected, onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 40.dp),
        label = { Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
        leadingIcon = when {
            selected -> ({ Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) })
            icon != null -> ({ Icon(icon, null, Modifier.size(18.dp)) })
            else -> null
        },
        shape = StudINShapes.button,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = enabled, selected = selected, borderColor = StudINTheme.colors.border, selectedBorderColor = Color.Transparent),
    )
}

/** Non-selectable informational chip. */
@Composable
fun StudINChip(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val c = @Composable {
        Row(Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(Spacing.xxs + 2.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (onClick != null) Surface(onClick = onClick, modifier = modifier.heightIn(min = 40.dp), shape = StudINShapes.button,
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, StudINTheme.colors.border)) { Box(contentAlignment = Alignment.Center) { c() } }
    else Surface(modifier = modifier, shape = StudINShapes.button, color = StudINTheme.colors.surfaceSecondary) { c() }
}

/** Segmented tabs (2–4 options) – used for screen sub-sections and time filters. */
@Composable
fun StudINSegmentedTabs(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, icons: List<ImageVector>? = null) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, label ->
            SegmentedButton(
                selected = i == selectedIndex, onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size, StudINShapes.button),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary, activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface, activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveBorderColor = StudINTheme.colors.border,
                ),
                icon = { if (icons != null && i == selectedIndex) Icon(icons[i], null, Modifier.size(18.dp)) else if (icons != null) Icon(icons[i], null, Modifier.size(18.dp)) },
                modifier = Modifier.heightIn(min = Spacing.touchTarget),
            ) { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

// ---------------------------------------------------------------- Progress

/** Rounded linear progress with accessible range semantics. */
@Composable
fun StudINLinearProgress(progress: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, trackColor: Color = StudINTheme.colors.focusTrack, height: Dp = 8.dp, label: String? = null) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(motionDuration(Motion.card)), label = "linear")
    LinearProgressIndicator(
        progress = { p },
        modifier = modifier.fillMaxWidth().height(height).semantics {
            if (label != null) contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
        },
        color = color, trackColor = trackColor, strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
    )
}

/** Circular progress ring (timer, daily goal). Flat, no glow. */
@Composable
fun StudINProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    stroke: Dp = 10.dp,
    color: Color = StudINTheme.colors.accent,
    trackColor: Color = StudINTheme.colors.focusTrack,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(motionDuration(Motion.screen)), label = "ring")
    Box(modifier.size(size).semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arc = Size(this.size.width - s, this.size.height - s)
            drawArc(trackColor, 0f, 360f, false, topLeft = Offset(s / 2, s / 2), size = arc, style = Stroke(s))
            if (p > 0f) drawArc(color, -90f, 360f * p, false, topLeft = Offset(s / 2, s / 2), size = arc, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

// ---------------------------------------------------------------- Metrics

/** Compact metric card used in 2/4-column grids. */
@Composable
fun StudINStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary, supporting: String? = null, onClick: (() -> Unit)? = null) {
    StudINCard(modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" + (supporting?.let { ", $it" } ?: "") }, onClick = onClick,
        contentPadding = PaddingValues(Spacing.sm + 2.dp)) {
        StudINIconTile(icon, accent, size = 32.dp)
        Spacer(Modifier.height(Spacing.xs))
        Text(value, style = StudINType.statValue, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary, maxLines = 2)
        if (supporting != null) Text(supporting, style = MaterialTheme.typography.labelMedium, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Square icon tile (12dp radius) used to anchor list rows and cards. */
@Composable
fun StudINIconTile(icon: ImageVector, accent: Color = MaterialTheme.colorScheme.primary, size: Dp = 40.dp, container: Color = accent.copy(alpha = 0.12f)) {
    Box(Modifier.size(size).background(container, StudINShapes.smallCard), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(size * 0.55f))
    }
}

/** Settings-style list row. */
@Composable
fun StudINListItem(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val row = @Composable {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Spacing.card, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { StudINIconTile(icon, iconTint, 36.dp); Spacer(Modifier.width(Spacing.sm)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
            }
            if (trailing != null) { Spacer(Modifier.width(Spacing.xs)); trailing() }
        }
    }
    if (onClick != null) Surface(onClick = onClick, modifier = modifier, color = Color.Transparent) { row() } else Box(modifier) { row() }
}

// ---------------------------------------------------------------- States

@Composable
fun StudINEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    illustration: Int? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = Spacing.xl, horizontal = Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            illustration != null -> StudINIllustration(illustration, contentDescription = null, height = 140.dp)
            icon != null -> StudINIconTile(icon, size = 56.dp)
        }
        Spacer(Modifier.height(Spacing.md))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.xxs))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.md))
            StudINPrimaryButton(actionLabel, onAction)
        }
    }
}

@Composable
fun StudINErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    StudINCard(modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.errorContainer, border = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(Spacing.sm))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
            if (onRetry != null) TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun StudINLoadingState(modifier: Modifier = Modifier, message: String? = null) {
    Column(modifier.fillMaxWidth().padding(Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
        if (message != null) { Spacer(Modifier.height(Spacing.sm)); Text(message, style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary) }
    }
}

// ---------------------------------------------------------------- Inputs & overlays

@Composable
fun StudINTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 8,
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(), enabled = enabled,
        label = label?.let { { Text(it) } }, placeholder = placeholder?.let { { Text(it, color = StudINTheme.colors.textSecondary) } },
        singleLine = singleLine, minLines = minLines, maxLines = maxLines, isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = trailingIcon, leadingIcon = leadingIcon, shape = StudINShapes.input,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = StudINTheme.colors.border, focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
fun StudINDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = "Cancel",
    icon: ImageVector? = null,
    destructive: Boolean = false,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let { { Icon(it, null) } },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = text,
        shape = StudINShapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onConfirm, colors = if (destructive) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.textButtonColors()) {
                Text(confirmLabel, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = dismissLabel?.let { { TextButton(onClick = onDismiss) { Text(it, style = MaterialTheme.typography.labelLarge) } } },
    )
}

@Composable
fun StudINBottomSheet(onDismiss: () -> Unit, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = StudINShapes.sheet, containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(bottom = Spacing.xl)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(Spacing.sm))
            }
            content()
        }
    }
}

/** Two equally weighted children with standard spacing. */
@Composable
fun StudINRow(modifier: Modifier = Modifier, spacing: Dp = Spacing.sm, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing), content = content)
}

/** Minimum-size modifier for custom clickable elements (WCAG target size). */
fun Modifier.minTouchTarget(): Modifier = this.sizeIn(minWidth = Spacing.touchTarget, minHeight = Spacing.touchTarget)

@Suppress("unused")
private val ShapeCircle = CircleShape
