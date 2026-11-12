package com.studin.app.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.MainActivity
import com.studin.app.ai.OnDeviceLlm
import com.studin.app.core.data.db.StudinDatabase
import com.studin.app.core.data.prefs.AppSettings
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.AppClock
import com.studin.app.core.data.repo.Profile
import com.studin.app.core.data.repo.ProfileRepository
import com.studin.app.core.data.repo.exam
import com.studin.app.core.designsystem.ThemeMode
import com.studin.app.platform.focus.FocusController
import com.studin.app.platform.notifications.ReminderScheduler
import com.studin.domain.model.ExamType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
    private val profiles: ProfileRepository,
    private val db: StudinDatabase,
    private val llm: OnDeviceLlm,
    private val focus: FocusController,
    private val clock: AppClock,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val profile: StateFlow<Profile?> = profiles.observe()
        .map { p -> p?.let { Profile(it.name, it.exam(), it.examDay, it.dailyGoalMinutes) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun consumeMessage() { _message.value = null }

    fun saveProfile(name: String, exam: ExamType, examDay: Long?, goal: Int) = viewModelScope.launch {
        profiles.save(Profile(name.ifBlank { "Student" }, exam, examDay, goal))
        _message.value = "Profile saved"
    }

    fun setTheme(v: ThemeMode) = viewModelScope.launch { prefs.setTheme(v) }
    fun setDefaultFocus(v: Int) = viewModelScope.launch { prefs.setDefaultFocusMinutes(v) }
    fun setGateQuestions(v: Int) = viewModelScope.launch { prefs.setGateQuestions(v) }
    fun setPassMinutes(v: Int) = viewModelScope.launch { prefs.setPassMinutes(v) }
    fun setSmartFilter(v: Boolean) = viewModelScope.launch { prefs.setSmartFilter(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { prefs.setHaptics(v) }
    fun setStreaks(v: Boolean) = viewModelScope.launch { prefs.setStreaks(v) }

    fun setReminders(enabled: Boolean) = viewModelScope.launch {
        prefs.setReminders(enabled)
        ReminderScheduler.schedule(context, prefs.current().reminderHour, enabled)
    }

    fun setReminderHour(hour: Int) = viewModelScope.launch {
        prefs.setReminderHour(hour)
        val s = prefs.current()
        ReminderScheduler.schedule(context, hour, s.remindersEnabled)
    }

    /** Export every table the student owns as a single JSON document. */
    fun export(uri: Uri) = viewModelScope.launch {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val root = JSONObject()
                root.put("app", "StudIN").put("format", 1).put("exportedAt", clock.now())
                db.profileDao().get()?.let {
                    root.put("profile", JSONObject().put("name", it.name).put("examType", it.examType).put("examDay", it.examDay ?: JSONObject.NULL)
                        .put("dailyGoalMinutes", it.dailyGoalMinutes).put("createdAt", it.createdAt))
                }
                root.put("topics", JSONArray().apply {
                    db.topicDao().all().forEach {
                        put(JSONObject().put("id", it.id).put("subject", it.subject).put("title", it.title).put("aliases", it.aliases)
                            .put("priority", it.priority).put("mastery", it.mastery).put("minutesStudied", it.minutesStudied)
                            .put("lastStudiedDay", it.lastStudiedDay ?: JSONObject.NULL).put("completed", it.completed))
                    }
                })
                root.put("focusRules", JSONArray().apply {
                    db.ruleDao().enabled().forEach { put(JSONObject().put("package", it.packageName).put("label", it.appLabel).put("mode", it.mode)) }
                })
                root.put("sessions", JSONArray().apply {
                    db.sessionDao().all().forEach {
                        put(JSONObject().put("id", it.id).put("label", it.label).put("topicId", it.topicId ?: JSONObject.NULL)
                            .put("plannedMinutes", it.plannedMinutes).put("start", it.startWallTime).put("end", it.endWallTime ?: JSONObject.NULL)
                            .put("status", it.status).put("focusedMs", it.focusedMs).put("pauses", it.pauses)
                            .put("blockedAttempts", it.blockedAttempts).put("reason", it.interruptedReason ?: JSONObject.NULL))
                    }
                })
                val cards = db.cardDao().all().groupBy { it.deckId }
                root.put("decks", JSONArray().apply {
                    db.deckDao().all().forEach { d ->
                        put(JSONObject().put("id", d.id).put("title", d.title).put("subject", d.subject).put("source", d.source).put("createdAt", d.createdAt)
                            .put("cards", JSONArray().apply {
                                cards[d.id].orEmpty().forEach { c ->
                                    put(JSONObject().put("id", c.id).put("front", c.front).put("back", c.back).put("tags", c.tags).put("dueAt", c.dueAt)
                                        .put("intervalDays", c.intervalDays).put("ease", c.easeFactor).put("repetitions", c.repetitions).put("lapses", c.lapses))
                                }
                            }))
                    }
                })
                root.put("reviews", JSONArray().apply {
                    db.reviewDao().all().forEach { put(JSONObject().put("cardId", it.cardId).put("at", it.reviewedAt).put("result", it.result).put("correct", it.correct).put("context", it.context)) }
                })
                root.put("classificationDecisions", JSONArray().apply {
                    db.decisionDao().since(0).forEach {
                        put(JSONObject().put("at", it.timestamp).put("app", it.sourceApp).put("label", it.label).put("confidence", it.confidence)
                            .put("tier", it.tier).put("ruleVersion", it.ruleVersion).put("signals", it.redactedReason).put("overridden", it.overridden))
                    }
                })
                context.contentResolver.openOutputStream(uri, "wt").use { out ->
                    requireNotNull(out) { "Couldn't open file" }
                    out.write(root.toString(2).toByteArray(Charsets.UTF_8))
                }
            }
        }
        _message.value = result.fold({ "Exported your data" }, { "Export failed: ${it.message}" })
    }

    /** Irreversibly delete everything, then restart into onboarding. */
    fun deleteAll() = viewModelScope.launch {
        if (focus.snapshot.value.state.isRunning) { focus.abort("Data deleted"); delay(300) }
        withContext(Dispatchers.IO) {
            db.clearAllTables()
            llm.deleteModel()
            prefs.clearAll()
        }
        ReminderScheduler.cancel(context)
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        delay(250)
        // Restart the process so every in-memory cache (timer, classifier, model) starts fresh.
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
