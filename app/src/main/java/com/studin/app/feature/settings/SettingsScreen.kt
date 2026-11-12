package com.studin.app.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.BuildConfig
import com.studin.app.core.data.repo.Profile
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINDialog
import com.studin.app.core.designsystem.StudINFilterChip
import com.studin.app.core.designsystem.StudINListItem
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINTextButton
import com.studin.app.core.designsystem.StudINTextField
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.StudINWordmark
import com.studin.app.core.designsystem.ThemeMode
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.app.feature.onboarding.ExamDatePicker
import com.studin.app.ui.navigation.Routes
import com.studin.domain.model.ExamType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** "6 PM" style label for a 0–23 hour. */
internal fun hourLabel(h: Int): String = when {
    h == 0 -> "12 AM"; h < 12 -> "$h AM"; h == 12 -> "12 PM"; else -> "${h - 12} PM"
}

internal fun themeLabel(m: ThemeMode): String = when (m) {
    ThemeMode.SYSTEM -> "Follow system"; ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark"
}

internal fun formatExamDay(day: Long?): String =
    day?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMM yyyy")) } ?: "Not set"

@Composable
fun SettingsScreen(onNavigate: (String) -> Unit, onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editProfile by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::export) }
    val c = StudINTheme.colors

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    StudINScaffold(title = "Settings", subtitle = "Everything stays on this phone", onBack = onBack, snackbarHostState = snackbar) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().studinContentWidth(),
            contentPadding = screenContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            item(key = "profile") {
                val p = profile
                SettingsSection("Profile") {
                    NavRow(Icons.Rounded.Person, p?.name?.ifBlank { "Student" } ?: "Student",
                        "${p?.exam?.displayName ?: "No exam"} · ${p?.dailyGoalMinutes ?: 0} min daily goal", value = "Edit") { editProfile = true }
                    RowDivider()
                    NavRow(Icons.Rounded.CalendarMonth, "Exam date", formatExamDay(p?.examDay)) { editProfile = true }
                }
            }
            item(key = "focus") {
                SettingsSection("Focus") {
                    SliderRow(Icons.Rounded.Timer, "Default session", "${s.defaultFocusMinutes} min", s.defaultFocusMinutes, 5..120, 5, onChange = vm::setDefaultFocus)
                    RowDivider()
                    SliderRow(
                        Icons.Rounded.Quiz, "Break-gate questions", if (s.gateQuestions == 0) "Pause only" else "${s.gateQuestions}",
                        s.gateQuestions, 0..5, supporting = "Answered before a break. Wrong answers carry no penalty.",
                        tint = c.ai, onChange = vm::setGateQuestions,
                    )
                    RowDivider()
                    SliderRow(Icons.Rounded.WatchLater, "Break pass length", "${s.passMinutes} min", s.passMinutes, 1..30, tint = c.accent, onChange = vm::setPassMinutes)
                    RowDivider()
                    SwitchRow(Icons.Rounded.FilterAlt, "Smart content filter", "Classify visible titles in Smart-mode apps, on this phone",
                        s.smartFilterEnabled, vm::setSmartFilter)
                    RowDivider()
                    SwitchRow(Icons.Rounded.Vibration, "Haptics", "Gentle vibration on review grades", s.hapticsEnabled, vm::setHaptics)
                }
            }
            item(key = "appearance") {
                SettingsSection("Appearance") {
                    StudINListItem("Theme", supporting = "Warm light, calm dark, or match your phone", icon = Icons.Rounded.DarkMode)
                    FlowRow(
                        Modifier.fillMaxWidth().padding(start = 64.dp, end = Spacing.card, bottom = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        ThemeMode.entries.forEach { m ->
                            StudINFilterChip(
                                themeLabel(m), s.themeMode == m, { vm.setTheme(m) },
                                icon = when (m) { ThemeMode.SYSTEM -> Icons.Rounded.SettingsSuggest; ThemeMode.LIGHT -> Icons.Rounded.LightMode; ThemeMode.DARK -> Icons.Rounded.DarkMode },
                            )
                        }
                    }
                }
            }
            item(key = "motivation") {
                SettingsSection("Motivation") {
                    SwitchRow(Icons.Rounded.LocalFireDepartment, "Streaks", "Opt-in, with one free miss each week", s.streaksEnabled, vm::setStreaks, tint = c.accent)
                    RowDivider()
                    SwitchRow(Icons.Rounded.Notifications, "Daily reminder", "One gentle nudge, skipped when your goal is met", s.remindersEnabled, vm::setReminders)
                    if (s.remindersEnabled) {
                        RowDivider()
                        SliderRow(Icons.Rounded.AlarmOn, "Reminder time", hourLabel(s.reminderHour), s.reminderHour, 5..23, onChange = vm::setReminderHour)
                    }
                }
            }
            item(key = "privacy") {
                SettingsSection("Privacy & data") {
                    NavRow(Icons.Rounded.HealthAndSafety, "Permission health", "See, allow or revoke every permission") { onNavigate(Routes.PERMISSIONS) }
                    RowDivider()
                    NavRow(Icons.Rounded.Policy, "Privacy policy", "What StudIN stores – only on your phone") { onNavigate(Routes.PRIVACY) }
                    RowDivider()
                    NavRow(Icons.Rounded.Download, "Export my data", "Everything as a JSON file you control") {
                        exporter.launch("studin-export-${LocalDate.now()}.json")
                    }
                    RowDivider()
                    NavRow(Icons.Rounded.DeleteForever, "Delete all data", "Profile, cards, sessions and AI model – permanent",
                        tint = MaterialTheme.colorScheme.error) { confirmDelete = true }
                }
            }
            item(key = "about") {
                StudINCard(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                    contentDescription = "StudIN version ${BuildConfig.VERSION_NAME}. Offline first."
                }) {
                    StudINWordmark(markSize = 36.dp)
                    Spacer(Modifier.height(Spacing.xs))
                    Text("Version ${BuildConfig.VERSION_NAME} · offline-first · nothing uploaded",
                        style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
            }
        }
    }

    if (editProfile) {
        ProfileDialog(profile, onDismiss = { editProfile = false }, onSave = { n, e, d, g -> vm.saveProfile(n, e, d, g); editProfile = false })
    }
    if (confirmDelete) {
        StudINDialog(
            title = "Delete everything?", onDismiss = { confirmDelete = false },
            confirmLabel = "Delete permanently", onConfirm = { confirmDelete = false; vm.deleteAll() },
            destructive = true, icon = Icons.Rounded.Warning,
            text = {
                Text("This permanently removes your profile, syllabus, flashcards, sessions, insights and the imported AI model from this phone. Export first if you want a copy.")
            },
        )
    }
}

