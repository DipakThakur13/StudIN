package com.studin.app.feature.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.repo.StudyRepository
import com.studin.domain.quiz.McqQuestion
import com.studin.domain.quiz.QuizGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class QuizUiState(
    val loading: Boolean = true,
    val questions: List<McqQuestion> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val correct: Int = 0,
    val poolSize: Int = 0,
) {
    val current: McqQuestion? get() = questions.getOrNull(index)
    val notEnough: Boolean get() = !loading && questions.isEmpty()
    val finished: Boolean get() = !loading && questions.isNotEmpty() && index >= questions.size
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val study: StudyRepository,
) : ViewModel() {
    private val deckId: Long? = savedState.get<Long>("deckId")?.takeIf { it > 0 }
    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = QuizUiState(loading = true)
        val pool = study.quizPool(deckId)
        val qs = withContext(Dispatchers.Default) {
            if (pool.distinctBy { it.answer.trim().lowercase() }.size < MIN_CARDS) emptyList() else QuizGenerator.generate(pool, 10)
        }
        _state.value = QuizUiState(loading = false, questions = qs, poolSize = pool.size)
    }

    fun answer(option: Int) {
        val s = _state.value
        val q = s.current ?: return
        if (s.selected != null) return
        val ok = option == q.correctIndex
        _state.update { it.copy(selected = option, correct = it.correct + if (ok) 1 else 0) }
        viewModelScope.launch { study.recordMcq(q.sourceId, ok, context = "quiz") }
    }

    fun next() = _state.update { if (it.selected == null) it else it.copy(index = it.index + 1, selected = null) }

    companion object { const val MIN_CARDS = 4 }
}
