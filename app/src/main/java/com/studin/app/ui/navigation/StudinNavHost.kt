package com.studin.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.studin.app.core.designsystem.Motion
import com.studin.app.core.designsystem.StudINLogo
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.motionDuration
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

/** Width at which the navigation bar becomes a navigation rail (Material window-size "medium"). */
private const val RAIL_MIN_WIDTH_DP = 600

/**
 * App shell: Material 3 Scaffold with a standard NavigationBar on compact widths and a
 * NavigationRail on medium/expanded widths. The bar is part of the layout (never an overlay),
 * and its insets are consumed before screens apply their own, so nothing is padded twice.
 */
@Composable
fun StudinNavHost(nav: NavHostController, startDestination: String) {
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val showNav = destination?.route in Routes.TOP_LEVEL
    val useRail = LocalConfiguration.current.screenWidthDp >= RAIL_MIN_WIDTH_DP
    val navigate: (String) -> Unit = { r ->
        if (r in Routes.TOP_LEVEL) nav.navigateTopLevel(r) else nav.navigate(r) { launchSingleTop = true }
    }
    val back: () -> Unit = { if (!nav.popBackStack()) nav.navigateTopLevel(Routes.HOME) }
    val isSelected: (TopLevel) -> Boolean = { item -> destination?.hierarchy?.any { it.route == item.route } == true }
    val enterMs = motionDuration(Motion.screen)
    val exitMs = motionDuration(Motion.small)
    val select: (TopLevel) -> Unit = { item -> if (!isSelected(item)) nav.navigateTopLevel(item.route) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (showNav && !useRail) StudinNavigationBar(isSelected, select) },
    ) { inner ->
        Row(Modifier.fillMaxSize().padding(inner).consumeWindowInsets(inner)) {
            if (showNav && useRail) {
                StudinNavigationRail(isSelected, select)
                VerticalDivider(color = StudINTheme.colors.border)
            }
            NavHost(
                navController = nav,
                startDestination = startDestination,
                modifier = Modifier.weight(1f).fillMaxSize(),
                enterTransition = { fadeIn(tween(enterMs)) },
                exitTransition = { fadeOut(tween(exitMs)) },
                popEnterTransition = { fadeIn(tween(enterMs)) },
                popExitTransition = { fadeOut(tween(exitMs)) },
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onFinished = {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    })
                }
                composable(Routes.HOME) { HomeScreen(onNavigate = navigate) }
                composable(Routes.FOCUS) { FocusScreen(onNavigate = navigate) }
                composable(Routes.RULES) { RulesScreen(onBack = back, onOpenPermissions = { navigate(Routes.PERMISSIONS) }) }
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
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun StudinNavigationBar(isSelected: (TopLevel) -> Boolean, onSelect: (TopLevel) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        TopLevel.entries.forEach { item ->
            val selected = isSelected(item)
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(item) },
                icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                label = { Text(item.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = StudINTheme.colors.textSecondary,
                    unselectedTextColor = StudINTheme.colors.textSecondary,
                ),
            )
        }
    }
}

@Composable
private fun StudinNavigationRail(isSelected: (TopLevel) -> Boolean, onSelect: (TopLevel) -> Unit) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface, header = { StudINLogo(size = 40.dp) }) {
        TopLevel.entries.forEach { item ->
            val selected = isSelected(item)
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(item) },
                icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                label = { Text(item.label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = StudINTheme.colors.textSecondary,
                    unselectedTextColor = StudINTheme.colors.textSecondary,
                ),
            )
        }
    }
}
