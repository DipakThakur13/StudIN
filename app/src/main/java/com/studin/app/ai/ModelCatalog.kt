package com.studin.app.ai

/** Which native runtime executes a model file. */
enum class ModelRuntime { LITERT_LM, MEDIAPIPE }

/**
 * A downloadable on-device tutor model, pinned to an exact Hugging Face revision and verified
 * against the SHA-256 and size Hugging Face publishes for that file.
 */
data class TutorModel(
    val id: String,
    val displayName: String,
    val repo: String,
    val revision: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val runtime: ModelRuntime,
    val license: String,
    /** Model has a reasoning ("thinking") mode that StudIN switches off for fast, direct answers. */
    val hasThinking: Boolean = false,
    /** KV-cache size StudIN asks for (≤ the file's context); smaller = less RAM. */
    val maxTokens: Int = 2048,
) {
    val downloadUrl: String get() = "https://huggingface.co/$repo/resolve/$revision/$fileName"
    val modelPage: String get() = "https://huggingface.co/$repo"
    val sizeLabel: String get() = if (sizeBytes >= 1_000_000_000) String.format(java.util.Locale.US, "%.1f GB", sizeBytes / 1e9) else "${(sizeBytes + 500_000) / 1_000_000} MB"
}

/**
 * Models StudIN offers, chosen by the phone's RAM (verified on Hugging Face, 13 Nov 2026):
 *  * 4–6 GB phones → Qwen2.5 1.5B Instruct (q8, 1.6 GB, Apache-2.0)
 *  * 8 GB+ phones  → Qwen3 4B (dynamic int4, 2.3 GB, Apache-2.0; GPU path needs LiteRT-LM ≥ 0.19.0)
 * Both repositories are public: no account or token is needed.
 */
object ModelCatalog {
    val QWEN25_1_5B = TutorModel(
        id = "qwen2.5-1.5b",
        displayName = "Qwen2.5 1.5B",
        repo = "litert-community/Qwen2.5-1.5B-Instruct",
        revision = "60257f9f1d00ca1f24cc5ce3b829b4139be1ca2b",
        fileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
        sizeBytes = 1_597_922_856L,
        sha256 = "c4a9b7289365ab80fe599b844b77ad253eb3c53fb0e5d963d699146a6f01255e",
        runtime = ModelRuntime.LITERT_LM,
        license = "Apache-2.0",
    )

    val QWEN3_4B = TutorModel(
        id = "qwen3-4b",
        displayName = "Qwen3 4B",
        repo = "litert-community/Qwen3-4B",
        revision = "0c8dd5465e2a88e1aa5fca7873e56c4165162ada",
        fileName = "qwen3_4b_mixed_int4.litertlm",
        sizeBytes = 2_272_935_936L,
        sha256 = "4877b236f09c0b519958612afeb3c6fc6575c7ce56080d0d3a5406150e0ff9ce",
        runtime = ModelRuntime.LITERT_LM,
        license = "Apache-2.0",
        hasThinking = true,
    )

    /** Gemma 3 1B downloaded by StudIN 2.1–2.2: still runs (MediaPipe) until replaced. */
    val LEGACY_GEMMA3_1B = TutorModel(
        id = "gemma3-1b",
        displayName = "Gemma 3 1B",
        repo = "litert-community/Gemma3-1B-IT",
        revision = "a9bd85643f05cdb1e5cd5af7ba4e74b0e8210ca0",
        fileName = "gemma3-1b-it-int4.task",
        sizeBytes = 554_661_243L,
        sha256 = "e3d981c01aeaaac69a84ffa0d4be13281b3176731063f1bea1c9fe6887bd9dee",
        runtime = ModelRuntime.MEDIAPIPE,
        license = "Gemma",
        maxTokens = 1024,
    )

    val downloadable = listOf(QWEN25_1_5B, QWEN3_4B)
    val all = downloadable + LEGACY_GEMMA3_1B

    /** Android reports a little less than the marketed RAM (an "8 GB" phone shows ~7.3–7.7 GB). */
    const val PLUS_MIN_RAM = 7_000_000_000L
    const val STANDARD_MIN_RAM = 3_300_000_000L

    /** The model for this phone, or null when only Lite AI should run (32-bit or < ~4 GB RAM). */
    fun recommendedFor(totalRamBytes: Long, is64Bit: Boolean): TutorModel? = when {
        !is64Bit || totalRamBytes < STANDARD_MIN_RAM -> null
        totalRamBytes >= PLUS_MIN_RAM -> QWEN3_4B
        else -> QWEN25_1_5B
    }

    fun byId(id: String?): TutorModel? = all.firstOrNull { it.id == id }
    fun byFileName(name: String?): TutorModel? = all.firstOrNull { it.fileName == name }

    /** Runtime for any model file, including user-imported ones. */
    fun runtimeFor(fileName: String): ModelRuntime =
        byFileName(fileName)?.runtime ?: if (fileName.endsWith(".litertlm")) ModelRuntime.LITERT_LM else ModelRuntime.MEDIAPIPE

    /** Human RAM label, e.g. "8 GB". */
    /**
     * The RAM size printed on the box. Android reports less than that (the kernel and modem keep
     * some), e.g. an 8 GB phone reports ~7.4 GB, so round up to the next common size.
     */
    fun ramLabel(totalRamBytes: Long): String {
        val gib = totalRamBytes / 1_073_741_824.0
        val size = RAM_SIZES.firstOrNull { it >= gib - 0.05 } ?: Math.round(gib).toInt()
        return "$size GB"
    }

    private val RAM_SIZES = listOf(1, 2, 3, 4, 6, 8, 12, 16, 18, 24, 32)
}
