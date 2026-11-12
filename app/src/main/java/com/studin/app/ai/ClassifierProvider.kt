package com.studin.app.ai

import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.StudyRepository
import com.studin.app.core.di.AppScope
import com.studin.domain.classifier.ContentClassifier
import com.studin.domain.classifier.ExplicitLists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps a ready-to-use, syllabus-aware [ContentClassifier] in memory. Rebuilt
 * whenever the student's topics or allow/block lists change.
 */
@Singleton
class ClassifierProvider @Inject constructor(
    studyRepo: StudyRepository,
    prefs: UserPreferences,
    @AppScope scope: CoroutineScope,
) {
    @Volatile var classifier: ContentClassifier = ContentClassifier()
        private set

    init {
        scope.launch {
            combine(studyRepo.observeTopics(), prefs.settings) { topics, s ->
                val terms = topics.flatMap { t -> listOf(t.title) + t.aliases.split(',').filter { it.isNotBlank() } }
                Triple(terms, s.allowKeywords, s.blockKeywords)
            }.distinctUntilChanged().collect { (terms, allow, block) ->
                classifier = ContentClassifier(terms, ExplicitLists(allow, block))
            }
        }
    }
}
