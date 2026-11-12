package com.studin.app.feature.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.db.DeckWithCounts
import com.studin.app.core.data.db.SyllabusTopicEntity
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.StudyRepository
import com.studin.app.platform.focus.FocusController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val study: StudyRepository,
    private val prefs: UserPreferences,
    private val focus: FocusController,
) : ViewModel() {

    val decks: StateFlow<List<DeckWithCounts>?> =
        study.observeDecks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val dueCount: StateFlow<Int> =
        study.observeDueCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val topics: StateFlow<List<SyllabusTopicEntity>?> =
        study.observeTopics().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun createDeck(title: String, onCreated: (Long) -> Unit) = viewModelScope.launch {
        onCreated(study.createDeck(title))
    }

    fun cyclePriority(t: SyllabusTopicEntity) = viewModelScope.launch {
        val next = when (t.priority) { 3 -> 1; 1 -> 2; else -> 3 }
        study.updateTopic(t.copy(priority = next))
    }

    fun toggleCompleted(t: SyllabusTopicEntity) = viewModelScope.launch {
        study.updateTopic(t.copy(completed = !t.completed))
    }

    fun addTopic(subject: String, title: String) = viewModelScope.launch { study.addTopic(subject, title) }

    fun deleteTopic(id: Long) = viewModelScope.launch { study.deleteTopic(id) }

    fun focusOn(t: SyllabusTopicEntity, then: () -> Unit) = viewModelScope.launch {
        val minutes = prefs.current().defaultFocusMinutes
        focus.start(minutes, t.id, t.title)
        then()
    }
}
