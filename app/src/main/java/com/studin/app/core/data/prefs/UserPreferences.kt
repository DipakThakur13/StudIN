package com.studin.app.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.studin.app.core.designsystem.ThemeMode
import com.studin.domain.focus.FocusSnapshot
import com.studin.domain.model.FocusState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studin_prefs")

data class AppSettings(
    val onboardingDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val streaksEnabled: Boolean = true,
    /** Questions asked at the break gate (0–5). 0 = just a mindful pause. */
    val gateQuestions: Int = 2,
    /** Minutes of access granted after passing / skipping the gate. */
    val passMinutes: Int = 5,
    val passUntil: Long = 0L,
    val smartFilterEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 18,
    val defaultFocusMinutes: Int = 25,
    val hapticsEnabled: Boolean = true,
    val aiModelFile: String? = null,
    val aiModelSha256: String? = null,
    val aiUseLlm: Boolean = true,
    val allowKeywords: Set<String> = emptySet(),
    val blockKeywords: Set<String> = emptySet(),
)

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val store = context.dataStore

    private object K {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val theme = stringPreferencesKey("theme_mode")
        val streaks = booleanPreferencesKey("streaks_enabled")
        val gateQ = intPreferencesKey("gate_questions")
        val passMin = intPreferencesKey("pass_minutes")
        val passUntil = longPreferencesKey("pass_until")
        val smart = booleanPreferencesKey("smart_filter")
        val reminders = booleanPreferencesKey("reminders")
        val reminderHour = intPreferencesKey("reminder_hour")
        val focusMin = intPreferencesKey("default_focus_minutes")
        val haptics = booleanPreferencesKey("haptics")
        val modelFile = stringPreferencesKey("ai_model_file")
        val modelSha = stringPreferencesKey("ai_model_sha")
        val useLlm = booleanPreferencesKey("ai_use_llm")
        val allowKw = stringSetPreferencesKey("allow_keywords")
        val blockKw = stringSetPreferencesKey("block_keywords")
        // Focus snapshot (persisted for process death / reboot reconciliation)
        val fState = stringPreferencesKey("f_state")
        val fId = longPreferencesKey("f_id")
        val fPlanned = longPreferencesKey("f_planned")
        val fAcc = longPreferencesKey("f_acc")
        val fResumed = longPreferencesKey("f_resumed")
        val fStartWall = longPreferencesKey("f_start_wall")
        val fPauses = intPreferencesKey("f_pauses")
        val fTopic = longPreferencesKey("f_topic")
        val fLabel = stringPreferencesKey("f_label")
        val fBoot = intPreferencesKey("f_boot")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            onboardingDone = p[K.onboarding] ?: false,
            themeMode = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            streaksEnabled = p[K.streaks] ?: true,
            gateQuestions = (p[K.gateQ] ?: 2).coerceIn(0, 5),
            passMinutes = (p[K.passMin] ?: 5).coerceIn(1, 30),
            passUntil = p[K.passUntil] ?: 0L,
            smartFilterEnabled = p[K.smart] ?: true,
            remindersEnabled = p[K.reminders] ?: true,
            reminderHour = p[K.reminderHour] ?: 18,
            defaultFocusMinutes = p[K.focusMin] ?: 25,
            hapticsEnabled = p[K.haptics] ?: true,
            aiModelFile = p[K.modelFile],
            aiModelSha256 = p[K.modelSha],
            aiUseLlm = p[K.useLlm] ?: true,
            allowKeywords = p[K.allowKw] ?: emptySet(),
            blockKeywords = p[K.blockKw] ?: emptySet(),
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setOnboardingDone(v: Boolean) = store.edit { it[K.onboarding] = v }
    suspend fun setTheme(v: ThemeMode) = store.edit { it[K.theme] = v.name }
    suspend fun setStreaks(v: Boolean) = store.edit { it[K.streaks] = v }
    suspend fun setGateQuestions(v: Int) = store.edit { it[K.gateQ] = v.coerceIn(0, 5) }
    suspend fun setPassMinutes(v: Int) = store.edit { it[K.passMin] = v.coerceIn(1, 30) }
    suspend fun setPassUntil(v: Long) = store.edit { it[K.passUntil] = v }
    suspend fun setSmartFilter(v: Boolean) = store.edit { it[K.smart] = v }
    suspend fun setReminders(v: Boolean) = store.edit { it[K.reminders] = v }
    suspend fun setReminderHour(v: Int) = store.edit { it[K.reminderHour] = v.coerceIn(0, 23) }
    suspend fun setDefaultFocusMinutes(v: Int) = store.edit { it[K.focusMin] = v.coerceIn(5, 180) }
    suspend fun setHaptics(v: Boolean) = store.edit { it[K.haptics] = v }
    suspend fun setUseLlm(v: Boolean) = store.edit { it[K.useLlm] = v }
    suspend fun setModel(file: String?, sha: String?) = store.edit {
        if (file == null) { it.remove(K.modelFile); it.remove(K.modelSha) } else { it[K.modelFile] = file; it[K.modelSha] = sha ?: "" }
    }
    suspend fun setAllowKeywords(v: Set<String>) = store.edit { it[K.allowKw] = v.take(200).toSet() }
    suspend fun setBlockKeywords(v: Set<String>) = store.edit { it[K.blockKw] = v.take(200).toSet() }

    // ---- Focus snapshot persistence
    suspend fun saveSnapshot(s: FocusSnapshot, bootCount: Int) = store.edit {
        it[K.fState] = s.state.name; it[K.fId] = s.sessionId; it[K.fPlanned] = s.plannedMs; it[K.fAcc] = s.accumulatedMs
        it[K.fResumed] = s.resumedAtElapsed; it[K.fStartWall] = s.startWallTime; it[K.fPauses] = s.pauseCount
        if (s.topicId != null) it[K.fTopic] = s.topicId!! else it.remove(K.fTopic)
        it[K.fLabel] = s.label; it[K.fBoot] = bootCount
    }

    /** @return persisted snapshot and the boot count it was saved under. */
    suspend fun loadSnapshot(): Pair<FocusSnapshot, Int> {
        val p = store.data.first()
        val state = p[K.fState]?.let { runCatching { FocusState.valueOf(it) }.getOrNull() } ?: FocusState.IDLE
        return FocusSnapshot(
            state = state, sessionId = p[K.fId] ?: 0, plannedMs = p[K.fPlanned] ?: 0, accumulatedMs = p[K.fAcc] ?: 0,
            resumedAtElapsed = p[K.fResumed] ?: -1, startWallTime = p[K.fStartWall] ?: 0, pauseCount = p[K.fPauses] ?: 0,
            topicId = p[K.fTopic], label = p[K.fLabel] ?: "",
        ) to (p[K.fBoot] ?: -1)
    }

    suspend fun clearAll() = store.edit { it.clear() }
}
