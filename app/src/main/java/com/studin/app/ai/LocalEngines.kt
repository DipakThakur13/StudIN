package com.studin.app.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Sampling settings for one answer. */
data class Sampling(val temperature: Float, val topK: Int, val topP: Float)

/**
 * A loaded native model. Calls are serialised by [OnDeviceLlm] (one answer at a time).
 * [generate] streams the accumulated text and returns a function that cancels generation.
 */
internal interface LocalEngine : AutoCloseable {
    val onGpu: Boolean
    fun generate(
        prompt: String,
        sampling: Sampling,
        maxOutputTokens: Int,
        onText: (String) -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ): () -> Unit
}

/** Qwen models (.litertlm) on Google's LiteRT-LM runtime: GPU (OpenCL) or CPU (XNNPACK). */
internal class LiteRtLmEngine(
    context: Context,
    file: File,
    override val onGpu: Boolean,
    private val disableThinking: Boolean,
    maxTokens: Int,
) : LocalEngine {
    private val engine = Engine(
        EngineConfig(
            modelPath = file.absolutePath,
            backend = if (onGpu) Backend.GPU() else Backend.CPU(),
            maxNumTokens = maxTokens,
            cacheDir = context.cacheDir.absolutePath,
        )
    ).also { it.initialize() }

    override fun generate(
        prompt: String,
        sampling: Sampling,
        maxOutputTokens: Int,
        onText: (String) -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ): () -> Unit {
        // A fresh conversation per question: StudIN builds the full prompt itself (TutorGuard),
        // so no hidden history can bleed between answers.
        val conversation = engine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(sampling.topK, sampling.topP.toDouble(), sampling.temperature.toDouble(), 0),
                maxOutputToken = maxOutputTokens,
                // Qwen3 reasons out loud by default; StudIN wants direct, short answers.
                thinkingConfig = ThinkingConfig(enableThinking = !disableThinking),
                extraContext = if (disableThinking) mapOf("enable_thinking" to false) else emptyMap(),
            )
        )
        val text = StringBuilder()
        val closed = AtomicBoolean(false)
        fun close() { if (closed.compareAndSet(false, true)) runCatching { conversation.close() } }
        conversation.sendMessageAsync(prompt, object : MessageCallback {
            override fun onMessage(message: Message) {
                val piece = message.toString()
                // The runtime streams deltas; tolerate cumulative chunks as well.
                if (text.isNotEmpty() && piece.length >= text.length && piece.startsWith(text)) text.setLength(0)
                text.append(piece)
                onText(text.toString())
            }
            override fun onDone() { onDone(); close() }
            override fun onError(throwable: Throwable) { onError(throwable); close() }
        })
        return { runCatching { conversation.cancelProcess() } }
    }

    override fun close() = engine.close()
}

/** Legacy Gemma 3 1B / imported .task files on MediaPipe LLM Inference (CPU). */
internal class MediaPipeEngine(context: Context, file: File, maxTokens: Int) : LocalEngine {
    override val onGpu: Boolean = false
    private val engine: LlmInference = LlmInference.createFromOptions(
        context,
        LlmInference.LlmInferenceOptions.builder()
            .setModelPath(file.absolutePath)
            .setMaxTokens(maxTokens)
            .setMaxTopK(40)
            .setPreferredBackend(LlmInference.Backend.CPU)
            .build(),
    )

    override fun generate(
        prompt: String,
        sampling: Sampling,
        maxOutputTokens: Int,
        onText: (String) -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ): () -> Unit {
        val session = LlmInferenceSession.createFromOptions(
            engine,
            LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(sampling.temperature).setTopK(sampling.topK).setTopP(sampling.topP).build(),
        )
        // Rules come first in StudIN prompts, so trimming the tail never drops instructions.
        val budget = 600
        val tokens = runCatching { engine.sizeInTokens(prompt) }.getOrDefault(0)
        session.addQueryChunk(if (tokens > budget) prompt.take((prompt.length.toLong() * budget / tokens).toInt()) else prompt)
        val text = StringBuilder()
        val closed = AtomicBoolean(false)
        fun close() { if (closed.compareAndSet(false, true)) runCatching { session.close() } }
        try {
            session.generateResponseAsync { partial, done ->
                if (partial != null) { text.append(partial); onText(text.toString()) }
                if (done) { onDone(); close() }
            }
        } catch (t: Throwable) { onError(t); close() }
        return { runCatching { session.cancelGenerateResponseAsync() } }
    }

    override fun close() = engine.close()
}
