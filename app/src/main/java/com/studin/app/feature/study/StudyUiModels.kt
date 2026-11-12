package com.studin.app.feature.study

import com.studin.app.core.data.db.ReviewLogEntity
import com.studin.app.core.data.db.SyllabusTopicEntity
import com.studin.domain.model.Grade
import kotlin.math.roundToInt

/** Presentation model for one subject card ("My subjects"). */
data class SubjectSummary(
    val subject: String,
    val chapterCount: Int,
    val completedCount: Int,
    /** 0..1 average topic mastery (estimated from focus minutes + quizzes). */
    val averageMastery: Float,
    val minutesStudied: Int,
    /** Best topic to continue with, or null when every chapter is complete. */
    val nextTopic: SyllabusTopicEntity?,
) {
    /** Share of chapters marked complete, 0..1. */
    val completion: Float get() = if (chapterCount == 0) 0f else completedCount / chapterCount.toFloat()
    val completionPercent: Int get() = (completion * 100).roundToInt()
    val masteryPercent: Int get() = (averageMastery * 100).roundToInt()
}

/**
 * Groups topics by subject, preserving the order in which subjects first appear (the DAO sorts by
 * subject). Each summary carries its recommended next topic.
 */
fun summarizeSubjects(topics: List<SyllabusTopicEntity>): List<SubjectSummary> =
    topics.groupBy { it.subject }.map { (subject, list) ->
        SubjectSummary(
            subject = subject,
            chapterCount = list.size,
            completedCount = list.count { it.completed },
            averageMastery = if (list.isEmpty()) 0f else list.map { it.mastery }.average().toFloat().coerceIn(0f, 1f),
            minutesStudied = list.sumOf { it.minutesStudied },
            nextTopic = nextTopic(list),
        )
    }

/**
 * The topic to continue with: incomplete only; higher priority first, then lower mastery, then
 * topics never studied before ones studied long ago, then alphabetical for stability.
 */
fun nextTopic(topics: List<SyllabusTopicEntity>): SyllabusTopicEntity? =
    topics.filter { !it.completed }.minWithOrNull(
        compareByDescending<SyllabusTopicEntity> { it.priority }
            .thenBy { it.mastery }
            .thenBy { it.lastStudiedDay ?: Long.MIN_VALUE }
            .thenBy { it.title.lowercase() },
    )

/** Priority cycle used by the tap-to-change badge: High → Low → Med → High. */
fun cyclePriority(priority: Int): Int = when (priority) { 3 -> 1; 1 -> 2; else -> 3 }

fun priorityLabel(p: Int) = when (p) { 3 -> "High"; 1 -> "Low"; else -> "Med" }

/** Last-7-days review/quiz activity derived from the review log (no invented numbers). */
data class StudyHistory(
    val flashcardReviews: Int,
    val quizAnswers: Int,
    val correct: Int,
    val activeDays: Int,
) {
    val total: Int get() = flashcardReviews + quizAnswers
    /** Null when there is nothing to measure. */
    val accuracyPercent: Int? get() = if (total == 0) null else (correct * 100f / total).roundToInt()
    val isEmpty: Boolean get() = total == 0
}

/** @param dayOf maps a wall-clock timestamp to an epoch day (injected for deterministic tests). */
fun studyHistory(logs: List<ReviewLogEntity>, dayOf: (Long) -> Long): StudyHistory = StudyHistory(
    flashcardReviews = logs.count { it.context == "review" },
    quizAnswers = logs.count { it.context == "quiz" || it.context == "gate" },
    correct = logs.count { it.correct },
    activeDays = logs.map { dayOf(it.reviewedAt) }.distinct().size,
)

/** Session summary for the review screen. */
data class ReviewSummary(val total: Int, val again: Int, val hard: Int, val good: Int, val easy: Int) {
    /** Share of cards recalled (anything but Again). */
    val recallRate: Float get() = if (total == 0) 0f else (total - again) / total.toFloat()
    val recallPercent: Int get() = (recallRate * 100).roundToInt()
}

fun reviewSummary(counts: Map<Grade, Int>): ReviewSummary = ReviewSummary(
    total = counts.values.sum(),
    again = counts[Grade.AGAIN] ?: 0,
    hard = counts[Grade.HARD] ?: 0,
    good = counts[Grade.GOOD] ?: 0,
    easy = counts[Grade.EASY] ?: 0,
)

/** Human label for when a card is next due, e.g. "Due now", "Due in 3d", "Due in 5h". */
fun dueLabel(dueAt: Long, now: Long): String {
    val diff = dueAt - now
    if (diff <= 0) return "Due now"
    val minutes = diff / 60_000L
    return when {
        minutes < 60 -> "Due in ${minutes.coerceAtLeast(1)}m"
        minutes < 60 * 24 -> "Due in ${minutes / 60}h"
        minutes < 60 * 24 * 60 -> "Due in ${minutes / (60 * 24)}d"
        else -> "Due in ${minutes / (60 * 24 * 30)}mo"
    }
}

/** Mastered share of a deck (cards with ≥ 21-day intervals), 0..1. */
fun deckMastery(total: Int, mastered: Int): Float = if (total <= 0) 0f else (mastered / total.toFloat()).coerceIn(0f, 1f)

const val ACCENT_SLOTS = 6

/** Maps a stored deck colour to one of [ACCENT_SLOTS] token accents (keeps raw colours out of the UI). */
fun deckAccentIndex(storedColor: Long, palette: List<Long>): Int {
    val i = palette.indexOf(storedColor)
    return if (i >= 0) i % ACCENT_SLOTS else Math.floorMod(storedColor ushr 8, ACCENT_SLOTS.toLong()).toInt()
}

/** Quiz score message, kept pure for tests. */
fun quizVerdict(correct: Int, total: Int): String {
    if (total == 0) return "No questions answered"
    val pct = correct * 100 / total
    return when {
        pct >= 90 -> "Excellent recall"
        pct >= 70 -> "Strong work"
        pct >= 50 -> "Good progress – review the misses"
        else -> "Keep practising – missed cards are now due for review"
    }
}
