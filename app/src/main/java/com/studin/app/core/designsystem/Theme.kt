package com.studin.app.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** StudIN brand palette – taken from the app icon (deep navy, mint glow, gold star). */
object StudinColors {
    val Navy950 = Color(0xFF030A1C)
    val Navy900 = Color(0xFF060E26)
    val Navy800 = Color(0xFF0B1636)
    val Navy700 = Color(0xFF122050)
    val Navy600 = Color(0xFF1B2D66)
    val Mint = Color(0xFF2EE6A6)
    val MintDeep = Color(0xFF14B88A)
    val Teal = Color(0xFF1FC8C0)
    val Cyan = Color(0xFF42C9F5)
    val Star = Color(0xFFFFD866)
    val Coral = Color(0xFFFF7A85)
    val Violet = Color(0xFF8E7CFF)
    val Ink = Color(0xFFEAF0FF)
    val InkMuted = Color(0xFF9AA8CC)
    val Paper = Color(0xFFF5F8FF)
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

val LocalStudinExtras = staticCompositionLocalOf {
    StudinExtras(
        Brush.linearGradient(listOf(StudinColors.Mint, StudinColors.Teal)),
        Brush.verticalGradient(listOf(StudinColors.Navy800, StudinColors.Navy950)),
        Color(0x1AFFFFFF), Color(0x26FFFFFF), StudinColors.Mint, StudinColors.Star, StudinColors.Coral,
        StudinColors.Star, StudinColors.InkMuted, true,
    )
}

private val DarkScheme = darkColorScheme(
    primary = StudinColors.Mint,
    onPrimary = StudinColors.Navy950,
    primaryContainer = Color(0xFF0E4D44),
    onPrimaryContainer = Color(0xFFB8FFE6),
    secondary = StudinColors.Cyan,
    onSecondary = StudinColors.Navy950,
    secondaryContainer = StudinColors.Navy600,
    onSecondaryContainer = StudinColors.Ink,
    tertiary = StudinColors.Star,
    onTertiary = StudinColors.Navy950,
    background = StudinColors.Navy900,
    onBackground = StudinColors.Ink,
    surface = StudinColors.Navy800,
    onSurface = StudinColors.Ink,
    surfaceVariant = StudinColors.Navy700,
    onSurfaceVariant = StudinColors.InkMuted,
    surfaceContainer = StudinColors.Navy800,
    surfaceContainerHigh = StudinColors.Navy700,
    surfaceContainerHighest = StudinColors.Navy600,
    surfaceContainerLow = Color(0xFF08122E),
    outline = Color(0xFF34457A),
    outlineVariant = Color(0xFF223263),
    error = StudinColors.Coral,
    onError = StudinColors.Navy950,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0D9E78),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F7E6),
    onPrimaryContainer = Color(0xFF00382A),
    secondary = Color(0xFF1A73B8),
    secondaryContainer = Color(0xFFD9E8FF),
    tertiary = Color(0xFFB98500),
    background = StudinColors.Paper,
    onBackground = Color(0xFF0B1636),
    surface = Color.White,
    onSurface = Color(0xFF0B1636),
    surfaceVariant = Color(0xFFE6ECFA),
    onSurfaceVariant = Color(0xFF4A5677),
    surfaceContainer = Color(0xFFF0F4FD),
    surfaceContainerHigh = Color(0xFFE8EEFB),
    outline = Color(0xFFB4C0DD),
    error = Color(0xFFD0384A),
)

private val StudinTypography = Typography().let { base ->
    val f = FontFamily.SansSerif
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = f, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        displayMedium = base.displayMedium.copy(fontFamily = f, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        displaySmall = base.displaySmall.copy(fontFamily = f, fontWeight = FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.copy(fontFamily = f, fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = f, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = f, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = f, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = f),
        bodyMedium = base.bodyMedium.copy(fontFamily = f),
        bodySmall = base.bodySmall.copy(fontFamily = f),
        labelLarge = base.labelLarge.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(fontFamily = f, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
    )
}

val StudinShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Composable
fun StudinTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
    val scheme = if (dark) DarkScheme else LightScheme
    val extras = if (dark) StudinExtras(
        heroGradient = Brush.linearGradient(listOf(StudinColors.Mint, StudinColors.Teal, StudinColors.Cyan)),
        backgroundGradient = Brush.verticalGradient(listOf(Color(0xFF0D1B47), StudinColors.Navy900, StudinColors.Navy950)),
        glass = Color(0x14FFFFFF), glassBorder = Color(0x22FFFFFF),
        success = StudinColors.Mint, warning = StudinColors.Star, danger = StudinColors.Coral,
        star = StudinColors.Star, muted = StudinColors.InkMuted, isDark = true,
    ) else StudinExtras(
        heroGradient = Brush.linearGradient(listOf(Color(0xFF12C894), Color(0xFF13A8B8), Color(0xFF2E8AE0))),
        backgroundGradient = Brush.verticalGradient(listOf(Color(0xFFE9F7F3), StudinColors.Paper, Color(0xFFEFF3FF))),
        glass = Color(0xCCFFFFFF), glassBorder = Color(0x1A0B1636),
        success = Color(0xFF0D9E78), warning = Color(0xFFB98500), danger = Color(0xFFD0384A),
        star = Color(0xFFE0A100), muted = Color(0xFF5B678A), isDark = false,
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalStudinExtras provides extras) {
        MaterialTheme(colorScheme = scheme, typography = StudinTypography, shapes = StudinShapes, content = content)
    }
}

object Studin {
    val extras: StudinExtras @Composable get() = LocalStudinExtras.current
}

val MonoNumbers = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
