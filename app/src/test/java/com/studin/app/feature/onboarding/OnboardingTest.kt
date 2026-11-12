package com.studin.app.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {
    @Test fun goalSnapsToQuarterHoursWithinRange() {
        assertEquals(30, snapGoal(0f)); assertEquals(30, snapGoal(37f)); assertEquals(45, snapGoal(38f))
        assertEquals(120, snapGoal(121f)); assertEquals(480, snapGoal(999f))
    }

    @Test fun introSlidesExplainEveryPillarWithAccessibleImages() {
        assertEquals(listOf("Welcome to StudIN", "Focus", "Remember", "AI Tutor", "Progress"), IntroSlides.map { it.eyebrow })
        IntroSlides.forEach {
            assertTrue(it.title.isNotBlank()); assertTrue(it.body.length in 40..220)
            assertTrue("image needs a description", it.imageDescription.length > 10)
        }
        assertEquals(IntroSlides.size, IntroSlides.map { it.image }.toSet().size)
    }
}
