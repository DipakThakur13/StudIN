package com.studin.app.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * StudIN "Warm Academic — Premium Edition" design tokens.
 * Every colour, size, radius and duration used by screens must come from here.
 * See docs/STUDIN_V2_DESIGN_SYSTEM.md.
 */

/** Raw palette. Screens must not use these directly – use MaterialTheme.colorScheme / StudINTheme.colors. */
internal object Palette {
    // Light
    const val Background = 0xFFFAFAF7
    const val Surface = 0xFFFFFFFF
    const val SurfaceSecondary = 0xFFF4F3EE
    const val Forest = 0xFF243F32
    const val ForestDark = 0xFF183126
    const val ForestMid = 0xFF355846
    const val ForestContainer = 0xFFDCE7DF
    const val Orange = 0xFFE66B38
    const val OrangeSoft = 0xFFF3A16D
    const val OrangeLight = 0xFFFFF0E7
    const val TextPrimary = 0xFF202823
    const val TextSecondary = 0xFF626D65
    const val Border = 0xFFE5E8E1
    const val Outline = 0xFFB9C4BB
    const val Success = 0xFF287A55
    const val SuccessContainer = 0xFFE3F1E8
    const val Warning = 0xFFB86B20
    const val WarningContainer = 0xFFFBEEDC
    const val Error = 0xFFB84040
    const val ErrorContainer = 0xFFF8E3E1
    const val Ai = 0xFF7865A5
    const val AiContainer = 0xFFEFEBF7
    const val FocusTrack = 0xFFE7EAE3

    // Dark
    const val DBackground = 0xFF111914
    const val DSurface = 0xFF1B2720
    const val DSurfaceSecondary = 0xFF243128
    const val DPrimary = 0xFFB4DEC0
    const val DOnPrimary = 0xFF0F2619
    const val DPrimaryContainer = 0xFF2F4A3B
    const val DAccent = 0xFFF4A273
    const val DAccentContainer = 0xFF3D2A1E
    const val DTextPrimary = 0xFFF4F6F1
    const val DTextSecondary = 0xFFBAC5BB
    const val DBorder = 0xFF34463A
    const val DOutline = 0xFF4E6354
    const val DSuccess = 0xFF81C8A0
    const val DSuccessContainer = 0xFF1F3A2B
    const val DWarning = 0xFFE8B66C
    const val DWarningContainer = 0xFF3B2E17
    const val DError = 0xFFFFAAA2
    const val DErrorContainer = 0xFF4E2421
    const val DAi = 0xFFC2B3EC
    const val DAiContainer = 0xFF2E2840
    const val DFocusTrack = 0xFF2A382F
}

/** Semantic colours that Material 3's scheme has no slot for. */
@Immutable
data class StudINColors(
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val ai: Color,
    val aiContainer: Color,
    val focusTrack: Color,
    val textSecondary: Color,
    val border: Color,
    val surfaceSecondary: Color,
    /** Exam countdown hero, active focus, milestones only (gradient policy §1.4). */
    val heroGradient: Brush,
    val onHero: Color,
    val accentGradient: Brush,
    val isDark: Boolean,
)

internal val LightStudINColors = StudINColors(
    accent = Color(Palette.Orange), onAccent = Color.White,
    accentContainer = Color(Palette.OrangeLight), onAccentContainer = Color(0xFF7A2E0E),
    success = Color(Palette.Success), successContainer = Color(Palette.SuccessContainer),
    warning = Color(Palette.Warning), warningContainer = Color(Palette.WarningContainer),
    ai = Color(Palette.Ai), aiContainer = Color(Palette.AiContainer),
    focusTrack = Color(Palette.FocusTrack), textSecondary = Color(Palette.TextSecondary),
    border = Color(Palette.Border), surfaceSecondary = Color(Palette.SurfaceSecondary),
    heroGradient = Brush.linearGradient(listOf(Color(Palette.Forest), Color(Palette.ForestMid))),
    onHero = Color(Palette.Background),
    accentGradient = Brush.linearGradient(listOf(Color(Palette.Orange), Color(Palette.OrangeSoft))),
    isDark = false,
)

internal val DarkStudINColors = StudINColors(
    accent = Color(Palette.DAccent), onAccent = Color(0xFF3A1708),
    accentContainer = Color(Palette.DAccentContainer), onAccentContainer = Color(0xFFFFD9C4),
    success = Color(Palette.DSuccess), successContainer = Color(Palette.DSuccessContainer),
    warning = Color(Palette.DWarning), warningContainer = Color(Palette.DWarningContainer),
    ai = Color(Palette.DAi), aiContainer = Color(Palette.DAiContainer),
    focusTrack = Color(Palette.DFocusTrack), textSecondary = Color(Palette.DTextSecondary),
    border = Color(Palette.DBorder), surfaceSecondary = Color(Palette.DSurfaceSecondary),
    heroGradient = Brush.linearGradient(listOf(Color(0xFF2C4A3A), Color(0xFF1E3529))),
    onHero = Color(Palette.DTextPrimary),
    accentGradient = Brush.linearGradient(listOf(Color(Palette.DAccent), Color(Palette.OrangeSoft))),
    isDark = true,
)

/** 4-dp based spacing scale (§1.6). */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val huge = 48.dp

    /** Default horizontal screen padding. */
    val screen = lg
    /** Internal card padding. */
    val card = md
    /** Space between sections. */
    val section = xl
    /** Space between related elements. */
    val related = xs
    /** Minimum interactive target. */
    val touchTarget = 48.dp
    /** Max readable content width on tablets / landscape. */
    val maxContentWidth = 720.dp
}

/** Corner radii (§1.7). */
object Radius {
    val card = 16.dp
    val smallCard = 12.dp
    val button = 12.dp
    val input = 12.dp
    val dialog = 24.dp
    val sheet = 24.dp
    val badge = 8.dp
}

object StudINShapes {
    val card = RoundedCornerShape(Radius.card)
    val smallCard = RoundedCornerShape(Radius.smallCard)
    val button = RoundedCornerShape(Radius.button)
    val input = RoundedCornerShape(Radius.input)
    val dialog = RoundedCornerShape(Radius.dialog)
    val sheet = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)
    val badge = RoundedCornerShape(Radius.badge)
}

internal val MaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.badge),
    small = RoundedCornerShape(Radius.smallCard),
    medium = RoundedCornerShape(Radius.card),
    large = RoundedCornerShape(Radius.card),
    extraLarge = RoundedCornerShape(Radius.dialog),
)

/** Motion durations in ms (§10). Use [rememberReducedMotion] to honour system settings. */
object Motion {
    const val small = 150
    const val card = 210
    const val screen = 260
    const val success = 380
}

private val Sans = FontFamily.SansSerif

/** Type scale (§1.5). Sizes are sp and scale with the user's font setting. */
internal val StudINTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 64.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.25).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

/** Extra text styles not covered by Material roles. */
object StudINType {
    /** Large focus timer digits – tabular numbers so the width doesn't jitter. */
    val timer = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 60.sp, fontFeatureSettings = "tnum", letterSpacing = (-1).sp)
    /** Hero metric values (36sp bold). */
    val heroValue = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, fontFeatureSettings = "tnum")
    /** Stat card values. */
    val statValue = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, fontFeatureSettings = "tnum")
}
