package com.studin.app.platform.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.FocusRepository
import com.studin.app.core.data.repo.StudyRepository
import com.studin.app.platform.focus.FocusController
import com.studin.domain.quiz.McqQuestion
import com.studin.domain.quiz.QuizGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GateStep {
    data object Choice : GateStep
    data class Quiz(val questions: List<McqQuestion>, val index: Int, val picked: Int?, val correctCount: Int) : GateStep
    data class Breathe(val secondsLeft: Int) : GateStep
    data class Granted(val minutes: Int, val correct: Int, val total: Int) : GateStep
}

data class GateUi(
    val step: GateStep = GateStep.Choice,
    val questionCount: Int = 2,
    val passMinutes: Int = 5,
)

@HiltViewModel
class GateViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val study: StudyRepository,
    private val focusRepo: FocusRepository,
    val controller: FocusController,
) : ViewModel() {
    private val _ui = MutableStateFlow(GateUi())
    val ui: StateFlow<GateUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val s = prefs.current()
            _ui.update { it.copy(questionCount = s.gateQuestions, passMinutes = s.passMinutes) }
        }
    }

    /** Proof-of-work break: questions if possible, otherwise a short mindful pause. */
    fun startBreakChallenge() = viewModelScope.launch {
        val n = _ui.value.questionCount
        val qs = if (n > 0) QuizGenerator.generate(study.quizPool(limit = 60), n) else emptyList()
        if (qs.isEmpty()) breathe() else _ui.update { it.copy(step = GateStep.Quiz(qs, 0, null, 0)) }
    }

    fun answer(option: Int) {
        val step = _ui.value.step as? GateStep.Quiz ?: return
        if (step.picked != null) return
        val q = step.questions[step.index]
        val correct = option == q.correctIndex
        _ui.update { it.copy(step = step.copy(picked = option, correctCount = step.correctCount + if (correct) 1 else 0)) }
        viewModelScope.launch { study.recordMcq(q.sourceId, correct, "gate") }
    }

    fun next() {
        val step = _ui.value.step as? GateStep.Quiz ?: return
        if (step.index + 1 < step.questions.size) {
            _ui.update { it.copy(step = step.copy(index = step.index + 1, picked = null)) }
        } else grant(step.correctCount, step.questions.size)
    }

    private fun breathe() = viewModelScope.launch {
        for (s in 10 downTo 1) {
            _ui.update { it.copy(step = GateStep.Breathe(s)) }
            delay(1_000)
        }
        grant(0, 0)
    }

    private fun grant(correct: Int, total: Int) {
        val m = _ui.value.passMinutes
        controller.grantPass(m)
        _ui.update { it.copy(step = GateStep.Granted(m, correct, total)) }
    }

    /** Unconditional emergency bypass – never trap the student. */
    fun emergency(decisionId: Long) {
        controller.grantPass(_ui.value.passMinutes)
        if (decisionId > 0) viewModelScope.launch { focusRepo.markOverridden(decisionId) }
    }
}
