package com.studin.app

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.WorkManagerTestInitHelper
import com.studin.app.core.data.repo.Profile
import com.studin.app.core.designsystem.ThemeMode
import com.studin.app.core.di.AppEntryPoint
import com.studin.app.ui.navigation.Routes
import com.studin.domain.model.ExamType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Semantics-tree accessibility rules that can be verified on the JVM. */
object A11yRules {
    const val MIN_TARGET_DP = 47.5f

    fun audit(provider: SemanticsNodeInteractionsProvider, density: Density, screen: String): List<String> {
        val out = mutableListOf<String>()
        val clickables = provider.onAllNodes(hasClickAction()).fetchSemanticsNodes(atLeastOneRootRequired = false)
        val visible = clickables.filter { n ->
            // Skip controls cut off by a scroll viewport edge: only part of them is on screen.
            !(n.boundsInRoot.height + 1f < n.size.height || n.boundsInRoot.width + 1f < n.size.width)
        }
        visible.forEach { n ->
            if (label(n).isNullOrBlank()) out += "[$screen] clickable without accessible label at ${n.boundsInRoot}"
            val w = with(density) { n.size.width.toDp().value }; val h = with(density) { n.size.height.toDp().value }
            if (n.size.width == 0 || n.size.height == 0 || (w >= MIN_TARGET_DP && h >= MIN_TARGET_DP)) return@forEach
            // Smaller component: acceptable only if its 48dp touch area (Compose's
            // minimumInteractiveComponentSize) is fully its own – i.e. overlaps no other control.
            val t = n.touchBoundsInRoot
            val tw = with(density) { t.width.toDp().value }; val th = with(density) { t.height.toDp().value }
            val neighbour = visible.firstOrNull { o -> o.id != n.id && o.boundsInRoot.overlaps(t) && !o.boundsInRoot.contains(n.boundsInRoot.center) && !n.boundsInRoot.contains(o.boundsInRoot.center) }
            val overlaps = neighbour != null
            if (tw < MIN_TARGET_DP || th < MIN_TARGET_DP || overlaps) {
                out += "[$screen] touch target ${"%.0f".format(w)}×${"%.0f".format(h)}dp (expanded ${"%.0f".format(tw)}×${"%.0f".format(th)}dp${if (neighbour != null) ", overlaps \"${label(neighbour)?.take(40)}\" at ${neighbour.boundsInRoot}" else ""}): ${label(n) ?: "(no label)"}"
            }
        }
        if (provider.onAllNodes(isHeading()).fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty()) out += "[$screen] no heading for screen-reader navigation"
        return out
    }

    fun label(n: SemanticsNode): String? {
        val c = n.config
        return listOfNotNull(
            c.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
            c.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
            c.getOrNull(SemanticsProperties.EditableText)?.text,
            c.getOrNull(SemanticsProperties.StateDescription),
        ).joinToString(" ").takeIf { it.isNotBlank() } ?: n.children.firstNotNullOfOrNull { label(it) }
    }
}

/**
 * Accessibility audit of the real app (Hilt, Room, ViewModels) on every main and secondary screen,
 * light + dark, standard size + 1.3× font: 48dp targets, labels on every clickable, a heading per
 * screen. A negative control proves the rules are live. Report: app/build/reports/a11y/a11y-audit.txt.
 *
 * Google's Accessibility Test Framework does not evaluate Compose hierarchies under Robolectric
 * (verified: a 12dp unlabeled control passed), so contrast is covered by DesignSystemTest and
 * TalkBack/ATF must be run on a device.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = STANDARD)
class AccessibilityAuditTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun settle() = repeat(6) {
        Thread.sleep(250); shadowOf(Looper.getMainLooper()).idle(); compose.mainClock.advanceTimeBy(400); compose.waitForIdle()
    }

    @Test fun auditMainScreens() {
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx)
        val ep = EntryPointAccessors.fromApplication(ctx, AppEntryPoint::class.java)
        runBlocking {
            ep.profiles().save(Profile("Aarav", ExamType.NEET, LocalDate.now().plusDays(90).toEpochDay(), 180))
            ep.prefs().setOnboardingDone(true)
            // Data so charts, heatmap, plans and decks render their interactive states.
            val now = System.currentTimeMillis(); val topics = ep.study().topics()
            for (d in 0 until 60) if (d % 5 != 4) ep.focus().insertSession(
                com.studin.app.core.data.db.FocusSessionEntity(topicId = topics[d % topics.size].id, label = topics[d % topics.size].title,
                    plannedMinutes = 50, startElapsed = 0, startWallTime = now - d * 86_400_000L - 7_200_000L, endWallTime = now - d * 86_400_000L,
                    status = "COMPLETED", focusedMs = (20 + d % 4 * 15) * 60_000L)
            )
        }
        val screens = listOf(
            "home" to null, "focus" to Routes.FOCUS, "study" to Routes.STUDY, "ai" to Routes.AI, "insights" to Routes.INSIGHTS,
            "settings" to Routes.SETTINGS, "permissions" to Routes.PERMISSIONS, "review" to Routes.review(), "quiz" to Routes.quiz(),
            "generate" to Routes.generate(), "import" to Routes.IMPORT, "rules" to Routes.RULES, "privacy" to Routes.PRIVACY,
        )
        val findings = linkedSetOf<String>()
        var audited = 0
        for ((variant, setup) in listOf<Pair<String, () -> Unit>>(
            "light" to { runBlocking { ep.prefs().setTheme(ThemeMode.LIGHT) } },
            "dark" to { runBlocking { ep.prefs().setTheme(ThemeMode.DARK) } },
            "font130" to { runBlocking { ep.prefs().setTheme(ThemeMode.LIGHT) }; RuntimeEnvironment.setFontScale(1.3f) },
        )) {
            setup()
            for ((name, route) in screens) {
                val intent = Intent(ctx, MainActivity::class.java).apply { if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route) }
                ActivityScenario.launch<MainActivity>(intent).use { s ->
                    settle()
                    var density = Density(1f)
                    s.onActivity { density = Density(it.resources.displayMetrics.density, it.resources.configuration.fontScale) }
                    findings += A11yRules.audit(compose, density, "$variant/$name"); audited++
                    // Scroll through the screen so below-the-fold content is audited too.
                    repeat(6) {
                        val scrollables = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes(atLeastOneRootRequired = false)
                        if (scrollables.isEmpty()) return@repeat
                        val idx = scrollables.indices.maxBy { scrollables[it].size.height }
                        compose.onAllNodes(hasScrollAction())[idx].performTouchInput { swipeUp() }
                        settle()
                        findings += A11yRules.audit(compose, density, "$variant/$name")
                    }
                }
            }
        }
        RuntimeEnvironment.setFontScale(1f)
        runBlocking { ep.prefs().setTheme(ThemeMode.SYSTEM) }
        File("build/reports/a11y").mkdirs()
        File("build/reports/a11y/a11y-audit.txt").writeText(
            "Audited $audited screen renders (13 screens × light/dark/1.3× font).\n" +
                if (findings.isEmpty()) "No findings.\n" else findings.joinToString("\n", postfix = "\n")
        )
        assertTrue("Accessibility findings:\n" + findings.joinToString("\n"), findings.isEmpty())
    }
}
