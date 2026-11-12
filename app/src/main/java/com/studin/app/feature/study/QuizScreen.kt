package com.studin.app.feature.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.designsystem.EmptyState
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.GradientProgressBar
import com.studin.app.core.designsystem.ProgressRing
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.domain.quiz.McqQuestion
import kotlin.math.roundToInt

@Composable
fun QuizScreen(deckId: Long?, onBack: () -> Unit, vm: QuizViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        StudyTopBar(
            title = "Quiz",
            subtitle = if (s.questions.isNotEmpty() && !s.finished) "Question ${s.index + 1} of ${s.questions.size} · ${s.correct} correct" else null,
            onBack = onBack,
        )
        when {
            s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            s.notEnough -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(
                    Icons.Rounded.Quiz, "Need a few more cards",
                    "A quiz needs at least ${QuizViewModel.MIN_CARDS} cards with different answers so the options make sense. " +
                        "Add cards or generate them from your notes.",
                )
                GhostButton("Back", onBack, Modifier.fillMaxWidth(0.7f))
            }
            s.finished -> QuizSummary(s.correct, s.questions.size, onBack, vm::load)
            else -> {
                GradientProgressBar(s.index.toFloat() / s.questions.size, Modifier.padding(horizontal = 20.dp))
                AnimatedContent(
                    targetState = s.index,
                    transitionSpec = { (slideInHorizontally(tween(300)) { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally(tween(220)) { -it / 3 } + fadeOut()) },
                    modifier = Modifier.weight(1f),
                    label = "question",
                ) { idx ->
                    val q = s.questions.getOrNull(idx)
                    if (q != null) QuestionView(q, if (idx == s.index) s.selected else null, vm::answer)
                }
                AnimatedVisibility(s.selected != null, enter = slideInVertically { it } + fadeIn()) {
                    GradientButton(
                        if (s.index + 1 >= s.questions.size) "See results" else "Next question",
                        vm::next, Modifier.fillMaxWidth().padding(20.dp), icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionView(q: McqQuestion, selected: Int?, onAnswer: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        GlassCard(Modifier.fillMaxWidth(), highlight = true) {
            Text("QUESTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(q.prompt, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(16.dp))
        q.options.forEachIndexed { i, option ->
            OptionCard(
                letter = ('A' + i).toString(), text = option,
                state = when {
                    selected == null -> OptionState.IDLE
                    i == q.correctIndex -> OptionState.CORRECT
                    i == selected -> OptionState.WRONG
                    else -> OptionState.DIMMED
                },
                onClick = { onAnswer(i) },
            )
            Spacer(Modifier.height(10.dp))
        }
        if (selected != null) {
            val ok = selected == q.correctIndex
            Text(
                if (ok) "Correct! Nice recall." else "Not quite – the answer is highlighted. This card will come back for review.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (ok) Studin.extras.success else Studin.extras.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private enum class OptionState { IDLE, CORRECT, WRONG, DIMMED }

@Composable
private fun OptionCard(letter: String, text: String, state: OptionState, onClick: () -> Unit) {
    val ex = Studin.extras
    val target = when (state) {
        OptionState.CORRECT -> StudinColors.Mint
        OptionState.WRONG -> ex.danger
        else -> MaterialTheme.colorScheme.onSurface
    }
    val accent by animateColorAsState(target, label = "opt")
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == OptionState.WRONG) shake.animateTo(0f, keyframes {
            durationMillis = 420
            -18f at 60; 16f at 120; -12f at 180; 8f at 240; -4f at 300; 0f at 420
        })
    }
    val stateLabel = when (state) { OptionState.CORRECT -> ", correct answer"; OptionState.WRONG -> ", your answer, wrong"; else -> "" }
    Surface(
        onClick = onClick,
        enabled = state == OptionState.IDLE,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .offset { IntOffset(shake.value.roundToInt(), 0) }
            .semantics { contentDescription = "Option $letter: $text$stateLabel" },
        shape = RoundedCornerShape(18.dp),
        color = when (state) {
            OptionState.CORRECT -> StudinColors.Mint.copy(alpha = 0.18f)
            OptionState.WRONG -> ex.danger.copy(alpha = 0.18f)
            else -> ex.glass
        },
        border = BorderStroke(if (state == OptionState.IDLE || state == OptionState.DIMMED) 1.dp else 1.6.dp,
            if (state == OptionState.IDLE || state == OptionState.DIMMED) ex.glassBorder else accent),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    OptionState.CORRECT -> Icon(Icons.Rounded.CheckCircle, null, tint = accent)
                    OptionState.WRONG -> Icon(Icons.Rounded.Cancel, null, tint = accent)
                    else -> Text(letter, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text, style = MaterialTheme.typography.bodyLarge,
                color = if (state == OptionState.DIMMED) ex.muted else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun QuizSummary(correct: Int, total: Int, onDone: () -> Unit, onRetry: () -> Unit) {
    val ex = Studin.extras
    val pct = if (total == 0) 0f else correct.toFloat() / total
    val message = when {
        pct >= 0.9f -> "Outstanding! You really know this."
        pct >= 0.7f -> "Great work – solid recall."
        pct >= 0.4f -> "Good effort. Missed cards are queued for review."
        else -> "Every mistake now is a mark saved later. Review and try again."
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        ProgressRing(
            pct, size = 210.dp,
            colors = if (pct >= 0.5f) listOf(StudinColors.Mint, StudinColors.Cyan, StudinColors.Mint)
            else listOf(StudinColors.Star, ex.danger, StudinColors.Star),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$correct/$total", style = MaterialTheme.typography.displaySmall)
                Text("${(pct * 100).roundToInt()}% accuracy", style = MaterialTheme.typography.labelMedium, color = ex.muted)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Quiz complete", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = ex.muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        GradientButton("Done", onDone, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        GhostButton("New quiz", onRetry, Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh)
    }
}
