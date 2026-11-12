package com.studin.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val FOCUS = "focus"
    const val RULES = "rules"
    const val STUDY = "study"
    const val DECK = "deck/{deckId}"
    const val REVIEW = "review?deckId={deckId}"
    const val QUIZ = "quiz?deckId={deckId}"
    const val IMPORT = "import"
    const val GENERATE = "generate?deckId={deckId}"
    const val AI = "ai"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
    const val PRIVACY = "privacy"

    fun deck(id: Long) = "deck/$id"
    fun review(deckId: Long? = null) = "review?deckId=${deckId ?: -1}"
    fun quiz(deckId: Long? = null) = "quiz?deckId=${deckId ?: -1}"
    fun generate(deckId: Long? = null) = "generate?deckId=${deckId ?: -1}"
}

enum class TopLevel(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    FOCUS(Routes.FOCUS, "Focus", Icons.Outlined.Timer, Icons.Rounded.Timer),
    STUDY(Routes.STUDY, "Study", Icons.Outlined.MenuBook, Icons.Rounded.MenuBook),
    AI(Routes.AI, "AI Tutor", Icons.Outlined.AutoAwesome, Icons.Rounded.AutoAwesome),
    INSIGHTS(Routes.INSIGHTS, "Insights", Icons.Outlined.Insights, Icons.Rounded.Insights),
}
