package com.studin.app.ai

import org.junit.Assert.assertEquals
import org.junit.Test

/** GPU use must never brick the tutor: a crash during a GPU attempt means CPU from then on. */
class GpuPolicyTest {
    private fun d(state: GpuState, user: Boolean = true, runtime: Boolean = true) = GpuPolicy.decide(state, user, runtime)

    @Test fun firstLoadTriesGpuAndMarksTheAttemptBeforehand() {
        assertEquals(GpuPolicy.Decision(useGpu = true, stateToSave = GpuState.PROBING), d(GpuState.UNKNOWN))
    }

    @Test fun leftoverProbeMeansTheLastGpuAttemptCrashed() {
        assertEquals(GpuPolicy.Decision(useGpu = false, stateToSave = GpuState.FAILED), d(GpuState.PROBING))
    }

    @Test fun knownGoodUsesGpuKnownBadUsesCpu() {
        assertEquals(GpuPolicy.Decision(useGpu = true, stateToSave = null), d(GpuState.OK))
        assertEquals(GpuPolicy.Decision(useGpu = false, stateToSave = null), d(GpuState.FAILED))
    }

    @Test fun userSwitchOrCpuOnlyRuntimeMeansCpu() {
        assertEquals(false, d(GpuState.OK, user = false).useGpu)
        assertEquals(false, d(GpuState.UNKNOWN, runtime = false).useGpu)
        assertEquals(null, d(GpuState.UNKNOWN, user = false).stateToSave)
    }

    @Test fun outcomes() {
        assertEquals(GpuState.FAILED, GpuPolicy.onGpuInitError())
        assertEquals(GpuState.OK, GpuPolicy.onGpuAnswered(GpuState.PROBING))
        assertEquals(GpuState.OK, GpuPolicy.onGpuAnswered(GpuState.OK))
        assertEquals(GpuState.FAILED, GpuPolicy.onGpuAnswered(GpuState.FAILED))
    }
}
