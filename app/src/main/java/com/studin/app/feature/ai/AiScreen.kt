package com.studin.app.feature.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.ai.AiEngine
import com.studin.app.ai.ContentCheck
import com.studin.app.ai.LlmState
import com.studin.app.core.data.db.ClassificationDecisionEntity
import com.studin.app.core.designsystem.Badge
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.GradientProgressBar
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.Pill
import com.studin.app.core.designsystem.SectionHeader
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.ui.navigation.BottomBarPadding
import com.studin.domain.model.Verdict
import java.text.DateFormat
import java.util.Date

private enum class AiTab(val label: String) { TUTOR("Tutor"), CHECK("Content check"), MODEL("Model") }

@Composable
fun AiScreen(onNavigate: (String) -> Unit, vm: AiViewModel = hiltViewModel()) {
    val llm by vm.llmState.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val llmActive = settings.aiUseLlm && (llm is LlmState.Ready || llm is LlmState.Installed)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("AI Tutor", style = MaterialTheme.typography.headlineMedium)
                Text("Private. Offline. On your phone.", style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted)
            }
            EngineStatusPill(llm, llmActive)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiTab.entries.forEachIndexed { i, t ->
                Pill(t.label, tab == i, { tab = i }, icon = when (t) {
                    AiTab.TUTOR -> Icons.Rounded.Psychology; AiTab.CHECK -> Icons.Rounded.FactCheck; AiTab.MODEL -> Icons.Rounded.Memory
                })
            }
        }
        Spacer(Modifier.height(8.dp))
        AnimatedContent(tab, label = "tab", transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, modifier = Modifier.weight(1f)) { t ->
            when (AiTab.entries[t]) {
                AiTab.TUTOR -> TutorTab(vm)
                AiTab.CHECK -> CheckTab(vm)
                AiTab.MODEL -> ModelTab(vm, llm, settings.aiUseLlm, settings.aiModelSha256)
            }
        }
    }
}

@Composable
private fun EngineStatusPill(llm: LlmState, active: Boolean) {
    val (label, color) = when {
        active && llm is LlmState.Ready -> "LLM ready" to StudinColors.Mint
        active && llm is LlmState.Installed -> "LLM on demand" to StudinColors.Cyan
        llm is LlmState.Loading -> "Loading…" to StudinColors.Star
        else -> "Lite AI" to StudinColors.Violet
    }
    val pulse = rememberInfiniteTransition(label = "p").animateFloat(0.4f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pa")
    Row(
        Modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).border(1.dp, color.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).alpha(pulse.value).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

// ------------------------------------------------------------------ Tutor

@Composable
private fun TutorTab(vm: AiViewModel) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size)
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "intro") {
                if (messages.isEmpty()) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBubble(Icons.Rounded.AutoAwesome, StudinColors.Mint, 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Hi! I'm your study buddy.", style = MaterialTheme.typography.titleMedium)
                                Text("I use your own flashcards and syllabus. Chats stay in memory and vanish when you leave.",
                                    style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        GhostButton("Clear chat", vm::clearChat, icon = Icons.Rounded.DeleteSweep)
                    }
                }
            }
            items(messages, key = { it.id }) { m -> ChatBubble(m) }
        }
        if (messages.isEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("What should I study today?", "Explain Coulomb's law", "Quiz me tips", "I can't focus").forEach {
                    Pill(it, false, { vm.send(it) })
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = BottomBarPadding - 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input, onValueChange = { input = it.take(2000) },
                modifier = Modifier.weight(1f), placeholder = { Text("Ask anything, or paste notes to summarise") },
                shape = RoundedCornerShape(22.dp), maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { vm.send(input); input = "" }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Studin.extras.glass, unfocusedContainerColor = Studin.extras.glass,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(Studin.extras.heroGradient),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { if (thinking) vm.stop() else { vm.send(input); input = "" } }) {
                    Icon(if (thinking) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send, contentDescription = if (thinking) "Stop" else "Send", tint = StudinColors.Navy950)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(m: ChatMessage) {
    val shape = if (m.fromUser) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 320.dp).clip(shape)
                .then(
                    if (m.fromUser) Modifier.background(Studin.extras.heroGradient)
                    else Modifier.background(Studin.extras.glass).border(1.dp, Studin.extras.glassBorder, shape)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .animateContentSize(),
        ) {
            if (!m.fromUser && m.streaming && m.text.isEmpty()) TypingDots()
            else SelectionContainer {
                Text(m.text, style = MaterialTheme.typography.bodyMedium, color = if (m.fromUser) StudinColors.Navy950 else MaterialTheme.colorScheme.onSurface)
            }
            if (!m.fromUser && m.engine != null && !m.streaming) {
                Spacer(Modifier.height(6.dp))
                Badge(m.engine.label, if (m.engine == AiEngine.LLM) StudinColors.Mint else StudinColors.Violet)
            }
        }
    }
}

