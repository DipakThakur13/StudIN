package com.studin.domain.ai

import com.studin.domain.classifier.Lexicon
import com.studin.domain.classifier.TextNormalizer
import com.studin.domain.importer.ParsedCard
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * "Lite" on-device intelligence that works on every phone with zero downloads:
 * extractive flashcard generation, keyword extraction, summarisation and
 * retrieval over the student's own material. Used directly, and as the fallback
 * whenever the optional on-device LLM is unavailable.
 */
object LiteAi {

    private val definitionPatterns = listOf(
        Regex("^(.{2,80}?)\\s+(?:is|are|was|were)\\s+(?:called|known as|defined as|termed)\\s+(.{3,200})$", RegexOption.IGNORE_CASE),
        Regex("^(.{2,80}?)\\s+(?:is|are)\\s+(?:a|an|the)\\s+(.{3,300})$", RegexOption.IGNORE_CASE),
        Regex("^(.{2,80}?)\\s+(?:refers to|means|denotes|represents|stands for)\\s+(.{3,300})$", RegexOption.IGNORE_CASE),
        Regex("^(.{2,60}?)\\s*[:\\-–—]\\s+(.{3,300})$"),
        Regex("^(.{2,80}?)\\s+(?:ko|को)\\s+(.{3,200}?)\\s+(?:kehte hain|कहते हैं)$", RegexOption.IGNORE_CASE),
    )
    private val formulaPattern = Regex("^([A-Za-zα-ωΑ-Ω][\\w\\s()]{0,40}?)\\s*=\\s*(.{1,120})$")

    fun splitSentences(text: String): List<String> =
        text.replace("\r", "")
            .split(Regex("(?<=[.!?।])\\s+|\\n+|•|\\u2022"))
            .map { it.trim().trimStart('-', '*', '•', ' ').trim() }
            .filter { it.length in 5..400 }

    /** Turn free-form notes into flashcards using definition, formula and cloze patterns. */
    fun notesToCards(notes: String, max: Int = 25): List<ParsedCard> {
        val cards = LinkedHashMap<String, ParsedCard>()
        val sentences = splitSentences(notes.take(20_000))
        val keywords = keywords(notes, 30).map { it.first }.toSet()
        for (raw in sentences) {
            val s = raw.trimEnd('.', '।')
            val def = definitionPatterns.firstNotNullOfOrNull { it.find(s) }
            if (def != null) {
                val term = def.groupValues[1].trim()
                val body = def.groupValues[2].trim()
                if (term.split(" ").size <= 8 && body.length >= 3) {
                    val q = if (s.contains("kehte") || s.contains("कहते")) "$body kise kehte hain?" else "What is $term?"
                    cards.putIfAbsent(q.lowercase(), ParsedCard(q, cap(body)))
                    continue
                }
            }
            formulaPattern.find(s)?.let {
                val lhs = it.groupValues[1].trim()
                cards.putIfAbsent("formula $lhs".lowercase(), ParsedCard("Formula: $lhs = ?", it.groupValues[2].trim()))
                return@let
            } ?: run {
                // Cloze: hide the most informative keyword or a number.
                val tokens = s.split(Regex("\\s+"))
                val target = tokens.firstOrNull { tok -> TextNormalizer.normalize(tok) in keywords && tok.length > 3 }
                    ?: tokens.firstOrNull { it.any(Char::isDigit) && it.length <= 12 }
                if (target != null && tokens.size in 5..45) {
                    val clean = target.trim(',', ';', '(', ')', '.', ':')
                    if (clean.length >= 2) {
                        val q = s.replaceFirst(clean, "_____")
                        cards.putIfAbsent(q.lowercase(), ParsedCard("Fill in the blank: $q", clean))
                    }
                }
            }
            if (cards.size >= max) break
        }
        return cards.values.take(max)
    }

    /** TF·IDF-like keyword scoring within the document (sentences as documents). */
    fun keywords(text: String, top: Int = 10): List<Pair<String, Double>> {
        val sentences = splitSentences(text).ifEmpty { listOf(text) }
        val df = HashMap<String, Int>()
        val tf = HashMap<String, Int>()
        for (s in sentences) {
            val toks = TextNormalizer.tokenize(s).filter { it.length > 3 && it !in Lexicon.STOP && it !in COMMON }
            toks.toSet().forEach { df.merge(it, 1, Int::plus) }
            toks.forEach { tf.merge(it, 1, Int::plus) }
        }
        val n = sentences.size.toDouble()
        return tf.map { (w, c) -> w to c * (1 + ln(n / (df[w] ?: 1))) * (1 + if (w.length > 7) 0.3 else 0.0) }
            .sortedByDescending { it.second }
            .take(top)
    }

