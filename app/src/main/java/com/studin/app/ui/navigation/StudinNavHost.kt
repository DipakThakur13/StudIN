package com.studin.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.studin.app.core.designsystem.GradientBackground
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors
import com.studin.app.feature.ai.AiScreen
import com.studin.app.feature.focus.FocusScreen
import com.studin.app.feature.focus.RulesScreen
import com.studin.app.feature.home.HomeScreen
import com.studin.app.feature.insights.InsightsScreen
import com.studin.app.feature.onboarding.OnboardingScreen
import com.studin.app.feature.settings.PermissionsScreen
import com.studin.app.feature.settings.PrivacyScreen
import com.studin.app.feature.settings.SettingsScreen
import com.studin.app.feature.study.DeckScreen
import com.studin.app.feature.study.GenerateCardsScreen
import com.studin.app.feature.study.ImportScreen
import com.studin.app.feature.study.QuizScreen
import com.studin.app.feature.study.ReviewScreen
import com.studin.app.feature.study.StudyScreen

@Composable
fun StudinNavHost(nav: NavHostController, startDestination: String) {
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = TopLevel.entries.any { it.route == route }
    val navigate: (String) -> Unit = { r ->
        if (TopLevel.entries.any { it.route == r }) nav.navigateTopLevel(r) else nav.navigate(r) { launchSingleTop = true }
    }
    val back: () -> Unit = { if (!nav.popBackStack()) nav.navigateTopLevel(Routes.HOME) }

    GradientBackground {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 12 } },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(260)) },
                popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(320)) { it / 12 } },
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onFinished = {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    })
                }
                composable(Routes.HOME) { HomeScreen(onNavigate = navigate) }
                composable(Routes.FOCUS) { FocusScreen(onNavigate = navigate) }
                composable(Routes.RULES) { RulesScreen(onBack = back) }
                composable(Routes.STUDY) { StudyScreen(onNavigate = navigate) }
                composable(Routes.DECK, arguments = listOf(navArgument("deckId") { type = NavType.LongType })) {
                    DeckScreen(deckId = it.arguments?.getLong("deckId") ?: -1L, onNavigate = navigate, onBack = back)
                }
                composable(Routes.REVIEW, arguments = listOf(navArgument("deckId") { type = NavType.LongType; defaultValue = -1L })) {
                    ReviewScreen(deckId = it.arguments?.getLong("deckId")?.takeIf { id -> id > 0 }, onBack = back)
                }
                composable(Routes.QUIZ, arguments = listOf(navArgument("deckId") { type = NavType.LongType; defaultValue = -1L })) {
                    QuizScreen(deckId = it.arguments?.getLong("deckId")?.takeIf { id -> id > 0 }, onBack = back)
                }
                composable(Routes.IMPORT) { ImportScreen(onNavigate = navigate, onBack = back) }
                composable(Routes.GENERATE, arguments = listOf(navArgument("deckId") { type = NavType.LongType; defaultValue = -1L })) {
                    GenerateCardsScreen(deckId = it.arguments?.getLong("deckId")?.takeIf { id -> id > 0 }, onNavigate = navigate, onBack = back)
                }
                composable(Routes.AI) { AiScreen(onNavigate = navigate) }
                composable(Routes.INSIGHTS) { InsightsScreen(onNavigate = navigate) }
                composable(Routes.SETTINGS) { SettingsScreen(onNavigate = navigate, onBack = back) }
                composable(Routes.PERMISSIONS) { PermissionsScreen(onBack = back) }
                composable(Routes.PRIVACY) { PrivacyScreen(onBack = back) }
            }
            AnimatedVisibility(
                visible = showBar,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                StudinBottomBar(current = route, onSelect = { nav.navigateTopLevel(it.route) })
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Floating glass bottom bar. Screens should add [BottomBarPadding] at the end of scroll content. */
@Composable
fun StudinBottomBar(current: String?, onSelect: (TopLevel) -> Unit) {
    val ex = Studin.extras
    Box(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(if (ex.isDark) Color(0xE60B1636) else Color(0xF2FFFFFF))
                .border(1.dp, ex.glassBorder, RoundedCornerShape(26.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            TopLevel.entries.forEach { item ->
                val selected = current == item.route
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                        .background(if (selected) Brush.linearGradient(listOf(StudinColors.Mint.copy(alpha = 0.22f), StudinColors.Cyan.copy(alpha = 0.12f))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                        .clickable(remember { MutableInteractionSource() }, null, role = Role.Tab) { onSelect(item) }
                        .semantics { this.selected = selected }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        if (selected) item.selectedIcon else item.icon, contentDescription = item.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary else ex.muted, modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else ex.muted, maxLines = 1)
                }
            }
        }
    }
}

/** Space to leave at the bottom of top-level screens so content clears the floating bar. */
val BottomBarPadding = 110.dp
