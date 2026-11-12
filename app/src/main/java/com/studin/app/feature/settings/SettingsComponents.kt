package com.studin.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINListItem
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINTheme

/** A titled group of setting rows inside one card. Rows are separated by inset dividers. */
@Composable
internal fun SettingsSection(title: String, modifier: Modifier = Modifier, supporting: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        StudINSectionHeader(title, supporting = supporting)
        StudINCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xxs), content = content)
    }
}

@Composable
internal fun RowDivider() = HorizontalDivider(color = StudINTheme.colors.border, modifier = Modifier.padding(start = 64.dp, end = Spacing.card))

/** Tappable row that opens something (chevron trailing unless [value] is given). */
@Composable
internal fun NavRow(
    icon: ImageVector,
    title: String,
    supporting: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    value: String? = null,
    onClick: () -> Unit,
) = StudINListItem(
    title = title, supporting = supporting, icon = icon, iconTint = tint, onClick = onClick,
    trailing = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StudINTheme.colors.textSecondary)
        }
    },
)

/** On/off row; the whole row is one toggleable target (announced as a switch). */
@Composable
internal fun SwitchRow(icon: ImageVector, title: String, supporting: String?, checked: Boolean, onChange: (Boolean) -> Unit, tint: Color = MaterialTheme.colorScheme.primary) {
    StudINListItem(
        title = title, supporting = supporting, icon = icon, iconTint = tint,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        trailing = { Switch(checked = checked, onCheckedChange = null) },
    )
}

/**
 * Labelled slider row. The slider gets its own ≥48dp row and an accessible name + value, so it is
 * usable with TalkBack and large fonts.
 */
@Composable
internal fun SliderRow(
    icon: ImageVector,
    title: String,
    valueLabel: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    supporting: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    onChange: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        StudINListItem(
            title = title, supporting = supporting, icon = icon, iconTint = tint,
            trailing = { Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) },
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange((((it - range.first) / step).toInt() * step + range.first).coerceIn(range.first, range.last)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            // Tick marks only when there are few positions; otherwise they become visual noise.
            steps = ((range.last - range.first) / step - 1).coerceAtLeast(0).let { if (it > 10) 0 else it },
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget).padding(start = 64.dp, end = Spacing.card)
                .semantics { contentDescription = title; stateDescription = valueLabel },
        )
        Spacer(Modifier.height(Spacing.xxs))
    }
}