    /** Extractive summary: highest scoring sentences in original order. */
    fun summarize(text: String, sentences: Int = 4): List<String> {
        val all = splitSentences(text)
        if (all.size <= sentences) return all
        val kw = keywords(text, 25).toMap()
        val scored = all.mapIndexed { i, s ->
            val toks = TextNormalizer.tokenize(s)
            val score = toks.sumOf { kw[it] ?: 0.0 } / sqrt(toks.size.coerceAtLeast(1).toDouble()) + if (i == 0) 0.5 else 0.0
            Triple(i, s, score)
        }
        return scored.sortedByDescending { it.third }.take(sentences).sortedBy { it.first }.map { it.second }
    }

    data class Doc(val id: Long, val title: String, val body: String, val kind: String)
    data class Hit(val doc: Doc, val score: Double)

    /** BM25 retrieval over the student's cards and syllabus. */
    fun retrieve(query: String, docs: List<Doc>, top: Int = 4): List<Hit> {
        val q = TextNormalizer.tokenize(query).filter { it !in Lexicon.STOP && it !in COMMON && it.length > 1 }.toSet()
        if (q.isEmpty() || docs.isEmpty()) return emptyList()
        val tokenized = docs.map { TextNormalizer.tokenize(it.title + " " + it.body) }
        val avg = tokenized.map { it.size }.average().coerceAtLeast(1.0)
        val df = HashMap<String, Int>()
        tokenized.forEach { t -> t.toSet().forEach { w -> if (w in q) df.merge(w, 1, Int::plus) } }
        val n = docs.size.toDouble()
        val k1 = 1.4; val b = 0.75
        return docs.indices.map { i ->
            val t = tokenized[i]
            val counts = t.groupingBy { it }.eachCount()
            var s = 0.0
            for (w in q) {
                val f = counts[w] ?: continue
                val idf = ln(1 + (n - (df[w] ?: 0) + 0.5) / ((df[w] ?: 0) + 0.5))
                s += idf * (f * (k1 + 1)) / (f + k1 * (1 - b + b * t.size / avg))
            }
            Hit(docs[i], s)
        }.filter { it.score > 0.2 }.sortedByDescending { it.score }.take(top)
    }

    /** Parse LLM output in "Q: … / A: …" form into cards (robust to numbering & markdown). */
    fun parseQaBlocks(text: String, max: Int = 30): List<ParsedCard> {
        val out = ArrayList<ParsedCard>()
        var q: String? = null
        val a = StringBuilder()
        fun flush() {
            val qq = q?.trim(); val aa = a.toString().trim()
            if (!qq.isNullOrEmpty() && aa.isNotEmpty()) out += ParsedCard(qq.take(500), aa.take(1000))
            q = null; a.setLength(0)
        }
        for (line0 in text.lines()) {
            val line = line0.trim().trimStart('*', '-', '#', ' ').replace("**", "")
            val qm = Regex("^(?:\\d+[.)]\\s*)?(?:Q|Question)\\s*\\d*\\s*[:.)-]\\s*(.+)$", RegexOption.IGNORE_CASE).find(line)
            val am = Regex("^(?:A|Answer|Ans)\\s*\\d*\\s*[:.)-]\\s*(.+)$", RegexOption.IGNORE_CASE).find(line)
            when {
                qm != null -> { flush(); q = qm.groupValues[1] }
                am != null && q != null -> { a.setLength(0); a.append(am.groupValues[1]) }
                line.isNotEmpty() && q != null && a.isNotEmpty() -> a.append(' ').append(line)
            }
            if (out.size >= max) break
        }
        flush()
        return out.take(max)
    }

    private fun cap(s: String) = s.replaceFirstChar { it.uppercase() }

    private val COMMON = setOf(
        "also", "which", "their", "there", "these", "those", "have", "has", "been", "being", "into", "than",
        "then", "when", "where", "while", "such", "more", "most", "some", "many", "only", "very", "used", "using",
        "called", "known", "defined", "between", "other", "each", "about", "after", "before", "because", "they",
        "them", "will", "would", "could", "should", "does", "doing", "done", "like", "make", "made", "same",
        "what", "explain", "tell", "give", "please", "kya", "hota", "hai", "hain", "kaise",
    )
}
