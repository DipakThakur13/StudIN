package com.studin.app.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EmergencyShare
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.R
import com.studin.app.core.designsystem.Motion
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINIconTile
import com.studin.app.core.designsystem.StudINIllustration
import com.studin.app.core.designsystem.StudINLinearProgress
import com.studin.app.core.designsystem.StudINListItem
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINShapes
import com.studin.app.core.designsystem.StudINTextButton
import com.studin.app.core.designsystem.StudINTextField
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.StudINWordmark
import com.studin.app.core.designsystem.motionDuration
import com.studin.domain.model.ExamType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** One illustrated slide that explains a part of the app. */
data class IntroSlide(@DrawableRes val image: Int, val eyebrow: String, val title: String, val body: String, val imageDescription: String)

val IntroSlides = listOf(
    IntroSlide(
        R.drawable.ill_welcome, "Welcome to StudIN", "Your calm study companion",
        "Focus sessions, smart revision and a private AI tutor in one place — built for JEE, NEET, UPSC, GATE, CAT and college students.",
        "A study desk with a phone showing a focus timer, a stack of books and a plant",
    ),
    IntroSlide(
        R.drawable.ill_focus, "Focus", "Protect your study time",
        "Start a timer and StudIN gently gates the apps you choose. Study videos stay open, and emergency calls are never blocked.",
        "A focus timer next to a shield, with distracting apps crossed out",
    ),
    IntroSlide(
        R.drawable.ill_recall, "Remember", "Learn it once, keep it for good",
        "Spaced-repetition flashcards and quick MCQs. Earn short breaks by answering a few of your own cards — no penalty for mistakes.",
        "Stacked flashcards and a multiple-choice question with the right answer ticked",
    ),
    IntroSlide(
        R.drawable.ill_ai_tutor, "AI Tutor", "A tutor that lives on your phone",
        "Turn notes or a photo of your notebook into flashcards, ask for explanations and get a daily plan. Everything is computed on your device.",
        "A friendly robot tutor wearing a graduation cap, with chat bubbles",
    ),
    IntroSlide(
        R.drawable.ill_insights, "Progress", "See progress you can trust",
        "Focus minutes, recall accuracy and consistency — honest numbers that show what's working, without guilt or shaming.",
        "A bar chart rising week by week, a streak flame and a consistency calendar",
    ),
)

private const val SETUP_STEPS = 3

@Composable
fun OnboardingScreen(onFinished: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    // Phase: intro carousel, then a 3-step setup. Saved so rotation keeps the position.
    var inSetup by rememberSaveable { mutableStateOf(false) }
    var step by rememberSaveable { mutableStateOf(0) }

    val enterMs = motionDuration(Motion.screen)
    val exitMs = motionDuration(Motion.small)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = inSetup,
            transitionSpec = { fadeIn(tween(enterMs)) togetherWith fadeOut(tween(exitMs)) },
            label = "onboardingPhase",
        ) { setup ->
            if (!setup) {
                IntroCarousel(onDone = { inSetup = true })
            } else {
                BackHandler { if (step > 0) step-- else inSetup = false }
                SetupFlow(
                    state = state, step = step,
                    onStep = { step = it },
                    onBackToIntro = { inSetup = false },
                    vm = vm, onFinished = onFinished,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Intro

@Composable
private fun IntroCarousel(onDone: () -> Unit) {
    val pager = rememberPagerState { IntroSlides.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == IntroSlides.lastIndex
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.xs, top = Spacing.xs).heightIn(min = Spacing.touchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudINWordmark(markSize = 28.dp)
            Spacer(Modifier.weight(1f))
            if (!last) StudINTextButton("Skip", onDone)
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { page ->
            IntroSlideContent(IntroSlides[page], page, IntroSlides.size)
        }
        Column(
            Modifier.fillMaxWidth().wrapContentWidth().widthIn(max = 560.dp).padding(horizontal = Spacing.screen, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageDots(pager.currentPage, IntroSlides.size)
            Spacer(Modifier.height(Spacing.md))
            StudINPrimaryButton(
                if (last) "Set up StudIN" else "Next",
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        }
    }
}

@Composable
private fun IntroSlideContent(slide: IntroSlide, index: Int, count: Int) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val landscape = maxWidth > h && h < 520.dp
        if (landscape) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = Spacing.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                StudINIllustration(slide.image, slide.imageDescription, Modifier.weight(1f), height = (h * 0.8f).coerceAtMost(260.dp))
                SlideText(slide, index, count, Modifier.weight(1f), TextAlign.Start, Alignment.Start)
            }
        } else {
            val art = (h * 0.48f).coerceIn(150.dp, 300.dp)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                StudINIllustration(slide.image, slide.imageDescription, Modifier.widthIn(max = 420.dp), height = art)
                Spacer(Modifier.height(Spacing.xl))
                SlideText(slide, index, count, Modifier.widthIn(max = 480.dp), TextAlign.Center, Alignment.CenterHorizontally)
            }
        }
    }
}

