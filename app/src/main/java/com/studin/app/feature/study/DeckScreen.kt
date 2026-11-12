package com.studin.app.feature.study

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.data.db.CardEntity
import com.studin.app.core.designsystem.Badge
import com.studin.app.core.designsystem.EmptyState
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun DeckScreen(deckId: Long, onNavigate: (String) -> Unit, onBack: () -> Unit, vm: DeckViewModel = hiltViewModel()) {
    val deck by vm.deck.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addCard by remember { mutableStateOf(false) }
    var editCard by remember { mutableStateOf<CardEntity?>(null) }
    var deleteCard by remember { mutableStateOf<CardEntity?>(null) }
    val now = remember(cards) { System.currentTimeMillis() }
    val accent = deck?.let { Color(it.color.toInt()) } ?: StudinColors.Mint
    val dueCount = cards?.count { it.dueAt <= now } ?: 0

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            StudyTopBar(
                title = deck?.title ?: "Deck",
                subtitle = cards?.let { "${it.size} cards · $dueCount due" },
                onBack = onBack,
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "Deck options") }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { menu = false; rename = true })
                        DropdownMenuItem(text = { Text("Export CSV") }, leadingIcon = { Icon(Icons.Rounded.IosShare, null) },
                            onClick = {
                                menu = false
                                vm.exportCsv { intent ->
                                    if (intent != null) context.startActivity(Intent.createChooser(intent, "Share deck"))
                                    else scope.launch { snackbar.showSnackbar("Couldn't export this deck") }
                                }
                            })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Delete deck", color = Studin.extras.danger) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteForever, null, tint = Studin.extras.danger) },
                            onClick = { menu = false; confirmDelete = true })
                    }
                },
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.navigationBarsPadding(),
            ) {
                item {
                    GlassCard(Modifier.fillMaxWidth(), highlight = dueCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (dueCount > 0) "$dueCount cards ready for review" else "Nothing due – nice work",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        GradientButton(
                            "Review", { onNavigate(Routes.review(deckId)) }, Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.PlayArrow, enabled = dueCount > 0,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GhostButton("Quiz", { onNavigate(Routes.quiz(deckId)) }, Modifier.weight(1f), icon = Icons.Rounded.Quiz)
                            GhostButton("Add with AI", { onNavigate(Routes.generate(deckId)) }, Modifier.weight(1f), icon = Icons.Rounded.AutoAwesome)
                        }
                    }
                }
                when {
                    cards == null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
                    cards!!.isEmpty() -> item {
                        EmptyState(Icons.Rounded.Style, "Empty deck", "Add your first card, or let AI create cards from your notes.")
                    }
                    else -> items(cards!!, key = { it.id }) { c ->
                        CardRow(c, c.dueAt <= now, accent, onEdit = { editCard = c }, onDelete = { deleteCard = c })
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { addCard = true },
            icon = { Icon(Icons.Rounded.Add, null) },
            text = { Text("Add card") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp))
    }

    if (rename) TextInputDialog(
        "Rename deck", "Title", "Save", initialFirst = deck?.title ?: "",
        onDismiss = { rename = false }, onConfirm = { t, _ -> vm.rename(t); rename = false },
    )
    if (confirmDelete) ConfirmDialog(
        "Delete deck?", "All ${cards?.size ?: 0} cards and their review history will be deleted. This can't be undone.", "Delete",
        onDismiss = { confirmDelete = false }, onConfirm = { confirmDelete = false; vm.delete(onBack) },
    )
    if (addCard) TextInputDialog(
        "New card", "Front (question)", "Add", secondLabel = "Back (answer)", singleLine = false,
        onDismiss = { addCard = false }, onConfirm = { f, b -> vm.addCard(f, b); addCard = false },
    )
    editCard?.let { c ->
        TextInputDialog(
            "Edit card", "Front (question)", "Save", initialFirst = c.front, secondLabel = "Back (answer)", initialSecond = c.back,
            singleLine = false, onDismiss = { editCard = null }, onConfirm = { f, b -> vm.updateCard(c, f, b); editCard = null },
        )
    }
    deleteCard?.let { c ->
        ConfirmDialog(
            "Delete card?", c.front.take(120), "Delete",
            onDismiss = { deleteCard = null }, onConfirm = { vm.deleteCard(c.id); deleteCard = null },
        )
    }
}

@Composable
private fun CardRow(c: CardEntity, due: Boolean, accent: Color, onEdit: () -> Unit, onDelete: () -> Unit) {
    val ex = Studin.extras
    GlassCard(Modifier.fillMaxWidth(), onClick = onEdit, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(c.front, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(
                    c.back, style = MaterialTheme.typography.bodyMedium, color = ex.muted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.08f)).padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete card", tint = ex.muted, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (due) Badge("Due", ex.warning)
            else Badge(intervalLabel(c.intervalDays), accent)
            if (c.lapses > 0) {
                Spacer(Modifier.width(8.dp))
                Text("${c.lapses} lapse${if (c.lapses > 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = ex.muted)
            }
        }
    }
}

private fun intervalLabel(days: Double): String = when {
    days < 1 -> "Learning"
    days < 21 -> "Every ${days.toInt()}d"
    else -> "Mastered"
}
