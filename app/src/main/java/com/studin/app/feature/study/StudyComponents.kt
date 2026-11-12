package com.studin.app.feature.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studin.app.core.data.repo.StudyRepository
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINShapes
import com.studin.app.core.designsystem.StudINTextField
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.StudINIconButton

/** Space a list needs at its end so the last item clears an extended FAB (56dp + 16dp margin + 16dp air). */
internal val FabClearance = 88.dp

/** Deck accent taken from theme tokens (stored deck colours are mapped, never drawn raw). */
@Composable
internal fun deckAccent(storedColor: Long): Color {
    val c = StudINTheme.colors
    val slots = listOf(MaterialTheme.colorScheme.primary, c.success, c.ai, c.accent, c.warning, MaterialTheme.colorScheme.secondary)
    return slots[deckAccentIndex(storedColor, StudyRepository.DECK_COLORS)]
}

/**
 * Sticky bottom action area for inner screens: sits above the navigation bar and the keyboard,
 * separated from content by a hairline.
 */
@Composable
internal fun BottomActionBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars).imePadding()) {
            HorizontalDivider(color = StudINTheme.colors.border)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                content = content,
            )
        }
    }
}

data class MenuAction(val label: String, val icon: ImageVector, val destructive: Boolean = false, val onClick: () -> Unit)

/** Overflow (⋮) button with a dropdown of actions. */
@Composable
internal fun OverflowMenu(actions: List<MenuAction>, contentDescription: String = "More options") {
    var open by remember { mutableStateOf(false) }
    Box {
        StudINIconButton(Icons.Rounded.MoreVert, contentDescription, { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = StudINShapes.smallCard, containerColor = MaterialTheme.colorScheme.surface) {
            actions.forEach { a ->
                val tint = if (a.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                DropdownMenuItem(
                    text = { Text(a.label, color = tint) },
                    leadingIcon = { Icon(a.icon, null, tint = tint) },
                    onClick = { open = false; a.onClick() },
                )
            }
        }
    }
}

/** Dialog with one or two text fields; confirm is disabled until the required fields are filled. */
@Composable
internal fun TextInputDialog(
    title: String,
    firstLabel: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    initialFirst: String = "",
    secondLabel: String? = null,
    initialSecond: String = "",
    singleLine: Boolean = true,
    message: String? = null,
) {
    var first by rememberSaveable { mutableStateOf(initialFirst) }
    var second by rememberSaveable { mutableStateOf(initialSecond) }
    val canConfirm = first.isNotBlank() && (secondLabel == null || second.isNotBlank())
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = StudINShapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                if (message != null) {
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary)
                    Spacer(Modifier.height(Spacing.sm))
                }
                StudINTextField(
                    value = first, onValueChange = { if (it.length <= 1000) first = it }, label = firstLabel,
                    singleLine = singleLine, minLines = if (singleLine) 1 else 2, maxLines = if (singleLine) 1 else 6,
                )
                if (secondLabel != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    StudINTextField(
                        value = second, onValueChange = { if (it.length <= 1000) second = it }, label = secondLabel,
                        singleLine = singleLine, minLines = if (singleLine) 1 else 2, maxLines = if (singleLine) 1 else 6,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(first.trim(), second.trim()) }, enabled = canConfirm) {
                Text(confirmText, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", style = MaterialTheme.typography.labelLarge) } },
    )
}

/** Destructive confirmation (delete deck / topic / card). */
@Composable
internal fun ConfirmDialog(title: String, message: String, confirmText: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = StudINShapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(confirmText, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", style = MaterialTheme.typography.labelLarge) } },
    )
}

/** Minimum comfortable width per button (in dp at 1.0× font scale) before a group stacks vertically. */
private val MinButtonWidth = 156.dp

/**
 * Equal-weight button group that stays on one row when there is room and stacks full-width
 * on compact widths or large font scales, so labels never squeeze onto two lines.
 */
@Composable
internal fun AdaptiveButtonGroup(vararg buttons: @Composable (Modifier) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        val perButton = maxWidth / buttons.size
        if (perButton / fontScale >= MinButtonWidth) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                buttons.forEach { b -> b(Modifier.weight(1f)) }
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                buttons.forEach { b -> b(Modifier.fillMaxWidth()) }
            }
        }
    }
}

/** Small bordered container used for samples / previews inside cards. */
@Composable
internal fun InsetBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(), shape = StudINShapes.smallCard, color = StudINTheme.colors.surfaceSecondary,
        border = BorderStroke(1.dp, StudINTheme.colors.border),
    ) { Box(Modifier.padding(Spacing.sm)) { content() } }
}
