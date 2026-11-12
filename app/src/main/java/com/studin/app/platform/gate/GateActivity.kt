package com.studin.app.platform.gate

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.R
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.designsystem.Motion
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINBadge
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINIllustration
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINProgressRing
import com.studin.app.core.designsystem.StudINSecondaryButton
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.StudINType
import com.studin.app.core.designsystem.StudinTheme
import com.studin.app.core.designsystem.ThemeMode
import com.studin.app.core.designsystem.motionDuration
import com.studin.domain.quiz.McqQuestion
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Break gate shown over a blocked/off-syllabus app during focus. Non-coercive:
 * "Back to studying", an earned break (MCQs or a mindful pause), and an
 * always-visible emergency bypass.
 */
@AndroidEntryPoint
class GateActivity : ComponentActivity() {
    @Inject lateinit var prefs: UserPreferences
    private val vm: GateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = backToStudying()
        })
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render()
    }

    private fun render() {
        val label = intent.getStringExtra(GateLauncher.EXTRA_LABEL) ?: "This app"
        val reason = intent.getStringExtra(GateLauncher.EXTRA_REASON) ?: "Blocked during focus"
        val decisionId = intent.getLongExtra(GateLauncher.EXTRA_DECISION, -1L)
        setContent {
            val theme by prefs.settings.map { it.themeMode }.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            StudinTheme(theme) {
                val ui by vm.ui.collectAsStateWithLifecycle()
                val snap by vm.controller.snapshot.collectAsStateWithLifecycle()
                val now by vm.controller.now.collectAsStateWithLifecycle()
                GateScreen(
                    label = label, reason = reason, ui = ui,
                    remainingMin = ((snap.remainingMs(maxOf(now, SystemClock.elapsedRealtime())) + 59_999) / 60_000).toInt(),
                    onBack = ::backToStudying,
                    onChallenge = vm::startBreakChallenge,
                    onAnswer = vm::answer, onNext = vm::next,
                    onContinue = { finish() },
                    onEmergency = { vm.emergency(decisionId); finish() },
                )
            }
        }
    }

    private fun backToStudying() {
        runCatching {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }
}

/** Text for the earned-break action (unit-tested in GateUiTest). */
internal fun breakActionLabel(questions: Int, passMinutes: Int): String =
    if (questions > 0) "Answer $questions question${if (questions > 1) "s" else ""} for a $passMinutes-min break"
    else "Take a short pause for a $passMinutes-min break"

@Composable
private fun GateScreen(
    label: String,
    reason: String,
    ui: GateUi,
    remainingMin: Int,
    onBack: () -> Unit,
    onChallenge: () -> Unit,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onContinue: () -> Unit,
    onEmergency: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val inMs = motionDuration(Motion.card)
                val outMs = motionDuration(Motion.small)
                StudINCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.xl)) {
                    AnimatedContent(
                        ui.step,
                        transitionSpec = { fadeIn(tween(inMs)) togetherWith fadeOut(tween(outMs)) },
                        contentKey = { it::class },
                        label = "gate",
                    ) { step ->
                        when (step) {
                            GateStep.Choice -> ChoiceContent(label, reason, remainingMin, ui, onBack, onChallenge)
                            is GateStep.Quiz -> QuizContent(step, onAnswer, onNext)
                            is GateStep.Breathe -> BreatheContent(step.secondsLeft)
                            is GateStep.Granted -> GrantedContent(step, label, onContinue, onBack)
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                if (ui.step !is GateStep.Granted) {
                    // Always available: the student is never trapped.
                    TextButton(
                        onClick = onEmergency,
                        modifier = Modifier.heightIn(min = Spacing.touchTarget).semantics { contentDescription = "Emergency: open $label anyway" },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Emergency — open anyway", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceContent(label: String, reason: String, remainingMin: Int, ui: GateUi, onBack: () -> Unit, onChallenge: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        StudINIllustration(R.drawable.ill_focus, contentDescription = null, height = 132.dp)
        Spacer(Modifier.height(Spacing.md))
        Text("$label can wait", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.xs))
        StudINBadge(reason.take(80), container = StudINTheme.colors.surfaceSecondary, content = StudINTheme.colors.textSecondary)
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = StudINTheme.colors.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xxs))
            Text(
                if (remainingMin > 0) "$remainingMin min left in your focus session" else "You're in a focus session",
                style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary, textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(Spacing.xl))
        StudINPrimaryButton("Back to studying", onBack, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.MenuBook)
        Spacer(Modifier.height(Spacing.xs))
        StudINSecondaryButton(
            breakActionLabel(ui.questionCount, ui.passMinutes), onChallenge, Modifier.fillMaxWidth(),
            icon = if (ui.questionCount > 0) Icons.Rounded.Quiz else Icons.Rounded.SelfImprovement,
        )
    }
}

