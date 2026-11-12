package com.studin.app.platform.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studin.app.R
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.AppClock
import com.studin.app.core.data.repo.FocusRepository
import com.studin.app.core.data.repo.ProfileRepository
import com.studin.app.core.data.repo.StudyRepository
import com.studin.app.ui.navigation.Routes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Gentle, once-a-day study nudge. Skipped when today's goal is already met. */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferences,
    private val profiles: ProfileRepository,
    private val study: StudyRepository,
    private val focus: FocusRepository,
    private val clock: AppClock,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = prefs.current()
        if (!settings.remindersEnabled || !settings.onboardingDone) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()

        val goal = profiles.get()?.dailyGoalMinutes ?: 60
        val todayStart = clock.startOfDay(clock.today())
        val focusedToday = (focus.observeSessionsSince(todayStart).first().sumOf { it.focusedMs } / 60_000L).toInt()
        if (focusedToday >= goal) return Result.success()
        val due = study.dueCount()

        val title = if (focusedToday == 0) "Ready for a focus block?" else "Nice start – ${goal - focusedToday} min to today's goal"
        val text = buildString {
            if (due > 0) append("$due cards due · ")
            append("today's goal $goal min")
        }
        val n = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_studin)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(Notifications.openApp(applicationContext, Routes.HOME, 7))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(Notifications.ID_REMINDER, n)
        } catch (_: SecurityException) {
        }
        return Result.success()
    }
}

object ReminderScheduler {
    private const val WORK = "studin-reminder"

    fun schedule(context: Context, hour: Int, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) { wm.cancelUniqueWork(WORK); return }
        val now = LocalDateTime.now()
        var next = now.withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }
}
