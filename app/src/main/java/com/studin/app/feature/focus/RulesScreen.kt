package com.studin.app.feature.focus

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINBadge
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINIconButton
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINLoadingState
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINSecondaryButton
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINShapes
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINTextField
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.domain.model.RuleMode

/**
 * @param onOpenPermissions opens Permission health (prominent disclosure first). When null the
 * system Accessibility settings are opened directly – the card above already explains what
 * Focus Guard reads.
 */
@Composable
fun RulesScreen(onBack: () -> Unit, onOpenPermissions: (() -> Unit)? = null, vm: RulesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshGuard() }
    val openGuard: () -> Unit = onOpenPermissions ?: {
        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        Unit
    }

    StudINScaffold(
        title = "App rules",
        subtitle = "${ui.countFor(RuleMode.BLOCK)} blocked · ${ui.countFor(RuleMode.SMART)} smart",
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            Modifier.studinContentWidth().fillMaxSize(),
            contentPadding = screenContentPadding(padding, top = Spacing.xxs),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "guard") { GuardCard(ui, onOpen = openGuard, onSmart = vm::setSmartEnabled) }
            item(key = "legend") { ModeLegend() }
            item(key = "suggest") {
                StudINSecondaryButton("Apply suggested setup", vm::applySuggested, Modifier.fillMaxWidth(), icon = Icons.Rounded.AutoAwesome)
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    "Sets common social, video and game apps to Smart or Block. Only installed apps are changed.",
                    style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                )
            }
            item(key = "keywords") { KeywordSection(ui, vm) }
            item(key = "appsHeader") {
                Spacer(Modifier.height(Spacing.sm))
                StudINSectionHeader("Installed apps", supporting = "Emergency and system apps are always allowed")
                StudINTextField(
                    value = ui.query,
                    onValueChange = vm::setQuery,
                    placeholder = "Search apps",
                    imeAction = ImeAction.Search,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = if (ui.query.isNotEmpty()) ({ StudINIconButton(Icons.Rounded.Close, "Clear search", { vm.setQuery("") }) }) else null,
                )
            }
            if (ui.loading) item(key = "loading") { StudINLoadingState(message = "Loading installed apps") }
            else if (ui.apps.isEmpty()) item(key = "none") {
                Text(
                    if (ui.query.isBlank()) "No launchable apps found." else "No apps match “${ui.query}”.",
                    style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg), textAlign = TextAlign.Center,
                )
            }
            items(ui.apps, key = { it.packageName }) { app -> AppRow(app, onMode = { vm.setMode(app, it) }) }
        }
    }
}

@Composable
private fun GuardCard(ui: RulesUi, onOpen: () -> Unit, onSmart: (Boolean) -> Unit) {
    val c = StudINTheme.colors
    StudINCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINIconTile(Icons.Rounded.Shield, if (ui.guardEnabled) c.success else MaterialTheme.colorScheme.primary, 40.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text("Focus Guard", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            StudINBadge(
                if (ui.guardEnabled) "On" else "Off",
                container = if (ui.guardEnabled) c.successContainer else c.surfaceSecondary,
                content = if (ui.guardEnabled) c.success else c.textSecondary,
                icon = if (ui.guardEnabled) Icons.Rounded.CheckCircle else null,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Smart mode needs Focus Guard, an optional accessibility service. During a session it reads only the visible video or post titles in Smart apps, classifies them on your phone and never stores or uploads them.",
            style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
        )
        Spacer(Modifier.height(Spacing.xs))
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Syllabus-aware filtering", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = ui.smartEnabled, onCheckedChange = onSmart,
                modifier = Modifier.semantics { contentDescription = "Syllabus-aware filtering" },
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        StudINOutlinedButton(if (ui.guardEnabled) "Manage Focus Guard" else "Turn on Focus Guard", onOpen, Modifier.fillMaxWidth(), icon = Icons.Rounded.Shield)
    }
}

@Composable
private fun ModeLegend() {
    StudINCard(Modifier.fillMaxWidth(), containerColor = StudINTheme.colors.surfaceSecondary, border = null) {
        Text("How rules work", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Spacing.xs))
        LegendRow("Allow", "Never interrupted.")
        LegendRow("Smart", "Study content stays open; clear distractions get a break screen.")
        LegendRow("Block", "Break screen during focus — an earned or emergency break is always available.")
    }
}