@Composable
private fun ProfileDialog(current: Profile?, onDismiss: () -> Unit, onSave: (String, ExamType, Long?, Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf(current?.name.orEmpty()) }
    var exam by rememberSaveable { mutableStateOf(current?.exam ?: ExamType.JEE) }
    var examDay by rememberSaveable { mutableStateOf(current?.examDay) }
    var goal by rememberSaveable { mutableIntStateOf(current?.dailyGoalMinutes ?: 120) }
    var pickDate by remember { mutableStateOf(false) }
    val examChanged = current != null && exam != current.exam
    val c = StudINTheme.colors

    StudINDialog(
        title = "Edit profile", onDismiss = onDismiss, confirmLabel = "Save", onConfirm = { onSave(name, exam, examDay, goal) },
        icon = Icons.Rounded.School,
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                StudINTextField(name, { name = it.take(40) }, label = "Name")
                Spacer(Modifier.height(Spacing.sm))
                Text("Exam", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Spacing.xxs + 2.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs - 2.dp), verticalArrangement = Arrangement.spacedBy(Spacing.xs - 2.dp)) {
                    ExamType.entries.forEach { e -> StudINFilterChip(e.displayName, exam == e, { exam = e }) }
                }
                if (examChanged) {
                    Spacer(Modifier.height(Spacing.xs))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Warning, contentDescription = "Warning", tint = c.warning, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            "Changing your exam replaces your syllabus topics (and their mastery) and adds a new starter deck. Your existing flashcards stay.",
                            style = MaterialTheme.typography.bodySmall, color = c.warning,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Exam date", style = MaterialTheme.typography.labelLarge)
                        Text(formatExamDay(examDay), style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                    }
                    if (examDay != null) StudINTextButton("Clear", { examDay = null })
                    StudINTextButton(if (examDay == null) "Set" else "Change", { pickDate = true })
                }
                Spacer(Modifier.height(Spacing.xs))
                Row {
                    Text("Daily goal", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("$goal min", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                androidx.compose.material3.Slider(
                    value = goal.toFloat(), onValueChange = { goal = (((it - 30) / 15).toInt() * 15 + 30).coerceIn(30, 480) },
                    valueRange = 30f..480f,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Daily goal, $goal minutes" },
                )
            }
        },
    )
    if (pickDate) ExamDatePicker(examDay, { pickDate = false }, { examDay = it; pickDate = false })
}
