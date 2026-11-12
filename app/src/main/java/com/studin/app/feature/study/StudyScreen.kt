package com.studin.app.feature.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.data.db.DeckWithCounts
import com.studin.app.core.data.db.SyllabusTopicEntity
import com.studin.app.core.designsystem.Badge
import com.studin.app.core.designsystem.EmptyState
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.GradientProgressBar
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.ProgressRing
import com.studin.app.core.designsystem.SectionHeader
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.ui.navigation.BottomBarPadding
import com.studin.app.ui.navigation.Routes

@Composable
fun StudyScreen(onNavigate: (String) -> Unit, vm: StudyViewModel = hiltViewModel()) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val due by vm.dueCount.collectAsStateWithLifecycle()
    val topics by vm.topics.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var showNewDeck by remember { mutableStateOf(false) }
    var showAddTopic by remember { mutableStateOf(false) }
    var deleteTopic by remember { mutableStateOf<SyllabusTopicEntity?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Study", style = MaterialTheme.typography.headlineLarge)
                Text("Active recall beats re-reading", style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted)
            }
            IconButton(onClick = { if (tab == 0) showNewDeck = true else showAddTopic = true }) {
                Icon(Icons.Rounded.Add, contentDescription = if (tab == 0) "New deck" else "Add topic")
            }
        }
        Spacer(Modifier.height(14.dp))
        SegmentedTabs(listOf("Decks", "Syllabus"), tab, { tab = it }, Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
            if (t == 0) DecksTab(decks, due, onNavigate, onNewDeck = { showNewDeck = true })
            else SyllabusTab(
                topics = topics,
                onCycle = vm::cyclePriority,
                onToggle = vm::toggleCompleted,
                onFocus = { vm.focusOn(it) { onNavigate(Routes.FOCUS) } },
                onDelete = { deleteTopic = it },
                onAdd = { showAddTopic = true },
            )
        }
    }

    if (showNewDeck) TextInputDialog(
        title = "New deck", firstLabel = "Deck title", confirmText = "Create",
        onDismiss = { showNewDeck = false },
        onConfirm = { title, _ -> showNewDeck = false; vm.createDeck(title) { onNavigate(Routes.deck(it)) } },
    )
    if (showAddTopic) TextInputDialog(
        title = "Add topic", firstLabel = "Subject", secondLabel = "Topic title", confirmText = "Add",
        onDismiss = { showAddTopic = false },
        onConfirm = { subject, title -> showAddTopic = false; vm.addTopic(subject, title) },
    )
    deleteTopic?.let { t ->
        ConfirmDialog(
            "Delete topic?", "\"${t.title}\" and its progress will be removed from your syllabus.", "Delete",
            onDismiss = { deleteTopic = null }, onConfirm = { vm.deleteTopic(t.id); deleteTopic = null },
        )
    }
}

@Composable
internal fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ex = Studin.extras
    BoxWithConstraints(
        modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(ex.glass).padding(4.dp)
    ) {
        val w = maxWidth / labels.size
        val offset by animateDpAsState(w * selected, spring(dampingRatio = 0.75f, stiffness = 400f), label = "tabIndicator")
        Box(Modifier.offset(x = offset).width(w).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(ex.heroGradient))
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, style = MaterialTheme.typography.labelLarge,
                        color = if (i == selected) StudinColors.Navy950 else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Decks tab

@Composable
private fun DecksTab(decks: List<DeckWithCounts>?, due: Int, onNavigate: (String) -> Unit, onNewDeck: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = BottomBarPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DueHero(due) { onNavigate(Routes.review()) } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Quiz", Icons.Rounded.Quiz, StudinColors.Cyan, Modifier.weight(1f)) { onNavigate(Routes.quiz()) }
                QuickAction("AI cards", Icons.Rounded.AutoAwesome, StudinColors.Violet, Modifier.weight(1f)) { onNavigate(Routes.generate()) }
                QuickAction("Import", Icons.Rounded.FileUpload, StudinColors.Star, Modifier.weight(1f)) { onNavigate(Routes.IMPORT) }
                QuickAction("New", Icons.Rounded.Add, StudinColors.Mint, Modifier.weight(1f), onNewDeck)
            }
        }
        item { SectionHeader("Your decks") }
        when {
            decks == null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
            decks.isEmpty() -> item {
                EmptyState(Icons.Rounded.Style, "No decks yet", "Create a deck, import a CSV or let on-device AI turn your notes into flashcards.") {
                    GradientButton("Generate with AI", { onNavigate(Routes.generate()) }, icon = Icons.Rounded.AutoAwesome)
                }
            }
            else -> items(decks, key = { it.id }) { d -> DeckCard(d) { onNavigate(Routes.deck(d.id)) } }
        }
    }
}

