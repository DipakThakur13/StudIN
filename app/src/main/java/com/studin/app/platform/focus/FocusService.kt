package com.studin.app.platform.focus

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.studin.app.R
import com.studin.app.core.data.repo.FocusRepository
import com.studin.app.platform.gate.GateLauncher
import com.studin.app.platform.notifications.Notifications
import com.studin.app.platform.permissions.PermissionChecker
import com.studin.app.ui.navigation.Routes
import com.studin.domain.focus.FocusPolicyEngine
import com.studin.domain.focus.FocusSnapshot
import com.studin.domain.model.FocusState
import com.studin.domain.model.Verdict
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * User-visible foreground service for a running focus session: shows the countdown
 * and, as a fallback when Focus Guard (accessibility) is off, checks the foreground
 * app via Usage Access against the student's own rules.
 */
@AndroidEntryPoint
class FocusService : LifecycleService() {
    @Inject lateinit var controller: FocusController
    @Inject lateinit var focusRepo: FocusRepository
    @Inject lateinit var permissions: PermissionChecker

    private val policy = FocusPolicyEngine()
    private var monitor: Job? = null
    private var foreground = false
    private val lastGate = HashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        // Satisfy the startForegroundService() contract before anything can stop us.
        goForeground(controller.snapshot.value)
        lifecycleScope.launch {
            controller.snapshot
                .distinctUntilChanged { a, b -> a.state == b.state && a.sessionId == b.sessionId && a.accumulatedMs == b.accumulatedMs }
                .collect { s -> onSnapshot(s) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // startForeground must be called promptly after startForegroundService().
        goForeground(controller.snapshot.value)
        when (intent?.action) {
            ACTION_PAUSE -> controller.pause()
            ACTION_RESUME -> controller.resume()
            ACTION_STOP -> controller.abort("Stopped from notification")
        }
        return START_STICKY
    }

    private fun onSnapshot(s: FocusSnapshot) {
        if (!s.state.isRunning) {
            monitor?.cancel()
            if (foreground) ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foreground = false
            stopSelf()
            return
        }
        goForeground(s)
        if (s.state == FocusState.ACTIVE) startMonitor() else monitor?.cancel()
    }

    private fun goForeground(s: FocusSnapshot) {
        val n = buildNotification(s)
        if (foreground) {
            notifyUpdate(n)
            return
        }
        try {
            val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            ServiceCompat.startForeground(this, Notifications.ID_FOCUS, n, type)
            foreground = true
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (12+) etc. The timer still runs in
            // FocusController while the app process is alive – never crash the session.
            Log.w(TAG, "startForeground refused: ${e.javaClass.simpleName}")
            stopSelf()
        }
    }

    private fun notifyUpdate(n: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        runCatching { NotificationManagerCompat.from(this).notify(Notifications.ID_FOCUS, n) }
    }

    private fun buildNotification(s: FocusSnapshot): Notification {
        val now = SystemClock.elapsedRealtime()
        val remaining = s.remainingMs(now)
        val running = s.state == FocusState.ACTIVE
        val title = if (s.label.isNotBlank()) "Focusing: ${s.label}" else "Focus session"
        val b = NotificationCompat.Builder(this, Notifications.CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_stat_studin)
            .setContentTitle(title)
            .setContentText(if (running) "Stay with it – you've got this" else "Paused · ${(remaining + 59_999) / 60_000} min left")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(Notifications.openApp(this, Routes.FOCUS, 11))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (running) {
            b.setWhen(System.currentTimeMillis() + remaining).setShowWhen(true).setUsesChronometer(true)
            if (Build.VERSION.SDK_INT >= 24) b.setChronometerCountDown(true)
            b.addAction(0, "Pause", action(ACTION_PAUSE, 1))
        } else if (s.state == FocusState.PAUSED) {
            b.addAction(0, "Resume", action(ACTION_RESUME, 2))
        }
        b.addAction(0, "Stop", action(ACTION_STOP, 3))
        return b.build()
    }

    private fun action(a: String, rc: Int): PendingIntent =
        PendingIntent.getService(
            this, rc, Intent(this, FocusService::class.java).setAction(a),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Usage-access fallback when the accessibility Focus Guard is not enabled. */
    private fun startMonitor() {
        if (monitor?.isActive == true) return
        monitor = lifecycleScope.launch {
            while (isActive) {
                delay(1_500)
                if (!controller.isSessionActive) continue
                val pkg = withContext(Dispatchers.Default) {
                    if (permissions.isAccessibilityEnabled() || !permissions.hasUsageAccess()) null
                    else UsageStatsBridge.currentForegroundPackage(this@FocusService, 10_000)
                } ?: continue
                if (pkg == packageName) continue
                val rule = focusRepo.ruleFor(pkg)
                val decision = policy.decide(pkg, rule, sessionActive = true, passActive = controller.isPassActive())
                if (decision.verdict != Verdict.BLOCK) continue
                val now = SystemClock.elapsedRealtime()
                if (now - (lastGate[pkg] ?: 0L) < GATE_COOLDOWN_MS) continue
                lastGate[pkg] = now
                controller.recordBlockedAttempt()
                GateLauncher.launch(this@FocusService, pkg, decision.reason)
            }
        }
    }

    companion object {
        private const val TAG = "FocusService"
        private const val GATE_COOLDOWN_MS = 10_000L
        const val ACTION_PAUSE = "com.studin.focus.PAUSE"
        const val ACTION_RESUME = "com.studin.focus.RESUME"
        const val ACTION_STOP = "com.studin.focus.STOP"

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, FocusService::class.java)) }
                .onFailure { Log.w(TAG, "Could not start focus service: ${it.javaClass.simpleName}") }
        }
    }
}
