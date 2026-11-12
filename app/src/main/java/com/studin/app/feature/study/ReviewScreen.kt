package com.studin.app.feature.study

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.R
import com.studin.app.core.data.db.CardEntity
import com.studin.app.core.data.repo.schedule
import com.studin.app.core.designsystem.Motion
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINCard
import com.studin.app.core.designsystem.StudINEmptyState
import com.studin.app.core.designsystem.StudINFlashcard
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
import com.studin.domain.model.Grade
import com.studin.domain.srs.Sm2Scheduler

@Composable
fun ReviewScreen(deckId: Long?, onBack: () -> Unit, vm: ReviewViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val haptics by vm.haptics.collectAsStateWithLifecycle()
    val hf = LocalHapticFeedback.current
    val total = s.queue.size.coerceAtLeast(1)
    val position = (s.index + 1).coerceAtMost(s.queue.size)

    StudINScaffold(
        title = "Review",
        subtitle = when {
            s.loading -> null
            s.empty -> if (deckId != null) "This deck" else "All decks"
            s.finished -> "Session complete"
            else -> "$position of ${s.queue.size}" + if (s.queue.size > s.initialCount) " · includes repeats" else ""
        },
        onBack = onBack,
        bottomBar = {
            val card = s.current
            if (!s.loading && !s.finished && card != null) {
                BottomActionBar {
                    if (!s.revealed) {
                        StudINPrimaryButton("Show answer", vm::reveal, Modifier.fillMaxWidth(), icon = Icons.Rounded.Visibility)
                    } else {
                        GradeButtons(card, enabled = !s.busy) { g ->
                            if (haptics) hf.performHapticFeedback(if (g == Grade.AGAIN) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
                            vm.grade(g)
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                s.loading -> StudINLoadingState(Modifier.align(Alignment.Center), message = "Loading due cards…")
                s.empty -> Column(Modifier.fillMaxSize().studinContentWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen)) {
                    StudINEmptyState(
                        title = "All caught up", message = "No cards are due right now. Spaced repetition will bring them back at the right time.",
                        illustration = R.drawable.ill_recall, actionLabel = "Back to Study", onAction = onBack,
                    )
                }
                s.finished -> ReviewSummaryContent(reviewSummary(s.counts), onAgain = vm::load, onDone = onBack)
                else -> Column(
                    Modifier.fillMaxSize().studinContentWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
                ) {
                    StudINLinearProgress(s.index / total.toFloat(), height = 6.dp, label = "Card $position of ${s.queue.size}")
                    Spacer(Modifier.height(Spacing.md))
                    val card = s.current
                    if (card != null) {
                        val inMs = motionDuration(Motion.card)
                        val outMs = motionDuration(Motion.small)
                        AnimatedContent(
                            targetState = card.id to s.index,
                            transitionSpec = { fadeIn(tween(inMs)) togetherWith fadeOut(tween(outMs)) },
                            label = "card",
                        ) { (id, _) ->
                            val c = s.queue.firstOrNull { it.id == id } ?: card
                            StudINFlashcard(
                                front = c.front, back = c.back, revealed = s.revealed,
                                onFlip = { if (!s.revealed) vm.reveal() },
                                modifier = Modifier.heightIn(min = 280.dp),
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            if (s.revealed) "How well did you remember it? Your answer schedules the next review."
                            else "Try to recall the answer before revealing it.",
                            style = MaterialTheme.typography.bodySmall, color = StudINTheme.colors.textSecondary,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.GradeButtons(card: CardEntity, enabled: Boolean, onGrade: (Grade) -> Unit) {
    val now = remember(card.id) { System.currentTimeMillis() }
    val c = StudINTheme.colors
    Grade.entries.forEach { g ->
        val preview = remember(card.id, g) { Sm2Scheduler.formatInterval(card.schedule(), g, now) }
        val tone = when (g) {
            Grade.AGAIN -> MaterialTheme.colorScheme.error
            Grade.HARD -> c.warning
            Grade.GOOD -> MaterialTheme.colorScheme.primary
            Grade.EASY -> c.success
        }
        Surface(
            onClick = { onGrade(g) }, enabled = enabled,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp).semantics { contentDescription = "${g.label}, next review in $preview" },
            shape = StudINShapes.button, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, c.border),
        ) {
            Column(Modifier.padding(vertical = Spacing.xs, horizontal = Spacing.xxs), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(g.label, style = MaterialTheme.typography.labelLarge, color = tone, maxLines = 1)
                Text(preview, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ReviewSummaryContent(summary: ReviewSummary, onAgain: () -> Unit, onDone: () -> Unit) {
    val c = StudINTheme.colors
    Column(
        Modifier.fillMaxSize().studinContentWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StudINProgressRing(summary.recallRate, size = 148.dp, stroke = 12.dp, color = c.success) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${summary.recallPercent}%", style = StudINType.heroValue)
                Text("recalled", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text("Session complete", style = MaterialTheme.typography.headlineSmall)
        Text(
            "You reviewed ${summary.total} card${if (summary.total == 1) "" else "s"}. Cards marked Again will come back soon.",
            style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
        StudINCard(Modifier.fillMaxWidth()) {
            GradeCountRow("Again", summary.again, MaterialTheme.colorScheme.error)
            GradeCountRow("Hard", summary.hard, c.warning)
            GradeCountRow("Good", summary.good, MaterialTheme.colorScheme.primary)
            GradeCountRow("Easy", summary.easy, c.success)
        }
        Spacer(Modifier.height(Spacing.lg))
        StudINPrimaryButton("Done", onDone, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.xs))
        StudINOutlinedButton("Check for more due cards", onAgain, Modifier.fillMaxWidth())
    }
}

@Composable
private fun GradeCountRow(label: String, count: Int, tone: Color) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(tone, CircleShape))
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.titleMedium)
    }
}
