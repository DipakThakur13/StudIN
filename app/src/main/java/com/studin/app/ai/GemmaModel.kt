package com.studin.app.ai

/**
 * The on-device AI Tutor model: Google's Gemma 3 1B instruction-tuned, int4, packaged by Google
 * for MediaPipe LLM Inference in the official LiteRT community repository on Hugging Face.
 *
 * The download is pinned to an exact repository revision and verified against the SHA-256 and
 * size published by Hugging Face, so a changed or tampered file is rejected.
 * The repository is gated: each user accepts Google's Gemma licence once and authorises the
 * download with their own Hugging Face read token.
 */
object GemmaModel {
    /**
     * Read token injected at build time (see app/build.gradle.kts); null when none was bundled.
     * Lets students download with one tap. If it is revoked, the app falls back to asking for
     * the student's own token.
     */
    val bundledToken: String? = com.studin.app.BuildConfig.HF_TOKEN.takeIf { it.isNotBlank() }

    const val DISPLAY_NAME = "Gemma 3 1B"
    const val REPO = "litert-community/Gemma3-1B-IT"
    /** Repository commit the file is pinned to (verified 12 Nov 2026). */
    const val REVISION = "a9bd85643f05cdb1e5cd5af7ba4e74b0e8210ca0"
    const val FILE_NAME = "gemma3-1b-it-int4.task"
    const val SIZE_BYTES = 554_661_243L
    const val SHA256 = "e3d981c01aeaaac69a84ffa0d4be13281b3176731063f1bea1c9fe6887bd9dee"

    const val DOWNLOAD_URL = "https://huggingface.co/$REPO/resolve/$REVISION/$FILE_NAME"
    /** Where the user signs in and accepts the Gemma licence. */
    const val MODEL_PAGE = "https://huggingface.co/$REPO"
    /** Where the user creates a free "Read" access token. */
    const val TOKENS_PAGE = "https://huggingface.co/settings/tokens"
    const val TERMS_PAGE = "https://ai.google.dev/gemma/terms"

    /** Human size, e.g. "555 MB". */
    val sizeLabel: String get() = "${(SIZE_BYTES + 500_000) / 1_000_000} MB"
}
