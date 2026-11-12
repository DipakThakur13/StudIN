package com.studin.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudyProfileEntity::class, SyllabusTopicEntity::class, FocusRuleEntity::class, FocusSessionEntity::class,
        DeckEntity::class, CardEntity::class, ReviewLogEntity::class, ClassificationDecisionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StudinDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun topicDao(): TopicDao
    abstract fun ruleDao(): RuleDao
    abstract fun sessionDao(): SessionDao
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun reviewDao(): ReviewDao
    abstract fun decisionDao(): DecisionDao

    companion object {
        const val NAME = "studin.db"
        /** Add explicit, tested migrations here for every schema bump (roadmap §6). */
        val MIGRATIONS = arrayOf<androidx.room.migration.Migration>()
    }
}
