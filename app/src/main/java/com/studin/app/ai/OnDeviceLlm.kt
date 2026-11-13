package com.studin.app.ai

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.content.Context
import android.net.Uri
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.OpenableColumns
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LlmState {
    /** No model imported – the Lite engine is used. */
    data object NotInstalled : LlmState
    data class Installed(val name: String, val sizeBytes: Long) : LlmState
    data class Loading(val name: String) : LlmState
    data class Ready(val name: String, val sizeBytes: Long) : LlmState
    data class Unsupported(val reason: String) : LlmState
    data class Error(val message: String) : LlmState
}

data class ImportProgress(val copiedBytes: Long, val totalBytes: Long)

/** One streamed piece of model output. [stoppedEarly] = StudIN cut generation (length cap / loop). */
data class LlmChunk(val text: String, val done: Boolean, val stoppedEarly: Boolean = false)

/**
 * Optional Tier-2 on-device LLM (roadmap §4) powered by MediaPipe LLM Inference.
 * The student imports a quantised model file (e.g. Gemma 3 1B int4 `.task`) from
 * their own storage, or downloads Gemma 3 1B via [ModelDownloads] – inference never sends anything off the phone.
 * Loaded lazily, power-aware, one generation at a time, unloaded on low memory.
 */
@Singleton
class OnDeviceLlm @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
    @AppScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<LlmState>(LlmState.NotInstalled)
    val state: StateFlow<LlmState> = _state.asStateFlow()

    @Volatile private var engine: LlmInference? = null
    private val lock = Mutex()
    /** Serialises loading so a prewarm and a first question can't load the model twice (~2x RAM). */
    private val loadLock = Mutex()
    private var idleJob: Job? = null
    private val modelsDir get() = File(context.filesDir, "models").apply { mkdirs() }

    init {
        scope.launch { refreshState() }
        // Give the ~0.6 GB model back to the system when Android is short on memory.
        context.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                @Suppress("DEPRECATION")
                if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
                    level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) unloadIfIdle()
            }
            override fun onConfigurationChanged(newConfig: Configuration) {}
            @Deprecated("Deprecated in Java") override fun onLowMemory() = unloadIfIdle()
        })
    }

    private fun unloadIfIdle() { if (!lock.isLocked) unload() }

    /** Unload after [IDLE_UNLOAD_MS] without use; rescheduled after every answer. */
    private fun scheduleIdleUnload() {
        idleJob?.cancel()
        idleJob = scope.launch { delay(IDLE_UNLOAD_MS); unloadIfIdle() }
    }

    /** Load in the background (e.g. when AI Tutor opens) so the first answer starts sooner. */
    fun prewarm() {
        scope.launch {
            val s = prefs.current()
            if (s.aiUseLlm && powerBudgetOk() && (state.value is LlmState.Installed || state.value is LlmState.Ready)) {
                if (ensureLoaded()) scheduleIdleUnload()
            }
        }
    }

    suspend fun refreshState() {
        val s = prefs.current()
        val f = s.aiModelFile?.let { File(modelsDir, it) }
        _state.value = when {
            !deviceSupported() -> LlmState.Unsupported("Needs a 64-bit phone with at least 4 GB RAM. StudIN Lite AI still works.")
            f == null || !f.exists() -> LlmState.NotInstalled
            engine != null -> LlmState.Ready(f.name, f.length())
            else -> LlmState.Installed(f.name, f.length())
        }
    }

    fun deviceSupported(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val is64 = android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        return is64 && mi.totalMem >= 3_500_000_000L && !am.isLowRamDevice
    }

    /** Battery budget: skip heavy inference on battery saver below 20 % unless charging. */
    fun powerBudgetOk(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return !(pm.isPowerSaveMode && !bm.isCharging && level in 0..19)
    }

    /**
     * Copy a model the user picked (SAF) into private storage, verifying size and
     * computing a SHA-256 checksum shown to the user for provenance.
     */
    suspend fun importModel(uri: Uri, onProgress: (ImportProgress) -> Unit = {}): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            var name = "model.task"; var size = -1L
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) { name = c.getString(0) ?: name; size = c.getLong(1) }
            }
            val safeName = name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)
            require(safeName.endsWith(".task") || safeName.endsWith(".bin") || safeName.endsWith(".litertlm")) {
                "Pick a MediaPipe model file (.task or .bin), e.g. Gemma 3 1B int4"
            }
            require(size <= 0 || size in 20_000_000L..5_000_000_000L) { "Model size looks wrong (${size / 1_000_000} MB)" }
            if (size > 0) require(modelsDir.usableSpace > size + 200_000_000L) { "Not enough free storage for this model" }
            unload()
            modelsDir.listFiles()?.forEach { it.delete() }
            val target = File(modelsDir, safeName)
            val digest = MessageDigest.getInstance("SHA-256")
            resolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Could not open file" }
                target.outputStream().use { out ->
                    val buf = ByteArray(1 shl 20); var copied = 0L; var last = 0L
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        out.write(buf, 0, n); digest.update(buf, 0, n); copied += n
                        if (copied - last > 8_000_000) { onProgress(ImportProgress(copied, size)); last = copied }
                    }
                    onProgress(ImportProgress(copied, size))
                }
            }
            val sha = digest.digest().joinToString("") { "%02x".format(it) }
            prefs.setModel(safeName, sha)
            refreshState()
            sha
        }.onFailure { _state.value = LlmState.Error(it.message ?: "Import failed") }
    }

    suspend fun deleteModel() = withContext(Dispatchers.IO) {
        unload()
        modelsDir.listFiles()?.forEach { it.delete() }
        prefs.setModel(null, null)
        refreshState()
    }

    /** Load the model if needed. Returns false when unavailable. */
    suspend fun ensureLoaded(): Boolean = withContext(Dispatchers.IO) { loadLock.withLock { loadLocked() } }

    private suspend fun loadLocked(): Boolean {
        idleJob?.cancel()
        if (engine != null) return true
        val file = prefs.current().aiModelFile?.let { File(modelsDir, it) }?.takeIf { it.exists() } ?: return false
        if (!deviceSupported()) return false
        _state.value = LlmState.Loading(file.name)
        return runCatching {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(file.absolutePath)
                .setMaxTokens(1024)
                .setMaxTopK(40)
                .setPreferredBackend(LlmInference.Backend.CPU)
                .build()
            engine = LlmInference.createFromOptions(context, options)
            _state.value = LlmState.Ready(file.name, file.length())
            true
        }.getOrElse {
            engine = null
            _state.value = LlmState.Error("Couldn't load model: ${it.message?.take(120)}")
            false
        }
    }

    fun unload() {
        runCatching { engine?.close() }
        engine = null
        val s = _state.value
        if (s is LlmState.Ready) _state.value = LlmState.Installed(s.name, s.sizeBytes)
    }

    /**
     * Streams the accumulated response. Sampling is deliberately calm (low temperature, small
     * top-K) to reduce invented facts. [stopWhen] is checked on every token; when it returns true
     * native generation is cancelled (saves time and battery) and the last chunk has
     * `stoppedEarly = true`.
     */
    fun generate(
        prompt: String,
        temperature: Float = 0.3f,
        topK: Int = 20,
        topP: Float = 0.9f,
        stopWhen: (String) -> Boolean = { false },
    ): Flow<LlmChunk> = callbackFlow {
        if (!lock.tryLock()) { close(IllegalStateException("AI is busy with another answer")); return@callbackFlow }
        var session: LlmInferenceSession? = null
        idleJob?.cancel()
        try {
            if (!powerBudgetOk()) throw IllegalStateException("Battery saver is on – using Lite AI to save power")
            if (!ensureLoaded()) throw IllegalStateException("On-device model not available")
            val e = engine ?: throw IllegalStateException("Model unloaded")
            val opts = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(temperature).setTopK(topK).setTopP(topP).build()
            session = LlmInferenceSession.createFromOptions(e, opts)
            // Prompts are built rules-first by TutorGuard; if still over budget, cut from the END
            // (question tail) so the instructions are never lost.
            val tokens = e.sizeInTokens(prompt)
            val fitted = if (tokens > MAX_INPUT_TOKENS) prompt.take((prompt.length.toLong() * MAX_INPUT_TOKENS / tokens).toInt()) else prompt
            session.addQueryChunk(fitted)
            val sb = StringBuilder()
            var stopped = false
            val s = session
            s.generateResponseAsync { partial, done ->
                if (stopped) return@generateResponseAsync
                if (partial != null) sb.append(partial)
                val text = sb.toString()
                if (!done && stopWhen(text)) {
                    stopped = true
                    runCatching { s.cancelGenerateResponseAsync() }
                    trySend(LlmChunk(text, done = true, stoppedEarly = true)); close()
                } else {
                    trySend(LlmChunk(text, done))
                    if (done) close()
                }
            }
        } catch (t: Throwable) {
            close(t)
        }
        awaitClose {
            runCatching { session?.close() }
            if (lock.isLocked) lock.unlock()
            scheduleIdleUnload()
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        /** Input budget (tokens); the engine's total context is 1024 incl. the answer. */
        private const val MAX_INPUT_TOKENS = 600
        /** Free the model after this long without use. */
        private const val IDLE_UNLOAD_MS = 3 * 60_000L
    }
}
