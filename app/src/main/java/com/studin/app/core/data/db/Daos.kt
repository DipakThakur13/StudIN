package com.studin.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM study_profile WHERE id = 1") fun observe(): Flow<StudyProfileEntity?>
    @Query("SELECT * FROM study_profile WHERE id = 1") suspend fun get(): StudyProfileEntity?
    @Upsert suspend fun upsert(p: StudyProfileEntity)
}

@Dao
interface TopicDao {
    @Query("SELECT * FROM syllabus_topic ORDER BY subject, priority DESC, title") fun observeAll(): Flow<List<SyllabusTopicEntity>>
    @Query("SELECT * FROM syllabus_topic") suspend fun all(): List<SyllabusTopicEntity>
    @Query("SELECT * FROM syllabus_topic WHERE id = :id") suspend fun byId(id: Long): SyllabusTopicEntity?
    @Insert suspend fun insertAll(t: List<SyllabusTopicEntity>)
    @Insert suspend fun insert(t: SyllabusTopicEntity): Long
    @Update suspend fun update(t: SyllabusTopicEntity)
    @Query("DELETE FROM syllabus_topic WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM syllabus_topic") suspend fun clear()
    @Query("UPDATE syllabus_topic SET minutesStudied = minutesStudied + :minutes, lastStudiedDay = :day, mastery = MIN(1.0, mastery + :masteryDelta) WHERE id = :id")
    suspend fun addStudy(id: Long, minutes: Int, day: Long, masteryDelta: Double)
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM focus_rule ORDER BY appLabel COLLATE NOCASE") fun observeAll(): Flow<List<FocusRuleEntity>>
    @Query("SELECT * FROM focus_rule WHERE enabled = 1") suspend fun enabled(): List<FocusRuleEntity>
    @Query("SELECT * FROM focus_rule WHERE packageName = :pkg") suspend fun byPackage(pkg: String): FocusRuleEntity?
    @Upsert suspend fun upsert(r: FocusRuleEntity)
    @Upsert suspend fun upsertAll(r: List<FocusRuleEntity>)
    @Query("DELETE FROM focus_rule WHERE packageName = :pkg") suspend fun delete(pkg: String)
}

@Dao
interface SessionDao {
    @Insert suspend fun insert(s: FocusSessionEntity): Long
    @Update suspend fun update(s: FocusSessionEntity)
    @Query("SELECT * FROM focus_session WHERE id = :id") suspend fun byId(id: Long): FocusSessionEntity?
    @Query("SELECT * FROM focus_session ORDER BY startWallTime DESC LIMIT :limit") fun observeRecent(limit: Int = 30): Flow<List<FocusSessionEntity>>
    @Query("SELECT * FROM focus_session WHERE startWallTime >= :from ORDER BY startWallTime") fun observeSince(from: Long): Flow<List<FocusSessionEntity>>
    @Query("SELECT * FROM focus_session WHERE startWallTime >= :from ORDER BY startWallTime") suspend fun since(from: Long): List<FocusSessionEntity>
    @Query("SELECT * FROM focus_session ORDER BY startWallTime") suspend fun all(): List<FocusSessionEntity>
    @Query("UPDATE focus_session SET blockedAttempts = blockedAttempts + 1 WHERE id = :id") suspend fun incrementBlocked(id: Long)
}

@Dao
interface DeckDao {
    @Insert suspend fun insert(d: DeckEntity): Long
    @Update suspend fun update(d: DeckEntity)
    @Query("DELETE FROM deck WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM deck WHERE id = :id") suspend fun byId(id: Long): DeckEntity?
    @Query("SELECT * FROM deck WHERE id = :id") fun observe(id: Long): Flow<DeckEntity?>
    @Query("SELECT * FROM deck ORDER BY createdAt DESC") suspend fun all(): List<DeckEntity>
    @Query("""
        SELECT d.id, d.title, d.subject, d.color, d.source,
               COUNT(c.id) AS total,
               SUM(CASE WHEN c.dueAt <= :now THEN 1 ELSE 0 END) AS due,
               SUM(CASE WHEN c.intervalDays >= 21 THEN 1 ELSE 0 END) AS mastered
        FROM deck d LEFT JOIN card c ON c.deckId = d.id
        GROUP BY d.id ORDER BY d.createdAt DESC
    """)
    fun observeWithCounts(now: Long): Flow<List<DeckWithCounts>>
}

@Dao
interface CardDao {
    @Insert suspend fun insert(c: CardEntity): Long
    @Insert suspend fun insertAll(c: List<CardEntity>)
    @Update suspend fun update(c: CardEntity)
    @Query("DELETE FROM card WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM card WHERE id = :id") suspend fun byId(id: Long): CardEntity?
    @Query("SELECT * FROM card WHERE deckId = :deckId ORDER BY createdAt") fun observeDeck(deckId: Long): Flow<List<CardEntity>>
    @Query("SELECT * FROM card WHERE deckId = :deckId ORDER BY createdAt") suspend fun deck(deckId: Long): List<CardEntity>
    @Query("SELECT * FROM card WHERE dueAt <= :now AND (:deckId IS NULL OR deckId = :deckId) ORDER BY dueAt LIMIT :limit")
    suspend fun due(now: Long, deckId: Long?, limit: Int = 200): List<CardEntity>
    @Query("SELECT COUNT(*) FROM card WHERE dueAt <= :now") fun observeDueCount(now: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM card WHERE dueAt <= :now") suspend fun dueCount(now: Long): Int
    @Query("SELECT COUNT(*) FROM card") fun observeTotal(): Flow<Int>
    @Query("SELECT * FROM card ORDER BY RANDOM() LIMIT :limit") suspend fun random(limit: Int): List<CardEntity>
    @Query("SELECT * FROM card") suspend fun all(): List<CardEntity>
}

@Dao
interface ReviewDao {
    @Insert suspend fun insert(r: ReviewLogEntity)
    @Query("SELECT * FROM review_log WHERE reviewedAt >= :from") fun observeSince(from: Long): Flow<List<ReviewLogEntity>>
    @Query("SELECT * FROM review_log WHERE reviewedAt >= :from") suspend fun since(from: Long): List<ReviewLogEntity>
    @Query("SELECT * FROM review_log") suspend fun all(): List<ReviewLogEntity>
}

@Dao
interface DecisionDao {
    @Insert suspend fun insert(d: ClassificationDecisionEntity): Long
    @Query("SELECT * FROM classification_decision ORDER BY timestamp DESC LIMIT :limit") fun observeRecent(limit: Int = 50): Flow<List<ClassificationDecisionEntity>>
    @Query("SELECT * FROM classification_decision WHERE timestamp >= :from") suspend fun since(from: Long): List<ClassificationDecisionEntity>
    @Query("UPDATE classification_decision SET overridden = 1 WHERE id = :id") suspend fun markOverridden(id: Long)
    /** Retention limit: decisions are kept for 30 days. */
    @Query("DELETE FROM classification_decision WHERE timestamp < :before") suspend fun prune(before: Long)
}
