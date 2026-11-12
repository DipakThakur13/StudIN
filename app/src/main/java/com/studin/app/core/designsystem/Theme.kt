package com.studin.app.core.designsystem

import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** User appearance preference (persisted in DataStore). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightScheme = lightColorScheme(
    primary = Color(Palette.Forest),
    onPrimary = Color.White,
    primaryContainer = Color(Palette.ForestContainer),
    onPrimaryContainer = Color(Palette.ForestDark),
    inversePrimary = Color(Palette.DPrimary),
    secondary = Color(0xFF4F6B5C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3ECE5),
    onSecondaryContainer = Color(Palette.ForestDark),
    tertiary = Color(Palette.Orange),
    onTertiary = Color.White,
    tertiaryContainer = Color(Palette.OrangeLight),
    onTertiaryContainer = Color(0xFF7A2E0E),
    background = Color(Palette.Background),
    onBackground = Color(Palette.TextPrimary),
    surface = Color(Palette.Surface),
    onSurface = Color(Palette.TextPrimary),
    surfaceVariant = Color(Palette.SurfaceSecondary),
    onSurfaceVariant = Color(Palette.TextSecondary),
    surfaceTint = Color(Palette.Forest),
    inverseSurface = Color(0xFF2E3530),
    inverseOnSurface = Color(0xFFF4F6F1),
    error = Color(Palette.Error),
    onError = Color.White,
    errorContainer = Color(Palette.ErrorContainer),
    onErrorContainer = Color(0xFF5C1A1A),
    outline = Color(Palette.Outline),
    outlineVariant = Color(Palette.Border),
    scrim = Color.Black,
    surfaceBright = Color(Palette.Surface),
    surfaceDim = Color(0xFFE9E8E2),
    surfaceContainerLowest = Color(Palette.Surface),
    surfaceContainerLow = Color(0xFFFCFCFA),
    surfaceContainer = Color(Palette.Surface),
    surfaceContainerHigh = Color(Palette.SurfaceSecondary),
    surfaceContainerHighest = Color(0xFFEDECE6),
)

private val DarkScheme = darkColorScheme(
    primary = Color(Palette.DPrimary),
    onPrimary = Color(Palette.DOnPrimary),
    primaryContainer = Color(Palette.DPrimaryContainer),
    onPrimaryContainer = Color(0xFFD0EBD8),
    inversePrimary = Color(Palette.Forest),
    secondary = Color(0xFFA9C7B4),
    onSecondary = Color(0xFF14281D),
    secondaryContainer = Color(0xFF2C3D33),
    onSecondaryContainer = Color(0xFFD4E6DA),
    tertiary = Color(Palette.DAccent),
    onTertiary = Color(0xFF3A1708),
    tertiaryContainer = Color(Palette.DAccentContainer),
    onTertiaryContainer = Color(0xFFFFD9C4),
    background = Color(Palette.DBackground),
    onBackground = Color(Palette.DTextPrimary),
    surface = Color(Palette.DSurface),
    onSurface = Color(Palette.DTextPrimary),
    surfaceVariant = Color(Palette.DSurfaceSecondary),
    onSurfaceVariant = Color(Palette.DTextSecondary),
    surfaceTint = Color(Palette.DPrimary),
    inverseSurface = Color(0xFFE6EAE3),
    inverseOnSurface = Color(0xFF1B2720),
    error = Color(Palette.DError),
    onError = Color(0xFF561E1A),
    errorContainer = Color(Palette.DErrorContainer),
    onErrorContainer = Color(0xFFFFDAD5),
    outline = Color(Palette.DOutline),
    outlineVariant = Color(Palette.DBorder),
    scrim = Color.Black,
    surfaceBright = Color(0xFF2A372F),
    surfaceDim = Color(Palette.DBackground),
    surfaceContainerLowest = Color(0xFF0C130F),
    surfaceContainerLow = Color(0xFF161F19),
    surfaceContainer = Color(Palette.DSurface),
    surfaceContainerHigh = Color(Palette.DSurfaceSecondary),
    surfaceContainerHighest = Color(0xFF2D3B32),
)

val LocalStudINColors = staticCompositionLocalOf { LightStudINColors }

/** True when the user disabled animations (Settings → Accessibility → Remove animations). */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Entry point for StudIN-specific tokens: `StudINTheme.colors.accent`, `StudINTheme.reducedMotion`. */
object StudINTheme {
    val colors: StudINColors
        @Composable @ReadOnlyComposable get() = LocalStudINColors.current
    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}

/** Duration helper that collapses to 0 when the user prefers reduced motion. */
@Composable
@ReadOnlyComposable
fun motionDuration(ms: Int): Int = if (LocalReducedMotion.current) 0 else ms

@Composable
fun StudinTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
    val scheme = if (dark) DarkScheme else LightScheme
    val studin = if (dark) DarkStudINColors else LightStudINColors
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
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
    CompositionLocalProvider(
        LocalStudINColors provides studin,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = StudINTypography, shapes = MaterialShapes) {
            CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
        }
    }
}
