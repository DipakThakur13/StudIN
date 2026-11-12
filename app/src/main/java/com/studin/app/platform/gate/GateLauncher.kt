package com.studin.app.platform.gate

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.studin.app.R
import com.studin.app.platform.notifications.Notifications

/** Opens the break gate over a distracting app, or falls back to a notification. */
object GateLauncher {
    const val EXTRA_PACKAGE = "gate.package"
    const val EXTRA_LABEL = "gate.label"
    const val EXTRA_REASON = "gate.reason"
    const val EXTRA_DECISION = "gate.decision"

    fun intent(context: Context, pkg: String, label: String, reason: String, decisionId: Long): Intent =
        Intent(context, GateActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra(EXTRA_PACKAGE, pkg).putExtra(EXTRA_LABEL, label)
            .putExtra(EXTRA_REASON, reason).putExtra(EXTRA_DECISION, decisionId)

    fun appLabel(context: Context, pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    /**
     * @param fromAccessibility accessibility services may start activities from the
     * background; other callers need the overlay permission on Android 10+.
     */
    fun launch(context: Context, pkg: String, reason: String, decisionId: Long = -1L, fromAccessibility: Boolean = false) {
        val label = appLabel(context, pkg)
        val i = intent(context, pkg, label, reason, decisionId)
        val canStart = fromAccessibility || Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context)
        if (canStart && runCatching { context.startActivity(i) }.isSuccess) return
        notifyFallback(context, i, label, reason)
    }

    fun notifyFallback(context: Context, gateIntent: Intent, label: String, reason: String) {
        val pi = PendingIntent.getActivity(context, 7, gateIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, Notifications.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_studin)
            .setContentTitle("$label is paused during focus")
            .setContentText("$reason · tap for a break option")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        notify(context, Notifications.ID_BLOCKED, n)
    }

    /** At most one gentle nudge for "maybe distracting" content (caller throttles). */
    fun notifyGentle(context: Context, label: String, reason: String) {
        val n = NotificationCompat.Builder(context, Notifications.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_studin)
            .setContentTitle("Still on track?")
            .setContentText("$label: $reason")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .build()
        notify(context, Notifications.ID_BLOCKED + 1, n)
    }

    private fun notify(context: Context, id: Int, n: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }
}
