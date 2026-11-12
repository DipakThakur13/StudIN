package com.studin.app.feature.ai

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
import com.studin.app.ai.ContentCheck
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

private enum class AiTab(val label: String, val icon: ImageVector) {
    TUTOR("Tutor", Icons.Rounded.Psychology), CHECK("Content check", Icons.AutoMirrored.Rounded.FactCheck), MODEL("Model", Icons.Rounded.Memory)
}

@Composable
fun AiScreen(onNavigate: (String) -> Unit, vm: AiViewModel = hiltViewModel()) {
    val llm by vm.llmState.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val status = engineStatus(llm, settings.aiUseLlm, thinking)

    StudINScaffold(
        title = "AI Tutor",
        subtitle = "Your personal, offline study assistant",
        actions = { EngineBadge(status); Spacer(Modifier.width(Spacing.md)) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Shorter label when the effective width (dp ÷ font scale) can't fit "Content check".
            val config = LocalConfiguration.current
            val compactTabs = config.screenWidthDp / config.fontScale < 390f
            StudINSegmentedTabs(
                options = AiTab.entries.map { if (compactTabs && it == AiTab.CHECK) "Check" else it.label }, selectedIndex = tab, onSelect = { tab = it },
                modifier = Modifier.studinContentWidth().padding(horizontal = Spacing.screen),
            )
            Spacer(Modifier.height(Spacing.xs))
            val ms = motionDuration(Motion.card)
            AnimatedContent(tab, label = "aiTab", transitionSpec = { fadeIn(tween(ms)) togetherWith fadeOut(tween(ms / 2)) }, modifier = Modifier.weight(1f)) { t ->
                when (AiTab.entries[t]) {
                    AiTab.TUTOR -> TutorTab(vm, onNavigate)
                    AiTab.CHECK -> CheckTab(vm)
                    AiTab.MODEL -> ModelTab(vm, llm, settings.aiUseLlm, settings.aiModelSha256, thinking)
                }
            }
        }
    }
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
private fun TutorTab(vm: AiViewModel, onNavigate: (String) -> Unit) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    var requestFocus by remember { mutableStateOf(false) }
    val reduced = StudINTheme.reducedMotion
    val padding = screenContentPadding(PaddingValues(), top = Spacing.xs, bottom = Spacing.md)

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last >= 0) { if (reduced) listState.scrollToItem(last) else listState.animateScrollToItem(last) }
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
                    else "Ask about a concept, summarise your notes or plan revision. I answer from your own cards and syllabus – and from your imported model, if you add one.",
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
                                StudINIconButton(Icons.Rounded.ContentCopy, "Copy reply", { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("StudIN reply", m.text))) } },
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        blocks.forEach { b ->
            when (b) {
                is ReplyBlock.Paragraph -> Text(b.text, style = MaterialTheme.typography.bodyMedium)
                is ReplyBlock.Bullet -> Row {
                    Box(Modifier.padding(top = 8.dp).size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(b.text, style = MaterialTheme.typography.bodyMedium)
                }
                is ReplyBlock.Numbered -> Row {
                    Text("${b.number}.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(22.dp))
                    Text(b.text, style = MaterialTheme.typography.bodyMedium)
                }
                is ReplyBlock.Code -> Text(
                    b.text,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().background(StudINTheme.colors.surfaceSecondary, StudINShapes.badge).padding(Spacing.xs),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Content check

private val samples = listOf(
    "Kinematics One Shot | JEE Physics",
    "BGMI funny moments gameplay",
    "Polity lecture: Fundamental Rights",
    "Physics in GTA 5 explained",
    "रसायन अध्याय 3 व्याख्यान",
    "Sunday vlog",
)

@Composable
private fun CheckTab(vm: AiViewModel) {
    val result by vm.check.collectAsStateWithLifecycle()
    val checkedText by vm.checkedText.collectAsStateWithLifecycle()
    val checking by vm.checking.collectAsStateWithLifecycle()
    val error by vm.checkError.collectAsStateWithLifecycle()
    val decisions by vm.decisions.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().imePadding().studinContentWidth(),
        contentPadding = screenContentPadding(PaddingValues()),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            StudINCard(Modifier.fillMaxWidth()) {
                Text("Test what Focus Guard would do", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    "Paste a video or post title. StudIN classifies it on this phone with the same rules Smart mode uses.",
                    style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                )
                Spacer(Modifier.height(Spacing.sm))
                StudINTextField(
                    value = title, onValueChange = { title = it.take(300) }, label = "Video or post title",
                    imeAction = ImeAction.Search, onImeAction = { vm.checkTitle(title) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                )
                Spacer(Modifier.height(Spacing.sm))
                StudINPrimaryButton("Check title", { vm.checkTitle(title) }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.FactCheck,
                    enabled = title.isNotBlank(), loading = checking)
                Spacer(Modifier.height(Spacing.sm))
                Text("Try an example", style = MaterialTheme.typography.labelLarge, color = StudINTheme.colors.textSecondary)
                Spacer(Modifier.height(Spacing.xs))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    samples.forEach { s -> StudINChip(s, onClick = { title = s; vm.checkTitle(s) }) }
                }
            }
        }
        error?.let { e -> item { StudINErrorState(e, onRetry = { vm.checkTitle(title) }) } }
        result?.let { r -> item(key = "verdict") { VerdictCard(r, checkedText.orEmpty()) } }
        item { HowItDecides() }
        item { StudINSectionHeader("Recent Focus Guard decisions", Modifier.padding(top = Spacing.xs), supporting = "Titles are never stored – only the matched keywords") }
        if (decisions.isEmpty()) {
            item {
                Text(
                    "No decisions yet. They appear here when Focus Guard checks Smart-mode apps during a focus session.",
                    style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary,
                )
            }
        } else {
            item {
                StudINCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xxs)) {
                    decisions.forEachIndexed { i, d ->
                        if (i > 0) HorizontalDivider(color = StudINTheme.colors.border, modifier = Modifier.padding(horizontal = Spacing.card))
                        DecisionRow(d)
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItDecides() {
    StudINCard(Modifier.fillMaxWidth(), containerColor = StudINTheme.colors.surfaceSecondary, border = null) {
        Text("How Smart mode decides", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.xs))
        TierLine("Tier 0", "Your allow / block keyword lists always win")
        TierLine("Tier 1", "Syllabus-aware lexicon with Hindi–English terms")
        TierLine("Tier 2", "Optional on-device model for unclear titles (advisory only)")
        Spacer(Modifier.height(Spacing.xxs))
        Text("A break screen only appears at high confidence. When unsure, StudIN allows.",
            style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
    }
}

@Composable
private fun TierLine(tier: String, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        StudINBadge(tier, container = MaterialTheme.colorScheme.surface)
        Spacer(Modifier.width(Spacing.xs))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

private data class VerdictStyle(val icon: ImageVector, val container: Color, val content: Color)

@Composable
private fun verdictStyle(v: Verdict): VerdictStyle {
    val c = StudINTheme.colors
    return when (v) {
        Verdict.ALLOW -> VerdictStyle(Icons.Rounded.CheckCircle, c.successContainer, c.success)
        Verdict.WARN -> VerdictStyle(Icons.Rounded.Warning, c.warningContainer, c.warning)
        Verdict.BLOCK -> VerdictStyle(Icons.Rounded.Shield, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        Verdict.UNKNOWN -> VerdictStyle(Icons.AutoMirrored.Rounded.HelpOutline, c.surfaceSecondary, c.textSecondary)
    }
}

@Composable
private fun VerdictCard(c: ContentCheck, evaluated: String) {
    val r = c.result
    val style = verdictStyle(r.verdict)
    StudINCard(Modifier.fillMaxWidth()) {
        Text("Evaluated", style = MaterialTheme.typography.labelMedium, color = StudINTheme.colors.textSecondary)
        Text("“$evaluated”", style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(Spacing.sm))
        Row(
            Modifier.fillMaxWidth().background(style.container, StudINShapes.smallCard).padding(Spacing.sm)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(style.icon, contentDescription = null, tint = style.content)
            Spacer(Modifier.width(Spacing.xs))
            Column(Modifier.weight(1f)) {
                Text(verdictLabel(r.verdict), style = MaterialTheme.typography.titleSmall, color = style.content)
                Text(r.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        if (r.confidence > 0.0) {
            MeterRow("Confidence in this verdict", r.confidence.toFloat(), style.content)
            Spacer(Modifier.height(Spacing.xs))
        }
        MeterRow("Looks like study material", r.studyProbability.toFloat(), MaterialTheme.colorScheme.primary)
        val signals = r.syllabusMatches.map { "Syllabus: $it" } + r.studySignals.map { "Study: $it" } + r.distractionSignals.map { "Distraction: $it" }
        if (signals.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            Text("Signals found", style = MaterialTheme.typography.labelLarge, color = StudINTheme.colors.textSecondary)
            Spacer(Modifier.height(Spacing.xxs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs + 2.dp), verticalArrangement = Arrangement.spacedBy(Spacing.xxs + 2.dp)) {
                signals.forEach { StudINBadge(it) }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Tier ${r.tier} · ${if (c.engine == AiEngine.LLM) "On-device model" else "Lite AI"} · ${c.latencyMs} ms · ${r.ruleVersion}",
            style = MaterialTheme.typography.labelSmall, color = StudINTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun MeterRow(label: String, value: Float, color: Color) {
    val pct = (value * 100).toInt().coerceIn(0, 100)
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$label: $pct percent" }) {
        Row {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text("$pct%", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(Spacing.xxs))
        StudINLinearProgress(value, color = color, height = 6.dp)
    }
}

@Composable
private fun DecisionRow(d: ClassificationDecisionEntity) {
    val v = runCatching { Verdict.valueOf(d.label) }.getOrDefault(Verdict.UNKNOWN)
    val style = verdictStyle(v)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(d.timestamp))
    val app = d.sourceApp.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.card, vertical = Spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "${decisionLabel(d.label)} in $app at $time. Signals: ${d.redactedReason}" + if (d.overridden) ". You overrode it" else "" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(style.icon, null, tint = style.content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.weight(1f)) {
            Text("${decisionLabel(d.label)} · $app", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(d.redactedReason, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, style = MaterialTheme.typography.labelMedium, color = StudINTheme.colors.textSecondary)
            if (d.overridden) Text("Overridden", style = MaterialTheme.typography.labelSmall, color = StudINTheme.colors.accent)
        }
    }
}

// ------------------------------------------------------------------ Model

@Composable
private fun ModelTab(vm: AiViewModel, llm: LlmState, useLlm: Boolean, sha: String?, thinking: Boolean) {
    val model by vm.model.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importModel) }
    val c = StudINTheme.colors
    LazyColumn(
        Modifier.fillMaxSize().studinContentWidth(),
        contentPadding = screenContentPadding(PaddingValues()),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            StudINCard(Modifier.fillMaxWidth()) {
                val (title, sub, tone) = when (llm) {
                    LlmState.NotInstalled -> Triple("Model unavailable", "No model imported. StudIN Lite AI answers from your cards and syllabus on every phone.", c.ai)
                    is LlmState.Installed -> Triple(llm.name, "${formatBytes(llm.sizeBytes)} · installed, loads on first use", c.ai)
                    is LlmState.Loading -> Triple("Model loading", "Loading ${llm.name}. This can take 5–20 seconds.", c.ai)
                    is LlmState.Ready -> Triple(llm.name, if (thinking) "${formatBytes(llm.sizeBytes)} · processing a reply" else "${formatBytes(llm.sizeBytes)} · ready, fully offline", c.success)
                    is LlmState.Unsupported -> Triple("Not supported on this phone", llm.reason, c.textSecondary)
                    is LlmState.Error -> Triple("Model error", llm.message, MaterialTheme.colorScheme.error)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudINIconTile(if (llm is LlmState.Error) Icons.Rounded.ErrorOutline else Icons.Rounded.Memory, tone, 44.dp)
                    Spacer(Modifier.width(Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() })
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                }
                if (llm is LlmState.Loading || (thinking && llm is LlmState.Ready)) {
                    Spacer(Modifier.height(Spacing.sm))
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.ai, trackColor = c.focusTrack)
                }
                if (!sha.isNullOrBlank() && (llm is LlmState.Installed || llm is LlmState.Ready)) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text("SHA-256 – compare with the publisher's checksum", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                    SelectionContainer {
                        Text(sha, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.fillMaxWidth().background(c.surfaceSecondary, StudINShapes.badge).padding(Spacing.xs))
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                when (llm) {
                    is LlmState.Installed -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        StudINPrimaryButton("Load now", { vm.load() }, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
                        StudINOutlinedButton("Delete", { confirmDelete = true }, icon = Icons.Rounded.Delete, contentColor = MaterialTheme.colorScheme.error)
                    }
                    is LlmState.Ready -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        StudINOutlinedButton("Unload", { vm.unload() }, Modifier.weight(1f), icon = Icons.Rounded.Stop, enabled = !thinking)
                        StudINOutlinedButton("Delete", { confirmDelete = true }, icon = Icons.Rounded.Delete, enabled = !thinking, contentColor = MaterialTheme.colorScheme.error)
                    }
                    is LlmState.Loading, is LlmState.Unsupported -> Unit
                    else -> StudINPrimaryButton(
                        if (model.importing) "Importing…" else "Import model file", { picker.launch(arrayOf("*/*")) },
                        Modifier.fillMaxWidth(), icon = Icons.Rounded.Upload, loading = model.importing,
                    )
                }
                if (model.importing) {
                    Spacer(Modifier.height(Spacing.sm))
                    val p = model.progress
                    if (p != null && p.totalBytes > 0) {
                        StudINLinearProgress(p.copiedBytes.toFloat() / p.totalBytes, color = c.ai, label = "Import progress")
                        Spacer(Modifier.height(Spacing.xxs))
                        Text("${formatBytes(p.copiedBytes)} of ${formatBytes(p.totalBytes)} · verifying checksum", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                    } else LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.ai, trackColor = c.focusTrack)
                }
                model.message?.let {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        }
        item {
            StudINCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.card, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StudINIconTile(Icons.Rounded.Bolt, c.accent, 36.dp)
                    Spacer(Modifier.width(Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text("Use on-device model", style = MaterialTheme.typography.titleMedium)
                        Text("Off = always use the instant Lite engine (saves battery).", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                    Switch(checked = useLlm, onCheckedChange = vm::setUseLlm,
                        modifier = Modifier.semantics { contentDescription = "Use on-device model" })
                }
            }
        }
        item {
            StudINCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudINIconTile(Icons.Rounded.Download, MaterialTheme.colorScheme.primary, 36.dp)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Getting a model", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                }
                Spacer(Modifier.height(Spacing.xs))
                Guide("Recommended: Gemma 3 1B IT, int4, MediaPipe .task format (about 0.5–0.6 GB).")
                Guide("Download it yourself from Kaggle (Gemma models) or Hugging Face (LiteRT community), accept the licence, then copy it to this phone.")
                Guide("Tap Import and pick the file. StudIN copies it into private storage and shows its SHA-256 so you can verify it.")
                Guide("Requirements: a 64-bit phone with at least 4 GB RAM. Runs fully offline – StudIN has no internet permission.")
                Guide("Lite AI (summaries, flashcards from notes, answers from your cards, smart filtering) always works as a fallback.")
            }
        }
    }
    if (confirmDelete) {
        StudINDialog(
            title = "Delete the model?", onDismiss = { confirmDelete = false },
            confirmLabel = "Delete", onConfirm = { confirmDelete = false; vm.deleteModel() }, destructive = true, icon = Icons.Rounded.Delete,
            text = { Text("The model file is removed from this phone. Lite AI keeps working; you can import the model again later.") },
        )
    }
}

@Composable
private fun Guide(text: String) {
    Row(Modifier.padding(vertical = Spacing.xxs)) {
        Box(Modifier.padding(top = 7.dp).size(5.dp).background(LocalContentColor.current.copy(alpha = 0.6f), CircleShape))
        Spacer(Modifier.width(Spacing.xs))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
