package com.studin.app.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "study_profile")
data class StudyProfileEntity(
    @PrimaryKey val id: Long = 1,
    val name: String,
    /** [com.studin.domain.model.ExamType] name. */
    val examType: String,
    /** Epoch day of the exam, or null when not set. */
    val examDay: Long?,
    val dailyGoalMinutes: Int,
    val locale: String = "en-IN",
    val createdAt: Long,
)

@Entity(
    tableName = "syllabus_topic",
    indices = [Index("profileId"), Index("subject")],
)
data class SyllabusTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long = 1,
    val parentId: Long? = null,
    val subject: String,
    val title: String,
    /** Comma separated aliases (Hindi-English terms, abbreviations). */
    val aliases: String = "",
    /** 1 low … 3 high weightage. */
    val priority: Int = 2,
    /** 0..1, updated from focus minutes + quiz accuracy. */
    val mastery: Double = 0.0,
    val minutesStudied: Int = 0,
    val lastStudiedDay: Long? = null,
    val completed: Boolean = false,
)

@Entity(tableName = "focus_rule")
data class FocusRuleEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    /** [com.studin.domain.model.RuleMode] name. */
    val mode: String,
    val enabled: Boolean = true,
    /** Max minutes per day outside focus sessions; 0 = no limit (advisory). */
    val dailyLimitMinutes: Int = 0,
)

@Entity(tableName = "focus_session", indices = [Index("startWallTime")])
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long?,
    val label: String,
    val plannedMinutes: Int,
    val startElapsed: Long,
    val startWallTime: Long,
    val endWallTime: Long? = null,
    /** [com.studin.domain.model.FocusState] name. */
    val status: String,
    val focusedMs: Long = 0,
    val pauses: Int = 0,
    val blockedAttempts: Int = 0,
    val interruptedReason: String? = null,
)

@Entity(tableName = "deck")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String = "",
    /** ARGB colour used for the deck card. */
    val color: Long = 0xFF2EE6A6,
    /** "starter", "manual", "csv", "ai", "scan". */
    val source: String = "manual",
    val createdAt: Long,
)

@Entity(
    tableName = "card",
    foreignKeys = [ForeignKey(entity = DeckEntity::class, parentColumns = ["id"], childColumns = ["deckId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("deckId"), Index("dueAt")],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    val tags: String = "",
    val dueAt: Long,
    val intervalDays: Double = 0.0,
    val easeFactor: Double = 2.5,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val createdAt: Long,
)

@Entity(
    tableName = "review_log",
    foreignKeys = [ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("cardId"), Index("reviewedAt")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val reviewedAt: Long,
    /** [com.studin.domain.model.Grade] name, or "MCQ_CORRECT"/"MCQ_WRONG" for quizzes. */
    val result: String,
    val correct: Boolean,
    /** "review", "quiz", "gate". */
    val context: String = "review",
)

@Entity(tableName = "classification_decision", indices = [Index("timestamp")])
data class ClassificationDecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sourceApp: String,
    /** [com.studin.domain.model.Verdict] name. */
    val label: String,
    val confidence: Double,
    val tier: Int,
    val ruleVersion: String,
    /** Explanation without the raw title (privacy: titles are never stored). */
    val redactedReason: String,
    /** Did the user override (skip/allow) this decision? */
    val overridden: Boolean = false,
)

/** Daily aggregates for charts. */
data class DayFocus(val day: Long, val minutes: Int)
data class DeckWithCounts(
    val id: Long, val title: String, val subject: String, val color: Long, val source: String,
    val total: Int, val due: Int, val mastered: Int,
)
