package com.studin.app.feature.study

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.ai.AiEngine
import com.studin.app.ai.LlmState
import com.studin.app.core.designsystem.Badge
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.LinearLoading
import com.studin.app.core.designsystem.Pill
import com.studin.app.core.designsystem.SectionHeader
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.ui.navigation.Routes

@Composable
fun GenerateCardsScreen(deckId: Long?, onNavigate: (String) -> Unit, onBack: () -> Unit, vm: GenerateCardsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val llm by vm.llmState.collectAsStateWithLifecycle()
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.scan(uri) }
    var editing by remember { mutableStateOf<DraftCard?>(null) }
    val ex = Studin.extras

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        StudyTopBar("AI flashcards", onBack, subtitle = "100% on-device · nothing leaves your phone")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            EngineCard(llm)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = s.notes, onValueChange = vm::onNotes,
                label = { Text("Your notes") },
                placeholder = { Text("Paste or type notes, e.g. \"Photosynthesis is the process by which…\"") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                supportingText = { Text("${s.notes.length} / ${GenerateCardsViewModel.MAX_NOTES}") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(
                    if (s.scanning) "Reading photo…" else "Scan notes photo",
                    { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    Modifier.weight(1f), icon = Icons.Rounded.DocumentScanner,
                )
                GhostButton("Paste", { clipboard.getText()?.text?.let(vm::appendNotes) }, Modifier.weight(0.6f), icon = Icons.Rounded.ContentPaste)
            }
            if (s.scanning) { Spacer(Modifier.height(10.dp)); LinearLoading() }

            AnimatedVisibility(s.error != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(14.dp))
                        .background(ex.danger.copy(alpha = 0.12f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = ex.danger)
                    Spacer(Modifier.width(8.dp))
                    Text(s.error ?: "", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(14.dp))

            AnimatedContent(
                targetState = when { s.generating -> 1; s.drafts != null -> 2; else -> 0 },
                transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "gen",
            ) { phase ->
                when (phase) {
                    0 -> GradientButton(
                        "Generate flashcards", vm::generate, Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.AutoAwesome, enabled = s.notes.isNotBlank() && !s.scanning,
                    )
                    1 -> ShimmerPlaceholder()
                    else -> Drafts(s, vm, onEdit = { editing = it })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        if (s.drafts?.isNotEmpty() == true) {
            GradientButton(
                if (s.saving) "Saving…" else "Save ${s.selectedCount} card${if (s.selectedCount == 1) "" else "s"}",
                onClick = { vm.save { id -> onNavigate(Routes.deck(id)) } },
                enabled = !s.saving && s.selectedCount > 0 && (s.targetDeckId != null || s.newDeckTitle.isNotBlank()),
                icon = Icons.Rounded.Save,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            )
        }
    }

    editing?.let { d ->
        TextInputDialog(
            "Edit card", "Front (question)", "Save", initialFirst = d.front, secondLabel = "Back (answer)", initialSecond = d.back,
            singleLine = false, onDismiss = { editing = null },
            onConfirm = { f, b -> vm.edit(d.key, f, b); editing = null },
        )
    }
}

@Composable
private fun EngineCard(llm: LlmState) {
    val ex = Studin.extras
    val (title, body, accent) = when (llm) {
        is LlmState.Ready, is LlmState.Installed, is LlmState.Loading ->
            Triple("On-device LLM ready", "Your imported model writes the cards. Falls back to Lite AI if it can't.", StudinColors.Violet)
        else -> Triple(
            "StudIN Lite AI",
            "Finds definitions, formulas and key facts in your notes. Import a model in AI Tutor for richer questions.",
            StudinColors.Mint,
        )
    }
    GlassCard(Modifier.fillMaxWidth(), highlight = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(Icons.Rounded.AutoAwesome, accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodySmall, color = ex.muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = ex.success, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Private: works offline, notes are never uploaded", style = MaterialTheme.typography.labelSmall, color = ex.muted)
        }
    }
}

@Composable
private fun ShimmerPlaceholder() {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-400f, 1200f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x")
    val base = Studin.extras.glass
    val brush = Brush.linearGradient(
        listOf(base, StudinColors.Mint.copy(alpha = 0.22f), base),
        start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x + 400f, 200f),
    )
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Reading your notes and writing questions…", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(12.dp))
        repeat(4) {
            Box(Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(18.dp)).background(brush))
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun Drafts(s: GenerateUiState, vm: GenerateCardsViewModel, onEdit: (DraftCard) -> Unit) {
    val ex = Studin.extras
    val drafts = s.drafts.orEmpty()
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${drafts.size} cards", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            s.engine?.let { Badge(it.label, if (it == AiEngine.LLM) StudinColors.Violet else StudinColors.Mint) }
            IconButton(onClick = vm::generate) { Icon(Icons.Rounded.Refresh, contentDescription = "Regenerate") }
        }
        if (s.keywords.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                s.keywords.forEach { Badge(it, StudinColors.Cyan) }
            }
        }
        Spacer(Modifier.height(12.dp))
        drafts.forEach { d ->
            GlassCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { vm.toggle(d.key) }, contentPadding = 12.dp, highlight = d.selected) {
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(
                        checked = d.selected, onCheckedChange = { vm.toggle(d.key) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    )
                    Column(Modifier.weight(1f).padding(top = 10.dp)) {
                        Text(d.front, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis,
                            color = if (d.selected) MaterialTheme.colorScheme.onSurface else ex.muted)
                        Spacer(Modifier.height(4.dp))
                        Text(d.back, style = MaterialTheme.typography.bodySmall, color = ex.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onEdit(d) }) { Icon(Icons.Rounded.Edit, contentDescription = "Edit card", tint = ex.muted) }
                }
            }
        }
        if (drafts.isNotEmpty()) {
            SectionHeader("Save to")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("New deck", s.targetDeckId == null, { vm.selectDeck(null) }, icon = Icons.Rounded.Add)
                s.decks.forEach { deck -> Pill(deck.title.take(28), s.targetDeckId == deck.id, { vm.selectDeck(deck.id) }) }
            }
            AnimatedVisibility(s.targetDeckId == null) {
                OutlinedTextField(
                    value = s.newDeckTitle, onValueChange = vm::onNewDeckTitle, label = { Text("New deck title") },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            GhostButton("Discard and edit notes", vm::discard, Modifier.fillMaxWidth(), color = ex.muted)
        }
    }
}
