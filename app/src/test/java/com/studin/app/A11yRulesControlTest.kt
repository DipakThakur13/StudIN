package com.studin.app

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.studin.app.core.designsystem.StudINPrimaryButton
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Negative/positive controls proving [A11yRules] actually detects problems. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = android.app.Application::class)
class A11yRulesControlTest {
    @get:Rule val compose = createComposeRule()

    @Test fun badControlIsReported() {
        // Raw semantics click: no automatic 48dp touch-area expansion, no label.
        // Two adjacent 12dp raw targets: expanded touch areas collide, and neither has a label.
        compose.setContent {
            Row {
                Box(Modifier.size(12.dp).background(Color.Gray).semantics { onClick { true } })
                Box(Modifier.size(12.dp).background(Color.DarkGray).semantics { onClick { true } })
            }
        }
        val f = A11yRules.audit(compose, compose.density, "control")
        assertTrue(f.toString(), f.any { "touch target" in it })
        assertTrue(f.toString(), f.any { "without accessible label" in it })
        assertTrue(f.toString(), f.any { "no heading" in it })
    }

    @Test fun studinComponentsPass() {
        compose.setContent { StudinTheme { Column { StudINSectionHeader("Today"); StudINPrimaryButton("Start focus", onClick = {}) } } }
        assertEquals(emptyList<String>(), A11yRules.audit(compose, compose.density, "good"))
    }
}
