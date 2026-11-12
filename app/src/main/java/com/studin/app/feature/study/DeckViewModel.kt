package com.studin.app.feature.study

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.db.CardEntity
import com.studin.app.core.data.db.DeckEntity
import com.studin.app.core.data.repo.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DeckViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val study: StudyRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val deckId: Long = savedState.get<Long>("deckId") ?: -1L

    val deck: StateFlow<DeckEntity?> = study.observeDeck(deckId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val cards: StateFlow<List<CardEntity>?> = study.observeCards(deckId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun rename(title: String) = viewModelScope.launch { study.renameDeck(deckId, title) }
    fun delete(then: () -> Unit) = viewModelScope.launch { study.deleteDeck(deckId); then() }
    fun addCard(front: String, back: String) = viewModelScope.launch { study.addCard(deckId, front, back) }
    fun updateCard(card: CardEntity, front: String, back: String) = viewModelScope.launch { study.updateCard(card.copy(front = front, back = back)) }
    fun deleteCard(id: Long) = viewModelScope.launch { study.deleteCard(id) }

    /** Writes the deck as CSV into the private cache and returns a share intent. */
    fun exportCsv(onReady: (Intent?) -> Unit) = viewModelScope.launch {
        val intent = runCatching {
            withContext(Dispatchers.IO) {
                val title = deck.value?.title ?: study.deck(deckId)?.title ?: "deck"
                val safe = title.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').take(40).ifEmpty { "deck" }
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "$safe.csv")
                file.writeText(study.exportDeckCsv(deckId))
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "$title – StudIN deck")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }.getOrNull()
        onReady(intent)
    }
}
