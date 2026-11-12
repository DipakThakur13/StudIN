package com.studin.app.feature.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.R
import com.studin.app.core.designsystem.Motion
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINBadge
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINEmptyState
import com.studin.app.core.designsystem.StudINLinearProgress
import com.studin.app.core.designsystem.StudINLoadingState
import com.studin.app.core.designsystem.StudINOutlinedButton
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINProgressRing
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINShapes
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.StudINType
import com.studin.app.core.designsystem.motionDuration
import com.studin.app.core.designsystem.studinContentWidth
import com.studin.domain.quiz.McqQuestion

private val Letters = listOf("A", "B", "C", "D", "E", "F")

@Composable
fun QuizScreen(deckId: Long?, onBack: () -> Unit, vm: QuizViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    StudINScaffold(
        title = "Quiz",
        subtitle = when {
            s.loading || s.notEnough -> if (deckId != null) "This deck" else "Mixed practice"
            s.finished -> "Results"
            else -> "Question ${s.index + 1} of ${s.questions.size} · ${s.correct} correct"
        },
        onBack = onBack,
        bottomBar = {
            if (!s.loading && !s.finished && s.current != null && s.selected != null) {
                BottomActionBar {
                    StudINPrimaryButton(
                        if (s.index + 1 >= s.questions.size) "See results" else "Next question", vm::next,
                        Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                s.loading -> StudINLoadingState(Modifier.align(Alignment.Center), message = "Building your quiz…")
                s.notEnough -> Column(Modifier.fillMaxSize().studinContentWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen)) {
                    StudINEmptyState(
                        title = "Not enough cards yet",
                        message = "A quiz needs at least ${QuizViewModel.MIN_CARDS} cards with different answers. Add cards or generate them from your notes.",
                        illustration = R.drawable.ill_recall, actionLabel = "Back", onAction = onBack,
                    )
                }
                s.finished -> QuizResults(s.correct, s.questions.size, onRetry = vm::load, onDone = onBack)
                else -> s.current?.let { q -> QuestionContent(q, s.index, s.questions.size, s.selected, vm::answer) }
            }
        }
    }
}

@Composable
private fun QuestionContent(q: McqQuestion, index: Int, total: Int, selected: Int?, onAnswer: (Int) -> Unit) {
    val c = StudINTheme.colors
    Column(
        Modifier.fillMaxSize().studinContentWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen, vertical = Spacing.xs),
    ) {
        StudINLinearProgress((index + if (selected != null) 1 else 0) / total.toFloat(), height = 6.dp, label = "Question ${index + 1} of $total")
        Spacer(Modifier.height(Spacing.md))
        StudINCard(Modifier.fillMaxWidth()) {
            StudINBadge("Question ${index + 1}", container = c.surfaceSecondary, content = c.textSecondary)
            Spacer(Modifier.height(Spacing.sm))
            Text(q.prompt, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        }
        Spacer(Modifier.height(Spacing.md))
        q.options.forEachIndexed { i, option ->
            OptionCard(
                letter = Letters.getOrElse(i) { "${i + 1}" }, text = option,
                state = when {
                    selected == null -> OptionState.Idle
                    i == q.correctIndex -> OptionState.Correct
                    i == selected -> OptionState.Wrong
                    else -> OptionState.Dimmed
                },
                onClick = { onAnswer(i) },
            )
            Spacer(Modifier.height(Spacing.xs))
        }
        if (selected != null) {
            val right = selected == q.correctIndex
            Spacer(Modifier.height(Spacing.xs))
            StudINCard(
                Modifier.fillMaxWidth(),
                containerColor = if (right) c.successContainer else MaterialTheme.colorScheme.errorContainer, border = null,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (right) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, contentDescription = null,
                        tint = if (right) c.success else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(if (right) "Correct" else "Not quite", style = MaterialTheme.typography.titleMedium,
                        color = if (right) c.success else MaterialTheme.colorScheme.onErrorContainer)
                }
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    if (right) "Nice recall. This card's schedule has been updated."
                    else "The answer is: ${q.options[q.correctIndex]}. This card is now due for review – no penalty.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (right) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

private enum class OptionState { Idle, Correct, Wrong, Dimmed }

@Composable
private fun OptionCard(letter: String, text: String, state: OptionState, onClick: () -> Unit) {
    val c = StudINTheme.colors
    val error = MaterialTheme.colorScheme.error
    val container by animateColorAsState(
        when (state) {
            OptionState.Correct -> c.successContainer
            OptionState.Wrong -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surface
        }, tween(motionDuration(Motion.small)), label = "option",
    )
    val borderColor = when (state) { OptionState.Correct -> c.success; OptionState.Wrong -> error; else -> c.border }
    val status = when (state) { OptionState.Correct -> "Correct answer"; OptionState.Wrong -> "Your answer – incorrect"; else -> null }
    Surface(
        onClick = onClick, enabled = state == OptionState.Idle,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics {
            contentDescription = "Option $letter: $text"
            if (status != null) stateDescription = status
        },
        shape = StudINShapes.button, color = container,
        border = BorderStroke(if (state == OptionState.Correct || state == OptionState.Wrong) 1.5.dp else 1.dp, borderColor),
    ) {
        Row(Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            val tileColor = when (state) { OptionState.Correct -> c.success; OptionState.Wrong -> error; else -> c.surfaceSecondary }
            Box(Modifier.size(32.dp).background(tileColor, StudINShapes.badge), contentAlignment = Alignment.Center) {
                when (state) {
                    OptionState.Correct -> Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp))
                    OptionState.Wrong -> Icon(Icons.Rounded.Cancel, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp))
                    else -> Text(letter, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text, style = MaterialTheme.typography.bodyLarge,
                    color = if (state == OptionState.Dimmed) c.textSecondary else MaterialTheme.colorScheme.onSurface,
                )
                if (status != null) Text(status, style = MaterialTheme.typography.labelMedium, color = if (state == OptionState.Correct) c.success else error)
            }
        }
    }
}

@Composable
private fun QuizResults(correct: Int, total: Int, onRetry: () -> Unit, onDone: () -> Unit) {
    val c = StudINTheme.colors
    val ratio = if (total == 0) 0f else correct / total.toFloat()
    Column(
        Modifier.fillMaxSize().studinContentWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StudINProgressRing(ratio, size = 148.dp, stroke = 12.dp, color = if (ratio >= 0.5f) c.success else c.accent) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$correct/$total", style = StudINType.heroValue)
                Text("correct", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text(quizVerdict(correct, total), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            "Every answer was saved to your review history. Missed cards are scheduled for review.",
            style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        StudINPrimaryButton("Done", onDone, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.xs))
        StudINOutlinedButton("Try another set", onRetry, Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh)
    }
}

