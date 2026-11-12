package com.studin.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.di.AppScope
import com.studin.app.platform.focus.FocusController
import com.studin.app.platform.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.studin.app.platform.notifications.Notifications
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StudinApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var focusController: FocusController
    @Inject lateinit var prefs: UserPreferences
    @Inject @AppScope lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        focusController.restore()
        // Keep the daily reminder registered (idempotent unique work).
        appScope.launch {
            val s = prefs.current()
            if (s.onboardingDone) ReminderScheduler.schedule(this@StudinApp, s.reminderHour, s.remindersEnabled)
        }
    }
}