@Composable
private fun DueHero(due: Int, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick = if (due > 0) onClick else null, highlight = due > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (due > 0) "Ready to review" else "All caught up", style = MaterialTheme.typography.labelLarge, color = Studin.extras.muted)
                Text(
                    if (due > 0) "$due card${if (due == 1) "" else "s"} due" else "Nothing due",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (due > 0) "A few minutes now saves hours later." else "Come back later or quiz yourself for fun.",
                    style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted,
                )
            }
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Studin.extras.heroGradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (due > 0) Icons.Rounded.Style else Icons.Rounded.CheckCircle, null, tint = StudinColors.Navy950, modifier = Modifier.size(32.dp))
            }
        }
        if (due > 0) {
            Spacer(Modifier.height(14.dp))
            GradientButton("Start review", onClick, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier, onClick = onClick, contentPadding = 12.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBubble(icon, accent, 40.dp)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DeckCard(d: DeckWithCounts, onClick: () -> Unit) {
    val accent = Color(d.color.toInt())
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).height(46.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.3f)))))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(d.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val subtitle = listOf(d.subject, sourceLabel(d.source)).filter { it.isNotBlank() }.joinToString(" · ")
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted, maxLines = 1)
            }
            if (d.due > 0) Badge("${d.due} due", StudinColors.Star) else Badge("${d.total} cards", accent)
        }
        Spacer(Modifier.height(12.dp))
        GradientProgressBar(if (d.total == 0) 0f else d.mastered.toFloat() / d.total, color = accent)
        Spacer(Modifier.height(6.dp))
        Text(
            "${d.mastered}/${d.total} mastered", style = MaterialTheme.typography.labelSmall, color = Studin.extras.muted,
        )
    }
}

internal fun sourceLabel(source: String) = when (source) {
    "starter" -> "Starter pack"; "csv" -> "Imported"; "ai" -> "AI generated"; "scan" -> "Scanned notes"; else -> ""
}

// ------------------------------------------------------------------ Syllabus tab

private data class SubjectGroup(val subject: String, val topics: List<SyllabusTopicEntity>) {
    val progress: Float get() = if (topics.isEmpty()) 0f else
        topics.sumOf { if (it.completed) 1.0 else it.mastery }.toFloat() / topics.size
}

@Composable
private fun SyllabusTab(
    topics: List<SyllabusTopicEntity>?,
    onCycle: (SyllabusTopicEntity) -> Unit,
    onToggle: (SyllabusTopicEntity) -> Unit,
    onFocus: (SyllabusTopicEntity) -> Unit,
    onDelete: (SyllabusTopicEntity) -> Unit,
    onAdd: () -> Unit,
) {
    if (topics == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }; return }
    val groups = remember(topics) { topics.groupBy { it.subject }.map { SubjectGroup(it.key, it.value) } }
    val expanded = remember { mutableStateListOf<String>().apply { groups.firstOrNull()?.let { add(it.subject) } } }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = BottomBarPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (groups.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.LibraryBooks, "No syllabus yet", "Add the topics you need to cover and StudIN will plan around them.") {
                    GradientButton("Add topic", onAdd, icon = Icons.Rounded.Add)
                }
            }
            return@LazyColumn
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(groups, key = { it.subject }) { g ->
                    GlassCard(contentPadding = 12.dp) {
                        ProgressRing(g.progress, size = 84.dp, stroke = 8.dp, glow = false) {
                            Text("${(g.progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(g.subject, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.width(84.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        groups.forEach { g ->
            val open = g.subject in expanded
            item(key = "h_${g.subject}") {
                SubjectHeader(g, open) { if (open) expanded.remove(g.subject) else expanded.add(g.subject) }
            }
            if (open) items(g.topics, key = { it.id }) { t ->
                Box(Modifier.animateItem()) {
                    TopicRow(t, { onCycle(t) }, { onToggle(t) }, { onFocus(t) }, { onDelete(t) })
                }
            }
        }
        item {
            Text(
                "Mastery grows with focused minutes and correct answers. It's an estimate, not a grade.",
                style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted, modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SubjectHeader(g: SubjectGroup, open: Boolean, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (open) 180f else 0f, label = "chev")
    val done = g.topics.count { it.completed }
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick, highlight = open) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(g.subject, style = MaterialTheme.typography.titleMedium)
                Text("$done of ${g.topics.size} topics done", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
            }
            Icon(Icons.Rounded.ExpandMore, contentDescription = if (open) "Collapse" else "Expand", modifier = Modifier.rotate(rot))
        }
        Spacer(Modifier.height(10.dp))
        GradientProgressBar(g.progress)
    }
}

@Composable
private fun TopicRow(t: SyllabusTopicEntity, onCycle: () -> Unit, onToggle: () -> Unit, onFocus: () -> Unit, onDelete: () -> Unit) {
    val ex = Studin.extras
    val prioColor = when (t.priority) { 3 -> ex.danger; 1 -> ex.muted; else -> ex.warning }
    GlassCard(Modifier.fillMaxWidth().padding(start = 12.dp), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (t.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (t.completed) "Mark not done" else "Mark done",
                    tint = if (t.completed) ex.success else ex.muted,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (t.completed) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (t.completed) ex.muted else MaterialTheme.colorScheme.onSurface,
                )
                Text("${t.minutesStudied} min studied", style = MaterialTheme.typography.labelSmall, color = ex.muted)
            }
            Text(
                priorityLabel(t.priority), style = MaterialTheme.typography.labelSmall, color = prioColor, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(prioColor.copy(alpha = 0.15f)).clickable(onClick = onCycle)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        GradientProgressBar(if (t.completed) 1f else t.mastery.toFloat(), height = 6.dp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mastery ${(t.mastery * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = ex.muted, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete topic", tint = ex.muted, modifier = Modifier.size(20.dp))
            }
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable(role = Role.Button, onClick = onFocus).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Focus on this", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
