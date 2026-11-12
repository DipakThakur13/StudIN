package com.studin.app.feature.ai

import com.studin.app.ai.LlmState

/** What the header badge says about the engine that will actually answer. Never over-claims. */
enum class EngineStatusKind { LITE, INSTALLED, LOADING, READY, PROCESSING, ERROR }

data class EngineStatus(val kind: EngineStatusKind, val label: String, val detail: String)

/**
 * Honest mapping from the on-device model state (+ the user's "use model" switch and whether a
 * reply is being generated) to a short status label.
 */
fun engineStatus(llm: LlmState, useLlm: Boolean, generating: Boolean = false): EngineStatus = when (llm) {
    LlmState.NotInstalled -> EngineStatus(EngineStatusKind.LITE, "Lite AI", "No model imported – answers come from your cards and syllabus")
    is LlmState.Unsupported -> EngineStatus(EngineStatusKind.LITE, "Lite AI", llm.reason)
    is LlmState.Loading -> EngineStatus(EngineStatusKind.LOADING, "Model loading", "Loading ${llm.name}…")
    is LlmState.Error -> EngineStatus(EngineStatusKind.ERROR, "Model error", llm.message)
    is LlmState.Installed ->
        if (!useLlm) EngineStatus(EngineStatusKind.LITE, "Lite AI", "On-device model turned off in Model settings")
        else EngineStatus(EngineStatusKind.INSTALLED, "Model installed", "${llm.name} loads on first use")
    is LlmState.Ready -> when {
        !useLlm -> EngineStatus(EngineStatusKind.LITE, "Lite AI", "On-device model turned off in Model settings")
        generating -> EngineStatus(EngineStatusKind.PROCESSING, "Processing", "${llm.name} is writing a reply")
        else -> EngineStatus(EngineStatusKind.READY, "On-device model", "${llm.name} is ready, fully offline")
    }
}

/** A displayable chunk of a tutor reply. */
sealed interface ReplyBlock {
    data class Paragraph(val text: String) : ReplyBlock
    data class Bullet(val text: String) : ReplyBlock
    data class Numbered(val number: String, val text: String) : ReplyBlock
    data class Code(val text: String) : ReplyBlock
}

private val bulletPrefix = Regex("^\\s*(?:[•\\-*])\\s+")
private val numberPrefix = Regex("^\\s*(\\d{1,2})[.)]\\s+")

/**
 * Splits a reply into paragraphs, bullet / numbered items and ``` code blocks so it can be
 * rendered with structure. Pure and total: any input produces a list (possibly empty).
 */
fun formatReply(text: String): List<ReplyBlock> {
    val out = mutableListOf<ReplyBlock>()
    val para = StringBuilder()
    val code = StringBuilder()
    var inCode = false
    fun flushPara() {
        if (para.isNotBlank()) out += ReplyBlock.Paragraph(para.toString().trim())
        para.setLength(0)
    }
    for (raw in text.replace("\r", "").split('\n')) {
        if (raw.trimStart().startsWith("```")) {
            if (inCode) { out += ReplyBlock.Code(code.toString().trimEnd('\n')); code.setLength(0); inCode = false }
            else { flushPara(); inCode = true }
            continue
        }
        if (inCode) { code.append(raw).append('\n'); continue }
        val bullet = bulletPrefix.find(raw)
        val numbered = numberPrefix.find(raw)
        when {
            raw.isBlank() -> flushPara()
            bullet != null -> { flushPara(); out += ReplyBlock.Bullet(raw.substring(bullet.range.last + 1).trim()) }
            numbered != null -> { flushPara(); out += ReplyBlock.Numbered(numbered.groupValues[1], raw.substring(numbered.range.last + 1).trim()) }
            else -> { if (para.isNotEmpty()) para.append('\n'); para.append(raw.trimEnd()) }
        }
    }
    // An unterminated fence still renders as code rather than disappearing.
    if (inCode && code.isNotBlank()) out += ReplyBlock.Code(code.toString().trimEnd('\n'))
    flushPara()
    return out
}


fun formatBytes(bytes: Long): String = if (bytes >= 1_000_000_000) String.format(java.util.Locale.US, "%.2f GB", bytes / 1e9) else "${(bytes + 500_000) / 1_000_000} MB"
