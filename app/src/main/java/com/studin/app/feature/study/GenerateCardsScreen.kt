package com.studin.app.feature.study

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.ai.AiEngine
import com.studin.app.ai.LlmState
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINBadge
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINChip
import com.studin.app.core.designsystem.StudINErrorState
import com.studin.app.core.designsystem.StudINFilterChip
import com.studin.app.core.designsystem.StudINIconButton
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINLoadingState
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINTextField
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.app.ui.navigation.Routes

@Composable
fun GenerateCardsScreen(deckId: Long?, onNavigate: (String) -> Unit, onBack: () -> Unit, vm: GenerateCardsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val llm by vm.llmState.collectAsStateWithLifecycle()
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.scan(uri) }
    var editing by remember { mutableStateOf<DraftCard?>(null) }
    val drafts = s.drafts

    StudINScaffold(
        title = "AI flashcards", subtitle = "Generated on your phone – notes never leave it", onBack = onBack,
        bottomBar = {
            BottomActionBar {
                if (drafts == null) {
                    StudINPrimaryButton(
                        if (s.generating) "Generating…" else "Generate flashcards", vm::generate, Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.AutoAwesome, enabled = s.notes.isNotBlank() && !s.scanning, loading = s.generating,
                    )
                } else {
                    StudINOutlinedButton("Discard", vm::discard, Modifier.weight(1f))
                    StudINPrimaryButton(
                        "Save ${s.selectedCount} card${if (s.selectedCount == 1) "" else "s"}",
                        { vm.save { id -> onNavigate(Routes.deck(id)) } }, Modifier.weight(1.6f),
                        icon = Icons.Rounded.Save, enabled = s.selectedCount > 0 && (s.targetDeckId != null || s.newDeckTitle.isNotBlank()), loading = s.saving,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().studinContentWidth(),
            contentPadding = screenContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "engine") { EngineCard(llm, s.engine) }
            if (drafts == null) {
                item(key = "notes") {
                    StudINTextField(
                        value = s.notes, onValueChange = vm::onNotes, label = "Your notes",
                        placeholder = "Paste or type notes – definitions, formulas and key facts work best.",
                        singleLine = false, minLines = 6, maxLines = 14,
                        supportingText = "${s.notes.length} / ${GenerateCardsViewModel.MAX_NOTES}",
                    )
                }
                item(key = "inputs") {
                    AdaptiveButtonGroup(
                        { m ->
                            StudINOutlinedButton(
                                if (s.scanning) "Reading photo…" else "Scan notes photo",
                                { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                m, icon = Icons.Rounded.DocumentScanner, enabled = !s.scanning,
                            )
                        },
                        { m -> StudINOutlinedButton("Paste", { clipboard.getText()?.text?.let(vm::appendNotes) }, m, icon = Icons.Rounded.ContentPaste) },
                    )
                }
                if (s.scanning) item(key = "scanning") { StudINLoadingState(message = "Recognising text on your phone…") }
                if (s.generating) item(key = "generating") { StudINLoadingState(message = "Finding definitions, formulas and key facts…") }
                s.error?.let { e -> item(key = "error") { StudINErrorState(e) } }
                item(key = "tips") {
                    Text(
                        "Tip: sentences like \"Mitochondria is the powerhouse of the cell\" or \"F = ma\" become clean question–answer cards.",
                        style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                    )
                }
            } else {
                s.error?.let { e -> item(key = "error") { StudINErrorState(e) } }
                if (s.keywords.isNotEmpty()) {
                    item(key = "keywords") {
                        Column {
                            Text("Key terms found", style = MaterialTheme.typography.titleSmall, color = StudINTheme.colors.textSecondary)
                            Spacer(Modifier.height(Spacing.xs))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                s.keywords.forEach { StudINChip(it) }
                            }
                        }
                    }
                }
                item(key = "drafts-header") {
                    StudINSectionHeader(
                        "Draft cards", Modifier.padding(top = Spacing.xs),
                        supporting = "${s.selectedCount} of ${drafts.size} selected · tap the pencil to edit",
                    )
                }
                items(drafts, key = { it.key }) { d -> DraftRow(d, onToggle = { vm.toggle(d.key) }, onEdit = { editing = d }) }
                item(key = "target") { TargetDeckPicker(s, vm::selectDeck, vm::onNewDeckTitle) }
            }
        }
    }

    editing?.let { d ->
        TextInputDialog(
            title = "Edit card", firstLabel = "Front (question)", secondLabel = "Back (answer)", confirmText = "Save", singleLine = false,
            initialFirst = d.front, initialSecond = d.back,
            onDismiss = { editing = null }, onConfirm = { f, b -> vm.edit(d.key, f, b); editing = null },
        )
    }
}

/** Honest description of which on-device engine is available / was used. */
@Composable
private fun EngineCard(llm: LlmState, used: AiEngine?) {
    val c = StudINTheme.colors
    val (title, body) = when {
        used == AiEngine.LLM -> "On-device model" to "These cards were drafted by the model you imported. Check them before saving."
        used == AiEngine.LITE -> "StudIN Lite AI" to "These cards were extracted from definitions, formulas and key facts in your notes. Check them before saving."
        llm is LlmState.Ready -> "On-device model ready" to "Your on-device AI Tutor (${llm.name}) drafts the cards. StudIN Lite AI is the automatic fallback."
        llm is LlmState.Installed -> "On-device model installed" to "Your AI Tutor model (${llm.name}) loads when you generate (if enabled in AI Tutor). Lite AI is the fallback."
        llm is LlmState.Loading -> "Loading on-device model…" to "Lite AI is used if the model isn't ready."
        else -> "StudIN Lite AI" to "Finds definitions, formulas and key facts in your notes. Download the AI Tutor on the AI Tutor screen for richer questions."
    }
    val usesModel = used == AiEngine.LLM || (used == null && (llm is LlmState.Ready || llm is LlmState.Installed))
    StudINCard(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINIconTile(if (usesModel) Icons.Rounded.Memory else Icons.Rounded.AutoAwesome, c.ai, 40.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            StudINBadge(if (usesModel) "On-device LLM" else "Lite", container = c.aiContainer, content = c.ai)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.success, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Spacing.xxs))
            Text("Works offline · notes are never uploaded", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
        }
    }
}

