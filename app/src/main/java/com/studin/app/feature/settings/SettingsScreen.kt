package com.studin.app.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.BuildConfig
import com.studin.app.core.data.repo.Profile
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.Pill
import com.studin.app.core.designsystem.SectionHeader
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.core.designsystem.StudinLogo
import com.studin.app.core.designsystem.ThemeMode
import com.studin.app.feature.onboarding.ExamDatePicker
import com.studin.app.ui.navigation.Routes
import com.studin.domain.model.ExamType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(onNavigate: (String) -> Unit, onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editProfile by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::export) }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", "Everything stays on this phone", onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionHeader("Profile")
                GlassCard(Modifier.fillMaxWidth()) {
                    val p = profile
                    SettingRow(Icons.Rounded.School, StudinColors.Mint, p?.name ?: "Student",
                        "${p?.exam?.displayName ?: "—"} · ${p?.dailyGoalMinutes ?: 0} min/day", onClick = { editProfile = true }) {
                        Text("Edit", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                    SettingRow(Icons.Rounded.CalendarMonth, StudinColors.Cyan, "Exam date",
                        p?.examDay?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMM yyyy")) } ?: "Not set",
                        onClick = { editProfile = true })
                }
            }
            item {
                SectionHeader("Focus")
                GlassCard(Modifier.fillMaxWidth()) {
                    SliderRow(Icons.Rounded.Timer, StudinColors.Mint, "Default session", "${s.defaultFocusMinutes} min",
                        s.defaultFocusMinutes, 5..120, 5, onChange = vm::setDefaultFocus)
                    SliderRow(Icons.Rounded.Quiz, StudinColors.Violet, "Break-gate questions",
                        if (s.gateQuestions == 0) "Pause only" else "${s.gateQuestions}", s.gateQuestions, 0..5,
                        subtitle = "Answered before a break pass. Wrong answers carry no penalty.", onChange = vm::setGateQuestions)
                    SliderRow(Icons.Rounded.WatchLater, StudinColors.Star, "Break pass length", "${s.passMinutes} min",
                        s.passMinutes, 1..30, onChange = vm::setPassMinutes)
                    SwitchRow(Icons.Rounded.FilterAlt, StudinColors.Cyan, "Smart content filter",
                        "Classify visible titles in Smart-mode apps on-device", s.smartFilterEnabled, vm::setSmartFilter)
                    SwitchRow(Icons.Rounded.Vibration, StudinColors.Teal, "Haptics", null, s.hapticsEnabled, vm::setHaptics)
                }
            }
            item {
                SectionHeader("Appearance")
                GlassCard(Modifier.fillMaxWidth()) {
                    SettingRow(Icons.Rounded.DarkMode, StudinColors.Violet, "Theme", "Dark looks best with the StudIN glow")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { m ->
                            Pill(m.name.lowercase().replaceFirstChar { it.uppercase() }, s.themeMode == m, { vm.setTheme(m) })
                        }
                    }
                }
            }
            item {
                SectionHeader("Motivation")
                GlassCard(Modifier.fillMaxWidth()) {
                    SwitchRow(Icons.Rounded.LocalFireDepartment, StudinColors.Star, "Streaks",
                        "Opt-in, with a free miss each week", s.streaksEnabled, vm::setStreaks)
                    SwitchRow(Icons.Rounded.Notifications, StudinColors.Cyan, "Daily reminder",
                        "One gentle nudge, skipped when your goal is met", s.remindersEnabled, vm::setReminders)
                    if (s.remindersEnabled) {
                        SliderRow(Icons.Rounded.AlarmOn, StudinColors.Mint, "Reminder time", hourLabel(s.reminderHour),
                            s.reminderHour, 5..23, onChange = vm::setReminderHour)
                    }
                }
            }
            item {
                SectionHeader("Privacy & data")
                GlassCard(Modifier.fillMaxWidth()) {
                    SettingRow(Icons.Rounded.HealthAndSafety, StudinColors.Mint, "Permission health", "See, grant or revoke every permission",
                        onClick = { onNavigate(Routes.PERMISSIONS) })
                    SettingRow(Icons.Rounded.Policy, StudinColors.Cyan, "Privacy policy", "What we store (only on your phone)",
                        onClick = { onNavigate(Routes.PRIVACY) })
                    SettingRow(Icons.Rounded.Download, StudinColors.Violet, "Export my data", "Everything as a JSON file you control",
                        onClick = { exporter.launch("studin-export-${LocalDate.now()}.json") })
                    SettingRow(Icons.Rounded.DeleteForever, StudinColors.Coral, "Delete all data", "Profile, cards, sessions, AI model – irreversible",
                        onClick = { confirmDelete = true })
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudinLogo(size = 56.dp, pulse = false)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("StudIN", style = MaterialTheme.typography.titleLarge)
                            Text("Version ${BuildConfig.VERSION_NAME} · offline-first", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                            Text("Focus deeper. Remember longer.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.navigationBarsPadding())
    }

    if (editProfile) {
        ProfileDialog(profile, onDismiss = { editProfile = false }, onSave = { n, e, d, g -> vm.saveProfile(n, e, d, g); editProfile = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { androidx.compose.material3.Icon(Icons.Rounded.Warning, null, tint = StudinColors.Coral) },
            title = { Text("Delete everything?") },
            text = { Text("This permanently removes your profile, syllabus, flashcards, sessions, insights and the imported AI model from this phone. Export first if you want a copy.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteAll() }) { Text("Delete", color = StudinColors.Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun hourLabel(h: Int): String = when {
    h == 0 -> "12 AM"; h < 12 -> "$h AM"; h == 12 -> "12 PM"; else -> "${h - 12} PM"
}

@Composable
private fun ProfileDialog(current: Profile?, onDismiss: () -> Unit, onSave: (String, ExamType, Long?, Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf(current?.name.orEmpty()) }
    var exam by rememberSaveable { mutableStateOf(current?.exam ?: ExamType.JEE) }
    var examDay by rememberSaveable { mutableStateOf(current?.examDay) }
    var goal by rememberSaveable { mutableIntStateOf(current?.dailyGoalMinutes ?: 120) }
    var pickDate by remember { mutableStateOf(false) }
    val examChanged = current != null && exam != current.exam

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.height(12.dp))
                Text("Exam", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExamType.entries.forEach { e -> Pill(e.displayName, exam == e, { exam = e }) }
                }
                if (examChanged) {
                    Spacer(Modifier.height(8.dp))
                    Text("Changing your exam replaces your syllabus topics (and their mastery) and adds a new starter deck. Your existing flashcards stay.",
                        style = MaterialTheme.typography.bodySmall, color = StudinColors.Star)
                }
                Spacer(Modifier.height(12.dp))
                SettingRow(Icons.Rounded.CalendarMonth, StudinColors.Cyan, "Exam date",
                    examDay?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMM yyyy")) } ?: "Not set",
                    onClick = { pickDate = true }) {
                    if (examDay != null) TextButton(onClick = { examDay = null }) { Text("Clear") } else androidx.compose.material3.Icon(Icons.Rounded.Edit, null)
                }
                SliderRow(Icons.Rounded.Timer, StudinColors.Mint, "Daily goal", "$goal min", goal, 30..480, 15, onChange = { goal = it })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, exam, examDay, goal) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (pickDate) ExamDatePicker(examDay, onDismiss = { pickDate = false }, onPicked = { examDay = it; pickDate = false })
}

