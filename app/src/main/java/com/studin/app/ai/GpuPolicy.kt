package com.studin.app.ai

/** What StudIN knows about running a model on this phone's GPU. */
enum class GpuState {
    /** Never tried. */
    UNKNOWN,
    /** A GPU load or first answer is in progress. Still set at the next app start = it crashed. */
    PROBING,
    /** Loaded and answered on the GPU. */
    OK,
    /** GPU failed or crashed: use the CPU for this model. */
    FAILED,
}

/**
 * Crash-safe GPU selection. GPU drivers can kill the process natively (e.g. out of memory during
 * the first inference), which no try/catch can intercept. So StudIN records PROBING *before*
 * touching the GPU and OK only after a successful answer; if the app dies in between, the next
 * start sees PROBING and switches that model to the CPU permanently.
 */
object GpuPolicy {
    data class Decision(val useGpu: Boolean, val stateToSave: GpuState?)

    /** Decide the backend for a load. [userAllowsGpu] = the "Use GPU acceleration" setting. */
    fun decide(state: GpuState, userAllowsGpu: Boolean, runtimeSupportsGpu: Boolean): Decision = when {
        !userAllowsGpu || !runtimeSupportsGpu -> Decision(false, null)
        state == GpuState.FAILED -> Decision(false, null)
        // Left over from a previous run that never finished → the GPU path crashed the app.
        state == GpuState.PROBING -> Decision(false, GpuState.FAILED)
        state == GpuState.OK -> Decision(true, null)
        else -> Decision(true, GpuState.PROBING)
    }

    /** After the GPU engine failed to initialise (a catchable error). */
    fun onGpuInitError(): GpuState = GpuState.FAILED

    /** After the first successful GPU answer. */
    fun onGpuAnswered(previous: GpuState): GpuState = if (previous == GpuState.PROBING || previous == GpuState.UNKNOWN) GpuState.OK else previous
}