@Composable
private fun DraftRow(d: DraftCard, onToggle: () -> Unit, onEdit: () -> Unit) {
    val c = StudINTheme.colors
    StudINCard(
        Modifier.fillMaxWidth(),
        onClick = onToggle, onClickLabel = if (d.selected) "Exclude card" else "Include card",
        containerColor = if (d.selected) MaterialTheme.colorScheme.surface else c.surfaceSecondary,
        contentPadding = PaddingValues(start = Spacing.xxs, end = Spacing.xxs, top = Spacing.xs, bottom = Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = d.selected, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.semantics {
                    contentDescription = "Include card: ${d.front}"
                    stateDescription = if (d.selected) "Included" else "Excluded"
                },
            )
            Column(Modifier.weight(1f).padding(top = Spacing.sm, bottom = Spacing.xs)) {
                Text(d.front, style = MaterialTheme.typography.titleMedium, maxLines = 4, overflow = TextOverflow.Ellipsis,
                    color = if (d.selected) MaterialTheme.colorScheme.onSurface else c.textSecondary)
                Spacer(Modifier.height(Spacing.xxs))
                Text(d.back, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            StudINIconButton(Icons.Rounded.Edit, "Edit card", onEdit)
        }
    }
}

@Composable
private fun TargetDeckPicker(s: GenerateUiState, onSelect: (Long?) -> Unit, onTitle: (String) -> Unit) {
    StudINCard(Modifier.fillMaxWidth().padding(top = Spacing.xs)) {
        Text("Save to", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            StudINFilterChip("New deck", selected = s.targetDeckId == null, onClick = { onSelect(null) })
            s.decks.forEach { d -> StudINFilterChip(d.title.take(32), selected = s.targetDeckId == d.id, onClick = { onSelect(d.id) }) }
        }
        if (s.targetDeckId == null) {
            Spacer(Modifier.height(Spacing.sm))
            StudINTextField(s.newDeckTitle, onTitle, label = "New deck name", placeholder = "e.g. Thermodynamics notes")
        }
    }
}

