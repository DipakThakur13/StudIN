package com.studin.app.feature.ai

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.sample
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.studin.app.ai.DownloadUi
import com.studin.app.ai.GemmaModel
import com.studin.app.core.designsystem.StudINBottomSheet
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.ClipData
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.R
import com.studin.app.ai.AiEngine
import com.studin.app.ai.LlmState
import com.studin.app.core.data.db.ClassificationDecisionEntity
import com.studin.app.core.designsystem.Motion
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINBadge
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINChip
import com.studin.app.core.designsystem.StudINDialog
import com.studin.app.core.designsystem.StudINErrorState
import com.studin.app.core.designsystem.StudINIconButton
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINIllustration
import com.studin.app.core.designsystem.StudINLinearProgress
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINSegmentedTabs
import com.studin.app.core.designsystem.StudINShapes
import com.studin.app.core.designsystem.StudINTextButton
import com.studin.app.core.designsystem.StudINTextField
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.motionDuration
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.app.ui.navigation.Routes
import com.studin.domain.model.Verdict
import java.text.DateFormat
import java.util.Date

@Composable
fun AiScreen(onNavigate: (String) -> Unit, vm: AiViewModel = hiltViewModel()) {
    val llm by vm.llmState.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    val download by vm.download.collectAsStateWithLifecycle()
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    var confirmDownload by rememberSaveable { mutableStateOf(false) }
    val access by vm.access.collectAsStateWithLifecycle()
    val status = engineStatus(llm, settings.aiUseLlm, thinking)
    val installed = llm is LlmState.Installed || llm is LlmState.Loading || llm is LlmState.Ready
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.importModel(uri) }

    StudINScaffold(
        title = "AI Tutor",
        subtitle = "Your personal, offline study assistant",
        actions = {
            val running = download as? DownloadUi.Running
            if (running != null) StudINBadge(
                if (running.waiting) "Waiting for network" else "Downloading ${(running.fraction * 100).toInt()}%",
                container = StudINTheme.colors.aiContainer, content = StudINTheme.colors.ai, icon = Icons.Rounded.Download,
            ) else EngineBadge(status)
            ModelMenu(
                installed = installed, useLlm = settings.aiUseLlm,
                onUseLlm = vm::setUseLlm, onImport = { importer.launch(arrayOf("*/*")) }, onRemove = { confirmRemove = true },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            TutorTab(vm, onNavigate, downloading = download is DownloadUi.Running) {
                ModelCard(
                    llm = llm, useLlm = settings.aiUseLlm, download = download, supported = vm.deviceSupportsModel,
                    checking = access == AccessCheck.Checking && !showSheet,
                    onDownload = {
                        vm.resetAccess()
                        val needsOwn = (download as? DownloadUi.Failed)?.needsToken == true
                        if (GemmaModel.bundledToken != null && !needsOwn) confirmDownload = true else showSheet = true
                    },
                    onCancel = vm::cancelDownload,
                    onUseLlm = { vm.setUseLlm(true) },
                )
            }
        }
    }
    if (showSheet) DownloadSheet(vm, onDismiss = { showSheet = false })
    if (confirmDownload) {
        val context = LocalContext.current
        val metered = remember { vm.isMetered() }
        StudINDialog(
            title = "Download AI Tutor?", onDismiss = { confirmDownload = false },
            confirmLabel = "Download ${GemmaModel.sizeLabel}", icon = Icons.Rounded.Download,
            onConfirm = { confirmDownload = false; vm.startBundledDownload(onNeedsOwnToken = { showSheet = true }) },
            text = {
                Column {
                    Text(
                        "Google's ${GemmaModel.DISPLAY_NAME} downloads once (${GemmaModel.sizeLabel}) and then runs fully offline on this phone." +
                            if (metered) " You're on mobile data — Wi-Fi is recommended." else " Wi-Fi is recommended.",
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "Gemma is provided under and subject to the Gemma Terms of Use. By downloading you agree to them and to Google's Gemma Prohibited Use Policy.",
                        style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                    )
                    StudINTextButton("Read the Gemma Terms of Use", {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GemmaModel.TERMS_PAGE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    })
                }
            },
        )
    }
    if (confirmRemove) StudINDialog(
        title = "Remove the AI Tutor model?", onDismiss = { confirmRemove = false },
        confirmLabel = "Remove", destructive = true, icon = Icons.Rounded.DeleteOutline,
        onConfirm = { confirmRemove = false; vm.deleteModel() },
        text = { Text("Frees about ${GemmaModel.sizeLabel}. StudIN Lite AI keeps working, and you can download Gemma again any time.") },
    )
}

