package com.studin.app.core.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.designsystem.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = android.app.Application::class)
class ThemePreferenceTest {
    @Test fun themeDefaultsToSystemAndPersists(): Unit = runBlocking {
        val prefs = UserPreferences(ApplicationProvider.getApplicationContext())
        prefs.clearAll()
        assertEquals(ThemeMode.SYSTEM, prefs.settings.first().themeMode)
        ThemeMode.entries.forEach { mode ->
            prefs.setTheme(mode)
            assertEquals(mode, prefs.settings.first().themeMode)
        }
        prefs.clearAll()
    }
}
