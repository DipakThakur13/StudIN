package com.studin.domain.classifier

import com.studin.domain.model.Verdict
import kotlin.math.exp
import kotlin.math.min

data class ClassificationResult(
    val verdict: Verdict,
    /** Probability that the content is study material (0..1). */
    val studyProbability: Double,
    /** Confidence in the verdict itself (0..1). */
    val confidence: Double,
    val tier: Int,
    val studySignals: List<String>,
    val distractionSignals: List<String>,
    val syllabusMatches: List<String>,
    val reason: String,
    val ruleVersion: String = Lexicon.VERSION,
)

/** User-controlled tier-0 lists (channels, domains, playlists, keywords). */
data class ExplicitLists(
    val allowKeywords: Set<String> = emptySet(),
    val blockKeywords: Set<String> = emptySet(),
)

/**
 * Tier 0 + Tier 1 syllabus-aware classifier (roadmap §4). Pure Kotlin, deterministic,
 * explainable and fast (<1 ms for typical titles). BLOCK is only emitted at high
 * confidence with multiple independent signals; uncertain content is allowed.
 */
class ContentClassifier(
    syllabusTerms: Collection<String> = emptyList(),
    private val lists: ExplicitLists = ExplicitLists(),
    private val thresholds: Thresholds = Thresholds(),
) {
    data class Thresholds(
        val allowAbove: Double = 0.70,
        val warnBelow: Double = 0.30,
        val blockBelow: Double = 0.08,
        val minBlockSignals: Int = 2,
    )

    private val syllabus: Map<String, Double> = buildSyllabusWeights(syllabusTerms)

    fun classify(title: String, source: String? = null): ClassificationResult {
        val text = if (source.isNullOrBlank()) title else "$title $source"
        val normalized = TextNormalizer.normalize(text)
        val tokens = TextNormalizer.tokenize(text)

        // ---- Tier 0: explicit user lists
        lists.blockKeywords.firstOrNull { it.isNotBlank() && normalized.contains(TextNormalizer.normalize(it)) }?.let {
            return ClassificationResult(Verdict.BLOCK, 0.01, 0.99, 0, emptyList(), listOf(it), emptyList(), "On your block list: \"$it\"")
        }
        lists.allowKeywords.firstOrNull { it.isNotBlank() && normalized.contains(TextNormalizer.normalize(it)) }?.let {
            return ClassificationResult(Verdict.ALLOW, 0.99, 0.99, 0, listOf(it), emptyList(), emptyList(), "On your allow list: \"$it\"")
        }
        if (tokens.isEmpty()) {
            return ClassificationResult(Verdict.UNKNOWN, 0.5, 0.0, 1, emptyList(), emptyList(), emptyList(), "No readable text")
        }

        // ---- Tier 1: n-gram lexicon matching
        val grams = ngrams(tokens, 3)
        val study = LinkedHashMap<String, Double>()
        val distract = LinkedHashMap<String, Double>()
        val syl = LinkedHashMap<String, Double>()
        for (g in grams) {
            if (g in Lexicon.STOP) continue
            Lexicon.STUDY[g]?.let { study[g] = it }
            Lexicon.DISTRACTION[g]?.let { distract[g] = it }
            syllabus[g]?.let { if (g !in study) syl[g] = it }
        }
        // Class numbers (class 11, class 12) and chapter numbers are strong school signals.
        if (Regex("\\b(class|kaksha|std)\\s?(9|10|11|12)\\b").containsMatchIn(normalized)) study["class 9-12"] = 1.6

        val studyScore = min(6.0, study.values.sum()) + min(5.0, syl.values.sum())
        val distractScore = (-distract.values.sum()).coerceAtMost(6.0)
        val score = studyScore - distractScore
        val evidence = study.size + syl.size + distract.size
        val p = sigmoid(0.9 * score)

        val studyList = study.keys.toList()
        val distList = distract.keys.toList()
        val sylList = syl.keys.toList()

        if (evidence == 0) {
            return ClassificationResult(Verdict.UNKNOWN, 0.5, 0.0, 1, emptyList(), emptyList(), emptyList(),
                "No study or distraction signals – allowed")
        }
        val verdict = when {
            p >= thresholds.allowAbove -> Verdict.ALLOW
            p <= thresholds.blockBelow && distract.size >= thresholds.minBlockSignals -> Verdict.BLOCK
            p <= thresholds.warnBelow -> Verdict.WARN
            else -> Verdict.UNKNOWN
        }
        val confidence = when (verdict) {
            Verdict.ALLOW -> p
            Verdict.BLOCK, Verdict.WARN -> 1 - p
            Verdict.UNKNOWN -> 1 - 2 * kotlin.math.abs(p - 0.5)
        }
        val reason = buildString {
            when (verdict) {
                Verdict.ALLOW -> append("Study content")
                Verdict.BLOCK -> append("Clearly off-syllabus")
                Verdict.WARN -> append("Possibly distracting")
                Verdict.UNKNOWN -> append("Mixed signals – allowed")
            }
            val parts = mutableListOf<String>()
            if (sylList.isNotEmpty()) parts += "syllabus: " + sylList.take(3).joinToString()
            if (studyList.isNotEmpty()) parts += "study: " + studyList.take(3).joinToString()
            if (distList.isNotEmpty()) parts += "distraction: " + distList.take(3).joinToString()
            if (parts.isNotEmpty()) append(" (").append(parts.joinToString("; ")).append(")")
        }
        return ClassificationResult(verdict, p, confidence.coerceIn(0.0, 1.0), 1, studyList, distList, sylList, reason)
    }

    companion object {
        fun sigmoid(x: Double) = 1.0 / (1.0 + exp(-x))

        fun ngrams(tokens: List<String>, maxN: Int): List<String> {
            val out = ArrayList<String>(tokens.size * maxN)
            for (i in tokens.indices) {
                val sb = StringBuilder()
                for (n in 0 until maxN) {
                    if (i + n >= tokens.size) break
                    if (n > 0) sb.append(' ')
                    sb.append(tokens[i + n])
                    out += sb.toString()
                }
            }
            return out
        }

        /** Turns the student's syllabus topic titles & aliases into weighted terms. */
        fun buildSyllabusWeights(terms: Collection<String>): Map<String, Double> {
            val out = HashMap<String, Double>()
            for (t in terms) {
                val toks = TextNormalizer.tokenize(t).filter { it.length > 2 && it !in Lexicon.STOP }
                if (toks.isEmpty()) continue
                // Whole phrase is strong evidence, individual distinctive words are weaker.
                if (toks.size in 2..3) out[toks.joinToString(" ")] = 2.6
                for (w in toks) if (w.length >= 4) out.merge(w, 1.4) { a, b -> maxOf(a, b) }
            }
            return out
        }
    }
}