@Composable
private fun TypingDots() {
    val t = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        repeat(3) { i ->
            val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(600, delayMillis = i * 150), RepeatMode.Reverse), label = "d$i")
            Box(Modifier.size(8.dp).alpha(a).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

// ------------------------------------------------------------------ Content check

@Composable
private fun CheckTab(vm: AiViewModel) {
    val result by vm.check.collectAsStateWithLifecycle()
    val checking by vm.checking.collectAsStateWithLifecycle()
    val decisions by vm.decisions.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = BottomBarPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("How Smart mode decides", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                TierLine("Tier 0", "Your allow / block lists – always win")
                TierLine("Tier 1", "Syllabus-aware lexicon with Hindi–English terms (<1 ms)")
                TierLine("Tier 2", "Optional on-device LLM for unclear titles (advisory only)")
                Spacer(Modifier.height(6.dp))
                Text("BLOCK only happens at high confidence. When unsure, StudIN allows.",
                    style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
            }
        }
        item {
            OutlinedTextField(
                value = title, onValueChange = { title = it.take(300) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Video or post title") }, shape = RoundedCornerShape(18.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.checkTitle(title) }),
                trailingIcon = {
                    if (checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = { vm.checkTitle(title) }) { Icon(Icons.Rounded.Search, "Check") }
                },
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Kinematics One Shot | JEE Physics",
                    "BGMI funny moments gameplay",
                    "Polity lecture: Fundamental Rights",
                    "Physics in GTA 5 explained",
                    "रसायन अध्याय 3 व्याख्यान",
                    "Sunday vlog",
                ).forEach { s -> Pill(s, false, { title = s; vm.checkTitle(s) }) }
            }
        }
        item { result?.let { VerdictCard(it) } }
        item { SectionHeader("Recent Focus Guard decisions") }
        if (decisions.isEmpty()) {
            item {
                Text("No decisions yet. They appear here when Focus Guard checks Smart-mode apps during a session (titles are never stored).",
                    style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
            }
        }
        items(decisions, key = { it.id }) { DecisionRow(it) }
    }
}

@Composable
private fun TierLine(tier: String, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Badge(tier, StudinColors.Cyan)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun verdictColor(v: Verdict): Color = when (v) {
    Verdict.ALLOW -> StudinColors.Mint
    Verdict.WARN -> StudinColors.Star
    Verdict.BLOCK -> StudinColors.Coral
    Verdict.UNKNOWN -> StudinColors.InkMuted
}

@Composable
private fun VerdictCard(c: ContentCheck) {
    val r = c.result
    val color = verdictColor(r.verdict)
    GlassCard(Modifier.fillMaxWidth(), highlight = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(
                when (r.verdict) { Verdict.ALLOW -> Icons.Rounded.CheckCircle; Verdict.BLOCK -> Icons.Rounded.Shield; Verdict.WARN -> Icons.Rounded.Warning; Verdict.UNKNOWN -> Icons.Rounded.Psychology },
                color, 52.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(r.verdict.name, style = MaterialTheme.typography.headlineSmall, color = color)
                Text(r.reason, style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        MeterRow("Confidence", r.confidence.toFloat(), color)
        Spacer(Modifier.height(10.dp))
        MeterRow("Study probability", r.studyProbability.toFloat(), StudinColors.Cyan)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            r.syllabusMatches.forEach { Badge("syllabus · $it", StudinColors.Mint) }
            r.studySignals.forEach { Badge("study · $it", StudinColors.Cyan) }
            r.distractionSignals.forEach { Badge("distraction · $it", StudinColors.Coral) }
        }
        Spacer(Modifier.height(10.dp))
        Text("Tier ${r.tier} · ${c.engine.label} · ${c.latencyMs} ms · ${r.ruleVersion}",
            style = MaterialTheme.typography.labelSmall, color = Studin.extras.muted)
    }
}

@Composable
private fun MeterRow(label: String, value: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(130.dp))
        Box(Modifier.weight(1f)) { GradientProgressBar(value, color = color) }
        Spacer(Modifier.width(10.dp))
        Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(40.dp))
    }
}

