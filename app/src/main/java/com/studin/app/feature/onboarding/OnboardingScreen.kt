package com.studin.app.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EmergencyShare
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.core.designsystem.StudinLogo
import com.studin.domain.model.ExamType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val PAGES = 5

@Composable
fun OnboardingScreen(onFinished: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    val canAdvance = when (pager.currentPage) { 1 -> state.exam != null; else -> true }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            PageIndicator(pager.currentPage, PAGES, Modifier.weight(1f))
            if (pager.currentPage < PAGES - 1) {
                Text("${pager.currentPage + 1}/$PAGES", style = MaterialTheme.typography.labelMedium, color = Studin.extras.muted)
            }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            userScrollEnabled = canAdvance || pager.currentPage != 1,
            beyondViewportPageCount = 1,
        ) { page ->
            Box(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
                when (page) {
                    0 -> WelcomePage()
                    1 -> ExamPage(state.exam, vm::setExam)
                    2 -> GoalPage(state, vm::setName, vm::setExamDay, vm::setGoal)
                    3 -> PrivacyPage(state.streaksEnabled, vm::setStreaks)
                    else -> ReadyPage(state)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedVisibility(pager.currentPage > 0) {
                GhostButton("Back", { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, icon = Icons.AutoMirrored.Rounded.ArrowBack)
            }
            val last = pager.currentPage == PAGES - 1
            GradientButton(
                text = when {
                    last -> if (state.saving) "Setting up…" else "Start studying"
                    pager.currentPage == 0 -> "Get started"
                    pager.currentPage == 1 && state.exam == null -> "Pick your exam"
                    else -> "Continue"
                },
                onClick = {
                    if (last) vm.finish(onFinished)
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                enabled = canAdvance && !state.saving,
                icon = if (last) Icons.Rounded.RocketLaunch else Icons.AutoMirrored.Rounded.ArrowForward,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PageIndicator(current: Int, count: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 28.dp else 8.dp, spring(dampingRatio = 0.6f), label = "dot")
            val color by animateColorAsState(
                if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f), label = "dotc",
            )
            Box(Modifier.height(8.dp).width(width).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Studin.extras.muted)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun Staggered(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(80L * index); visible = true }
    AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 3 }) { content() }
}

@Composable
private fun WelcomePage() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        StudinLogo(size = 140.dp)
        Spacer(Modifier.height(12.dp))
        Text("StudIN", style = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp))
        Text("Focus deeper. Remember longer.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(28.dp))
        val bullets = listOf(
            Triple(Icons.Rounded.TrackChanges, "Syllabus-aware focus", "Study videos stay open. Distractions get a gentle gate."),
            Triple(Icons.Rounded.Quiz, "Active-recall breaks", "Earn a break by answering a few of your own flashcards."),
            Triple(Icons.Rounded.AutoAwesome, "On-device AI", "A private tutor that works offline. Nothing leaves your phone."),
        )
        bullets.forEachIndexed { i, (icon, title, body) ->
            Staggered(i + 1) {
                GlassCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(icon, listOf(StudinColors.Mint, StudinColors.Cyan, StudinColors.Star)[i], 44.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Text(body, style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamPage(selected: ExamType?, onSelect: (ExamType) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageTitle("What are you preparing for?", "We'll load a matching syllabus and a starter flashcard deck. All offline.")
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(ExamType.entries) { exam ->
                val isSel = exam == selected
                val elevation by animateDpAsState(if (isSel) 18.dp else 0.dp, label = "e")
                Surface(
                    onClick = { onSelect(exam) },
                    modifier = Modifier.fillMaxWidth().height(118.dp)
                        .shadow(elevation, RoundedCornerShape(22.dp), ambientColor = StudinColors.Mint, spotColor = StudinColors.Mint),
                    shape = RoundedCornerShape(22.dp),
                    color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Studin.extras.glass,
                    border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.primary else Studin.extras.glassBorder),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(exam.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2)
                            if (isSel) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(exam.tagline, style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalPage(
    state: OnboardingState,
    onName: (String) -> Unit,
    onExamDay: (Long?) -> Unit,
    onGoal: (Int) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle("Set your rhythm", "You can change all of this later in Settings.")
        OutlinedTextField(
            value = state.name, onValueChange = onName, label = { Text("Your name (optional)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(16.dp))
        GlassCard(Modifier.fillMaxWidth(), onClick = { showPicker = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.CalendarMonth, StudinColors.Cyan)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Exam date", style = MaterialTheme.typography.titleMedium)
                    val d = state.examDay
                    Text(
                        if (d == null) "Optional – tap to set" else {
                            val date = LocalDate.ofEpochDay(d)
                            "${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} · ${ChronoUnit.DAYS.between(LocalDate.now(), date)} days to go"
                        },
                        style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted,
                    )
                }
                if (state.examDay != null) TextButton(onClick = { onExamDay(null) }) { Text("Clear") }
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.Tune, StudinColors.Mint)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Daily focus goal", style = MaterialTheme.typography.titleMedium)
                    Text("≈ ${"%.1f".format(state.dailyGoalMinutes / 60f)} hrs/day", style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted)
                }
                Text("${state.dailyGoalMinutes} min", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = state.dailyGoalMinutes.toFloat(),
                onValueChange = { onGoal(((it / 15f).toInt() * 15).coerceIn(30, 480)) },
                valueRange = 30f..480f, steps = (480 - 30) / 15 - 1,
            )
            Text("Tip: start realistic. Consistency beats intensity.", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
        }
    }
    if (showPicker) ExamDatePicker(state.examDay, onDismiss = { showPicker = false }, onPicked = { onExamDay(it); showPicker = false })
}

@Composable
fun ExamDatePicker(current: Long?, onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
    val todayUtc = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = current?.let { LocalDate.ofEpochDay(it).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            override fun isSelectableYear(year: Int) = year >= LocalDate.now().year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) } ?: onDismiss()
            }) { Text("Set date") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(pickerState) }
}

@Composable
private fun PrivacyPage(streaks: Boolean, onStreaks: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageTitle("Our privacy promise", "StudIN is built to help you, not to watch you.")
        val items = listOf(
            Triple(Icons.Rounded.CloudOff, "Offline-first", "The app has no internet permission. Your data never leaves this phone."),
            Triple(Icons.Rounded.Psychology, "AI runs on-device", "Classification and the tutor run locally. Nothing is uploaded."),
            Triple(Icons.Rounded.Shield, "You control every permission", "Each one is optional, explained, and can be revoked anytime."),
            Triple(Icons.Rounded.EmergencyShare, "Emergency exit, always", "Calls, settings and an unconditional skip are never blocked."),
        )
        items.forEachIndexed { i, (icon, title, body) ->
            Staggered(i) {
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(icon, StudinColors.Mint, 42.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(body, style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.LocalFireDepartment, StudinColors.Star)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Study streaks", style = MaterialTheme.typography.titleMedium)
                    Text("Opt-in and forgiving – one free miss per week, no shaming.", style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted)
                }
                Switch(checked = streaks, onCheckedChange = onStreaks)
            }
        }
    }
}

@Composable
private fun ReadyPage(state: OnboardingState) {
    var notifGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < 33) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        StudinLogo(size = 96.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            "You're all set${if (state.name.isNotBlank()) ", ${state.name.trim()}" else ""}!",
            style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${state.exam?.displayName ?: "Your exam"} · ${state.dailyGoalMinutes} min/day",
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        GlassCard(Modifier.fillMaxWidth(), highlight = !notifGranted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.Notifications, StudinColors.Cyan)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Timer & reminders", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (notifGranted) "Notifications are on." else "Show your running timer and one gentle daily nudge.",
                        style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted,
                    )
                }
                if (notifGranted) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                else TextButton(onClick = { if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow") }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Rounded.Lock, StudinColors.Violet)
                Spacer(Modifier.width(14.dp))
                Text(
                    "Usage access and Focus Guard are optional. Turn them on later in Settings → Permission health when you want app blocking.",
                    style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted, modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Your plan is advisory – you're always in control.", style = MaterialTheme.typography.bodySmall,
            color = Studin.extras.muted, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.size(1.dp).background(Color.Transparent))
    }
}