@Composable
private fun QuizContent(step: GateStep.Quiz, onAnswer: (Int) -> Unit, onNext: () -> Unit) {
    val q: McqQuestion = step.questions[step.index]
    val c = StudINTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudINBadge("Question ${step.index + 1} of ${step.questions.size}")
            Spacer(Modifier.weight(1f))
            Text("No penalty for mistakes", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
        }
        Spacer(Modifier.height(Spacing.md))
        Text(q.prompt, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.md))
        q.options.forEachIndexed { i, opt ->
            val picked = step.picked
            val isCorrect = picked != null && i == q.correctIndex
            val isWrongPick = picked != null && i == picked && i != q.correctIndex
            val status = when { isCorrect -> "Correct answer"; isWrongPick -> "Your answer, incorrect"; else -> null }
            StudINCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxs).heightIn(min = 52.dp)
                    .semantics { if (status != null) contentDescription = "Option ${'A' + i}: $opt. $status" },
                onClick = { onAnswer(i) },
                enabled = picked == null,
                containerColor = when { isCorrect -> c.successContainer; isWrongPick -> MaterialTheme.colorScheme.errorContainer; else -> MaterialTheme.colorScheme.surface },
                border = BorderStroke(if (isCorrect || isWrongPick) 1.5.dp else 1.dp, when { isCorrect -> c.success; isWrongPick -> MaterialTheme.colorScheme.error; else -> c.border }),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${'A' + i}", style = MaterialTheme.typography.labelLarge, color = c.textSecondary, modifier = Modifier.width(24.dp))
                    Text(opt, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    when {
                        isCorrect -> StatusTag(Icons.Rounded.CheckCircle, "Correct", c.success)
                        isWrongPick -> StatusTag(Icons.Rounded.Cancel, "Not quite", MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (step.picked != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                if (step.picked == q.correctIndex) "Nice recall." else "Good try — the right answer is marked. It will come back in your reviews.",
                style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(Spacing.md))
            StudINPrimaryButton(if (step.index + 1 < step.questions.size) "Next question" else "Finish", onNext, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatusTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.xxs))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Ten-second mindful pause: a simple countdown ring, no looping animation. */
@Composable
private fun BreatheContent(secondsLeft: Int) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Take a breath", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.lg))
        StudINProgressRing(secondsLeft / 10f, size = 144.dp, stroke = 10.dp, color = MaterialTheme.colorScheme.primary) {
            Text("$secondsLeft", style = StudINType.heroValue, modifier = Modifier.semantics { contentDescription = "$secondsLeft seconds" })
        }
        Spacer(Modifier.height(Spacing.lg))
        Text("Breathe in slowly, then out. A short pause helps you choose on purpose.",
            style = MaterialTheme.typography.bodyMedium, color = StudINTheme.colors.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GrantedContent(step: GateStep.Granted, label: String, onContinue: () -> Unit, onBack: () -> Unit) {
    val c = StudINTheme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).background(c.successContainer, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.success, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(Spacing.md))
        Text("${step.minutes}-minute break unlocked", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite })
        if (step.total > 0) {
            Spacer(Modifier.height(Spacing.xxs))
            Text("You got ${step.correct} of ${step.total} right.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
        Spacer(Modifier.height(Spacing.xl))
        StudINPrimaryButton("Continue to $label", onContinue, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.xs))
        StudINOutlinedButton("Back to studying instead", onBack, Modifier.fillMaxWidth())
    }
}
