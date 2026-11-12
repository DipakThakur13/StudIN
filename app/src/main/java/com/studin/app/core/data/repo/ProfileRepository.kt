package com.studin.app.core.data.repo

import com.studin.app.core.data.db.CardEntity
import com.studin.app.core.data.db.DeckEntity
import com.studin.app.core.data.db.StudinDatabase
import com.studin.app.core.data.db.StudyProfileEntity
import com.studin.app.core.data.db.SyllabusTopicEntity
import com.studin.domain.catalog.ExamCatalog
import com.studin.domain.model.ExamType
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class Profile(val name: String, val exam: ExamType, val examDay: Long?, val dailyGoalMinutes: Int)

@Singleton
class ProfileRepository @Inject constructor(private val db: StudinDatabase, private val clock: AppClock) {

    fun observe(): Flow<StudyProfileEntity?> = db.profileDao().observe()

    suspend fun get(): Profile? = db.profileDao().get()?.toProfile()

    /**
     * Creates or updates the profile. When the exam changes, the syllabus is
     * re-seeded from the offline catalog and a starter deck is added.
     */
    suspend fun save(profile: Profile) = db.withTransaction {
        val old = db.profileDao().get()
        db.profileDao().upsert(
            StudyProfileEntity(
                name = profile.name.trim().take(40), examType = profile.exam.name, examDay = profile.examDay,
                dailyGoalMinutes = profile.dailyGoalMinutes.coerceIn(15, 720), createdAt = old?.createdAt ?: clock.now(),
            )
        )
        if (old?.examType != profile.exam.name) {
            db.topicDao().clear()
            val topics = ExamCatalog.subjectsFor(profile.exam).flatMap { s ->
                s.topics.map { t -> SyllabusTopicEntity(subject = s.name, title = t.title, aliases = t.aliases.joinToString(","), priority = t.priority) }
            }
            db.topicDao().insertAll(topics)
            val (title, cards) = ExamCatalog.starterCards(profile.exam)
            val now = clock.now()
            val deckId = db.deckDao().insert(DeckEntity(title = title, subject = profile.exam.displayName, color = 0xFF2EE6A6, source = "starter", createdAt = now))
            db.cardDao().insertAll(cards.map { CardEntity(deckId = deckId, front = it.front, back = it.back, tags = it.tags, dueAt = now, createdAt = now) })
        }
    }

    private fun StudyProfileEntity.toProfile() = Profile(
        name, runCatching { ExamType.valueOf(examType) }.getOrDefault(ExamType.JEE), examDay, dailyGoalMinutes,
    )
}

fun StudyProfileEntity.exam(): ExamType = runCatching { ExamType.valueOf(examType) }.getOrDefault(ExamType.JEE)
