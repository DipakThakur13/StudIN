package com.studin.app.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINDialog
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINLinearProgress
import com.studin.app.core.designsystem.StudINPermissionExplanation
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.app.platform.permissions.PermissionChecker
import com.studin.app.platform.permissions.PermissionStatus
import com.studin.app.platform.permissions.StudinPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(val checker: PermissionChecker) : ViewModel() {
    private val _status = MutableStateFlow(checker.status())
    val status: StateFlow<List<PermissionStatus>> = _status.asStateFlow()
    fun refresh() { _status.value = checker.status() }
}

private fun StudinPermission.icon(): ImageVector = when (this) {
    StudinPermission.NOTIFICATIONS -> Icons.Rounded.Notifications
    StudinPermission.USAGE_ACCESS -> Icons.Rounded.DataUsage
    StudinPermission.ACCESSIBILITY -> Icons.Rounded.Accessibility
    StudinPermission.OVERLAY -> Icons.Rounded.Layers
    StudinPermission.BATTERY -> Icons.Rounded.BatteryChargingFull
}

/** What the student loses while a permission is off – shown so the denied state reads clearly. */
internal fun StudinPermission.whenOff(): String = when (this) {
    StudinPermission.NOTIFICATIONS -> "While off: the timer runs without a notification and reminders are silent."
    StudinPermission.USAGE_ACCESS -> "While off: StudIN can't see which app is open, so app rules only work through Focus Guard."
    StudinPermission.ACCESSIBILITY -> "While off: Smart mode can't read titles; Block rules still work if Usage access is on."
    StudinPermission.OVERLAY -> "While off: the break screen arrives as a notification instead of appearing over the app."
    StudinPermission.BATTERY -> "While off: some phones may pause the timer during long sessions."
}

/** Short summary line for the health card. */
internal fun permissionSummary(granted: Int, total: Int): String = when {
    total > 0 && granted == total -> "Everything is set up"
    granted >= 2 -> "Core features are ready"
    else -> "Timer, flashcards and AI work now"
}

@Composable
fun PermissionsScreen(onBack: () -> Unit, vm: PermissionsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val statuses by vm.status.collectAsStateWithLifecycle()
    var disclosure by rememberSaveable { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    fun open(p: StudinPermission) {
        val intent = vm.checker.settingsIntent(p) ?: return
        try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun grant(p: StudinPermission, granted: Boolean) {
        when {
            p == StudinPermission.ACCESSIBILITY && !granted -> disclosure = true
            p == StudinPermission.NOTIFICATIONS && !granted && Build.VERSION.SDK_INT >= 33 -> notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> open(p)
        }
    }

    StudINScaffold(title = "Permission health", subtitle = "All optional · all reversible", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().studinContentWidth(),
            contentPadding = screenContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "summary") {
                val granted = statuses.count { it.granted }
                val total = statuses.size
                StudINCard(
                    Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$granted of $total permissions allowed. ${permissionSummary(granted, total)}" },
                    containerColor = StudINTheme.colors.surfaceSecondary, border = null,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudINIconTile(Icons.Rounded.VerifiedUser, MaterialTheme.colorScheme.primary, 44.dp)
                        Spacer(Modifier.width(Spacing.sm))
                        Column(Modifier.weight(1f)) {
                            Text(permissionSummary(granted, total), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                            Text("$granted of $total enabled", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    StudINLinearProgress(if (total == 0) 0f else granted.toFloat() / total)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Everything is optional and reversible. App blocking needs Usage access or Focus Guard; the rest of StudIN works without any of them.",
                        style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                    )
                }
            }
            items(statuses, key = { it.permission.name }) { st ->
                val p = st.permission
                StudINPermissionExplanation(
                    icon = p.icon(), title = p.title, why = p.why, granted = st.granted,
                    actionLabel = if (st.granted) "Manage" else "Allow",
                    onAction = { grant(p, st.granted) },
                    footnote = if (st.granted) null else p.whenOff(),
                )
            }
            item(key = "revoke") {
                StudINCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudINIconTile(Icons.Rounded.VisibilityOff, StudINTheme.colors.textSecondary, 36.dp)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("How to revoke", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    listOf(
                        "Tap Manage on any item above – it opens the exact Android setting.",
                        "Focus Guard: Settings → Accessibility → StudIN Focus Guard → Off.",
                        "Usage access: Settings → Apps → Special access → Usage access → StudIN.",
                        "Uninstalling StudIN removes everything; nothing is kept elsewhere.",
                    ).forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }

    if (disclosure) {
        StudINDialog(
            title = "Turn on Focus Guard?", onDismiss = { disclosure = false },
            confirmLabel = "Agree & open settings", onConfirm = { disclosure = false; open(StudinPermission.ACCESSIBILITY) },
            dismissLabel = "Not now", icon = Icons.Rounded.Accessibility,
            text = {
                Column {
                    Text("Focus Guard uses Android's Accessibility API. Here is exactly what it does:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    listOf(
                        "Reads the name of the app in front of you, and – only in apps you set to Smart mode – the visible video or post titles.",
                        "Works only while a focus session you started is running.",
                        "Titles are classified on this phone. They are never uploaded, sold or stored.",
                        "It never taps, types or changes settings for you, and never blocks calls or Settings.",
                        "You can turn it off anytime in Settings → Accessibility.",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            },
        )
    }
}
