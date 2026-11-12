package com.studin.app

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.WorkManagerTestInitHelper
import com.github.takahirom.roborazzi.captureRoboImage
import com.studin.app.core.data.db.FocusSessionEntity
import com.studin.app.core.data.repo.Profile
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Renders every main screen of the real app (real Hilt graph, Room, DataStore) on the
 * JVM and writes PNGs to app/build/screenshots. Doubles as a smoke test: any crash
 * while composing a screen fails the build.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h880dp-xxhdpi")
class ScreenshotTour {
    @get:Rule val compose = createEmptyComposeRule()

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun shot(name: String, route: String?) {
        val intent = Intent(ctx, MainActivity::class.java).apply { if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route) }
        ActivityScenario.launch<MainActivity>(intent).use {
            // Room/DataStore emit on real background threads: give them real time to settle.
            repeat(6) {
                Thread.sleep(350)
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                compose.mainClock.advanceTimeBy(400)
                compose.waitForIdle()
            }
            compose.onRoot().captureRoboImage("build/screenshots/$name.png")
        }
    }

    @Test fun tour() {
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx)
        shot("01_onboarding", null)

        val ep = EntryPointAccessors.fromApplication(ctx, com.studin.app.core.di.AppEntryPoint::class.java)
        runBlocking {
            ep.profiles().save(Profile("Aarav", ExamType.JEE, LocalDate.now().plusDays(142).toEpochDay(), 240))
            ep.prefs().setOnboardingDone(true)
            val now = System.currentTimeMillis()
            val day = 86_400_000L
            val topics = ep.study().topics()
            for (d in 0 until 40) {
                if (d % 6 == 5) continue
                val mins = listOf(25, 50, 90, 45, 30, 75)[d % 6]
                val id = ep.focus().insertSession(
                    FocusSessionEntity(topicId = topics[d % topics.size].id, label = topics[d % topics.size].title, plannedMinutes = mins,
                        startElapsed = 0, startWallTime = now - d * day - 3 * 3_600_000L, endWallTime = now - d * day,
                        status = FocusState.COMPLETED.name, focusedMs = mins * 60_000L, blockedAttempts = d % 3)
                )
                if (d < 12) ep.study().recordTopicStudy(topics[(d * 7) % topics.size].id, mins * 3)
                check(id > 0)
            }
            ep.study().dueCards().take(6).forEachIndexed { i, c -> ep.study().review(c, if (i % 3 == 0) Grade.AGAIN else Grade.GOOD) }
            ep.focus().setRule("com.google.android.youtube", "YouTube", RuleMode.SMART)
            ep.focus().setRule("com.instagram.android", "Instagram", RuleMode.BLOCK)
        }
        shot("02_home", null)
        shot("03_focus", Routes.FOCUS)
        shot("04_study", Routes.STUDY)
        shot("05_review", Routes.review())
        shot("06_quiz", Routes.quiz())
        shot("07_generate", Routes.generate())
        shot("08_ai", Routes.AI)
        shot("09_insights", Routes.INSIGHTS)
        shot("10_settings", Routes.SETTINGS)
        shot("11_permissions", Routes.PERMISSIONS)
        shot("12_rules", Routes.RULES)
        shot("13_import", Routes.IMPORT)
    }
}
