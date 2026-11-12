package com.studin.app.core.data.repo

import com.studin.app.core.data.db.ClassificationDecisionEntity
import com.studin.app.core.data.db.FocusRuleEntity
import com.studin.app.core.data.db.FocusSessionEntity
import com.studin.app.core.data.db.StudinDatabase
import com.studin.domain.classifier.ClassificationResult
import com.studin.domain.model.RuleMode
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusRepository @Inject constructor(private val db: StudinDatabase, private val clock: AppClock) {

    /** In-memory rule cache for the hot path (accessibility events). */
    private val ruleCache = ConcurrentHashMap<String, RuleMode>()
    @Volatile private var cacheLoaded = false

    fun observeRules(): Flow<List<FocusRuleEntity>> = db.ruleDao().observeAll()
    suspend fun setRule(pkg: String, label: String, mode: RuleMode) {
        db.ruleDao().upsert(FocusRuleEntity(pkg, label, mode.name, true))
        ruleCache[pkg] = mode
    }
    suspend fun removeRule(pkg: String) { db.ruleDao().delete(pkg); ruleCache.remove(pkg) }
    suspend fun ruleFor(pkg: String): RuleMode? {
        if (!cacheLoaded) reloadRules()
        return ruleCache[pkg]
    }
    suspend fun reloadRules() {
        val rules = db.ruleDao().enabled()
        ruleCache.clear()
        rules.forEach { r -> runCatching { RuleMode.valueOf(r.mode) }.getOrNull()?.let { ruleCache[r.packageName] = it } }
        cacheLoaded = true
    }
    suspend fun hasAnyRules(): Boolean { if (!cacheLoaded) reloadRules(); return ruleCache.values.any { it != RuleMode.ALLOW } }

    // Sessions
    suspend fun insertSession(s: FocusSessionEntity): Long = db.sessionDao().insert(s)
    suspend fun updateSession(s: FocusSessionEntity) = db.sessionDao().update(s)
    suspend fun session(id: Long) = db.sessionDao().byId(id)
    fun observeRecentSessions(limit: Int = 30) = db.sessionDao().observeRecent(limit)
    fun observeSessionsSince(from: Long) = db.sessionDao().observeSince(from)
    suspend fun incrementBlocked(sessionId: Long) = db.sessionDao().incrementBlocked(sessionId)

    // Decisions (no raw titles stored)
    suspend fun logDecision(sourceApp: String, r: ClassificationResult): Long {
        db.decisionDao().prune(clock.now() - 30L * 86_400_000L)
        return db.decisionDao().insert(
            ClassificationDecisionEntity(
                timestamp = clock.now(), sourceApp = sourceApp, label = r.verdict.name, confidence = r.confidence,
                tier = r.tier, ruleVersion = r.ruleVersion, redactedReason = redact(r),
            )
        )
    }
    suspend fun markOverridden(decisionId: Long) = db.decisionDao().markOverridden(decisionId)
    fun observeDecisions(limit: Int = 50) = db.decisionDao().observeRecent(limit)

    /** Keep only the lexicon signals, never the full title. */
    private fun redact(r: ClassificationResult): String =
        (r.syllabusMatches + r.studySignals + r.distractionSignals).take(4).joinToString(", ").ifEmpty { "no signals" }
}
