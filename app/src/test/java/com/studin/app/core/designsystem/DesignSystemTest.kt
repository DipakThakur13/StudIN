package com.studin.app.core.designsystem

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTest {
    @Test fun niceStepGivesRoundAxisLabels() {
        assertEquals(1, niceStep(1))
        assertEquals(50, niceStep(90))      // 0 / 50 / 100
        assertEquals(200, niceStep(240))    // 0 / 200 / 400 – goal 240 fits
        assertEquals(1000, niceStep(1680))  // weekly totals
        for (max in 1..5000) {
            val step = niceStep(max)
            val top = ((max + step - 1) / step) * step
            assertTrue("top >= max for $max", top >= max)
            assertTrue("no more than ~4 gridlines for $max", top / step <= 5)
        }
    }

    @Test fun spacingScaleIsOn4dpGrid() {
        listOf(Spacing.xxs, Spacing.xs, Spacing.sm, Spacing.md, Spacing.lg, Spacing.xl, Spacing.xxl, Spacing.xxxl, Spacing.huge)
            .forEach { assertEquals(0f, it.value % 4f, 0f) }
        assertEquals(20.dp, Spacing.screen); assertEquals(16.dp, Spacing.card); assertEquals(24.dp, Spacing.section)
        assertEquals(48.dp, Spacing.touchTarget)
    }

    @Test fun radiiMatchSpec() {
        assertEquals(16.dp, Radius.card); assertEquals(12.dp, Radius.smallCard); assertEquals(12.dp, Radius.button)
        assertEquals(12.dp, Radius.input); assertEquals(24.dp, Radius.dialog); assertEquals(24.dp, Radius.sheet); assertEquals(8.dp, Radius.badge)
    }

    @Test fun motionDurationsWithinSpecRanges() {
        assertTrue(Motion.small in 120..180); assertTrue(Motion.card in 180..240)
        assertTrue(Motion.screen in 220..300); assertTrue(Motion.success in 300..450)
    }

    @Test fun textContrastMeetsWcagAA() {
        fun lum(c: Long): Double {
            fun ch(v: Long): Double { val s = v / 255.0; return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4) }
            return 0.2126 * ch((c shr 16) and 0xFF) + 0.7152 * ch((c shr 8) and 0xFF) + 0.0722 * ch(c and 0xFF)
        }
        fun contrast(a: Long, b: Long): Double { val l1 = lum(a); val l2 = lum(b); return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05) }
        // Body and secondary text ≥ 4.5:1 on background and surfaces, both themes.
        listOf(Palette.Background, Palette.Surface, Palette.SurfaceSecondary).forEach {
            assertTrue(contrast(Palette.TextPrimary, it) >= 4.5); assertTrue(contrast(Palette.TextSecondary, it) >= 4.5)
            assertTrue("primary on bg", contrast(Palette.Forest, it) >= 4.5)
        }
        listOf(Palette.DBackground, Palette.DSurface, Palette.DSurfaceSecondary).forEach {
            assertTrue(contrast(Palette.DTextPrimary, it) >= 4.5); assertTrue(contrast(Palette.DTextSecondary, it) >= 4.5)
            assertTrue("dark primary", contrast(Palette.DPrimary, it) >= 4.5)
        }
        // White text on forest buttons; white on orange accent is a large-text/UI-component pair (≥ 3:1).
        assertTrue(contrast(0xFFFFFFFF, Palette.Forest) >= 4.5)
        assertTrue(contrast(0xFFFFFFFF, Palette.Orange) >= 3.0)
    }
}
