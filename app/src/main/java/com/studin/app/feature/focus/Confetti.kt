package com.studin.app.feature.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.studin.app.core.designsystem.StudinColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val angle: Float, val speed: Float, val size: Float, val color: Color,
    val spin: Float, val originX: Float, val circle: Boolean,
)

/** One-shot celebratory confetti burst, keyed by [trigger]. Purely decorative. */
@Composable
fun ConfettiBurst(trigger: Any, modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    val progress = remember(trigger) { Animatable(0f) }
    val particles = remember(trigger) {
        val rnd = Random(trigger.hashCode())
        val colors = listOf(StudinColors.Mint, StudinColors.Cyan, StudinColors.Star, StudinColors.Violet, StudinColors.Coral, Color.White)
        List(140) {
            Particle(
                angle = (-Math.PI / 2 + (rnd.nextFloat() - 0.5f) * 2.2f).toFloat(),
                speed = 0.9f + rnd.nextFloat() * 1.3f,
                size = 6f + rnd.nextFloat() * 10f,
                color = colors[rnd.nextInt(colors.size)],
                spin = (rnd.nextFloat() - 0.5f) * 1440f,
                originX = 0.3f + rnd.nextFloat() * 0.4f,
                circle = rnd.nextInt(4) == 0,
            )
        }
    }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(2_800, easing = LinearEasing))
        onDone()
    }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val w = size.width; val h = size.height
        val alpha = (1f - t).coerceIn(0f, 1f)
        particles.forEach { p ->
            val v = p.speed * h * 0.9f
            val x = w * p.originX + cos(p.angle) * v * t
            val y = h * 0.55f + sin(p.angle) * v * t + 0.5f * (h * 1.6f) * t * t
            val c = p.color.copy(alpha = alpha)
            if (p.circle) drawCircle(c, p.size / 2, Offset(x, y))
            else rotate(p.spin * t, Offset(x, y)) {
                drawRect(c, Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
            }
        }
    }
}
