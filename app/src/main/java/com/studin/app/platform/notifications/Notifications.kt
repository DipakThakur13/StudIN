package com.studin.app.platform.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.studin.app.MainActivity
import com.studin.app.R

object Notifications {
    const val CHANNEL_FOCUS = "focus_session"
    const val CHANNEL_REMINDERS = "study_reminders"
    const val CHANNEL_ALERTS = "focus_alerts"
    const val CHANNEL_DOWNLOADS = "model_download"

    const val ID_FOCUS = 1001
    const val ID_REMINDER = 1002
    const val ID_BLOCKED = 1003
    const val ID_COMPLETE = 1004
    const val ID_MODEL_DOWNLOAD = 1005

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(listOf(
            NotificationChannel(CHANNEL_FOCUS, context.getString(R.string.channel_focus), NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the running focus timer"; setShowBadge(false)
            },
            NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Gentle daily study reminders"
            },
            NotificationChannel(CHANNEL_DOWNLOADS, "AI Tutor download", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progress while the on-device AI Tutor model downloads"; setShowBadge(false)
            },
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shown when a blocked app is opened during focus"
            },
        ))
    }

    /** PendingIntent that opens the app at [route] (see Routes). */
    fun openApp(context: Context, route: String? = null, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply { if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route) }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
