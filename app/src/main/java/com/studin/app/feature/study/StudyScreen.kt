package com.studin.app.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.R
import com.studin.app.core.data.db.DeckWithCounts
import com.studin.app.core.data.db.SyllabusTopicEntity
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINBadge
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINChip
import com.studin.app.core.designsystem.StudINEmptyState
import com.studin.app.core.designsystem.StudINIconButton
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINLinearProgress
import com.studin.app.core.designsystem.StudINLoadingState
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINStatCard
import com.studin.app.core.designsystem.StudINTextButton
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.StudINType
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.app.ui.navigation.Routes

/** Content width at which subject cards switch to a two-column grid. */
private val TwoColumnMinWidth = 560.dp

@Composable
fun StudyScreen(onNavigate: (String) -> Unit, vm: StudyViewModel = hiltViewModel()) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val due by vm.dueCount.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val topics by vm.topics.collectAsStateWithLifecycle()
    val exam by vm.examName.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var showNewDeck by rememberSaveable { mutableStateOf(false) }
    var addTopicSubject by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<SyllabusTopicEntity?>(null) }

    StudINScaffold(
        title = "Study",
        subtitle = exam?.let { "$it syllabus" } ?: "Syllabus, flashcards and practice",
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = if (maxWidth >= TwoColumnMinWidth) 2 else 1
            LazyColumn(
                modifier = Modifier.fillMaxSize().studinContentWidth(),
                contentPadding = screenContentPadding(padding),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                item(key = "due") {
                    DueReviewsCard(due, onReview = { onNavigate(Routes.review()) }, onQuiz = { onNavigate(Routes.quiz()) })
                }

                // ---------------- My subjects
                item(key = "subjects-header") {
                    StudINSectionHeader(
                        "My subjects", Modifier.padding(top = Spacing.md),
                        action = "Add topic", onAction = { addTopicSubject = "" },
                        supporting = topics?.let { t -> "${t.size} chapters · ${t.count { it.completed }} complete" },
                    )
                }
                val subs = subjects
                when {
                    subs == null -> item(key = "subjects-loading") { StudINLoadingState() }
                    subs.isEmpty() -> item(key = "subjects-empty") {
                        StudINCard(Modifier.fillMaxWidth()) {
                            StudINEmptyState(
                                title = "No syllabus yet", message = "Add the chapters you're studying to track progress and plan focus sessions.",
                                icon = Icons.AutoMirrored.Rounded.MenuBook, actionLabel = "Add topic", onAction = { addTopicSubject = "" },
                            )
                        }
                    }
                    else -> subjectGrid(
                        subjects = subs, columns = columns, expanded = expanded,
                        topicsFor = { s -> topics.orEmpty().filter { it.subject == s } },
                        onToggle = { s -> expanded = if (expanded == s) null else s },
                        onContinue = { t -> vm.focusOn(t) { onNavigate(Routes.FOCUS) } },
                        onToggleDone = vm::toggleCompleted,
                        onCyclePriority = vm::cyclePriority,
                        onDelete = { deleteTarget = it },
                        onAddChapter = { s -> addTopicSubject = s },
                    )
                }

                // ---------------- Flashcard decks
                item(key = "decks-header") {
                    StudINSectionHeader("Flashcard decks", Modifier.padding(top = Spacing.md), action = "New deck", onAction = { showNewDeck = true })
                }
                item(key = "deck-actions") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        StudINChip("Generate with AI", icon = Icons.Rounded.AutoAwesome, onClick = { onNavigate(Routes.generate()) })
                        StudINChip("Import CSV", icon = Icons.Rounded.FileUpload, onClick = { onNavigate(Routes.IMPORT) })
                    }
                }
                val ds = decks
                when {
                    ds == null -> item(key = "decks-loading") { StudINLoadingState() }
                    ds.isEmpty() -> item(key = "decks-empty") {
                        StudINEmptyState(
                            title = "No decks yet", message = "Turn your notes into flashcards with on-device AI, or import a CSV.",
                            illustration = R.drawable.ill_recall, actionLabel = "Generate with AI", onAction = { onNavigate(Routes.generate()) },
                        )
                    }
                    else -> items(ds, key = { "deck-${it.id}" }) { d -> DeckRow(d) { onNavigate(Routes.deck(d.id)) } }
                }

                // ---------------- Study history (only when there is real data)
                val h = history
                if (h != null && !h.isEmpty) {
                    item(key = "history-header") {
                        StudINSectionHeader("Study history", Modifier.padding(top = Spacing.md), supporting = "Last 7 days · active on ${h.activeDays} day${if (h.activeDays == 1) "" else "s"}")
                    }
                    item(key = "history") {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            StudINStatCard("Cards reviewed", "${h.flashcardReviews}", Icons.Rounded.Style, Modifier.weight(1f).fillMaxHeight())
                            StudINStatCard("Quiz answers", "${h.quizAnswers}", Icons.Rounded.Quiz, Modifier.weight(1f).fillMaxHeight(), accent = StudINTheme.colors.ai)
                            StudINStatCard(
                                "Correct", h.accuracyPercent?.let { "$it%" } ?: "–", Icons.Rounded.FactCheck, Modifier.weight(1f).fillMaxHeight(),
                                accent = StudINTheme.colors.success,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewDeck) {
        TextInputDialog(
            title = "New deck", firstLabel = "Deck name", confirmText = "Create",
            onDismiss = { showNewDeck = false },
            onConfirm = { title, _ -> showNewDeck = false; vm.createDeck(title) { onNavigate(Routes.deck(it)) } },
        )
    }
    addTopicSubject?.let { preset ->
        TextInputDialog(
            title = if (preset.isBlank()) "Add topic" else "Add chapter to $preset",
            firstLabel = "Subject", secondLabel = "Chapter / topic", confirmText = "Add",
            initialFirst = preset,
            onDismiss = { addTopicSubject = null },
            onConfirm = { subject, title -> addTopicSubject = null; vm.addTopic(subject, title); expanded = subject },
        )
    }
    deleteTarget?.let { t ->
        ConfirmDialog(
            title = "Delete topic?", message = "\"${t.title}\" and its study minutes will be removed from your syllabus.", confirmText = "Delete",
            onDismiss = { deleteTarget = null }, onConfirm = { vm.deleteTopic(t.id); deleteTarget = null },
        )
    }
}

// ------------------------------------------------------------------ Due reviews

@Composable
private fun DueReviewsCard(due: Int, onReview: () -> Unit, onQuiz: () -> Unit) {
    val c = StudINTheme.colors
    StudINCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINIconTile(if (due > 0) Icons.Rounded.Style else Icons.Rounded.TaskAlt, if (due > 0) c.accent else c.success, 44.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text("Due reviews", style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
                Text(if (due > 0) "$due card${if (due == 1) "" else "s"} due" else "All caught up", style = StudINType.statValue)
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            if (due > 0) "Reviewing on time beats re-reading – a few minutes now saves hours later."
            else "Nothing is due. Practise with MCQs or add new cards.",
            style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
        )
        Spacer(Modifier.height(Spacing.sm))
        if (due > 0) {
            AdaptiveButtonGroup(
                { m -> StudINPrimaryButton("Review now", onReview, m, icon = Icons.Rounded.PlayArrow) },
                { m -> StudINOutlinedButton("Quiz", onQuiz, m, icon = Icons.Rounded.Quiz) },
            )
        } else {
            StudINPrimaryButton("Practice with a quiz", onQuiz, Modifier.fillMaxWidth(), icon = Icons.Rounded.Quiz)
        }
    }
}

// ------------------------------------------------------------------ Subjects

private fun LazyListScope.subjectGrid(
    subjects: List<SubjectSummary>,
    columns: Int,
    expanded: String?,
    topicsFor: (String) -> List<SyllabusTopicEntity>,
    onToggle: (String) -> Unit,
    onContinue: (SyllabusTopicEntity) -> Unit,
    onToggleDone: (SyllabusTopicEntity) -> Unit,
    onCyclePriority: (SyllabusTopicEntity) -> Unit,
    onDelete: (SyllabusTopicEntity) -> Unit,
    onAddChapter: (String) -> Unit,
) {
    subjects.chunked(columns).forEach { row ->
        item(key = "subject-row-${row.first().subject}") {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { s ->
                    SubjectCard(s, expanded == s.subject, Modifier.weight(1f).fillMaxHeight(), onToggle = { onToggle(s.subject) }, onContinue = onContinue)
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        val open = row.firstOrNull { it.subject == expanded }
        if (open != null) {
            item(key = "chapters-${open.subject}") {
                ChapterList(open.subject, topicsFor(open.subject), onContinue, onToggleDone, onCyclePriority, onDelete, onAddChapter = { onAddChapter(open.subject) })
            }
        }
    }
}

@Composable
private fun SubjectCard(s: SubjectSummary, expanded: Boolean, modifier: Modifier, onToggle: () -> Unit, onContinue: (SyllabusTopicEntity) -> Unit) {
    val c = StudINTheme.colors
    StudINCard(
        modifier.semantics { stateDescription = if (expanded) "Chapters shown" else "Chapters hidden" },
        onClick = onToggle, onClickLabel = if (expanded) "Hide chapters" else "Show chapters",
        border = androidx.compose.foundation.BorderStroke(if (expanded) 1.5.dp else 1.dp, if (expanded) MaterialTheme.colorScheme.primary else c.border),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.subject, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${s.chapterCount} chapters · ${s.completedCount} done", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = c.textSecondary)
        }
        Spacer(Modifier.height(Spacing.sm))
        StudINLinearProgress(s.completion, height = 6.dp, label = "${s.subject}: ${s.completionPercent}% of chapters complete")
        Spacer(Modifier.height(Spacing.xs))
        Row {
            Text("${s.completionPercent}% complete", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text("Mastery ${s.masteryPercent}%", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
        }
        Spacer(Modifier.weight(1f))
        val next = s.nextTopic
        if (next != null) {
            Spacer(Modifier.height(Spacing.xs))
            HorizontalDivider(color = c.border)
            StudINTextButton("Continue: ${next.title}", { onContinue(next) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
        } else {
            Spacer(Modifier.height(Spacing.sm))
            StudINBadge("All chapters complete", container = c.successContainer, content = c.success, icon = Icons.Rounded.CheckCircle)
        }
    }
}

@Composable
private fun ChapterList(
    subject: String,
    topics: List<SyllabusTopicEntity>,
    onFocus: (SyllabusTopicEntity) -> Unit,
    onToggleDone: (SyllabusTopicEntity) -> Unit,
    onCyclePriority: (SyllabusTopicEntity) -> Unit,
    onDelete: (SyllabusTopicEntity) -> Unit,
    onAddChapter: () -> Unit,
) {
    StudINCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs), containerColor = MaterialTheme.colorScheme.surface) {
        Text(
            "$subject · chapter progress", style = MaterialTheme.typography.titleSmall, color = StudINTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = Spacing.card, vertical = Spacing.xs),
        )
        topics.forEachIndexed { i, t ->
            if (i > 0) HorizontalDivider(Modifier.padding(start = 56.dp), color = StudINTheme.colors.border)
            ChapterRow(t, onFocus = { onFocus(t) }, onToggleDone = { onToggleDone(t) }, onCyclePriority = { onCyclePriority(t) }, onDelete = { onDelete(t) })
        }
        HorizontalDivider(color = StudINTheme.colors.border)
        StudINTextButton("Add chapter", onAddChapter, Modifier.padding(horizontal = Spacing.xs), icon = Icons.Rounded.Add)
    }
}

@Composable
private fun ChapterRow(t: SyllabusTopicEntity, onFocus: () -> Unit, onToggleDone: () -> Unit, onCyclePriority: () -> Unit, onDelete: () -> Unit) {
    val c = StudINTheme.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = Spacing.xxs, end = Spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
        IconToggleButton(
            checked = t.completed, onCheckedChange = { onToggleDone() },
            modifier = Modifier.semantics { contentDescription = "Mark ${t.title} complete"; stateDescription = if (t.completed) "Complete" else "Not complete" },
        ) {
            Icon(
                if (t.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, contentDescription = null,
                tint = if (t.completed) c.success else c.textSecondary,
            )
        }
        Column(Modifier.weight(1f).padding(vertical = Spacing.xs)) {
            Text(
                t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = if (t.completed) c.textSecondary else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriorityBadge(t.priority)
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    listOf(if (t.minutesStudied > 0) "${t.minutesStudied} min" else "Not started", "${(t.mastery * 100).toInt()}% mastery").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            StudINLinearProgress(t.mastery.toFloat(), height = 4.dp, label = "${t.title} mastery ${(t.mastery * 100).toInt()} percent")
        }
        StudINIconButton(Icons.Rounded.PlayArrow, "Focus on ${t.title}", onFocus, tint = MaterialTheme.colorScheme.primary)
        OverflowMenu(
            listOf(
                MenuAction("Priority: ${priorityLabel(t.priority)} → ${priorityLabel(cyclePriority(t.priority))}", Icons.Rounded.Flag, onClick = onCyclePriority),
                MenuAction("Delete topic", Icons.Rounded.DeleteOutline, destructive = true, onClick = onDelete),
            ),
            contentDescription = "More options for ${t.title}",
        )
    }
}

@Composable
private fun PriorityBadge(priority: Int) {
    val c = StudINTheme.colors
    val (container, content) = when (priority) {
        3 -> c.accentContainer to c.onAccentContainer
        1 -> c.surfaceSecondary to c.textSecondary
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    StudINBadge(priorityLabel(priority), container = container, content = content,
        modifier = Modifier.semantics { contentDescription = "${priorityLabel(priority)} priority" })
}

// ------------------------------------------------------------------ Decks

@Composable
private fun DeckRow(d: DeckWithCounts, onClick: () -> Unit) {
    val c = StudINTheme.colors
    val accent = deckAccent(d.color)
    val mastery = deckMastery(d.total, d.mastered)
    StudINCard(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "${d.title}. ${d.total} cards, ${d.due} due, ${d.mastered} mastered"
        },
        onClick = onClick, onClickLabel = "Open deck",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINIconTile(Icons.Rounded.Style, accent, 40.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(d.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${d.total} card${if (d.total == 1) "" else "s"} · ${d.mastered} mastered" + if (d.source == "starter") " · Starter pack" else "",
                    style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                )
            }
            Spacer(Modifier.width(Spacing.xs))
            if (d.due > 0) StudINBadge("${d.due} due", container = c.accentContainer, content = c.onAccentContainer)
            else StudINBadge("Up to date", container = c.successContainer, content = c.success)
        }
        Spacer(Modifier.height(Spacing.sm))
        StudINLinearProgress(mastery, height = 6.dp, color = accent)
    }
}

