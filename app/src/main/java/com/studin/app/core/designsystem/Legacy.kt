package com.studin.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/*
 * V1 compatibility shim. Remapped onto V2 tokens so un-migrated screens still compile and look
 * coherent during the migration. Delete this file once nothing references it.
 */

@Deprecated("Use MaterialTheme.colorScheme / StudINTheme.colors")
object StudinColors {
    val Navy950 = Color(Palette.TextPrimary)
    val Navy900 = Color(Palette.Background)
    val Navy800 = Color(Palette.Surface)
    val Navy700 = Color(Palette.SurfaceSecondary)
    val Navy600 = Color(Palette.Border)
    val Mint = Color(Palette.Forest)
    val MintDeep = Color(Palette.ForestDark)
    val Teal = Color(Palette.ForestMid)
    val Cyan = Color(Palette.Success)
    val Star = Color(Palette.Orange)
    val Coral = Color(Palette.Error)
    val Violet = Color(Palette.Ai)
    val Ink = Color(Palette.TextPrimary)
    val InkMuted = Color(Palette.TextSecondary)
    val Paper = Color(Palette.Background)
}

@Immutable
data class StudinExtras(
    val heroGradient: Brush,
    val backgroundGradient: Brush,
    val glass: Color,
    val glassBorder: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val star: Color,
    val muted: Color,
    val isDark: Boolean,
)

internal fun legacyExtras(c: StudINColors, primary: Color) = StudinExtras(
    heroGradient = Brush.linearGradient(listOf(primary, primary)),
    backgroundGradient = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
    glass = c.surfaceSecondary, glassBorder = c.border, success = c.success, warning = c.warning,
    danger = if (c.isDark) Color(Palette.DError) else Color(Palette.Error), star = c.accent, muted = c.textSecondary, isDark = c.isDark,
)

val LocalStudinExtras = staticCompositionLocalOf { legacyExtras(LightStudINColors, Color(Palette.Forest)) }

@Deprecated("Use StudINTheme")
object Studin {
    val extras: StudinExtras @Composable get() = LocalStudinExtras.current
}

@Deprecated("Use StudINType.timer / statValue")
val MonoNumbers = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
