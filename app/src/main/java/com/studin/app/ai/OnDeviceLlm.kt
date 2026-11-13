package com.studin.app.ai

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.content.Context
import android.net.Uri
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.OpenableColumns
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LlmState {
    /** No model installed – the Lite engine is used. */
    data object NotInstalled : LlmState
    data class Installed(val name: String, val sizeBytes: Long) : LlmState
    data class Loading(val name: String) : LlmState
    data class Ready(val name: String, val sizeBytes: Long, val onGpu: Boolean = false) : LlmState
    data class Unsupported(val reason: String) : LlmState
    data class Error(val message: String) : LlmState
}

data class ImportProgress(val copiedBytes: Long, val totalBytes: Long)

/** One streamed piece of model output. [stoppedEarly] = StudIN cut generation (length cap / loop). */
data class LlmChunk(val text: String, val done: Boolean, val stoppedEarly: Boolean = false)

/**
 * The on-device AI Tutor model. Picks the model for the phone's RAM ([ModelCatalog]), runs it on
 * the GPU when the phone supports it (crash-safe, see [GpuPolicy]) or on the CPU, one answer at a
 * time. Loaded lazily (or prewarmed), unloaded after idle time or under memory pressure.
 * Inference never sends anything off the phone.
 */
@Singleton
class OnDeviceLlm @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
    @AppScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<LlmState>(LlmState.NotInstalled)
    val state: StateFlow<LlmState> = _state.asStateFlow()

    @Volatile private var engine: LocalEngine? = null
    @Volatile private var engineModelId: String? = null
    private val lock = Mutex()
    /** Serialises loading so a prewarm and a first question can't load the model twice (~2x RAM). */
    private val loadLock = Mutex()
    private var idleJob: Job? = null
    private val modelsDir get() = File(context.filesDir, "models").apply { mkdirs() }

    val totalRamBytes: Long get() = currentTotalRam()
    /**
     * The app's own process must be 64-bit: LiteRT-LM ships arm64/x86_64 libraries only, so a
     * 32-bit APK on a 64-bit phone can't run the models even though the phone could.
     */
    private val is64Bit: Boolean get() = processIs64Bit(System.getProperty("os.arch")) && android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

    /** Model StudIN offers for this phone (Qwen3 4B on 8 GB+, Qwen2.5 1.5B on 4–6 GB), or null. */
    val recommended: TutorModel? get() = ModelCatalog.recommendedFor(currentTotalRam(), is64Bit)

    private fun currentTotalRam(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.totalMem
    }

    init {
        scope.launch { refreshState() }
        // Give the model's memory back to the system when Android is short on memory.
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

    private fun displayName(fileName: String) = ModelCatalog.byFileName(fileName)?.displayName ?: fileName

    suspend fun refreshState() {
        val s = prefs.current()
        val f = s.aiModelFile?.let { File(modelsDir, it) }
        _state.value = when {
            !deviceSupported() -> LlmState.Unsupported("The full AI Tutor needs a 64-bit phone with at least 4 GB RAM. StudIN Lite AI still works.")
            f == null || !f.exists() -> LlmState.NotInstalled
            engine != null -> LlmState.Ready(displayName(f.name), f.length(), engine?.onGpu == true)
            else -> LlmState.Installed(displayName(f.name), f.length())
        }
    }

    fun deviceSupported(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return recommended != null && !am.isLowRamDevice
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
                "Pick an on-device model file (.litertlm, .task or .bin)"
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
        val settings = prefs.current()
        val file = settings.aiModelFile?.let { File(modelsDir, it) }?.takeIf { it.exists() } ?: return false
        if (!deviceSupported()) return false
        val model = ModelCatalog.byFileName(file.name)
        val modelId = model?.id ?: file.name
        val runtime = ModelCatalog.runtimeFor(file.name)
        _state.value = LlmState.Loading(displayName(file.name))
        return runCatching {
            engine = when (runtime) {
                ModelRuntime.MEDIAPIPE -> MediaPipeEngine(context, file, model?.maxTokens ?: 1024)
                ModelRuntime.LITERT_LM -> loadLiteRtLm(file, modelId, model, settings.aiGpuEnabled)
            }
            engineModelId = modelId
            _state.value = LlmState.Ready(displayName(file.name), file.length(), engine?.onGpu == true)
            true
        }.getOrElse {
            engine = null
            _state.value = LlmState.Error("Couldn't load ${displayName(file.name)}: ${it.message?.take(120)}")
            false
        }
    }

    /** GPU first when allowed and not known to fail; any GPU init error falls back to the CPU. */
    private suspend fun loadLiteRtLm(file: File, modelId: String, model: TutorModel?, userAllowsGpu: Boolean): LocalEngine {
        val thinking = model?.hasThinking ?: file.name.contains("qwen3", ignoreCase = true)
        val maxTokens = model?.maxTokens ?: 2048
        val decision = GpuPolicy.decide(prefs.gpuState(modelId), userAllowsGpu, runtimeSupportsGpu = true)
        decision.stateToSave?.let { prefs.setGpuState(modelId, it) } // written before touching the GPU
        if (decision.useGpu) {
            var gpu: LocalEngine? = null
            try {
                gpu = LiteRtLmEngine(context, file, onGpu = true, disableThinking = thinking, maxTokens = maxTokens)
                // One tiny answer proves the GPU path works end to end (and compiles its kernels,
                // so the first real answer starts sooner). Only then is the GPU marked OK.
                warmUp(gpu)
                prefs.setGpuState(modelId, GpuPolicy.onGpuAnswered(prefs.gpuState(modelId)))
                return gpu
            } catch (t: Throwable) {
                runCatching { gpu?.close() }
                prefs.setGpuState(modelId, GpuPolicy.onGpuInitError())
            }
        }
        return LiteRtLmEngine(context, file, onGpu = false, disableThinking = thinking, maxTokens = maxTokens)
    }

    private suspend fun warmUp(e: LocalEngine) {
        val done = CompletableDeferred<Unit>()
        val cancel = e.generate(
            "Reply with OK.", Sampling(0.1f, 1, 1f), maxOutputTokens = 2,
            onText = {}, onDone = { done.complete(Unit) }, onError = { done.completeExceptionally(it) },
        )
        if (withTimeoutOrNull(WARMUP_TIMEOUT_MS) { done.await() } == null) {
            cancel()
            throw IllegalStateException("GPU warm-up timed out")
        }
    }

    fun unload() {
        runCatching { engine?.close() }
        engine = null
        engineModelId = null
        val s = _state.value
        if (s is LlmState.Ready) _state.value = LlmState.Installed(s.name, s.sizeBytes)
    }

    /**
     * Switch GPU use on/off; the model reloads on the next answer. Switching it back on gives
     * the GPU one fresh try even if it failed before.
     */
    suspend fun setGpuEnabled(enabled: Boolean) {
        prefs.setGpuEnabled(enabled)
        if (enabled) ModelCatalog.all.forEach { prefs.setGpuState(it.id, GpuState.UNKNOWN) }
        if (!lock.isLocked) { unload(); refreshState() }
    }

    /**
     * Streams the accumulated response. Sampling is deliberately calm (low temperature, small
     * top-K) to reduce invented facts. [stopWhen] is checked on every update; when it returns
     * true generation is cancelled (saves time and battery) and the last chunk has
     * `stoppedEarly = true`.
     */
    fun generate(
        prompt: String,
        temperature: Float = 0.3f,
        topK: Int = 20,
        topP: Float = 0.9f,
        maxOutputTokens: Int = 512,
        stopWhen: (String) -> Boolean = { false },
    ): Flow<LlmChunk> = callbackFlow {
        if (!lock.tryLock()) { close(IllegalStateException("AI is busy with another answer")); return@callbackFlow }
        idleJob?.cancel()
        val cancel = AtomicReference<(() -> Unit)?>(null)
        val stopped = AtomicBoolean(false)
        // Completed when the native side has really finished, so the next answer never overlaps it.
        val finished = CompletableDeferred<Unit>()
        try {
            if (!powerBudgetOk()) throw IllegalStateException("Battery saver is on – using Lite AI to save power")
            if (!ensureLoaded()) throw IllegalStateException("On-device model not available")
            val e = engine ?: throw IllegalStateException("Model unloaded")
            val modelId = engineModelId
            cancel.set(e.generate(
                prompt, Sampling(temperature, topK, topP), maxOutputTokens,
                onText = { text ->
                    if (stopped.get()) return@generate
                    if (stopWhen(text)) {
                        stopped.set(true)
                        cancel.get()?.invoke()
                        trySend(LlmChunk(text, done = true, stoppedEarly = true)); close()
                    } else trySend(LlmChunk(text, done = false))
                },
                onDone = {
                    finished.complete(Unit)
                    if (e.onGpu && modelId != null) scope.launch { prefs.setGpuState(modelId, GpuPolicy.onGpuAnswered(prefs.gpuState(modelId))) }
                    if (stopped.compareAndSet(false, true)) { trySend(LlmChunk("", done = true)); close() }
                },
                onError = { t ->
                    finished.complete(Unit)
                    if (stopped.compareAndSet(false, true)) close(t)
                },
            ))
        } catch (t: Throwable) {
            finished.complete(Unit)
            close(t)
        }
        awaitClose {
            // Collector left early (screen closed, new question): stop the model too.
            if (stopped.compareAndSet(false, true)) cancel.get()?.invoke()
            scope.launch {
                withTimeoutOrNull(CANCEL_WAIT_MS) { finished.await() }
                lock.unlock()
                scheduleIdleUnload()
            }
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        /**
         * ART sets `os.arch` from the process's instruction set: "aarch64" / "x86_64" for 64-bit,
         * "armv7l" / "armv8l" / "i686" for 32-bit processes (even on a 64-bit CPU).
         */
        fun processIs64Bit(osArch: String?): Boolean = osArch?.contains("64") == true

        /** Free the model after this long without use. */
        private const val IDLE_UNLOAD_MS = 3 * 60_000L
        /** How long to wait for the native side to wind down after a cancel. */
        private const val CANCEL_WAIT_MS = 4_000L
        /** GPU kernel compilation on first load can take a while on some phones. */
        private const val WARMUP_TIMEOUT_MS = 60_000L
    }
}
