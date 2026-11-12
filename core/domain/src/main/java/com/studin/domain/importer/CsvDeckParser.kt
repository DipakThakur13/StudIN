package com.studin.domain.importer

/** A validated flashcard row. */
data class ParsedCard(val front: String, val back: String, val tags: String = "")

data class CsvImportResult(
    val cards: List<ParsedCard>,
    val skipped: Int,
    val errors: List<String>,
)

class CsvImportException(message: String) : IllegalArgumentException(message)

/**
 * Strict, bounded CSV parser for flashcard decks (roadmap §7 threat model:
 * malicious deck imports). RFC-4180 quoting, size caps, control-character
 * stripping, and per-field length limits. Never evaluates content.
 */
object CsvDeckParser {
    const val MAX_BYTES = 1_000_000
    const val MAX_ROWS = 5_000
    const val MAX_FIELD = 1_000

    fun parse(input: String): CsvImportResult {
        if (input.length > MAX_BYTES) throw CsvImportException("File too large (max 1 MB)")
        val rows = splitRows(input.removePrefix("﻿"))
        if (rows.size > MAX_ROWS + 1) throw CsvImportException("Too many rows (max $MAX_ROWS)")
        val cards = ArrayList<ParsedCard>()
        val errors = ArrayList<String>()
        var skipped = 0
        rows.forEachIndexed { idx, fields ->
            if (fields.all { it.isBlank() }) return@forEachIndexed
            if (idx == 0 && isHeader(fields)) return@forEachIndexed
            if (fields.size < 2) {
                skipped++; if (errors.size < 20) errors += "Row ${idx + 1}: needs front and back"
                return@forEachIndexed
            }
            val front = clean(fields[0]); val back = clean(fields[1])
            val tags = if (fields.size > 2) clean(fields[2]).take(120) else ""
            when {
                front.isEmpty() || back.isEmpty() -> { skipped++; if (errors.size < 20) errors += "Row ${idx + 1}: empty field" }
                fields[0].length > MAX_FIELD || fields[1].length > MAX_FIELD -> { skipped++; if (errors.size < 20) errors += "Row ${idx + 1}: field longer than $MAX_FIELD chars" }
                else -> cards += ParsedCard(front, back, tags)
            }
        }
        return CsvImportResult(cards, skipped, errors)
    }

    private fun isHeader(f: List<String>): Boolean {
        val a = f[0].trim().lowercase(); val b = f.getOrNull(1)?.trim()?.lowercase()
        return (a in setOf("front", "question", "q", "term")) && (b in setOf("back", "answer", "a", "definition"))
    }

    /** Remove control characters (except newline/tab) and trim. */
    fun clean(s: String): String = buildString(s.length) {
        for (c in s) if (c == '\n' || c == '\t' || !c.isISOControl()) append(c)
    }.trim()

    /** Neutralise spreadsheet formula injection when exporting. */
    fun escapeForExport(s: String): String {
        val safe = if (s.isNotEmpty() && s[0] in "=+-@\t\r") "'$s" else s
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    internal fun splitRows(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val delimiter = detectDelimiter(text)
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else field.append(c)
            } else when (c) {
                '"' -> if (field.isEmpty()) inQuotes = true else field.append(c)
                delimiter -> { row.add(field.toString()); field.setLength(0) }
                '\r' -> {}
                '\n' -> {
                    row.add(field.toString()); field.setLength(0); rows.add(row); row = ArrayList()
                    if (rows.size > MAX_ROWS + 1) return rows
                }
                else -> field.append(c)
            }
            if (field.length > MAX_FIELD * 4) throw CsvImportException("Field too long near row ${rows.size + 1}")
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
        return rows
    }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull() ?: return ','
        return when {
            firstLine.count { it == '\t' } > firstLine.count { it == ',' } -> '\t'
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            else -> ','
        }
    }
}
