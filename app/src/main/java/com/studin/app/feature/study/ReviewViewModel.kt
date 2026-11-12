package com.studin.app.feature.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.db.CardEntity
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.StudyRepository
import com.studin.domain.model.Grade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val loading: Boolean = true,
    val queue: List<CardEntity> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val initialCount: Int = 0,
    val counts: Map<Grade, Int> = emptyMap(),
    val busy: Boolean = false,
) {
    val current: CardEntity? get() = queue.getOrNull(index)
    val finished: Boolean get() = !loading && index >= queue.size && initialCount > 0
    val empty: Boolean get() = !loading && initialCount == 0
    val reviewed: Int get() = counts.values.sum()
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val study: StudyRepository,
    prefs: UserPreferences,
) : ViewModel() {
    private val deckId: Long? = savedState.get<Long>("deckId")?.takeIf { it > 0 }

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    val haptics: StateFlow<Boolean> = prefs.settings.map { it.hapticsEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Each card can come back after "Again" at most this many times per session. */
    private val requeues = HashMap<Long, Int>()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = ReviewUiState(loading = true)
        val due = study.dueCards(deckId, limit = 100)
        requeues.clear()
        _state.value = ReviewUiState(loading = false, queue = due, initialCount = due.size)
    }

    fun reveal() = _state.update { it.copy(revealed = true) }

    fun grade(g: Grade) {
        val s = _state.value
        val card = s.current ?: return
        if (s.busy || !s.revealed) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val updated = study.review(card, g)
            _state.update { st ->
                var queue = st.queue
                if (g == Grade.AGAIN && (requeues[card.id] ?: 0) < 2) {
                    requeues[card.id] = (requeues[card.id] ?: 0) + 1
                    queue = queue + updated
                }
                st.copy(
                    queue = queue, index = st.index + 1, revealed = false, busy = false,
                    counts = st.counts + (g to ((st.counts[g] ?: 0) + 1)),
                )
            }
        }
    }
}