@Composable
private fun LegendRow(title: String, body: String) {
    Row(Modifier.padding(vertical = Spacing.xxs), verticalAlignment = Alignment.Top) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(56.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
    }
}

@Composable
private fun KeywordSection(ui: RulesUi, vm: RulesViewModel) {
    StudINCard(Modifier.fillMaxWidth()) {
        Text("Keyword lists", style = MaterialTheme.typography.titleMedium)
        Text(
            "Channels, creators or words that should always pass or always get the break screen in Smart apps.",
            style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.md))
        KeywordEditor("Always allow", "e.g. Physics Wallah", ui.allowKeywords, Icons.Rounded.CheckCircle, StudINTheme.colors.success,
            onAdd = { vm.addKeyword(true, it) }, onRemove = { vm.removeKeyword(true, it) })
        Spacer(Modifier.height(Spacing.md))
        KeywordEditor("Always gate", "e.g. bigg boss", ui.blockKeywords, Icons.Rounded.Block, MaterialTheme.colorScheme.error,
            onAdd = { vm.addKeyword(false, it) }, onRemove = { vm.removeKeyword(false, it) })
    }
}

@Composable
private fun KeywordEditor(title: String, hint: String, words: Set<String>, icon: ImageVector, tint: Color, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var text by rememberSaveable(title) { mutableStateOf("") }
    val canAdd = text.trim().length >= 2
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.xs))
        Text(title, style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(Spacing.xs))
    if (words.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            words.sorted().forEach { w -> RemovableChip(w, onRemove = { onRemove(w) }) }
        }
        Spacer(Modifier.height(Spacing.xs))
    }
    StudINTextField(
        value = text,
        onValueChange = { text = it.take(40) },
        placeholder = hint,
        imeAction = ImeAction.Done,
        onImeAction = { if (canAdd) { onAdd(text); text = "" } },
        trailingIcon = { StudINIconButton(Icons.Rounded.Add, "Add to $title", { onAdd(text); text = "" }, enabled = canAdd) },
    )
}

/** Keyword chip with a 48dp remove target. */
@Composable
private fun RemovableChip(text: String, onRemove: () -> Unit) {
    Surface(shape = StudINShapes.button, color = StudINTheme.colors.surfaceSecondary) {
        Row(Modifier.padding(start = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            StudINIconButton(Icons.Rounded.Close, "Remove $text", onRemove, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, onMode: (RuleMode?) -> Unit) {
    val c = StudINTheme.colors
    StudINCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (app.icon != null) Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp).clip(StudINShapes.smallCard))
            else StudINIconTile(Icons.Rounded.Android, MaterialTheme.colorScheme.primary, 40.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (app.emergency) StudINBadge("Always allowed", container = c.successContainer, content = c.success, icon = Icons.Rounded.Lock)
        }
        if (!app.emergency) {
            Spacer(Modifier.height(Spacing.sm))
            ModeSelector(app.label, app.mode ?: RuleMode.ALLOW, onMode)
        }
    }
}

/** Compact three-way Allow / Smart / Block control (radio semantics, 44dp+ targets). */
@Composable
private fun ModeSelector(appLabel: String, current: RuleMode, onMode: (RuleMode) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val options = listOf(RuleMode.ALLOW to "Allow", RuleMode.SMART to "Smart", RuleMode.BLOCK to "Block")
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        options.forEach { (mode, label) ->
            val selected = current == mode
            val selectedColor = if (mode == RuleMode.BLOCK) scheme.error else scheme.primary
            val onSelected = if (mode == RuleMode.BLOCK) scheme.onError else scheme.onPrimary
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onMode(mode) })
                    .semantics { contentDescription = "$label $appLabel" },
                shape = StudINShapes.button,
                color = if (selected) selectedColor else scheme.surface,
                contentColor = if (selected) onSelected else scheme.onSurface,
                border = if (selected) null else BorderStroke(1.dp, StudINTheme.colors.border),
            ) {
                Row(Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (selected) { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(Spacing.xxs)) }
                    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}
