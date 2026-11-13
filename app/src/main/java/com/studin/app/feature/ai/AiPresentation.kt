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
        if (!useLlm) EngineStatus(EngineStatusKind.LITE, "Lite AI", "On-device model turned off in the AI Tutor menu")
        else EngineStatus(EngineStatusKind.INSTALLED, "Model installed", "${llm.name} loads on first use")
    is LlmState.Ready -> when {
        !useLlm -> EngineStatus(EngineStatusKind.LITE, "Lite AI", "On-device model turned off in the AI Tutor menu")
        generating -> EngineStatus(EngineStatusKind.PROCESSING, "Processing", "${llm.name} is writing a reply")
        else -> EngineStatus(EngineStatusKind.READY, "On-device model", "${llm.name} is ready, fully offline · ${if (llm.onGpu) "GPU" else "CPU"}")
    }
}

/** A displayable chunk of a tutor reply. */
sealed interface ReplyBlock {
    data class Heading(val text: String) : ReplyBlock
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
        val heading = Regex("^\\s{0,3}#{1,6}\\s+(.+)$").find(raw)
        if (heading != null) { flushPara(); out += ReplyBlock.Heading(heading.groupValues[1].trim().trimEnd('#').trim()); continue }
        val bullet = bulletPrefix.find(raw)
        val numbered = numberPrefix.find(raw)
        when {
            raw.isBlank() -> flushPara()
            // A marker with nothing after it ("-", "•", "3.") is noise, not an empty bullet.
            raw.trim().let { it.length <= 3 && it.matches(Regex("[•\\-*]|\\d{1,2}[.)]")) } -> flushPara()
            bullet != null -> {
                flushPara()
                raw.substring(bullet.range.last + 1).trim().takeIf { it.isNotEmpty() }?.let { out += ReplyBlock.Bullet(it) }
            }
            numbered != null -> {
                flushPara()
                raw.substring(numbered.range.last + 1).trim().takeIf { it.isNotEmpty() }?.let { out += ReplyBlock.Numbered(numbered.groupValues[1], it) }
            }
            else -> { if (para.isNotEmpty()) para.append('\n'); para.append(raw.trimEnd()) }
        }
    }
    // An unterminated fence still renders as code rather than disappearing.
    if (inCode && code.isNotBlank()) out += ReplyBlock.Code(code.toString().trimEnd('\n'))
    flushPara()
    return out
}


fun formatBytes(bytes: Long): String = if (bytes >= 1_000_000_000) String.format(java.util.Locale.US, "%.2f GB", bytes / 1e9) else "${(bytes + 500_000) / 1_000_000} MB"

/** A run of reply text with inline markdown styling resolved. */
data class InlineSpan(val text: String, val bold: Boolean = false, val italic: Boolean = false, val code: Boolean = false)

private val inlineToken = Regex("`([^`\n]+)`|\\*\\*([^*\n]+)\\*\\*|__([^_\n]+)__|(?<![*\\w])\\*([^*\\s][^*\n]*?)\\*(?![*\\w])")

/**
 * Resolves `code`, **bold**, __bold__ and *italic* so model output never shows raw markdown.
 * Unbalanced markers (e.g. mid-stream "**Hea") are hidden rather than printed.
 */
fun parseInline(text: String): List<InlineSpan> {
    val out = ArrayList<InlineSpan>()
    fun plain(t: String) { val c = t.replace("**", "").replace("`", ""); if (c.isNotEmpty()) out += InlineSpan(c) }
    var i = 0
    for (m in inlineToken.findAll(text)) {
        if (m.range.first > i) plain(text.substring(i, m.range.first))
        val g = m.groupValues
        when {
            g[1].isNotEmpty() -> out += InlineSpan(g[1], code = true)
            g[2].isNotEmpty() -> out += InlineSpan(g[2], bold = true)
            g[3].isNotEmpty() -> out += InlineSpan(g[3], bold = true)
            g[4].isNotEmpty() -> out += InlineSpan(g[4], italic = true)
        }
        i = m.range.last + 1
    }
    if (i < text.length) plain(text.substring(i))
    return out
}

/** Plain text of a reply (markdown removed) – used for Copy and screen readers. */
fun plainReply(text: String): String = formatReply(text).joinToString("\n") { b ->
    fun p(t: String) = parseInline(t).joinToString("") { it.text }
    when (b) {
        is ReplyBlock.Heading -> p(b.text)
        is ReplyBlock.Paragraph -> p(b.text)
        is ReplyBlock.Bullet -> "• " + p(b.text)
        is ReplyBlock.Numbered -> "${b.number}. " + p(b.text)
        is ReplyBlock.Code -> b.text
    }
}
