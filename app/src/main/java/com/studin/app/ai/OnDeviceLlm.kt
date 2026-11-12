package com.studin.app.ai

import android.app.ActivityManager
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

/**
 * Optional Tier-2 on-device LLM (roadmap §4) powered by MediaPipe LLM Inference.
 * The student imports a quantised model file (e.g. Gemma 3 1B int4 `.task`) from
 * their own storage – nothing is downloaded by the app and nothing leaves the phone.
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

    private var engine: LlmInference? = null
    private val lock = Mutex()
    private val modelsDir get() = File(context.filesDir, "models").apply { mkdirs() }

    init {
        scope.launch { refreshState() }
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
    suspend fun ensureLoaded(): Boolean = withContext(Dispatchers.IO) {
        if (engine != null) return@withContext true
        val file = prefs.current().aiModelFile?.let { File(modelsDir, it) }?.takeIf { it.exists() } ?: return@withContext false
        if (!deviceSupported()) return@withContext false
        _state.value = LlmState.Loading(file.name)
        runCatching {
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

    /** Streams the accumulated response text. Errors are emitted as exceptions. */
    fun generate(prompt: String, temperature: Float = 0.6f): Flow<String> = callbackFlow {
        if (!lock.tryLock()) { close(IllegalStateException("AI is busy with another answer")); return@callbackFlow }
        var session: LlmInferenceSession? = null
        try {
            if (!powerBudgetOk()) throw IllegalStateException("Battery saver is on – using Lite AI to save power")
            if (!ensureLoaded()) throw IllegalStateException("On-device model not available")
            val e = engine ?: throw IllegalStateException("Model unloaded")
            val opts = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(temperature).setTopK(40).setTopP(0.95f).build()
            session = LlmInferenceSession.createFromOptions(e, opts)
            // Keep prompts inside the context budget.
            val budget = 700
            val trimmed = if (e.sizeInTokens(prompt) > budget) prompt.takeLast(2_400) else prompt
            session.addQueryChunk(trimmed)
            val sb = StringBuilder()
            session.generateResponseAsync { partial, done ->
                if (partial != null) { sb.append(partial); trySend(sb.toString()) }
                if (done) close()
            }
        } catch (t: Throwable) {
            close(t)
        }
        awaitClose {
            runCatching { session?.close() }
            if (lock.isLocked) lock.unlock()
        }
    }.flowOn(Dispatchers.IO)
}