@Composable
private fun EngineBadge(status: EngineStatus) {
    val c = StudINTheme.colors
    val (container, content, icon) = when (status.kind) {
        EngineStatusKind.READY, EngineStatusKind.PROCESSING -> Triple(c.successContainer, c.success, Icons.Rounded.CheckCircle)
        EngineStatusKind.LOADING, EngineStatusKind.INSTALLED -> Triple(c.aiContainer, c.ai, Icons.Rounded.Memory)
        EngineStatusKind.ERROR -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Rounded.ErrorOutline)
        EngineStatusKind.LITE -> Triple(c.aiContainer, c.ai, Icons.Rounded.AutoAwesome)
    }
    StudINBadge(
        status.label, container = container, content = content, icon = icon,
        modifier = Modifier.semantics { contentDescription = "AI engine: ${status.label}. ${status.detail}" },
    )
}

// ------------------------------------------------------------------ Tutor

private data class QuickAction(val title: String, val supporting: String, val icon: ImageVector)

@Composable
private fun TutorTab(vm: AiViewModel, onNavigate: (String) -> Unit, downloading: Boolean, modelCard: @Composable () -> Unit) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    var requestFocus by remember { mutableStateOf(false) }
    val reduced = StudINTheme.reducedMotion
    val padding = screenContentPadding(PaddingValues(), top = Spacing.xs, bottom = Spacing.md)

    // New message: bring it into view once. While a reply streams, follow it at most ~3×/s and only
    // if the student hasn't scrolled up to read (no per-token animated scrolling → no jank).
    LaunchedEffect(messages.size) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (messages.isNotEmpty() && last >= 0) { if (reduced) listState.scrollToItem(last) else listState.animateScrollToItem(last) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { vm.messages.value.lastOrNull()?.takeIf { it.streaming }?.text?.length ?: -1 }
            .filter { it > 0 }
            .sample(300)
            .collect {
                val info = listState.layoutInfo
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@collect
                if (lastVisible >= info.totalItemsCount - 2) listState.scrollToItem(info.totalItemsCount - 1, Int.MAX_VALUE / 2)
            }
    }
    LaunchedEffect(requestFocus) { if (requestFocus) { runCatching { focus.requestFocus() }; requestFocus = false } }

    fun prefill(text: String) { input = text; requestFocus = true }
    fun submit() { if (input.isNotBlank() && !thinking) { vm.send(input); input = "" } }

    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            Modifier.weight(1f).studinContentWidth(), state = listState,
            contentPadding = padding, verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (messages.isEmpty()) {
                item(key = "intro") { TutorIntro() }
                item(key = "model") { modelCard() }
                item(key = "qa-header") { StudINSectionHeader("Quick actions", Modifier.padding(top = Spacing.xs)) }
                item(key = "qa") {
                    val actions = listOf(
                        QuickAction("Explain a topic", "Get a clear, simple explanation", Icons.AutoMirrored.Rounded.MenuBook) to { prefill("Explain ") },
                        QuickAction("Quiz me", "Practise with MCQs from your cards", Icons.Rounded.Quiz) to { onNavigate(Routes.quiz()) },
                        QuickAction("Summarise notes", "Paste notes, get the key points", Icons.Rounded.Description) to { prefill("Summarise these notes:\n") },
                        QuickAction("Plan my revision", "An advisory plan for today", Icons.AutoMirrored.Rounded.EventNote) to { vm.send("What should I study today?") },
                    )
                    val accents = listOf(MaterialTheme.colorScheme.primary, StudINTheme.colors.accent, StudINTheme.colors.ai, StudINTheme.colors.success)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        actions.chunked(2).forEachIndexed { row, pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                pair.forEachIndexed { col, (a, onClick) ->
                                    QuickActionCard(a, accents[row * 2 + col], onClick, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                item(key = "privacy") {
                    Text(
                        "Chats stay in memory on this phone and are cleared when you leave AI Tutor.",
                        style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            } else {
                if (downloading) item(key = "model-mini") { modelCard() }
                item(key = "conv-header") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Conversation", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                        StudINTextButton("Clear", vm::clearChat, icon = Icons.Rounded.DeleteSweep)
                    }
                }
                items(messages, key = { it.id }) { m ->
                    if (m.fromUser) UserBubble(m) else TutorBubble(m, onRetry = { vm.retry(m.id) }, canRetry = !thinking)
                }
            }
        }
        HorizontalDivider(color = StudINTheme.colors.border)
        Row(
            Modifier.studinContentWidth().padding(horizontal = Spacing.screen, vertical = Spacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            StudINTextField(
                value = input, onValueChange = { input = it.take(2_000) },
                modifier = Modifier.weight(1f).focusRequester(focus),
                placeholder = "Ask anything, or paste notes to summarise…",
                singleLine = false, maxLines = 4, imeAction = ImeAction.Send, onImeAction = ::submit,
            )
            Spacer(Modifier.width(Spacing.xs))
            if (thinking) {
                FilledIconButton(
                    onClick = vm::stop, modifier = Modifier.padding(bottom = 4.dp).size(Spacing.touchTarget), shape = StudINShapes.button,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = StudINTheme.colors.surfaceSecondary, contentColor = MaterialTheme.colorScheme.onSurface),
                ) { Icon(Icons.Rounded.Stop, contentDescription = "Stop generating") }
            } else {
                FilledIconButton(
                    onClick = ::submit, enabled = input.isNotBlank(), modifier = Modifier.padding(bottom = 4.dp).size(Spacing.touchTarget),
                    shape = StudINShapes.button,
                ) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send") }
            }
        }
    }
}

@Composable
private fun TutorIntro() {
    val config = LocalConfiguration.current
    val compact = config.screenWidthDp / config.fontScale < 390f
    StudINCard(Modifier.fillMaxWidth(), containerColor = StudINTheme.colors.surfaceSecondary, border = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINIllustration(
                R.drawable.ill_ai_tutor, contentDescription = null, backdrop = false,
                modifier = Modifier.width(if (compact) 84.dp else 124.dp), height = if (compact) 72.dp else 104.dp,
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text("Hi! I'm your study buddy.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    if (compact) "Ask, summarise or plan – answered from your own cards and syllabus."
                    else "Ask about a concept, summarise your notes or plan revision. I answer from your own cards and syllabus – and with Gemma, once you download it.",
                    style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(a: QuickAction, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    StudINCard(modifier.heightIn(min = 104.dp), onClick = onClick, contentPadding = PaddingValues(Spacing.sm + 2.dp), onClickLabel = a.title) {
        StudINIconTile(a.icon, accent, 36.dp)
        Spacer(Modifier.height(Spacing.xs))
        Text(a.title, style = MaterialTheme.typography.titleSmall)
        Text(a.supporting, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
    }
}

@Composable
private fun UserBubble(m: ChatMessage) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            modifier = Modifier.widthIn(max = maxWidth * 0.85f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
            color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            SelectionContainer {
                Text(m.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = Spacing.sm + 2.dp, vertical = Spacing.xs + 2.dp)
                    .semantics { contentDescription = "You: ${m.text}" })
            }
        }
    }
}

@Composable
private fun TutorBubble(m: ChatMessage, onRetry: () -> Unit, canRetry: Boolean) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Column(Modifier.widthIn(max = maxWidth * 0.92f)) {
            when {
                m.failed -> StudINErrorState("Couldn't answer: ${m.text}", onRetry = if (canRetry && m.prompt != null) onRetry else null)
                m.streaming && m.text.isEmpty() -> ThinkingRow()
                else -> Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, StudINTheme.colors.border),
                ) {
                    Column(Modifier.padding(start = Spacing.sm + 2.dp, end = Spacing.xxs, top = Spacing.sm, bottom = Spacing.xxs)) {
                        SelectionContainer(Modifier.padding(end = Spacing.sm)
                            .then(if (!m.streaming) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier)) {
                            ReplyText(m.text)
                        }
                        if (m.engine == AiEngine.LLM && !m.streaming) {
                            Text(
                                "AI can make mistakes — check important facts with your textbook.",
                                style = MaterialTheme.typography.labelSmall, color = StudINTheme.colors.textSecondary,
                                modifier = Modifier.padding(top = Spacing.xs, end = Spacing.sm),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            m.engine?.let {
                                StudINBadge(
                                    if (it == AiEngine.LLM) "On-device model" else "Lite AI",
                                    container = StudINTheme.colors.aiContainer, content = StudINTheme.colors.ai,
                                )
                            }
                            if (m.streaming) {
                                Spacer(Modifier.width(Spacing.xs))
                                Text("Writing…", style = MaterialTheme.typography.labelMedium, color = StudINTheme.colors.textSecondary)
                            }
                            Spacer(Modifier.weight(1f))
                            if (!m.streaming) {
                                StudINIconButton(Icons.Rounded.ContentCopy, "Copy reply", { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("StudIN reply", plainReply(m.text)))) } },
                                    tint = StudINTheme.colors.textSecondary)
                            } else Spacer(Modifier.height(Spacing.touchTarget))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingRow() {
    Row(
        Modifier.background(StudINTheme.colors.surfaceSecondary, StudINShapes.smallCard).padding(horizontal = Spacing.sm, vertical = Spacing.sm)
            .semantics { contentDescription = "Tutor is thinking"; liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = StudINTheme.colors.ai)
        Spacer(Modifier.width(Spacing.xs))
        Text("Thinking…", style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary)
    }
}

/** Renders paragraphs, bullets, numbered items and code blocks from [formatReply]. */
@Composable
private fun ReplyText(text: String) {
    val blocks = remember(text) { formatReply(text) }
    val codeBg = StudINTheme.colors.surfaceSecondary
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        blocks.forEach { b ->
            when (b) {
                is ReplyBlock.Heading -> Text(rich(b.text, codeBg), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                is ReplyBlock.Paragraph -> Text(rich(b.text, codeBg), style = MaterialTheme.typography.bodyMedium)
                is ReplyBlock.Bullet -> Row {
                    Box(Modifier.padding(top = 8.dp).size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(rich(b.text, codeBg), style = MaterialTheme.typography.bodyMedium)
                }
                is ReplyBlock.Numbered -> Row {
                    Text("${b.number}.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(22.dp))
                    Text(rich(b.text, codeBg), style = MaterialTheme.typography.bodyMedium)
                }
                is ReplyBlock.Code -> Text(
                    b.text,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().background(codeBg, StudINShapes.badge).padding(Spacing.xs),
                )
            }
        }
    }
}

/** Inline markdown → styled text (bold, italic, monospace code chips). */
private fun rich(text: String, codeBg: Color): AnnotatedString = buildAnnotatedString {
    parseInline(text).forEach { s ->
        when {
            s.code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(" ${s.text} ") }
            s.bold -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(s.text) }
            s.italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(s.text) }
            else -> append(s.text)
        }
    }
}


// ------------------------------------------------------------------ AI Tutor model (Gemma)

@Composable
private fun ModelMenu(installed: Boolean, useLlm: Boolean, onUseLlm: (Boolean) -> Unit, onImport: () -> Unit, onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        StudINIconButton(Icons.Rounded.MoreVert, "AI Tutor options", { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = StudINShapes.smallCard) {
            if (installed) {
                DropdownMenuItem(
                    text = { Text("Use ${GemmaModel.DISPLAY_NAME}") },
                    leadingIcon = { Icon(if (useLlm) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null) },
                    onClick = { onUseLlm(!useLlm); open = false },
                )
                DropdownMenuItem(
                    text = { Text("Remove AI Tutor model") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                    onClick = { open = false; onRemove() },
                )
            }
            DropdownMenuItem(
                text = { Text("Import model file…") },
                leadingIcon = { Icon(Icons.Rounded.FileOpen, null) },
                onClick = { open = false; onImport() },
            )
        }
    }
}

/** Download offer / progress / ready state for the on-device Gemma tutor. */
@Composable
private fun ModelCard(
    llm: LlmState,
    useLlm: Boolean,
    download: DownloadUi,
    supported: Boolean,
    checking: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onUseLlm: () -> Unit,
) {
    val c = StudINTheme.colors
    when {
        download is DownloadUi.Running -> StudINCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudINIconTile(Icons.Rounded.Download, c.ai, 40.dp)
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text("Downloading AI Tutor", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (download.waiting) "Waiting for an internet connection…"
                        else "${formatBytes(download.bytes)} of ${GemmaModel.sizeLabel} · ${(download.fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            StudINLinearProgress(download.fraction, color = c.ai, label = "AI Tutor download progress")
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Keeps going in the background — you can leave this screen.",
                    style = MaterialTheme.typography.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f),
                )
                StudINTextButton("Cancel", onCancel)
            }
        }
        llm is LlmState.Installed || llm is LlmState.Loading || llm is LlmState.Ready -> StudINCard(
            Modifier.fillMaxWidth(), containerColor = c.successContainer, border = null,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.success)
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (useLlm) "${GemmaModel.DISPLAY_NAME} is your AI Tutor" else "${GemmaModel.DISPLAY_NAME} is installed but switched off",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        when {
                            !useLlm -> "Lite AI is answering. Turn Gemma on to use it."
                            llm is LlmState.Loading -> "Loading the model into memory…"
                            else -> "Runs fully offline on this phone."
                        },
                        style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                    )
                }
                if (!useLlm) StudINTextButton("Turn on", onUseLlm)
            }
        }
        !supported || llm is LlmState.Unsupported -> StudINCard(Modifier.fillMaxWidth(), containerColor = c.surfaceSecondary, border = null) {
            Text("Full AI Tutor isn't available on this phone", style = MaterialTheme.typography.titleSmall)
            Text(
                "${GemmaModel.DISPLAY_NAME} needs a 64-bit phone with at least 4 GB RAM. StudIN Lite AI keeps working here.",
                style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
            )
        }
        else -> StudINCard(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, c.ai.copy(alpha = 0.5f))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudINIconTile(Icons.Rounded.AutoAwesome, c.ai, 40.dp)
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text("Get the full AI Tutor", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    Text("Google's ${GemmaModel.DISPLAY_NAME} · ${GemmaModel.sizeLabel} · one-time download", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Richer explanations, better summaries and smarter flashcards. After the download it runs fully offline on this phone.",
                style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
            )
            if (download is DownloadUi.Failed) {
                Spacer(Modifier.height(Spacing.sm))
                StudINErrorState(download.message)
            }
            Spacer(Modifier.height(Spacing.sm))
            StudINPrimaryButton(
                when { checking -> "Connecting…"; download is DownloadUi.Failed -> "Try again"; else -> "Download AI Tutor" }, onDownload, Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Download, loading = checking,
            )
        }
    }
}

