package com.studin.app.feature.study

import com.studin.app.core.data.db.ReviewLogEntity
import com.studin.app.core.data.db.SyllabusTopicEntity
import com.studin.app.core.data.repo.StudyRepository
import com.studin.domain.model.Grade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyUiModelsTest {

    private fun topic(id: Long, subject: String, title: String, priority: Int = 2, mastery: Double = 0.0, completed: Boolean = false, minutes: Int = 0, lastDay: Long? = null) =
        SyllabusTopicEntity(id = id, subject = subject, title = title, priority = priority, mastery = mastery, completed = completed, minutesStudied = minutes, lastStudiedDay = lastDay)

    @Test fun subjectsKeepOrderAndAggregate() {
        val topics = listOf(
            topic(1, "Chemistry", "Mole Concept", completed = true, mastery = 0.8, minutes = 120),
            topic(2, "Chemistry", "Bonding", mastery = 0.2, minutes = 30),
            topic(3, "Physics", "Kinematics", mastery = 0.5),
        )
        val s = summarizeSubjects(topics)
        assertEquals(listOf("Chemistry", "Physics"), s.map { it.subject })
        val chem = s[0]
        assertEquals(2, chem.chapterCount)
        assertEquals(1, chem.completedCount)
        assertEquals(50, chem.completionPercent)
        assertEquals(50, chem.masteryPercent)
        assertEquals(150, chem.minutesStudied)
        assertEquals("Bonding", chem.nextTopic?.title)
    }

    @Test fun emptySubjectListIsEmpty() {
        assertTrue(summarizeSubjects(emptyList()).isEmpty())
    }

    @Test fun nextTopicPrefersPriorityThenLowMasteryThenNeverStudied() {
        val list = listOf(
            topic(1, "P", "Low priority, untouched", priority = 1),
            topic(2, "P", "High, strong", priority = 3, mastery = 0.9),
            topic(3, "P", "High, weak, studied", priority = 3, mastery = 0.1, lastDay = 100),
            topic(4, "P", "High, weak, never studied", priority = 3, mastery = 0.1, lastDay = null),
        )
        assertEquals("High, weak, never studied", nextTopic(list)?.title)
    }

    @Test fun nextTopicIgnoresCompletedAndReturnsNullWhenDone() {
        assertNull(nextTopic(listOf(topic(1, "P", "Done", completed = true))))
        assertEquals("Open", nextTopic(listOf(topic(1, "P", "Done", priority = 3, completed = true), topic(2, "P", "Open", priority = 1)))?.title)
    }

    @Test fun priorityCyclesThroughAllLevels() {
        assertEquals(1, cyclePriority(3))
        assertEquals(2, cyclePriority(1))
        assertEquals(3, cyclePriority(2))
        assertEquals("High", priorityLabel(3)); assertEquals("Med", priorityLabel(2)); assertEquals("Low", priorityLabel(1))
    }

    @Test fun historyCountsContextsAccuracyAndDays() {
        val day = 86_400_000L
        val logs = listOf(
            ReviewLogEntity(cardId = 1, reviewedAt = 0, result = "GOOD", correct = true, context = "review"),
            ReviewLogEntity(cardId = 1, reviewedAt = 10, result = "AGAIN", correct = false, context = "review"),
            ReviewLogEntity(cardId = 2, reviewedAt = day + 5, result = "MCQ_CORRECT", correct = true, context = "quiz"),
            ReviewLogEntity(cardId = 3, reviewedAt = 2 * day, result = "MCQ_WRONG", correct = false, context = "gate"),
        )
        val h = studyHistory(logs) { it / day }
        assertEquals(2, h.flashcardReviews)
        assertEquals(2, h.quizAnswers)
        assertEquals(50, h.accuracyPercent)
        assertEquals(3, h.activeDays)
        assertTrue(studyHistory(emptyList()) { it }.isEmpty)
        assertNull(studyHistory(emptyList()) { it }.accuracyPercent)
    }

    @Test fun reviewSummaryCountsAndRecall() {
        val s = reviewSummary(mapOf(Grade.AGAIN to 1, Grade.GOOD to 2, Grade.EASY to 1))
        assertEquals(4, s.total)
        assertEquals(0, s.hard)
        assertEquals(75, s.recallPercent)
        assertEquals(0f, reviewSummary(emptyMap()).recallRate, 0f)
    }

    @Test fun dueLabels() {
        val now = 1_000_000L
        assertEquals("Due now", dueLabel(now, now))
        assertEquals("Due now", dueLabel(now - 5, now))
        assertEquals("Due in 1m", dueLabel(now + 10_000, now))
        assertEquals("Due in 10m", dueLabel(now + 10 * 60_000, now))
        assertEquals("Due in 5h", dueLabel(now + 5 * 3_600_000L, now))
        assertEquals("Due in 3d", dueLabel(now + 3 * 86_400_000L, now))
        assertEquals("Due in 3mo", dueLabel(now + 90 * 86_400_000L, now))
    }

    @Test fun deckMasteryIsBounded() {
        assertEquals(0f, deckMastery(0, 0), 0f)
        assertEquals(0.25f, deckMastery(8, 2), 0.0001f)
        assertEquals(1f, deckMastery(2, 5), 0f)
    }

    @Test fun deckAccentMapsKnownAndUnknownColoursIntoSlots() {
        val palette = StudyRepository.DECK_COLORS
        palette.forEachIndexed { i, c -> assertEquals(i % ACCENT_SLOTS, deckAccentIndex(c, palette)) }
        listOf(0L, -1L, 0xFF123456, Long.MIN_VALUE).forEach { assertTrue(deckAccentIndex(it, palette) in 0 until ACCENT_SLOTS) }
    }

    @Test fun quizVerdictBands() {
        assertEquals("No questions answered", quizVerdict(0, 0))
        assertEquals("Excellent recall", quizVerdict(9, 10))
        assertEquals("Strong work", quizVerdict(7, 10))
        assertTrue(quizVerdict(5, 10).startsWith("Good progress"))
        assertTrue(quizVerdict(1, 10).startsWith("Keep practising"))
    }
}
