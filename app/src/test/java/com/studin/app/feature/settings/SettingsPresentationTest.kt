package com.studin.app.feature.settings

import com.studin.app.core.designsystem.ThemeMode
import com.studin.app.platform.permissions.StudinPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SettingsPresentationTest {
    @Test fun hourLabels() {
        assertEquals("12 AM", hourLabel(0))
        assertEquals("9 AM", hourLabel(9))
        assertEquals("12 PM", hourLabel(12))
        assertEquals("6 PM", hourLabel(18))
    }

    @Test fun themeLabelsCoverEveryMode() {
        assertEquals(listOf("Follow system", "Light", "Dark"), ThemeMode.entries.map(::themeLabel))
    }

    @Test fun examDayFormatting() {
        assertEquals("Not set", formatExamDay(null))
        assertEquals("3 Apr 2027", formatExamDay(LocalDate.of(2027, 4, 3).toEpochDay()))
    }

    @Test fun everyPermissionExplainsTheDeniedState() {
        StudinPermission.entries.forEach { assertTrue(it.name, it.whenOff().startsWith("While off:")) }
    }

    @Test fun permissionSummary() {
        assertEquals("Everything is set up", permissionSummary(5, 5))
        assertEquals("Core features are ready", permissionSummary(2, 5))
        assertEquals("Timer, flashcards and AI work now", permissionSummary(0, 5))
    }

    @Test fun privacyPolicyCoversRequiredTopics() {
        val titles = privacySections.map { it.title }
        listOf("Your data stays on your device", "Focus Guard (Accessibility) disclosure", "Retention", "Your controls")
            .forEach { assertTrue(it, it in titles) }
    }
}
