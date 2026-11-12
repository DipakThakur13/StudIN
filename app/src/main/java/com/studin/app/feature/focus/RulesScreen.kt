package com.studin.app.feature.focus

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.designsystem.Badge
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.SectionHeader
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.domain.model.RuleMode

@Composable
fun RulesScreen(onBack: () -> Unit, vm: RulesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshGuard() }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text("App rules", style = MaterialTheme.typography.headlineMedium)
                    Text("${ui.countFor(RuleMode.BLOCK)} blocked · ${ui.countFor(RuleMode.SMART)} smart", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                }
            }
        }
        item { ModeLegend() }
        item {
            GlassCard(Modifier.fillMaxWidth(), highlight = !ui.guardEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Shield, if (ui.guardEnabled) StudinColors.Mint else Studin.extras.warning)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Smart mode needs Focus Guard", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Focus Guard is an optional accessibility service. During a session it reads only the visible video or post titles in Smart apps, classifies them on your phone and never stores or uploads them.",
                            style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(if (ui.guardEnabled) "Focus Guard on" else "Focus Guard off", if (ui.guardEnabled) StudinColors.Mint else Studin.extras.warning)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }) { Text(if (ui.guardEnabled) "Manage" else "Turn on") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Syllabus-aware filtering", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = ui.smartEnabled, onCheckedChange = vm::setSmartEnabled)
                }
            }
        }
        item {
            GhostButton("Apply suggested setup", vm::applySuggested, Modifier.fillMaxWidth(), icon = Icons.Rounded.AutoAwesome, color = MaterialTheme.colorScheme.primary)
        }
        item { KeywordSection(ui, vm) }
        item {
            SectionHeader("Installed apps")
            OutlinedTextField(
                value = ui.query, onValueChange = vm::setQuery, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps") }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = { if (ui.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Rounded.Close, "Clear search") } },
                shape = RoundedCornerShape(16.dp),
            )
        }
        if (ui.loading) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        items(ui.apps, key = { it.packageName }) { app -> AppRow(app, onMode = { vm.setMode(app, it) }) }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun ModeLegend() {
    GlassCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        LegendRow("Allow", "Never interrupted.", StudinColors.Mint)
        LegendRow("Smart", "Study content stays open; clear distractions get a break screen.", StudinColors.Cyan)
        LegendRow("Block", "A break screen during focus – you can always choose an earned or emergency break.", StudinColors.Coral)
    }
}

@Composable
private fun LegendRow(title: String, body: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Badge(title, color, Modifier.width(72.dp))
        Spacer(Modifier.width(10.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
    }
}

@Composable
private fun KeywordSection(ui: RulesUi, vm: RulesViewModel) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Your keyword lists", style = MaterialTheme.typography.titleMedium)
        Text("Channels, creators or words that should always pass or always be gated in Smart apps.",
            style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
        Spacer(Modifier.height(12.dp))
        KeywordEditor("Always allow", ui.allowKeywords, StudinColors.Mint, onAdd = { vm.addKeyword(true, it) }, onRemove = { vm.removeKeyword(true, it) })
        Spacer(Modifier.height(12.dp))
        KeywordEditor("Always gate", ui.blockKeywords, StudinColors.Coral, onAdd = { vm.addKeyword(false, it) }, onRemove = { vm.removeKeyword(false, it) })
    }
}

@Composable
private fun KeywordEditor(title: String, words: Set<String>, color: androidx.compose.ui.graphics.Color, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var text by rememberSaveable(title) { mutableStateOf("") }
    Text(title, style = MaterialTheme.typography.labelLarge, color = color)
    Spacer(Modifier.height(6.dp))
    if (words.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            words.sorted().forEach { w ->
                InputChip(selected = false, onClick = { onRemove(w) }, label = { Text(w) },
                    trailingIcon = { Icon(Icons.Rounded.Close, "Remove $w", Modifier.size(16.dp)) })
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text, onValueChange = { text = it.take(40) }, modifier = Modifier.weight(1f), singleLine = true,
            placeholder = { Text(if (color == StudinColors.Mint) "e.g. Physics Wallah" else "e.g. bigg boss") },
            shape = RoundedCornerShape(14.dp),
        )
        IconButton(onClick = { onAdd(text); text = "" }, enabled = text.trim().length >= 2, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Add, "Add to $title")
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, onMode: (RuleMode?) -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (app.icon != null) Image(app.icon, null, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
            else IconBubble(Icons.Rounded.Android, StudinColors.Cyan)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (app.emergency) {
                Icon(Icons.Rounded.Lock, null, tint = StudinColors.Mint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Badge("Always allowed", StudinColors.Mint)
            }
        }
        if (!app.emergency) {
            Spacer(Modifier.height(10.dp))
            val options = listOf(RuleMode.ALLOW to "Allow", RuleMode.SMART to "Smart", RuleMode.BLOCK to "Block")
            val current = app.mode ?: RuleMode.ALLOW
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, (mode, label) ->
                    val activeColor = when (mode) { RuleMode.ALLOW -> StudinColors.Mint; RuleMode.SMART -> StudinColors.Cyan; RuleMode.BLOCK -> StudinColors.Coral }
                    SegmentedButton(
                        selected = current == mode,
                        onClick = { onMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = activeColor.copy(alpha = 0.22f),
                            activeContentColor = MaterialTheme.colorScheme.onSurface,
                            activeBorderColor = activeColor,
                        ),
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}