/** Three steps: accept Google's Gemma licence → create a Read token → paste it and download. */
@Composable
private fun DownloadSheet(vm: AiViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val access by vm.access.collectAsStateWithLifecycle()
    var token by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val metered = remember { vm.isMetered() }
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    StudINBottomSheet(onDismiss = onDismiss, title = "Download AI Tutor") {
        Text(
            "${GemmaModel.DISPLAY_NAME} (int4, ${GemmaModel.sizeLabel}) comes from the LiteRT community repository on Hugging Face, where Google publishes Gemma for phones. " +
                "Google asks every user to accept the Gemma licence once — it's free.",
            style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.md))
        SetupStep(1, "Accept the Gemma licence", "Sign in to Hugging Face (free) and tap “Acknowledge license” on the model page.") {
            StudINOutlinedButton("Open model page", { open(GemmaModel.MODEL_PAGE) }, icon = Icons.AutoMirrored.Rounded.OpenInNew)
        }
        SetupStep(2, "Create an access token", "In Settings → Access Tokens, create a token with the “Read” role and copy it.") {
            StudINOutlinedButton("Open token settings", { open(GemmaModel.TOKENS_PAGE) }, icon = Icons.AutoMirrored.Rounded.OpenInNew)
        }
        SetupStep(3, "Paste it and download", null) {
            StudINTextField(
                value = token, onValueChange = { token = it.trim().take(200); vm.resetAccess() },
                label = "Hugging Face token", placeholder = "hf_…",
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                isError = access is AccessCheck.Error,
                supportingText = (access as? AccessCheck.Error)?.message,
                trailingIcon = {
                    StudINIconButton(if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (show) "Hide token" else "Show token", { show = !show })
                },
            )
        }
        if (metered) {
            Text(
                "You're on mobile data. This is a ${GemmaModel.sizeLabel} download — Wi-Fi is recommended.",
                style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.warning,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
        }
        StudINPrimaryButton(
            if (access == AccessCheck.Checking) "Checking access…" else "Download ${GemmaModel.sizeLabel}",
            onClick = { vm.startDownload(token, onStarted = onDismiss) },
            modifier = Modifier.fillMaxWidth(), icon = Icons.Rounded.Download,
            enabled = token.length >= 8, loading = access == AccessCheck.Checking,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Your token is used only to authorise this download and is cleared when it finishes. The file is checked against " +
                "Google's published SHA-256 before use. By downloading you accept the Gemma Terms of Use.",
            style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
        )
        StudINTextButton("Read the Gemma Terms of Use", { open(GemmaModel.TERMS_PAGE) })
    }
}

@Composable
private fun SetupStep(n: Int, title: String, body: String?, action: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = Spacing.md), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(28.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("$n", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer) }
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (body != null) Text(body, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
            Spacer(Modifier.height(Spacing.xs))
            action()
        }
    }
}
