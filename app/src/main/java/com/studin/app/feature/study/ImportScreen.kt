package com.studin.app.feature.study

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINBadge
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINErrorState
import com.studin.app.core.designsystem.StudINIconButton
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINTextField
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.app.ui.navigation.Routes
import com.studin.domain.importer.CsvDeckParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Above this length the pasted/loaded CSV is shown as a summary instead of an editable field. */
private const val EDITABLE_TEXT_LIMIT = 6_000

@Composable
fun ImportScreen(onNavigate: (String) -> Unit, onBack: () -> Unit, vm: ImportViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.loadFile(uri) }
    val preview = s.preview
    val canImport = !s.importing && preview != null && preview.cards.isNotEmpty()

    StudINScaffold(
        title = "Import deck", subtitle = "CSV file or pasted text", onBack = onBack, snackbarHostState = snackbar,
        bottomBar = {
            BottomActionBar {
                StudINPrimaryButton(
                    when {
                        preview == null || preview.cards.isEmpty() -> "Import"
                        else -> "Import ${preview.cards.size} card${if (preview.cards.size == 1) "" else "s"}"
                    },
                    onClick = {
                        vm.import { id, count ->
                            scope.launch {
                                launch { snackbar.showSnackbar("Imported $count cards") }
                                delay(700); onNavigate(Routes.deck(id))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), enabled = canImport, loading = s.importing, icon = Icons.Rounded.FileUpload,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().studinContentWidth(),
            contentPadding = screenContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "format") { FormatCard() }
            item(key = "sources") {
                Column(Modifier.padding(top = Spacing.xs)) {
                    AdaptiveButtonGroup(
                        { m ->
                            StudINPrimaryButton(
                                "Choose file", { picker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/csv", "text/tab-separated-values")) },
                                m, icon = Icons.Rounded.FileOpen, loading = s.reading,
                            )
                        },
                        { m -> StudINOutlinedButton("Paste", { clipboard.getText()?.text?.let { vm.onTextChange(it) } }, m, icon = Icons.Rounded.ContentPaste) },
                    )
                }
            }
            s.fileName?.let { name ->
                item(key = "file") {
                    StudINCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.card, vertical = Spacing.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StudINIconTile(Icons.Rounded.Description, size = 36.dp)
                            Spacer(Modifier.width(Spacing.sm))
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${s.text.length / 1024 + 1} KB loaded", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
                            }
                        }
                    }
                }
            }
            item(key = "title") {
                StudINTextField(s.deckTitle, vm::onTitleChange, label = "Deck title", placeholder = "e.g. Organic reactions")
            }
            item(key = "text") {
                if (s.text.length > EDITABLE_TEXT_LIMIT) {
                    StudINCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Large CSV loaded", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${s.text.length} characters – too long to edit here. It will be imported as-is after validation.",
                                    style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                                )
                            }
                            StudINIconButton(Icons.Rounded.Close, "Clear loaded text", { vm.onTextChange("") })
                        }
                    }
                } else {
                    StudINTextField(
                        s.text, vm::onTextChange, label = "CSV text", placeholder = "front,back,tags", singleLine = false, minLines = 4, maxLines = 10,
                        supportingText = "${s.text.length} characters · max 1 MB",
                    )
                }
            }
            s.error?.let { e -> item(key = "error") { StudINErrorState(e) } }
            if (preview != null) {
                item(key = "preview-header") {
                    StudINSectionHeader(
                        "Preview", Modifier.padding(top = Spacing.md),
                        supporting = "${preview.cards.size} valid card${if (preview.cards.size == 1) "" else "s"}" +
                            if (preview.skipped > 0) " · ${preview.skipped} skipped" else "",
                    )
                }
                if (preview.cards.isEmpty()) {
                    item(key = "preview-empty") {
                        Text("No valid rows yet. Each row needs a front and a back.", style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary)
                    }
                } else {
                    item(key = "preview-list") {
                        StudINCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.xxs)) {
                            preview.cards.take(5).forEachIndexed { i, c ->
                                if (i > 0) HorizontalDivider(color = StudINTheme.colors.border)
                                Column(Modifier.padding(horizontal = Spacing.card, vertical = Spacing.sm)) {
                                    Text(c.front, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(c.back, style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (c.tags.isNotBlank()) { Spacer(Modifier.height(Spacing.xxs)); StudINBadge(c.tags) }
                                }
                            }
                            if (preview.cards.size > 5) {
                                HorizontalDivider(color = StudINTheme.colors.border)
                                Text(
                                    "+ ${preview.cards.size - 5} more", style = MaterialTheme.typography.labelLarge, color = StudINTheme.colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = Spacing.card, vertical = Spacing.sm),
                                )
                            }
                        }
                    }
                }
                if (preview.errors.isNotEmpty()) {
                    item(key = "preview-errors") {
                        StudINCard(Modifier.fillMaxWidth(), containerColor = StudINTheme.colors.warningContainer, border = null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = StudINTheme.colors.warning, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(Spacing.xs))
                                Text("Rows that will be skipped", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(Modifier.height(Spacing.xxs))
                            preview.errors.take(6).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            if (preview.errors.size > 6) Text("…and ${preview.errors.size - 6} more", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatCard() {
    val c = StudINTheme.colors
    StudINCard(Modifier.fillMaxWidth()) {
        Text("Format", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            "One card per line: front, back and optional tags. A header row is optional. Commas, tabs or semicolons all work; wrap text containing commas in \"quotes\".",
            style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
        )
        Spacer(Modifier.height(Spacing.sm))
        InsetBox {
            Text(
                "front,back,tags\n\"Unit of force?\",Newton,physics\nAvogadro's number?,6.022 × 10²³,chemistry",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = c.success, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(
                "Max 1 MB and ${CsvDeckParser.MAX_ROWS} cards. Files are validated on your phone and never uploaded.",
                style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
            )
        }
    }
}
