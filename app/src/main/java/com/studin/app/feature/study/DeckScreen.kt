package com.studin.app.feature.study

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.R
import com.studin.app.core.data.db.CardEntity
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINBadge
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINEmptyState
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINLinearProgress
import com.studin.app.core.designsystem.StudINLoadingState
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINShapes
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.StudINType
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun DeckScreen(deckId: Long, onNavigate: (String) -> Unit, onBack: () -> Unit, vm: DeckViewModel = hiltViewModel()) {
    val deck by vm.deck.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var rename by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var addCard by rememberSaveable { mutableStateOf(false) }
    var editCard by remember { mutableStateOf<CardEntity?>(null) }
    var deleteCard by remember { mutableStateOf<CardEntity?>(null) }
    val now = remember(cards) { System.currentTimeMillis() }
    val list = cards
    val dueCount = list?.count { it.dueAt <= now } ?: 0
    val mastered = list?.count { it.intervalDays >= 21 } ?: 0
    val accent = deck?.let { deckAccent(it.color) } ?: MaterialTheme.colorScheme.primary

    StudINScaffold(
        title = deck?.title ?: "Deck",
        subtitle = list?.let { "${it.size} card${if (it.size == 1) "" else "s"} · $dueCount due" },
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            OverflowMenu(
                listOf(
                    MenuAction("Rename deck", Icons.Rounded.Edit) { rename = true },
                    MenuAction("Export CSV", Icons.Rounded.IosShare) {
                        vm.exportCsv { intent ->
                            if (intent != null) context.startActivity(Intent.createChooser(intent, "Share deck"))
                            else scope.launch { snackbar.showSnackbar("Couldn't export this deck") }
                        }
                    },
                    MenuAction("Delete deck", Icons.Rounded.DeleteOutline, destructive = true) { confirmDelete = true },
                ),
                contentDescription = "Deck options",
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addCard = true }, shape = StudINShapes.card,
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) }, text = { Text("Add card", style = MaterialTheme.typography.labelLarge) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().studinContentWidth(),
            contentPadding = screenContentPadding(padding, bottom = FabClearance),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "header") {
                StudINCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        DeckStat("Cards", "${list?.size ?: 0}", Modifier.weight(1f))
                        DeckDivider()
                        DeckStat("Due now", "$dueCount", Modifier.weight(1f), highlight = dueCount > 0)
                        DeckDivider()
                        DeckStat("Mastered", "$mastered", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    StudINLinearProgress(deckMastery(list?.size ?: 0, mastered), color = accent, height = 6.dp, label = "$mastered of ${list?.size ?: 0} cards mastered")
                    Spacer(Modifier.height(Spacing.md))
                    StudINPrimaryButton(
                        if (dueCount > 0) "Review $dueCount due" else "Nothing due – review later",
                        { onNavigate(Routes.review(deckId)) }, Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.PlayArrow, enabled = dueCount > 0,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    AdaptiveButtonGroup(
                        { m -> StudINOutlinedButton("Quiz", { onNavigate(Routes.quiz(deckId)) }, m, icon = Icons.Rounded.Quiz) },
                        { m -> StudINOutlinedButton("Add with AI", { onNavigate(Routes.generate(deckId)) }, m, icon = Icons.Rounded.AutoAwesome) },
                    )
                }
            }
            item(key = "cards-header") { StudINSectionHeader("Cards", Modifier.padding(top = Spacing.md), supporting = "Tap a card to edit it") }
            when {
                list == null -> item(key = "loading") { StudINLoadingState() }
                list.isEmpty() -> item(key = "empty") {
                    StudINEmptyState(
                        title = "This deck is empty", message = "Add cards yourself, or let on-device AI draft them from your notes.",
                        illustration = R.drawable.ill_recall, actionLabel = "Add with AI", onAction = { onNavigate(Routes.generate(deckId)) },
                    )
                }
                else -> items(list, key = { it.id }) { c ->
                    CardRow(c, now, onEdit = { editCard = c }, onDelete = { deleteCard = c })
                }
            }
        }
    }

    if (rename) {
        TextInputDialog(
            title = "Rename deck", firstLabel = "Deck name", confirmText = "Save", initialFirst = deck?.title.orEmpty(),
            onDismiss = { rename = false }, onConfirm = { t, _ -> vm.rename(t); rename = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete deck?", message = "\"${deck?.title}\" and all ${list?.size ?: 0} cards will be permanently removed.", confirmText = "Delete",
            onDismiss = { confirmDelete = false }, onConfirm = { confirmDelete = false; vm.delete(onBack) },
        )
    }
    if (addCard) {
        TextInputDialog(
            title = "Add card", firstLabel = "Front (question)", secondLabel = "Back (answer)", confirmText = "Add", singleLine = false,
            onDismiss = { addCard = false }, onConfirm = { f, b -> vm.addCard(f, b); addCard = false },
        )
    }
    editCard?.let { c ->
        TextInputDialog(
            title = "Edit card", firstLabel = "Front (question)", secondLabel = "Back (answer)", confirmText = "Save", singleLine = false,
            initialFirst = c.front, initialSecond = c.back,
            onDismiss = { editCard = null }, onConfirm = { f, b -> vm.updateCard(c, f, b); editCard = null },
        )
    }
    deleteCard?.let { c ->
        ConfirmDialog(
            title = "Delete card?", message = "\"${c.front.take(80)}\" will be removed with its review history.", confirmText = "Delete",
            onDismiss = { deleteCard = null }, onConfirm = { vm.deleteCard(c.id); deleteCard = null },
        )
    }
}

@Composable
private fun DeckStat(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    Column(modifier.semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = StudINType.statValue, color = if (highlight) StudINTheme.colors.accent else MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
    }
}

@Composable
private fun DeckDivider() {
    androidx.compose.material3.VerticalDivider(Modifier.fillMaxHeight().padding(vertical = Spacing.xxs), color = StudINTheme.colors.border)
}

@Composable
private fun CardRow(c: CardEntity, now: Long, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = StudINTheme.colors
    val isDue = c.dueAt <= now
    val label = if (c.repetitions == 0 && c.lapses == 0) "New" else dueLabel(c.dueAt, now)
    StudINCard(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "Card. Front: ${c.front}. Back: ${c.back}. $label" },
        onClick = onEdit, onClickLabel = "Edit card",
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = Spacing.card, top = Spacing.sm, bottom = Spacing.sm, end = Spacing.xxs),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            StudINIconTile(Icons.Rounded.Style, if (isDue) colors.accent else MaterialTheme.colorScheme.primary, 32.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f).padding(top = Spacing.xxs)) {
                Text(c.front, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Spacing.xxs))
                Text(c.back, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Spacing.xs))
                StudINBadge(
                    label,
                    container = if (isDue) colors.accentContainer else colors.surfaceSecondary,
                    content = if (isDue) colors.onAccentContainer else colors.textSecondary,
                )
            }
            OverflowMenu(
                listOf(
                    MenuAction("Edit card", Icons.Rounded.Edit, onClick = onEdit),
                    MenuAction("Delete card", Icons.Rounded.DeleteOutline, destructive = true, onClick = onDelete),
                ),
                contentDescription = "Card options",
            )
        }
    }
}

