package com.studin.app.feature.study

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.ai.AiEngine
import com.studin.app.ai.LlmState
import com.studin.app.ai.OcrEngine
import com.studin.app.ai.StudyAssistant
import com.studin.app.core.data.db.DeckEntity
import com.studin.app.core.data.repo.StudyRepository
import com.studin.domain.importer.ParsedCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DraftCard(val key: Int, val front: String, val back: String, val selected: Boolean = true)

data class GenerateUiState(
    val notes: String = "",
    val scanning: Boolean = false,
    val generating: Boolean = false,
    val saving: Boolean = false,
    val usedScan: Boolean = false,
    val drafts: List<DraftCard>? = null,
    val engine: AiEngine? = null,
    val keywords: List<String> = emptyList(),
    val decks: List<DeckEntity> = emptyList(),
    /** Target deck; null means "create a new deck". */
    val targetDeckId: Long? = null,
    val newDeckTitle: String = "",
    val error: String? = null,
) {
    val selectedCount: Int get() = drafts?.count { it.selected } ?: 0
}

@HiltViewModel
class GenerateCardsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val study: StudyRepository,
    private val assistant: StudyAssistant,
    private val ocr: OcrEngine,
) : ViewModel() {
    private val presetDeck: Long? = savedState.get<Long>("deckId")?.takeIf { it > 0 }
    private val _state = MutableStateFlow(GenerateUiState(targetDeckId = presetDeck))
    val state: StateFlow<GenerateUiState> = _state.asStateFlow()
    val llmState: StateFlow<LlmState> get() = assistant.llmState

    init {
        viewModelScope.launch {
            val decks = study.allDecks()
            _state.update { s ->
                s.copy(decks = decks, targetDeckId = s.targetDeckId?.takeIf { id -> decks.any { it.id == id } })
            }
        }
    }

    fun onNotes(text: String) = _state.update { it.copy(notes = text.take(MAX_NOTES), error = null) }
    fun appendNotes(text: String) = _state.update {
        val joined = if (it.notes.isBlank()) text else it.notes.trimEnd() + "\n\n" + text
        it.copy(notes = joined.take(MAX_NOTES), error = null)
    }
    fun selectDeck(id: Long?) = _state.update { it.copy(targetDeckId = id) }
    fun onNewDeckTitle(t: String) = _state.update { it.copy(newDeckTitle = t.take(80)) }

    fun scan(uri: Uri) {
        if (_state.value.scanning) return
        _state.update { it.copy(scanning = true, error = null) }
        viewModelScope.launch {
            runCatching { ocr.recognize(uri) }
                .onSuccess { text ->
                    if (text.isBlank()) _state.update { it.copy(scanning = false, error = "No text found in that photo. Try a sharper, well-lit shot.") }
                    else { appendNotes(text); _state.update { it.copy(scanning = false, usedScan = true) } }
                }
                .onFailure { e -> _state.update { it.copy(scanning = false, error = "Couldn't read the photo: ${e.message?.take(80) ?: "unknown error"}") } }
        }
    }

    fun generate() {
        val s = _state.value
        if (s.generating || s.notes.isBlank()) return
        if (s.notes.trim().length < 20) { _state.update { it.copy(error = "Add a bit more text – a few sentences work best.") }; return }
        _state.update { it.copy(generating = true, error = null) }
        viewModelScope.launch {
            runCatching { assistant.generateCards(s.notes, max = 20) }
                .onSuccess { r ->
                    _state.update {
                        it.copy(
                            generating = false,
                            drafts = r.cards.mapIndexed { i, c -> DraftCard(i, c.front, c.back) },
                            engine = r.engine, keywords = r.keywords,
                            newDeckTitle = it.newDeckTitle.ifBlank { r.keywords.firstOrNull()?.replaceFirstChar { c -> c.uppercase() }?.let { k -> "$k notes" } ?: "My notes" },
                            error = if (r.cards.isEmpty()) "Couldn't find card-worthy facts. Try definitions like \"X is …\" or formulas." else null,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(generating = false, error = e.message ?: "Generation failed") } }
        }
    }

    fun toggle(key: Int) = _state.update { s -> s.copy(drafts = s.drafts?.map { if (it.key == key) it.copy(selected = !it.selected) else it }) }
    fun edit(key: Int, front: String, back: String) = _state.update { s ->
        s.copy(drafts = s.drafts?.map { if (it.key == key) it.copy(front = front.take(1000), back = back.take(1000)) else it })
    }
    fun discard() = _state.update { it.copy(drafts = null, engine = null, keywords = emptyList()) }

    fun save(onDone: (Long) -> Unit) {
        val s = _state.value
        val chosen = s.drafts?.filter { it.selected && it.front.isNotBlank() && it.back.isNotBlank() }.orEmpty()
        if (s.saving || chosen.isEmpty()) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                val deckId = s.targetDeckId ?: study.createDeck(
                    s.newDeckTitle.ifBlank { "AI flashcards" }, subject = "", source = if (s.usedScan) "scan" else "ai",
                )
                study.addCards(deckId, chosen.map { ParsedCard(it.front, it.back, "ai") })
                deckId
            }.onSuccess { id -> _state.update { it.copy(saving = false) }; onDone(id) }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e.message ?: "Couldn't save cards") } }
        }
    }

    companion object { const val MAX_NOTES = 12_000 }
}
