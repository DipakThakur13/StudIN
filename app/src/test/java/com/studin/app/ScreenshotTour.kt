package com.studin.app

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.WorkManagerTestInitHelper
import com.github.takahirom.roborazzi.captureRoboImage
import com.studin.app.core.data.db.FocusSessionEntity
import com.studin.app.core.data.repo.Profile
import com.studin.app.core.designsystem.ThemeMode
import com.studin.app.core.di.AppEntryPoint
import com.studin.app.ui.navigation.Routes
import com.studin.domain.model.ExamType
import com.studin.domain.model.FocusState
import com.studin.domain.model.Grade
import com.studin.domain.model.RuleMode
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Screenshot validation matrix (V2 spec, phase 15). Renders the real app – real Hilt graph,
 * Room, DataStore, ViewModels – on the JVM with Robolectric native graphics and writes PNGs to
 * app/build/screenshots/<variant>/<name>.png. Any crash while composing fails the build, so it
 * doubles as an end-to-end smoke test for onboarding, navigation, focus state and AI quick actions.
 *
 * Not covered here (needs a device): soft keyboard, TalkBack, real OEM behaviour.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = STANDARD)
class ScreenshotTour {
    @get:Rule val compose = createEmptyComposeRule()

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val ep get() = EntryPointAccessors.fromApplication(ctx, AppEntryPoint::class.java)
    private var scenario: ActivityScenario<MainActivity>? = null

    /** Room/DataStore emit on real background threads: give them real time, then idle Compose. */
    private fun settle(rounds: Int = 6) = repeat(rounds) {
        Thread.sleep(300)
        shadowOf(Looper.getMainLooper()).idle()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
    }

    private fun launch(route: String? = null) {
        scenario?.close()
        val intent = Intent(ctx, MainActivity::class.java).apply { if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route) }
        scenario = ActivityScenario.launch(intent)
        settle()
    }

    private fun capture(variant: String, name: String) = compose.onRoot().captureRoboImage("build/screenshots/$variant/$name.png")

    private fun shot(variant: String, name: String, route: String?) { launch(route); capture(variant, name) }

    private fun clickText(text: String): Boolean {
        val nodes = compose.onAllNodesWithText(text, substring = false, ignoreCase = false)
        if (nodes.fetchSemanticsNodes().isEmpty()) return false
        nodes[0].performClick(); settle(3); return true
    }

    private val mainScreens = listOf(
        "home" to null, "focus" to Routes.FOCUS, "study" to Routes.STUDY, "ai" to Routes.AI, "insights" to Routes.INSIGHTS,
    )

    @Test fun tour() {
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx)

        // ---------------- Onboarding: intro story + setup, driven through the real UI
        launch()
        for (i in 1..5) { capture("light", "onboarding_${i}_intro"); if (i < 5) clickText("Next") }
        clickText("Set up StudIN"); capture("light", "onboarding_6_exam")
        clickText("JEE Main / Advanced"); capture("light", "onboarding_6b_exam_selected")
        clickText("Continue"); capture("light", "onboarding_7_goal")
        clickText("Continue"); capture("light", "onboarding_8_ready")
        clickText("Start studying"); settle()

        // ---------------- Empty states (profile exists, no activity yet)
        shot("light", "home_empty", null)
        shot("light", "insights_empty", Routes.INSIGHTS)

        // ---------------- Seed realistic data
        runBlocking {
            ep.profiles().save(Profile("Aarav", ExamType.JEE, LocalDate.now().plusDays(142).toEpochDay(), 240))
            val now = System.currentTimeMillis(); val day = 86_400_000L
            val topics = ep.study().topics()
            for (d in 0 until 60) {
                if (d % 6 == 5) continue
                val mins = listOf(25, 50, 90, 45, 30, 75)[d % 6]
                ep.focus().insertSession(
                    FocusSessionEntity(topicId = topics[d % topics.size].id, label = topics[d % topics.size].title, plannedMinutes = mins,
                        startElapsed = 0, startWallTime = now - d * day - 3 * 3_600_000L, endWallTime = now - d * day,
                        status = FocusState.COMPLETED.name, focusedMs = mins * 60_000L, blockedAttempts = d % 3)
                )
                if (d < 12) ep.study().recordTopicStudy(topics[(d * 7) % topics.size].id, mins * 3)
            }
            ep.study().dueCards().take(6).forEachIndexed { i, c -> ep.study().review(c, if (i % 3 == 0) Grade.AGAIN else Grade.GOOD) }
            ep.focus().setRule("com.google.android.youtube", "YouTube", RuleMode.SMART)
            ep.focus().setRule("com.instagram.android", "Instagram", RuleMode.BLOCK)
        }

        // ---------------- Light theme, standard phone: every screen
        mainScreens.forEach { (n, r) -> shot("light", n, r) }
        listOf(
            "review" to Routes.review(), "quiz" to Routes.quiz(), "generate" to Routes.generate(), "import" to Routes.IMPORT,
            "settings" to Routes.SETTINGS, "permissions" to Routes.PERMISSIONS, "privacy" to Routes.PRIVACY, "rules" to Routes.RULES,
        ).forEach { (n, r) -> shot("light", n, r) }
        runBlocking { ep.study().allDecks().firstOrNull()?.let { shot("light", "deck", Routes.deck(it.id)) } }

        // ---------------- States: review revealed, AI conversation, focus lifecycle
        launch(Routes.review()); if (clickText("Show answer")) capture("light", "review_revealed")
        launch(Routes.AI)
        if (clickText("Plan my revision")) { settle(10); capture("light", "ai_conversation") }
        launch(Routes.INSIGHTS); if (clickText("30D")) capture("light", "insights_30d"); if (clickText("12W")) capture("light", "insights_12w")

        val fc = ep.focusController()
        fc.start(25, null, "Kinematics"); settle()
        shot("light", "focus_active", Routes.FOCUS)
        fc.pause(); settle(); shot("light", "focus_paused", Routes.FOCUS)
        fc.resume(); settle(); fc.finishEarly(); settle(); shot("light", "focus_completed", Routes.FOCUS)
        fc.dismiss(); settle()

        // ---------------- Dark theme
        runBlocking { ep.prefs().setTheme(ThemeMode.DARK) }
        mainScreens.forEach { (n, r) -> shot("dark", n, r) }
        shot("dark", "settings", Routes.SETTINGS)
        runBlocking { ep.prefs().setTheme(ThemeMode.LIGHT) }

        // ---------------- Window sizes
        listOf("compact" to COMPACT, "large" to LARGE, "tablet" to TABLET).forEach { (variant, q) ->
            RuntimeEnvironment.setQualifiers(q)
            mainScreens.forEach { (n, r) -> shot(variant, n, r) }
        }
        RuntimeEnvironment.setQualifiers(STANDARD)

        // ---------------- Increased font scale
        RuntimeEnvironment.setFontScale(1.3f)
        mainScreens.forEach { (n, r) -> shot("font130", n, r) }
        RuntimeEnvironment.setFontScale(1f)
        scenario?.close()
    }
}

/** Typical 6.4" phone. */
const val STANDARD = "w400dp-h880dp-xxhdpi"
/** Small/compact phone (e.g. 5" 720p). */
const val COMPACT = "w360dp-h640dp-xhdpi"
/** Large phone. */
const val LARGE = "w480dp-h1000dp-xxhdpi"
/** 10" tablet, landscape-ish width → navigation rail. */
const val TABLET = "w900dp-h1280dp-xhdpi"
