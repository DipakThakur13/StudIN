package com.studin.app.feature.ai

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.ai.AiEngine
import com.studin.app.ai.ChatTurn
import com.studin.app.ai.DownloadUi
import com.studin.app.ai.ModelDownloads
import com.studin.app.ai.ImportProgress
import com.studin.app.ai.LlmState
import com.studin.app.ai.ModelCatalog
import com.studin.app.ai.TutorModel
import com.studin.app.ai.OnDeviceLlm
import com.studin.app.ai.StudyAssistant
import com.studin.app.core.data.prefs.AppSettings
import com.studin.app.core.data.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One chat bubble. Kept in memory only (never persisted or logged).
 * [prompt] is the user question a tutor reply answers, so a failed reply can be retried.
 */
data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val engine: AiEngine? = null,
    val streaming: Boolean = false,
    val failed: Boolean = false,
    val prompt: String? = null,
)

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
    private val downloads: ModelDownloads,
) : ViewModel() {

    val llmState: StateFlow<LlmState> = assistant.llmState
    val settings: StateFlow<AppSettings> = prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** The model offered for this phone's RAM: Qwen3 4B (8 GB+), Qwen2.5 1.5B (4–6 GB), or null. */
    val recommended: TutorModel? = llm.recommended
    /** Total RAM as the phone reports it, for "picked for your phone (8 GB RAM)". */
    val ramLabel: String = ModelCatalog.ramLabel(llm.totalRamBytes)
    /** False on 32-bit or < 4 GB RAM phones: no model download is offered there. */
    val deviceSupportsModel: Boolean = llm.deviceSupported()

    /** The catalog model currently installed (null for none or a user-imported file). */
    val installedModel: StateFlow<TutorModel?> = prefs.settings
        .map { ModelCatalog.byFileName(it.aiModelFile) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---- Model download (public Hugging Face repositories, no account needed)
    val download: StateFlow<DownloadUi> = downloads.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadUi.Idle)

    init {
        // Load the model in the background while the student types, so the first answer is quicker.
        llm.prewarm()
        // When a download finishes: forget the job and warm the new model up.
        viewModelScope.launch {
            download.collect { d ->
                if (d is DownloadUi.Done) { downloads.clearFinished(); llm.refreshState(); llm.ensureLoaded() }
            }
        }
    }

    /** Start the background download of the model picked for this phone. */
    fun startDownload() { recommended?.let(downloads::start) }
    fun cancelDownload() = downloads.cancel()
    fun isMetered(): Boolean = downloads.isMetered()
    fun setGpuEnabled(v: Boolean) = viewModelScope.launch { llm.setGpuEnabled(v) }

    // ---- Tutor (kept in memory only)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()
    private var chatJob: Job? = null
    private var nextId = 1L

    fun send(text: String) {
        val q = text.trim().take(2_000)
        if (q.isEmpty() || _thinking.value) return
        val replyId = nextId + 1
        _messages.update { it + ChatMessage(nextId, true, q) + ChatMessage(replyId, false, "", streaming = true, prompt = q) }
        nextId += 2
        generate(replyId, q)
    }

    /** Re-run the question behind a failed reply, replacing that reply in place. */
    fun retry(replyId: Long) {
        if (_thinking.value) return
        val failed = _messages.value.firstOrNull { it.id == replyId && it.failed } ?: return
        val q = failed.prompt ?: return
        _messages.update { list -> list.map { if (it.id == replyId) it.copy(text = "", failed = false, streaming = true, engine = null) else it } }
        generate(replyId, q)
    }

    private fun generate(replyId: Long, q: String) {
        val history = _messages.value.filter { !it.streaming && !it.failed && it.id < replyId - 1 }.map { ChatTurn(it.fromUser, it.text) }
        _thinking.value = true
        chatJob = viewModelScope.launch {
            try {
                // conflate + ~60 ms pacing: the UI re-renders ≈16×/s instead of once per token.
                assistant.chat(q, history).conflate().collect { (partial, engine) ->
                    _messages.update { list -> list.map { if (it.id == replyId) it.copy(text = partial, engine = engine) else it } }
                    delay(60)
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                _messages.update { list ->
                    list.map { if (it.id == replyId) it.copy(text = t.message?.take(160) ?: "Something went wrong", failed = true) else it }
                }
            } finally {
                _messages.update { list ->
                    list.map {
                        if (it.id != replyId) it
                        else if (!it.failed && it.text.isBlank()) it.copy(streaming = false, failed = true, text = "No answer was produced.")
                        else it.copy(streaming = false)
                    }
                }
                _thinking.value = false
            }
        }
    }

    /** Stop generating; whatever was streamed so far is kept. */
    fun stop() { chatJob?.cancel() }
    fun clearChat() { chatJob?.cancel(); _messages.value = emptyList() }

    // ---- Model management
    private val _model = MutableStateFlow(ModelUi())
    val model: StateFlow<ModelUi> = _model.asStateFlow()

    fun importModel(uri: Uri) {
        if (_model.value.importing) return
        viewModelScope.launch {
            _model.value = ModelUi(importing = true)
            val r = llm.importModel(uri) { p -> _model.update { it.copy(progress = p) } }
            _model.value = ModelUi(message = r.fold({ "Model imported. AI Tutor will use it." }, { it.message ?: "Import failed" }))
            if (r.isSuccess) prefs.setUseLlm(true)
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
