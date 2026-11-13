package com.studin.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RAM tiers (what Android reports, not the box size) → which model StudIN offers. */
class ModelCatalogTest {
    private fun gb(v: Double) = (v * 1e9).toLong()

    @Test fun fourAndSixGbPhonesGetQwen25() {
        assertEquals(ModelCatalog.QWEN25_1_5B, ModelCatalog.recommendedFor(gb(3.6), is64Bit = true)) // 4 GB phone
        assertEquals(ModelCatalog.QWEN25_1_5B, ModelCatalog.recommendedFor(gb(3.9), is64Bit = true))
        assertEquals(ModelCatalog.QWEN25_1_5B, ModelCatalog.recommendedFor(gb(5.5), is64Bit = true)) // 6 GB phone
    }

    @Test fun eightGbAndUpGetQwen3() {
        assertEquals(ModelCatalog.QWEN3_4B, ModelCatalog.recommendedFor(gb(7.4), is64Bit = true)) // 8 GB phone
        assertEquals(ModelCatalog.QWEN3_4B, ModelCatalog.recommendedFor(gb(11.2), is64Bit = true)) // 12 GB phone
        assertEquals(ModelCatalog.QWEN3_4B, ModelCatalog.recommendedFor(gb(15.5), is64Bit = true))
    }

    @Test fun smallOr32BitPhonesGetLiteOnly() {
        assertNull(ModelCatalog.recommendedFor(gb(2.8), is64Bit = true)) // 3 GB phone
        assertNull(ModelCatalog.recommendedFor(gb(1.9), is64Bit = true))
        assertNull(ModelCatalog.recommendedFor(gb(7.4), is64Bit = false))
    }

    @Test fun ramLabelShowsTheBoxSize() {
        assertEquals("4 GB", ModelCatalog.ramLabel(gb(3.7)))
        assertEquals("6 GB", ModelCatalog.ramLabel(gb(5.6)))
        assertEquals("8 GB", ModelCatalog.ramLabel(gb(7.4)))
        assertEquals("12 GB", ModelCatalog.ramLabel(gb(11.3)))
        assertEquals("3 GB", ModelCatalog.ramLabel(gb(2.9)))
    }

    @Test fun lookupsAndRuntime() {
        assertEquals(ModelCatalog.QWEN3_4B, ModelCatalog.byId("qwen3-4b"))
        assertEquals(ModelCatalog.QWEN25_1_5B, ModelCatalog.byFileName(ModelCatalog.QWEN25_1_5B.fileName))
        assertEquals(ModelCatalog.LEGACY_GEMMA3_1B, ModelCatalog.byFileName("gemma3-1b-it-int4.task"))
        assertNull(ModelCatalog.byFileName("my-own-model.task"))
        assertNull(ModelCatalog.byId(null))
        assertEquals(ModelRuntime.LITERT_LM, ModelCatalog.runtimeFor("anything.litertlm"))
        assertEquals(ModelRuntime.MEDIAPIPE, ModelCatalog.runtimeFor("anything.task"))
        assertTrue(ModelCatalog.QWEN3_4B.hasThinking)
        assertFalse(ModelCatalog.QWEN25_1_5B.hasThinking)
        assertFalse(ModelCatalog.LEGACY_GEMMA3_1B in ModelCatalog.downloadable)
    }
}
