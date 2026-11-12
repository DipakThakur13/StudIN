package com.studin.app.feature.ai

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.ai.AiEngine
import com.studin.app.ai.ChatTurn
import com.studin.app.ai.ContentCheck
import com.studin.app.ai.ImportProgress
import com.studin.app.ai.LlmState
import com.studin.app.ai.OnDeviceLlm
import com.studin.app.ai.StudyAssistant
import com.studin.app.core.data.db.ClassificationDecisionEntity
import com.studin.app.core.data.prefs.AppSettings
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.FocusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val id: Long, val fromUser: Boolean, val text: String, val engine: AiEngine? = null, val streaming: Boolean = false)

data class ModelUi(
    val importing: Boolean = false,
    val progress: ImportProgress? = null,
    val message: String? = null,
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val assistant: StudyAssistant,
    private val llm: OnDeviceLlm,
    private val prefs: UserPreferences,
    focus: FocusRepository,
) : ViewModel() {

    val llmState: StateFlow<LlmState> = assistant.llmState
    val settings: StateFlow<AppSettings> = prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val decisions: StateFlow<List<ClassificationDecisionEntity>> =
        focus.observeDecisions(20).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- Tutor (kept in memory only)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()
    private var chatJob: Job? = null
    private var nextId = 1L

    fun send(text: String) {
        val q = text.trim()
        if (q.isEmpty() || _thinking.value) return
        val history = _messages.value.filter { !it.streaming }.map { ChatTurn(it.fromUser, it.text) }
        val replyId = nextId + 1
        _messages.update { it + ChatMessage(nextId, true, q) + ChatMessage(replyId, false, "", streaming = true) }
        nextId += 2
        _thinking.value = true
        chatJob = viewModelScope.launch {
            try {
                assistant.chat(q, history).collect { (partial, engine) ->
                    _messages.update { list -> list.map { if (it.id == replyId) it.copy(text = partial, engine = engine) else it } }
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                _messages.update { list -> list.map { if (it.id == replyId) it.copy(text = "Sorry, I couldn't answer that: ${t.message}") else it } }
            } finally {
                _messages.update { list ->
                    list.map { if (it.id == replyId) it.copy(streaming = false, text = it.text.ifBlank { "…" }) else it }
                }
                _thinking.value = false
            }
        }
    }

    fun stop() { chatJob?.cancel() }
    fun clearChat() { chatJob?.cancel(); _messages.value = emptyList() }

    // ---- Content check
    private val _check = MutableStateFlow<ContentCheck?>(null)
    val check: StateFlow<ContentCheck?> = _check.asStateFlow()
    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    fun checkTitle(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _checking.value = true
            _check.value = runCatching { assistant.checkContent(title) }.getOrNull()
            _checking.value = false
        }
    }

    // ---- Model management
    private val _model = MutableStateFlow(ModelUi())
    val model: StateFlow<ModelUi> = _model.asStateFlow()

    fun importModel(uri: Uri) {
        if (_model.value.importing) return
        viewModelScope.launch {
            _model.value = ModelUi(importing = true)
            val r = llm.importModel(uri) { p -> _model.update { it.copy(progress = p) } }
            _model.value = ModelUi(message = r.fold({ "Model imported and verified (SHA-256 below)." }, { it.message ?: "Import failed" }))
        }
    }

    fun load() = viewModelScope.launch {
        _model.update { it.copy(message = null) }
        val ok = llm.ensureLoaded()
        _model.update { it.copy(message = if (ok) "Model loaded – ready offline." else "Couldn't load the model on this device.") }
    }

    fun unload() = viewModelScope.launch { llm.unload(); llm.refreshState() }
    fun deleteModel() = viewModelScope.launch { llm.deleteModel(); _model.value = ModelUi(message = "Model removed. Lite AI is active.") }
    fun setUseLlm(v: Boolean) = viewModelScope.launch { prefs.setUseLlm(v) }
}
