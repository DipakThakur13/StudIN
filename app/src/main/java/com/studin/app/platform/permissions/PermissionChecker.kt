package com.studin.app.platform.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.studin.app.platform.accessibility.StudinAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class StudinPermission(val title: String, val why: String, val required: Boolean) {
    NOTIFICATIONS("Notifications", "Shows your running timer and gentle study reminders.", false),
    USAGE_ACCESS("Usage access", "Lets StudIN see which app is open during a focus session and build your screen-time insights. Data stays on your phone.", false),
    ACCESSIBILITY("Focus Guard (Accessibility)", "Optional. Reads visible video titles in apps you set to Smart mode so study videos stay allowed and distractions are gated. Classified on-device, never uploaded.", false),
    OVERLAY("Display over other apps", "Lets the break screen appear on top of a blocked app. Without it StudIN falls back to a notification.", false),
    BATTERY("Unrestricted battery", "Prevents aggressive OEM battery savers from killing your timer mid-session.", false),
}

data class PermissionStatus(val permission: StudinPermission, val granted: Boolean)

@Singleton
class PermissionChecker @Inject constructor(@ApplicationContext private val context: Context) {

    fun status(): List<PermissionStatus> = StudinPermission.entries.map { PermissionStatus(it, isGranted(it)) }

    fun isGranted(p: StudinPermission): Boolean = when (p) {
        StudinPermission.NOTIFICATIONS -> Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        StudinPermission.USAGE_ACCESS -> hasUsageAccess()
        StudinPermission.ACCESSIBILITY -> isAccessibilityEnabled()
        StudinPermission.OVERLAY -> Settings.canDrawOverlays(context)
        StudinPermission.BATTERY -> (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)
    }

    fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        else @Suppress("DEPRECATION") ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(context, StudinAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        return splitter.any { ComponentName.unflattenFromString(it) == expected }
    }

    /** Settings screen the user must visit to grant/revoke [p] (null for runtime permissions). */
    fun settingsIntent(p: StudinPermission): Intent? = when (p) {
        StudinPermission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= 26)
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) else null
        StudinPermission.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        StudinPermission.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        StudinPermission.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        StudinPermission.BATTERY -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
