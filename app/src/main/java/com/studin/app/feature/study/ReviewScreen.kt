package com.studin.app.feature.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studin.app.core.data.db.CardEntity
import com.studin.app.core.data.repo.schedule
import com.studin.app.core.designsystem.EmptyState
import com.studin.app.core.designsystem.GhostButton
import com.studin.app.core.designsystem.GradientButton
import com.studin.app.core.designsystem.GradientProgressBar
import com.studin.app.core.designsystem.ProgressRing
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.domain.model.Grade
import com.studin.domain.srs.Sm2Scheduler

@Composable
fun ReviewScreen(deckId: Long?, onBack: () -> Unit, vm: ReviewViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val haptics by vm.haptics.collectAsStateWithLifecycle()
    val hf = LocalHapticFeedback.current

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        StudyTopBar(
            title = "Review",
            subtitle = if (s.loading || s.empty) null else "${minOf(s.index + 1, s.queue.size)} / ${s.queue.size}",
            onBack = onBack,
        )
        when {
            s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            s.empty -> CaughtUp(onBack)
            s.finished -> ReviewSummary(s, onBack, onAgain = vm::load)
            else -> {
                GradientProgressBar(
                    if (s.queue.isEmpty()) 0f else s.index.toFloat() / s.queue.size,
                    Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(16.dp))
                AnimatedContent(
                    targetState = s.index,
                    transitionSpec = {
                        (slideInHorizontally(tween(320)) { it } + fadeIn()) togetherWith (slideOutHorizontally(tween(260)) { -it / 2 } + fadeOut())
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    label = "card",
                ) { idx ->
                    val card = s.queue.getOrNull(idx)
                    if (card != null) {
                        FlipCard(
                            card = card,
                            revealed = s.revealed && idx == s.index,
                            onReveal = vm::reveal,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                val card = s.current
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 72.dp)) {
                    AnimatedContent(s.revealed, label = "actions", transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
                    }) { revealed ->
                        if (!revealed || card == null) {
                            GradientButton("Show answer", vm::reveal, Modifier.fillMaxWidth(), icon = Icons.Rounded.TouchApp)
                        } else {
                            GradeButtons(card) { g ->
                                if (haptics) hf.performHapticFeedback(
                                    if (g == Grade.AGAIN) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                                )
                                vm.grade(g)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(card: CardEntity, revealed: Boolean, onReveal: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(
        if (revealed) 180f else 0f, spring(dampingRatio = 0.72f, stiffness = 220f), label = "flip",
    )
    val showBack = rotation > 90f
    val ex = Studin.extras
    Surface(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(28.dp))
            .clickable(remember { MutableInteractionSource() }, null, enabled = !revealed, role = Role.Button, onClick = onReveal)
            .semantics { contentDescription = if (showBack) "Answer: ${card.back}" else "Question: ${card.front}. Tap to reveal answer" },
        shape = RoundedCornerShape(28.dp),
        color = ex.glass,
        border = BorderStroke(
            1.2.dp,
            if (showBack) Brush.linearGradient(listOf(StudinColors.Mint, StudinColors.Cyan))
            else Brush.linearGradient(listOf(ex.glassBorder, ex.glassBorder)),
        ),
    ) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { if (showBack) rotationY = 180f }
                .background(
                    if (showBack) Brush.verticalGradient(listOf(StudinColors.Mint.copy(alpha = 0.10f), Color.Transparent))
                    else Brush.verticalGradient(listOf(StudinColors.Violet.copy(alpha = 0.08f), Color.Transparent))
                )
                .padding(24.dp),
        ) {
            Text(
                if (showBack) "ANSWER" else "QUESTION",
                style = MaterialTheme.typography.labelSmall,
                color = if (showBack) MaterialTheme.colorScheme.primary else ex.muted,
                modifier = Modifier.align(Alignment.TopStart),
            )
            Column(
                Modifier.align(Alignment.Center).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showBack) {
                    Text(card.front, style = MaterialTheme.typography.bodyMedium, color = ex.muted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text(card.back, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                } else {
                    Text(card.front, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                }
            }
            if (!showBack) Text(
                "Tap to flip", style = MaterialTheme.typography.labelMedium, color = ex.muted,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun GradeButtons(card: CardEntity, onGrade: (Grade) -> Unit) {
    val now = remember(card.id) { System.currentTimeMillis() }
    val ex = Studin.extras
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Grade.entries.forEach { g ->
            val color = when (g) { Grade.AGAIN -> ex.danger; Grade.HARD -> ex.warning; Grade.GOOD -> StudinColors.Mint; Grade.EASY -> StudinColors.Cyan }
            val preview = remember(card.id, g) { Sm2Scheduler.formatInterval(card.schedule(), g, now) }
            Surface(
                onClick = { onGrade(g) },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                shape = RoundedCornerShape(16.dp),
                color = color.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
            ) {
                Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(g.label, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                    Text(preview, style = MaterialTheme.typography.labelSmall, color = ex.muted)
                }
            }
        }
    }
}

@Composable
private fun ReviewSummary(s: ReviewUiState, onDone: () -> Unit, onAgain: () -> Unit) {
    val ex = Studin.extras
    val total = s.reviewed.coerceAtLeast(1)
    val recalled = (s.counts[Grade.GOOD] ?: 0) + (s.counts[Grade.EASY] ?: 0) + (s.counts[Grade.HARD] ?: 0)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(true, enter = scaleIn() + fadeIn()) {
            ProgressRing(recalled.toFloat() / total, size = 200.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${recalled * 100 / total}%", style = MaterialTheme.typography.displaySmall)
                    Text("recalled", style = MaterialTheme.typography.labelMedium, color = ex.muted)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Session complete", style = MaterialTheme.typography.headlineSmall)
        Text("${s.reviewed} reviews · scheduled with spaced repetition", style = MaterialTheme.typography.bodyMedium, color = ex.muted)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Grade.entries.forEach { g ->
                val color = when (g) { Grade.AGAIN -> ex.danger; Grade.HARD -> ex.warning; Grade.GOOD -> StudinColors.Mint; Grade.EASY -> StudinColors.Cyan }
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.12f)).padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${s.counts[g] ?: 0}", style = MaterialTheme.typography.titleLarge, color = color)
                    Text(g.label, style = MaterialTheme.typography.labelSmall, color = ex.muted)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        GradientButton("Done", onDone, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        GhostButton("Check for more due cards", onAgain, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CaughtUp(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyState(
            Icons.Rounded.Spa, "All caught up",
            "No cards are due right now. Your memory is doing its work – come back later, or keep the momentum with a quiz.",
        )
        Box(Modifier.size(1.dp))
        GhostButton("Back to Study", onBack, Modifier.fillMaxWidth(0.8f))
        Spacer(Modifier.height(10.dp))
        Text(
            "Tip: tap Quiz or Generate on the Study tab to practise more.",
            style = MaterialTheme.typography.bodySmall, color = Studin.extras.muted, textAlign = TextAlign.Center,
        )
    }
}
