package com.studin.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.studin.app.core.data.prefs.AppSettings
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.designsystem.StudinTheme
import com.studin.app.ui.navigation.Routes
import com.studin.app.ui.navigation.StudinNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var prefs: UserPreferences

    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Tiny, local DataStore read; decides the first screen without a flash.
        val initial: AppSettings = runBlocking { prefs.settings.first() }
        pendingRoute.value = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            val settings by prefs.settings.collectAsStateWithLifecycle(initial)
            val route by pendingRoute.collectAsStateWithLifecycle()
            StudinTheme(settings.themeMode) {
                val nav = rememberNavController()
                StudinNavHost(nav, if (initial.onboardingDone) Routes.HOME else Routes.ONBOARDING)
                LaunchedEffect(route) {
                    val r = route ?: return@LaunchedEffect
                    if (settings.onboardingDone) nav.navigate(r) { launchSingleTop = true }
                    pendingRoute.value = null
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    companion object { const val EXTRA_ROUTE = "studin.route" }
}
