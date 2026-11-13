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

sealed interface AccessCheck {
    data object Idle : AccessCheck
    data object Checking : AccessCheck
    data class Error(val message: String) : AccessCheck
}

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

    /** False on 32-bit or < 4 GB RAM phones: the Gemma download is not offered there. */
    val deviceSupportsModel: Boolean = llm.deviceSupported()

    // ---- Gemma download
    val download: StateFlow<DownloadUi> = downloads.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadUi.Idle)
    private val _access = MutableStateFlow<AccessCheck>(AccessCheck.Idle)
    /** Result of validating the Hugging Face token before the download starts. */
    val access: StateFlow<AccessCheck> = _access.asStateFlow()

    init {
        // Load the model in the background while the student types, so the first answer is quicker.
        llm.prewarm()
        // When a download finishes: forget the job (its input held the token) and warm the model up.
        viewModelScope.launch {
            download.collect { d ->
                if (d is DownloadUi.Done) { downloads.clearFinished(); llm.refreshState(); llm.ensureLoaded() }
            }
        }
    }

    /** Check the token with Hugging Face, then start the background download. */
    fun startDownload(token: String, onStarted: () -> Unit) {
        if (_access.value == AccessCheck.Checking || token.isBlank()) return
        _access.value = AccessCheck.Checking
        viewModelScope.launch {
            downloads.verifyAccess(token)
                .onSuccess { downloads.start(token); _access.value = AccessCheck.Idle; onStarted() }
                .onFailure { _access.value = AccessCheck.Error(it.message ?: "Couldn't reach Hugging Face. Check your connection.") }
        }
    }

    /** One-tap download with the token bundled into this build; [onNeedsOwnToken] if it can't be used. */
    fun startBundledDownload(onNeedsOwnToken: () -> Unit) {
        val token = com.studin.app.ai.GemmaModel.bundledToken ?: return onNeedsOwnToken()
        if (_access.value == AccessCheck.Checking) return
        _access.value = AccessCheck.Checking
        viewModelScope.launch {
            downloads.verifyAccess(token)
                .onSuccess { downloads.start(token); _access.value = AccessCheck.Idle }
                .onFailure {
                    // Bundled token revoked / not licensed: let the student use their own account instead.
                    _access.value = AccessCheck.Error(
                        if (it is com.studin.app.ai.DownloadFailure) "The built-in download key isn't working right now. You can use your own free Hugging Face account instead."
                        else it.message ?: "Couldn't reach Hugging Face. Check your connection.",
                    )
                    if (it is com.studin.app.ai.DownloadFailure) onNeedsOwnToken()
                }
        }
    }

    fun resetAccess() { _access.value = AccessCheck.Idle }
    fun cancelDownload() = downloads.cancel()
    fun isMetered(): Boolean = downloads.isMetered()

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
