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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import com.studin.app.core.designsystem.Badge
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.ProgressRing
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.platform.permissions.PermissionChecker
import com.studin.app.platform.permissions.PermissionStatus
import com.studin.app.platform.permissions.StudinPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@HiltViewModel
class PermissionsViewModel @Inject constructor(val checker: PermissionChecker) : ViewModel() {
    private val _status = MutableStateFlow(checker.status())
    val status: StateFlow<List<PermissionStatus>> = _status.asStateFlow()
    fun refresh() { _status.value = checker.status() }
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

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Permission health", "All optional · all reversible", onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val granted = statuses.count { it.granted }
                GlassCard(Modifier.fillMaxWidth(), highlight = granted == statuses.size) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(granted.toFloat() / statuses.size.coerceAtLeast(1), size = 92.dp, stroke = 10.dp) {
                            Text("$granted/${statuses.size}", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                when { granted == statuses.size -> "Everything's set"; granted >= 2 -> "Core features ready"; else -> "Timer & flashcards work now" },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text("Grant only what you need. App blocking needs Usage access or Focus Guard; the rest of StudIN works without them.",
                                style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                        }
                    }
                }
            }
            items(statuses, key = { it.permission.name }) { st -> PermissionCard(st, onGrant = { grant(st.permission, st.granted) }) }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(Icons.Rounded.VisibilityOff, StudinColors.Violet, 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("How to revoke", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Tap Manage on any item above – it opens the exact Android setting.",
                        "Focus Guard: Settings → Accessibility → StudIN Focus Guard → Off.",
                        "Usage access: Settings → Apps → Special access → Usage access → StudIN.",
                        "Uninstalling StudIN removes everything; nothing is kept elsewhere.",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            }
        }
    }

    if (disclosure) {
        AlertDialog(
            onDismissRequest = { disclosure = false },
            icon = { Icon(Icons.Rounded.Accessibility, null, tint = StudinColors.Mint) },
            title = { Text("Enable Focus Guard?") },
            text = {
                Column {
                    Text("Focus Guard uses Android's Accessibility API. Before you turn it on, here is exactly what it does:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "Reads the name of the app in front of you, and – only in apps you set to Smart mode – the visible video/post titles.",
                        "Works only while a focus session you started is running.",
                        "Titles are classified on this phone. They are never uploaded, sold or stored.",
                        "It never taps, types or changes settings for you, and never blocks calls or Settings.",
                        "You can turn it off anytime in Settings → Accessibility.",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { disclosure = false; open(StudinPermission.ACCESSIBILITY) }) { Text("Agree & open settings") }
            },
            dismissButton = { TextButton(onClick = { disclosure = false }) { Text("Not now") } },
        )
    }
}

@Composable
private fun PermissionCard(st: PermissionStatus, onGrant: () -> Unit) {
    val p = st.permission
    val (icon, color) = when (p) {
        StudinPermission.NOTIFICATIONS -> Icons.Rounded.Notifications to StudinColors.Cyan
        StudinPermission.USAGE_ACCESS -> Icons.Rounded.DataUsage to StudinColors.Mint
        StudinPermission.ACCESSIBILITY -> Icons.Rounded.Accessibility to StudinColors.Violet
        StudinPermission.OVERLAY -> Icons.Rounded.Layers to StudinColors.Star
        StudinPermission.BATTERY -> Icons.Rounded.BatteryChargingFull to StudinColors.Teal
    }
    GlassCard(Modifier.fillMaxWidth(), highlight = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon, color, 44.dp)
            Spacer(Modifier.width(12.dp))
            Text(p.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Badge(if (st.granted) "Granted" else "Off", if (st.granted) StudinColors.Mint else Studin.extras.muted)
        }
        Spacer(Modifier.height(8.dp))
        Text(p.why, style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
        Spacer(Modifier.height(12.dp))
        if (st.granted) GhostButton("Manage", onGrant, Modifier.fillMaxWidth())
        else GradientButton("Grant", onGrant, Modifier.fillMaxWidth())
    }
}