@Composable
private fun DecisionRow(d: ClassificationDecisionEntity) {
    val v = runCatching { Verdict.valueOf(d.label) }.getOrDefault(Verdict.UNKNOWN)
    val color = verdictColor(v)
    GlassCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${d.label} · ${d.sourceApp.substringAfterLast('.')}", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(d.redactedReason, style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${(d.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = color)
                Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(d.timestamp)), style = MaterialTheme.typography.labelSmall, color = Studin.extras.muted)
                if (d.overridden) Text("overridden", style = MaterialTheme.typography.labelSmall, color = StudinColors.Star)
            }
        }
    }
}

// ------------------------------------------------------------------ Model

@Composable
private fun ModelTab(vm: AiViewModel, llm: LlmState, useLlm: Boolean, sha: String?) {
    val model by vm.model.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importModel) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = BottomBarPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(Modifier.fillMaxWidth(), highlight = llm is LlmState.Ready) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Memory, if (llm is LlmState.Ready) StudinColors.Mint else StudinColors.Violet, 52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        val (title, sub) = when (llm) {
                            LlmState.NotInstalled -> "No model imported" to "StudIN Lite AI is active – works on every phone."
                            is LlmState.Installed -> llm.name to "${mb(llm.sizeBytes)} · loads on first use"
                            is LlmState.Loading -> "Loading ${llm.name}…" to "This can take 5–20 seconds"
                            is LlmState.Ready -> llm.name to "${mb(llm.sizeBytes)} · ready, fully offline"
                            is LlmState.Unsupported -> "Not supported here" to llm.reason
                            is LlmState.Error -> "Something went wrong" to llm.message
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                    }
                }
                if (llm is LlmState.Loading) { Spacer(Modifier.height(10.dp)); LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape)) }
                if (!sha.isNullOrBlank() && (llm is LlmState.Installed || llm is LlmState.Ready)) {
                    Spacer(Modifier.height(12.dp))
                    Text("SHA-256 (compare with the publisher's checksum)", style = MaterialTheme.typography.labelSmall, color = Studin.extras.muted)
                    SelectionContainer {
                        Text(sha, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (llm) {
                        is LlmState.Installed -> {
                            GradientButton("Load", vm::load, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
                            GhostButton("Delete", vm::deleteModel, icon = Icons.Rounded.Delete, color = StudinColors.Coral)
                        }
                        is LlmState.Ready -> {
                            GhostButton("Unload", vm::unload, Modifier.weight(1f), icon = Icons.Rounded.Stop)
                            GhostButton("Delete", vm::deleteModel, icon = Icons.Rounded.Delete, color = StudinColors.Coral)
                        }
                        is LlmState.Unsupported -> {}
                        else -> GradientButton(
                            if (model.importing) "Importing…" else "Import model file", { picker.launch(arrayOf("*/*")) },
                            Modifier.weight(1f), icon = Icons.Rounded.Upload, enabled = !model.importing,
                        )
                    }
                }
                if (model.importing) {
                    Spacer(Modifier.height(12.dp))
                    val p = model.progress
                    if (p != null && p.totalBytes > 0) {
                        GradientProgressBar(p.copiedBytes.toFloat() / p.totalBytes)
                        Text("${mb(p.copiedBytes)} / ${mb(p.totalBytes)} · verifying checksum", style = MaterialTheme.typography.labelSmall, color = Studin.extras.muted)
                    } else LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape))
                }
                model.message?.let { Spacer(Modifier.height(10.dp)); Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Bolt, StudinColors.Star)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Use on-device LLM", style = MaterialTheme.typography.titleMedium)
                        Text("Turn off to always use the instant Lite engine (saves battery).", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                    }
                    Switch(checked = useLlm, onCheckedChange = vm::setUseLlm)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Download, StudinColors.Cyan)
                    Spacer(Modifier.width(12.dp))
                    Text("Getting a model", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
                Guide("Recommended: Gemma 3 1B IT, int4, MediaPipe `.task` format (≈0.5–0.6 GB).")
                Guide("Download it yourself from Kaggle (Gemma models) or Hugging Face (LiteRT community) in a browser, accept the licence, then copy it to this phone.")
                Guide("Tap Import and pick the file. StudIN copies it into private storage and shows its SHA-256 so you can verify it.")
                Guide("Requirements: 64-bit phone with at least 4 GB RAM. Runs fully offline – StudIN has no internet permission.")
                Guide("Lite AI (summaries, flashcards from notes, retrieval over your cards, smart filtering) always works as a fallback.")
            }
        }
    }
}

@Composable
private fun Guide(text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun mb(bytes: Long) = if (bytes >= 1_000_000_000) "%.2f GB".format(bytes / 1e9) else "${bytes / 1_000_000} MB"
