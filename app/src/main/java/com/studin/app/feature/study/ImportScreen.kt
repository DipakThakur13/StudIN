package com.studin.app.feature.study

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.designsystem.Badge
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.LinearLoading
import com.studin.app.core.designsystem.SectionHeader
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.ui.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ImportScreen(onNavigate: (String) -> Unit, onBack: () -> Unit, vm: ImportViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.loadFile(uri) }
    val ex = Studin.extras

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            StudyTopBar("Import deck", onBack, subtitle = "CSV or pasted text")
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.Description, StudinColors.Star)
                        Spacer(Modifier.width(12.dp))
                        Text("Format", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "One card per line: front,back[,tags]. A header row (front,back) is optional. " +
                            "Commas, tabs or semicolons work. Wrap text containing commas in \"quotes\".",
                        style = MaterialTheme.typography.bodyMedium, color = ex.muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "front,back,tags\n\"Unit of force?\",Newton,physics\nAvogadro's number?,6.022 × 10²³,chemistry",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ex.glass).padding(10.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shield, null, tint = ex.success, modifier = Modifier.padding(end = 6.dp))
                        Text(
                            "Max 1 MB / 5,000 cards. Files are validated on your phone and never uploaded.",
                            style = MaterialTheme.typography.labelSmall, color = ex.muted,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row {
                    GradientButton(
                        "Choose file", {
                            picker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/csv", "text/tab-separated-values"))
                        }, Modifier.weight(1f), icon = Icons.Rounded.FileUpload,
                    )
                    Spacer(Modifier.width(10.dp))
                    GhostButton("Paste", {
                        clipboard.getText()?.text?.let { vm.onTextChange(it) }
                    }, Modifier.weight(0.7f), icon = Icons.Rounded.ContentPaste)
                }
                if (s.reading) { Spacer(Modifier.height(10.dp)); LinearLoading() }
                s.fileName?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Loaded: $it", style = MaterialTheme.typography.labelMedium, color = ex.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = s.deckTitle, onValueChange = vm::onTitleChange, label = { Text("Deck title") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = if (s.text.length > 6_000) s.text.take(6_000) else s.text,
                    onValueChange = { if (s.text.length <= 6_000) vm.onTextChange(it) },
                    readOnly = s.text.length > 6_000,
                    label = { Text(if (s.text.length > 6_000) "CSV (large file – preview only)" else "…or paste CSV here") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 260.dp),
                )

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

                s.preview?.let { p ->
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Preview")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge("${p.cards.size} cards", ex.success)
                        if (p.skipped > 0) { Spacer(Modifier.width(8.dp)); Badge("${p.skipped} skipped", ex.warning) }
                    }
                    Spacer(Modifier.height(10.dp))
                    p.cards.take(5).forEach { c ->
                        GlassCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentPadding = 12.dp) {
                            Text(c.front, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(c.back, style = MaterialTheme.typography.bodySmall, color = ex.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (p.cards.size > 5) Text("+ ${p.cards.size - 5} more", style = MaterialTheme.typography.labelMedium, color = ex.muted)
                    if (p.errors.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        p.errors.take(6).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = ex.warning) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            GradientButton(
                if (s.importing) "Importing…" else "Import ${s.preview?.cards?.size?.takeIf { it > 0 }?.let { "$it cards" } ?: ""}".trim(),
                onClick = {
                    vm.import { id, count ->
                        scope.launch {
                            snackbar.showSnackbar("Imported $count cards")
                        }
                        scope.launch { delay(700); onNavigate(Routes.deck(id)) }
                    }
                },
                enabled = !s.importing && (s.preview?.cards?.isNotEmpty() == true),
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp))
    }
}
