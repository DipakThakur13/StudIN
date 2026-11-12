package com.studin.domain.quiz

import kotlin.random.Random

data class QuizSource(val id: Long, val question: String, val answer: String)

data class McqQuestion(
    val sourceId: Long,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
)

/**
 * Builds multiple-choice questions from flashcards. Distractors are other cards'
 * answers from the same pool, preferring similar length so options look plausible.
 */
object QuizGenerator {
    fun generate(pool: List<QuizSource>, count: Int, seed: Long = System.nanoTime()): List<McqQuestion> {
        val usable = pool.filter { it.question.isNotBlank() && it.answer.isNotBlank() }.distinctBy { it.answer.trim().lowercase() }
        if (usable.size < 2 || count <= 0) return emptyList()
        val rnd = Random(seed)
        return usable.shuffled(rnd).take(count).map { card ->
            val distractors = usable.asSequence()
                .filter { it.id != card.id }
                .sortedBy { kotlin.math.abs(it.answer.length - card.answer.length) + rnd.nextInt(12) }
                .take(3)
                .map { it.answer.trim() }
                .toList()
            val options = (distractors + card.answer.trim()).shuffled(rnd)
            McqQuestion(card.id, card.question.trim(), options, options.indexOf(card.answer.trim()))
        }
    }
}