@Composable
private fun SlideText(slide: IntroSlide, index: Int, count: Int, modifier: Modifier, align: TextAlign, horizontal: Alignment.Horizontal) {
    Column(modifier.semantics(mergeDescendants = true) { stateDescription = "Slide ${index + 1} of $count" }, horizontalAlignment = horizontal) {
        Text(slide.eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = StudINTheme.colors.accent, textAlign = align)
        Spacer(Modifier.height(Spacing.xs))
        Text(slide.title, style = MaterialTheme.typography.headlineMedium, textAlign = align, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.sm))
        Text(slide.body, style = MaterialTheme.typography.bodyLarge, color = StudINTheme.colors.textSecondary, textAlign = align)
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "Page ${current + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val w by animateDpAsState(if (i == current) 24.dp else 8.dp, tween(motionDuration(Motion.small)), label = "dot")
            Box(
                Modifier.height(8.dp).width(w).background(
                    if (i == current) MaterialTheme.colorScheme.primary else StudINTheme.colors.border, CircleShape,
                )
            )
        }
    }
}

// ------------------------------------------------------------------ Setup

@Composable
private fun SetupFlow(
    state: OnboardingState,
    step: Int,
    onStep: (Int) -> Unit,
    onBackToIntro: () -> Unit,
    vm: OnboardingViewModel,
    onFinished: () -> Unit,
) {
    val canContinue = when (step) { 0 -> state.exam != null; else -> true }
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars).imePadding(),
    ) {
        Column(Modifier.fillMaxWidth().wrapContentWidth().widthIn(max = 640.dp).padding(horizontal = Spacing.screen, vertical = Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Step ${step + 1} of $SETUP_STEPS", style = MaterialTheme.typography.labelLarge, color = StudINTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                StudINTextButton(if (step == 0) "Back to intro" else "Back", { if (step == 0) onBackToIntro() else onStep(step - 1) })
            }
            StudINLinearProgress((step + 1) / SETUP_STEPS.toFloat(), height = 6.dp, label = "Setup progress, step ${step + 1} of $SETUP_STEPS")
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).wrapContentWidth().widthIn(max = 640.dp)
                    .padding(horizontal = Spacing.screen, vertical = Spacing.md),
            ) {
                when (step) {
                    0 -> ExamStep(state.exam, vm::setExam)
                    1 -> GoalStep(state, vm::setName, vm::setExamDay, vm::setGoal)
                    else -> ReadyStep(state, vm::setStreaks)
                }
            }
        }
        Column(Modifier.fillMaxWidth().wrapContentWidth().widthIn(max = 640.dp).padding(horizontal = Spacing.screen, vertical = Spacing.md)) {
            if (step < SETUP_STEPS - 1) {
                StudINPrimaryButton("Continue", { onStep(step + 1) }, Modifier.fillMaxWidth(), enabled = canContinue, icon = Icons.AutoMirrored.Rounded.ArrowForward)
                if (step == 0 && state.exam == null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text("Choose an exam to continue", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            } else {
                StudINPrimaryButton("Start studying", { vm.finish(onFinished) }, Modifier.fillMaxWidth(), loading = state.saving, enabled = state.exam != null)
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(Spacing.xs))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = StudINTheme.colors.textSecondary)
    Spacer(Modifier.height(Spacing.xl))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExamStep(selected: ExamType?, onSelect: (ExamType) -> Unit) {
    StepHeader("What are you preparing for?", "StudIN loads the matching syllabus, chapter weightage and starter flashcards. You can change this later.")
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 520.dp) 3 else 2
        val gap = Spacing.sm
        val cell: Dp = (maxWidth - gap * (columns - 1)) / columns
        FlowRow(horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap), maxItemsInEachRow = columns) {
            ExamType.entries.forEach { exam ->
                val isSelected = exam == selected
                Surface(
                    modifier = Modifier.width(cell).heightIn(min = 88.dp)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(exam) }),
                    shape = StudINShapes.card,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else StudINTheme.colors.border),
                ) {
                    Column(Modifier.padding(Spacing.sm + 2.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(exam.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (isSelected) Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(Spacing.xxs))
                        Text(exam.tagline, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalStep(state: OnboardingState, onName: (String) -> Unit, onExamDay: (Long?) -> Unit, onGoal: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    StepHeader("Set a realistic goal", "Consistency beats intensity. You can adjust all of this in Settings.")
    StudINTextField(state.name, onName, label = "Your name (optional)", placeholder = "e.g. Aarav")
    Spacer(Modifier.height(Spacing.md))
    StudINCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        StudINListItem(
            title = "Exam date",
            supporting = state.examDay?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMM yyyy")) } ?: "Optional — powers your countdown",
            icon = Icons.Rounded.CalendarMonth,
            onClick = { showPicker = true },
            trailing = { if (state.examDay != null) StudINTextButton("Clear", { onExamDay(null) }) else StudINTextButton("Set", { showPicker = true }) },
        )
    }
    Spacer(Modifier.height(Spacing.md))
    StudINCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Daily focus goal", style = MaterialTheme.typography.titleMedium)
                Text("≈ ${"%.1f".format(state.dailyGoalMinutes / 60f)} hours a day", style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
            }
            Text("${state.dailyGoalMinutes} min", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = state.dailyGoalMinutes.toFloat(),
            onValueChange = { onGoal(snapGoal(it)) },
            valueRange = 30f..480f,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget).semantics { contentDescription = "Daily focus goal, ${state.dailyGoalMinutes} minutes" },
        )
    }
    if (showPicker) ExamDatePicker(state.examDay, onDismiss = { showPicker = false }, onPicked = { onExamDay(it); showPicker = false })
}

@Composable
private fun ReadyStep(state: OnboardingState, onStreaks: (Boolean) -> Unit) {
    val context = LocalContext.current
    var notifGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }
    StudINIllustration(R.drawable.ill_privacy, "A locked phone, an on-device chip and a crossed-out cloud", height = 160.dp)
    Spacer(Modifier.height(Spacing.lg))
    StepHeader("Private by design", "StudIN works fully offline. Here's our promise:")
    StudINCard(Modifier.fillMaxWidth()) {
        Promise(Icons.Rounded.CloudOff, "No internet permission", "The app can't upload anything — your notes, chats and stats stay on this phone.")
        Promise(Icons.Rounded.Memory, "On-device AI", "Classification, flashcards and the tutor run locally.")
        Promise(Icons.Rounded.ToggleOn, "You're in control", "Every permission is optional and can be turned off any time.")
        Promise(Icons.Rounded.EmergencyShare, "Never trapped", "Pause or end a session any time. Emergency calls and Settings are never blocked.", last = true)
    }
    Spacer(Modifier.height(Spacing.md))
    StudINCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        StudINListItem(
            "Study streaks", supporting = "Opt-in and forgiving — one free miss a week, no shaming.",
            icon = Icons.Rounded.LocalFireDepartment, iconTint = StudINTheme.colors.accent,
            onClick = { onStreaks(!state.streaksEnabled) },
            trailing = { Switch(state.streaksEnabled, onCheckedChange = onStreaks) },
        )
        StudINListItem(
            "Timer & reminders", supporting = if (notifGranted) "Notifications allowed" else "Show the running timer and a gentle daily nudge",
            icon = Icons.Rounded.NotificationsActive,
            trailing = {
                if (notifGranted) Icon(Icons.Rounded.CheckCircle, "Allowed", tint = StudINTheme.colors.success)
                else StudINOutlinedButton("Allow", { if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) })
            },
        )
    }
    Spacer(Modifier.height(Spacing.sm))
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Rounded.Lock, null, tint = StudINTheme.colors.textSecondary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(Spacing.xs))
        Text("App blocking (usage access, Focus Guard) is optional — enable it later in Settings → Permission health.",
            style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
    }
}

@Composable
private fun Promise(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(bottom = if (last) 0.dp else Spacing.sm), verticalAlignment = Alignment.Top) {
        StudINIconTile(icon, size = 36.dp)
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary)
        }
    }
}

/** Snap a slider value to 15-minute increments within 30–480. */
fun snapGoal(raw: Float): Int = ((Math.round(raw / 15f) * 15)).coerceIn(30, 480)

/** Shared with Settings. Only future dates are selectable. */
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
        shape = StudINShapes.dialog,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) } ?: onDismiss()
            }) { Text("Set date") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(pickerState) }
}
