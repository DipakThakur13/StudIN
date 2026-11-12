package com.studin.app.platform.gate

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.designsystem.Badge
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.MonoNumbers
import com.studin.app.core.designsystem.ProgressRing
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.core.designsystem.StudinTheme
import com.studin.app.core.designsystem.ThemeMode
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
            val theme by prefs.settings.map { it.themeMode }.collectAsStateWithLifecycle(ThemeMode.DARK)
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
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xF0060E26), Color(0xFA030A1C)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlassCard(Modifier.fillMaxWidth(), highlight = true, contentPadding = 22.dp) {
                AnimatedContent(ui.step, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "gate") { step ->
                    when (step) {
                        GateStep.Choice -> ChoiceContent(label, reason, remainingMin, ui, onBack, onChallenge)
                        is GateStep.Quiz -> QuizContent(step, onAnswer, onNext)
                        is GateStep.Breathe -> BreatheContent(step.secondsLeft)
                        is GateStep.Granted -> GrantedContent(step, label, onContinue, onBack)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (ui.step !is GateStep.Granted) {
                TextButton(
                    onClick = onEmergency,
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Emergency: open $label anyway" },
                ) {
                    Icon(Icons.Rounded.Warning, null, tint = Studin.extras.warning, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Emergency – open anyway", color = Studin.extras.warning, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ChoiceContent(label: String, reason: String, remainingMin: Int, ui: GateUi, onBack: () -> Unit, onChallenge: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        IconBubble(Icons.Rounded.School, StudinColors.Mint, 64.dp)
        Spacer(Modifier.height(14.dp))
        Text("$label can wait", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Badge(reason.take(60))
        Spacer(Modifier.height(10.dp))
        Text(
            if (remainingMin > 0) "$remainingMin min left in your focus session. You're doing great." else "You're in a focus session.",
            style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        GradientButton("Back to studying", onBack, Modifier.fillMaxWidth(), icon = Icons.Rounded.School)
        Spacer(Modifier.height(10.dp))
        val text = if (ui.questionCount > 0)
            "Answer ${ui.questionCount} quick question${if (ui.questionCount > 1) "s" else ""} for a ${ui.passMinutes}-min break"
        else "Take a mindful pause for a ${ui.passMinutes}-min break"
        GhostButton(text, onChallenge, Modifier.fillMaxWidth(), icon = if (ui.questionCount > 0) Icons.Rounded.Quiz else Icons.Rounded.SelfImprovement)
    }
}

@Composable
private fun QuizContent(step: GateStep.Quiz, onAnswer: (Int) -> Unit, onNext: () -> Unit) {
    val q: McqQuestion = step.questions[step.index]
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge("Question ${step.index + 1} of ${step.questions.size}")
            Spacer(Modifier.weight(1f))
            Text("No penalty for mistakes", style = MaterialTheme.typography.labelSmall, color = Studin.extras.muted)
        }
        Spacer(Modifier.height(14.dp))
        Text(q.prompt, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        q.options.forEachIndexed { i, opt ->
            val picked = step.picked
            val color = when {
                picked == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                i == q.correctIndex -> StudinColors.Mint
                i == picked -> StudinColors.Coral
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, color, RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = if (picked != null && (i == q.correctIndex || i == picked)) 0.14f else 0f))
                    .clickable(enabled = picked == null, role = Role.Button) { onAnswer(i) }
                    .heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(('A' + i).toString(), fontWeight = FontWeight.Bold, color = color.copy(alpha = 1f))
                Spacer(Modifier.width(12.dp))
                Text(opt, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (picked != null && i == q.correctIndex) Icon(Icons.Rounded.CheckCircle, "Correct answer", tint = StudinColors.Mint)
                if (picked != null && i == picked && i != q.correctIndex) Icon(Icons.Rounded.Close, "Your answer", tint = StudinColors.Coral)
            }
        }
        if (step.picked != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (step.picked == q.correctIndex) "Nice recall! 🎯" else "Good try – the right answer is highlighted. It'll come back in your reviews.",
                style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted,
            )
            Spacer(Modifier.height(12.dp))
            GradientButton(if (step.index + 1 < step.questions.size) "Next question" else "Finish", onNext, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BreatheContent(secondsLeft: Int) {
    val t = rememberInfiniteTransition(label = "breathe")
    val scale by t.animateFloat(0.8f, 1.1f, infiniteRepeatable(tween(2_500), RepeatMode.Reverse), label = "s")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Breathe in… and out", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(18.dp))
        ProgressRing((10 - secondsLeft) / 10f, Modifier.scale(scale), size = 160.dp, stroke = 10.dp) {
            Text("$secondsLeft", style = MonoNumbers.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize))
        }
        Spacer(Modifier.height(18.dp))
        Text("A short pause helps you choose on purpose.", style = MaterialTheme.typography.bodyMedium, color = Studin.extras.muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GrantedContent(step: GateStep.Granted, label: String, onContinue: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(StudinColors.Mint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.CheckCircle, null, tint = StudinColors.Mint, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("${step.minutes}-minute break unlocked", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        if (step.total > 0) {
            Spacer(Modifier.height(6.dp))
            Text("You got ${step.correct} of ${step.total} right.", color = Studin.extras.muted)
        }
        Spacer(Modifier.height(20.dp))
        GradientButton("Continue to $label", onContinue, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        GhostButton("Actually, back to studying", onBack, Modifier.fillMaxWidth())
    }
}
