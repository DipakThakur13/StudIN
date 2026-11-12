package com.studin.app.core.data.repo

import androidx.room.withTransaction
import com.studin.app.core.data.db.CardEntity
import com.studin.app.core.data.db.DeckEntity
import com.studin.app.core.data.db.DeckWithCounts
import com.studin.app.core.data.db.ReviewLogEntity
import com.studin.app.core.data.db.StudinDatabase
import com.studin.app.core.data.db.SyllabusTopicEntity
import com.studin.domain.importer.CsvDeckParser
import com.studin.domain.importer.CsvImportResult
import com.studin.domain.importer.ParsedCard
import com.studin.domain.model.Grade
import com.studin.domain.quiz.QuizSource
import com.studin.domain.srs.CardSchedule
import com.studin.domain.srs.Sm2Scheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyRepository @Inject constructor(private val db: StudinDatabase, private val clock: AppClock) {
    private val cards get() = db.cardDao()
    private val decks get() = db.deckDao()

    // ---------------- Syllabus
    fun observeTopics(): Flow<List<SyllabusTopicEntity>> = db.topicDao().observeAll()
    suspend fun topics(): List<SyllabusTopicEntity> = db.topicDao().all()
    suspend fun topic(id: Long) = db.topicDao().byId(id)
    suspend fun updateTopic(t: SyllabusTopicEntity) = db.topicDao().update(t)
    suspend fun addTopic(subject: String, title: String, priority: Int = 2): Long =
        db.topicDao().insert(SyllabusTopicEntity(subject = subject.trim().take(60), title = title.trim().take(120), priority = priority.coerceIn(1, 3)))
    suspend fun deleteTopic(id: Long) = db.topicDao().delete(id)
    suspend fun recordTopicStudy(topicId: Long, minutes: Int) {
        // Diminishing returns: ~6 hours of focused study → +0.6 mastery; quizzes add the rest.
        db.topicDao().addStudy(topicId, minutes, clock.today(), masteryDelta = minutes / 600.0)
    }

    // ---------------- Decks
    fun observeDecks(): Flow<List<DeckWithCounts>> = decks.observeWithCounts(clock.now())
    fun observeDeck(id: Long): Flow<DeckEntity?> = decks.observe(id)
    suspend fun deck(id: Long) = decks.byId(id)
    suspend fun allDecks() = decks.all()
    suspend fun createDeck(title: String, subject: String = "", source: String = "manual", color: Long = DECK_COLORS.random()): Long =
        decks.insert(DeckEntity(title = title.trim().take(80).ifEmpty { "Untitled deck" }, subject = subject.take(60), source = source, color = color, createdAt = clock.now()))
    suspend fun renameDeck(id: Long, title: String) { decks.byId(id)?.let { decks.update(it.copy(title = title.trim().take(80))) } }
    suspend fun deleteDeck(id: Long) = decks.delete(id)

    // ---------------- Cards
    fun observeCards(deckId: Long): Flow<List<CardEntity>> = cards.observeDeck(deckId)
    fun observeDueCount(): Flow<Int> = cards.observeDueCount(clock.now())
    fun observeTotalCards(): Flow<Int> = cards.observeTotal()
    suspend fun dueCount() = cards.dueCount(clock.now())
    suspend fun dueCards(deckId: Long? = null, limit: Int = 200) = cards.due(clock.now(), deckId, limit)
    suspend fun card(id: Long) = cards.byId(id)
    suspend fun deleteCard(id: Long) = cards.delete(id)
    suspend fun addCard(deckId: Long, front: String, back: String, tags: String = ""): Long {
        val now = clock.now()
        return cards.insert(CardEntity(deckId = deckId, front = CsvDeckParser.clean(front).take(1000), back = CsvDeckParser.clean(back).take(1000), tags = tags.take(120), dueAt = now, createdAt = now))
    }
    suspend fun updateCard(card: CardEntity) = cards.update(card.copy(front = CsvDeckParser.clean(card.front).take(1000), back = CsvDeckParser.clean(card.back).take(1000)))

    suspend fun addCards(deckId: Long, parsed: List<ParsedCard>): Int {
        val now = clock.now()
        val list = parsed.take(CsvDeckParser.MAX_ROWS).map {
            CardEntity(deckId = deckId, front = it.front.take(1000), back = it.back.take(1000), tags = it.tags.take(120), dueAt = now, createdAt = now)
        }
        cards.insertAll(list)
        return list.size
    }

    /** Validated CSV import into a new deck. Throws [com.studin.domain.importer.CsvImportException] on hostile input. */
    suspend fun importCsv(deckTitle: String, csv: String): Pair<Long, CsvImportResult> {
        val result = CsvDeckParser.parse(csv)
        if (result.cards.isEmpty()) return -1L to result
        val id = db.withTransaction {
            val deckId = createDeck(deckTitle, source = "csv")
            addCards(deckId, result.cards)
            deckId
        }
        return id to result
    }

    suspend fun exportDeckCsv(deckId: Long): String = buildString {
        append("front,back,tags\n")
        cards.deck(deckId).forEach {
            append(CsvDeckParser.escapeForExport(it.front)).append(',')
                .append(CsvDeckParser.escapeForExport(it.back)).append(',')
                .append(CsvDeckParser.escapeForExport(it.tags)).append('\n')
        }
    }

    /** Apply an SM-2 review and log it. */
    suspend fun review(card: CardEntity, grade: Grade): CardEntity {
        val now = clock.now()
        val next = Sm2Scheduler.review(card.schedule(), grade, now)
        val updated = card.copy(dueAt = next.dueAt, intervalDays = next.intervalDays, easeFactor = next.easeFactor, repetitions = next.repetitions, lapses = next.lapses)
        db.withTransaction {
            cards.update(updated)
            db.reviewDao().insert(ReviewLogEntity(cardId = card.id, reviewedAt = now, result = grade.name, correct = grade != Grade.AGAIN))
        }
        return updated
    }

    /** Log an MCQ answer (quiz or gate). Wrong answers make the card due again – no other penalty. */
    suspend fun recordMcq(cardId: Long, correct: Boolean, context: String) {
        val now = clock.now()
        db.withTransaction {
            db.reviewDao().insert(ReviewLogEntity(cardId = cardId, reviewedAt = now, result = if (correct) "MCQ_CORRECT" else "MCQ_WRONG", correct = correct, context = context))
            if (!correct) cards.byId(cardId)?.let { cards.update(it.copy(dueAt = minOf(it.dueAt, now))) }
        }
    }

    suspend fun quizPool(deckId: Long? = null, limit: Int = 60): List<QuizSource> {
        val src = if (deckId != null) cards.deck(deckId) else (cards.due(clock.now(), null, limit) + cards.random(limit)).distinctBy { it.id }
        return src.map { QuizSource(it.id, it.front, it.back) }
    }

    companion object {
        val DECK_COLORS = listOf(0xFF2EE6A6, 0xFF42C9F5, 0xFF8E7CFF, 0xFFFFD866, 0xFFFF7A85, 0xFF1FC8C0)
    }
}

fun CardEntity.schedule() = CardSchedule(repetitions, intervalDays, easeFactor, lapses, dueAt)
