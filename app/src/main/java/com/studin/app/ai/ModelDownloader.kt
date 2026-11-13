package com.studin.app.ai

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException

/** Why a model download could not complete. [retryable] failures may succeed on a later attempt. */
sealed class DownloadFailure(message: String, val retryable: Boolean) : IOException(message) {
    /** No token was given and the repository is gated. */
    class TokenRequired : DownloadFailure("This model needs a Hugging Face access token.", false)
    /** The token was rejected (wrong, expired or revoked). */
    class TokenRejected : DownloadFailure("Hugging Face rejected that access token. Create a new “Read” token and try again.", false)
    /** Signed in, but the model's licence hasn't been accepted for this account yet (gated repos). */
    class LicenceNotAccepted : DownloadFailure("Accept the model's licence on its Hugging Face page first, then try again.", false)
    class NotFound : DownloadFailure("The model file wasn't found on Hugging Face.", false)
    class Checksum(expected: String, actual: String) :
        DownloadFailure("The downloaded file failed verification (SHA-256 ${actual.take(12)}… ≠ ${expected.take(12)}…). It was deleted.", false)
    class Incomplete(got: Long, expected: Long) : DownloadFailure("Download interrupted at ${got / 1_000_000} of ${expected / 1_000_000} MB.", true)
    class Http(code: Int) : DownloadFailure("Hugging Face returned HTTP $code.", code >= 500 || code == 429)
}

/**
 * Minimal, dependency-free resumable downloader for the AI Tutor model (pure JVM, unit-tested).
 *
 * * The bearer token is sent **only** to the Hugging Face resolve URL; Hugging Face answers with a
 *   redirect to a signed CDN URL, which is fetched without credentials.
 * * Partial files are resumed with an HTTP `Range` request.
 * * The finished file must match the pinned size and SHA-256, otherwise it is deleted.
 */
class ModelDownloader(
    private val userAgent: String = "StudIN-Android",
    private val open: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) {
    /**
     * Checks that [token] can access [url] without downloading anything (used before enqueueing,
     * so the user gets immediate, specific feedback). Throws a [DownloadFailure] when it can't.
     */
    fun checkAccess(url: String, token: String?, whoAmIUrl: String? = WHOAMI_URL) {
        if (token.isNullOrBlank()) throw DownloadFailure.TokenRequired()
        // Hugging Face answers an invalid token exactly like no token (401 GatedRepo), so validate
        // the token itself first to tell "wrong token" apart from "licence not accepted".
        if (whoAmIUrl != null) {
            val w = request(URL(whoAmIUrl), token, rangeFrom = null)
            try {
                when (val code = w.responseCode) {
                    in 200..299 -> Unit
                    401, 403 -> throw DownloadFailure.TokenRejected()
                    else -> throw DownloadFailure.Http(code)
                }
            } finally { w.disconnect() }
        }
        val c = request(URL(url), token, rangeFrom = 0, rangeTo = 0)
        try {
            val code = c.responseCode
            // A valid token that still can't read the file = licence not accepted for this account.
            if (code == 401 || code == 403) throw DownloadFailure.LicenceNotAccepted()
            failIfError(code, c, token)
        } finally { c.disconnect() }
    }

    /**
     * Downloads [url] into [part] (resuming if it already holds bytes), then verifies it.
     * @return [part], complete and verified.
     */
    fun download(
        url: String,
        token: String?,
        part: File,
        expectedSize: Long,
        expectedSha256: String,
        onProgress: (bytes: Long, total: Long) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false },
    ): File {
        part.parentFile?.mkdirs()
        if (part.length() > expectedSize) part.delete()
        if (part.length() < expectedSize) fetch(URL(url), token, part, expectedSize, onProgress, isCancelled)
        if (part.length() != expectedSize) throw DownloadFailure.Incomplete(part.length(), expectedSize)
        val actual = sha256(part)
        if (!actual.equals(expectedSha256, ignoreCase = true)) {
            part.delete()
            throw DownloadFailure.Checksum(expectedSha256, actual)
        }
        return part
    }

    private fun fetch(start: URL, token: String?, part: File, expectedSize: Long, onProgress: (Long, Long) -> Unit, isCancelled: () -> Boolean) {
        var url = start
        var auth = token
        var resumeFrom = part.length()
        repeat(MAX_REDIRECTS) {
            val c = request(url, auth, rangeFrom = resumeFrom.takeIf { it > 0 })
            try {
                val code = c.responseCode
                if (code in 300..399) {
                    val location = c.getHeaderField("Location") ?: throw DownloadFailure.Http(code)
                    url = URL(url, location)
                    // Credentials stay with Hugging Face; the signed CDN URL needs none.
                    if (!url.host.equals(start.host, ignoreCase = true)) auth = null
                    return@repeat
                }
                failIfError(code, c, token)
                val append = code == HttpURLConnection.HTTP_PARTIAL && resumeFrom > 0
                if (!append) resumeFrom = 0 // server ignored Range: start over
                c.inputStream.use { input ->
                    FileOutputStream(part, append).use { out ->
                        val buf = ByteArray(256 * 1024)
                        var done = resumeFrom
                        var lastReport = 0L
                        while (true) {
                            if (isCancelled()) throw CancellationException("Download cancelled")
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (done - lastReport >= REPORT_EVERY || done == expectedSize) { onProgress(done, expectedSize); lastReport = done }
                        }
                        out.flush()
                    }
                }
                return
            } finally {
                c.disconnect()
            }
        }
        throw DownloadFailure.Http(310)
    }

    private fun request(url: URL, token: String?, rangeFrom: Long?, rangeTo: Long? = null): HttpURLConnection =
        open(url).apply {
            instanceFollowRedirects = false
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept-Encoding", "identity")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer ${token.trim()}")
            if (rangeFrom != null) setRequestProperty("Range", "bytes=$rangeFrom-${rangeTo ?: ""}")
        }

    private fun failIfError(code: Int, c: HttpURLConnection, token: String?) {
        if (code in 200..399) return
        val errorCode = c.getHeaderField("X-Error-Code")
        throw when {
            code == 401 && token.isNullOrBlank() -> DownloadFailure.TokenRequired()
            code == 401 && errorCode == "GatedRepo" -> DownloadFailure.LicenceNotAccepted()
            code == 401 -> DownloadFailure.TokenRejected()
            code == 403 -> DownloadFailure.LicenceNotAccepted()
            code == 404 -> DownloadFailure.NotFound()
            code == 416 -> DownloadFailure.Incomplete(0, 0)
            else -> DownloadFailure.Http(code)
        }
    }

    companion object {
        const val WHOAMI_URL = "https://huggingface.co/api/whoami-v2"
        private const val MAX_REDIRECTS = 6
        private const val REPORT_EVERY = 1_000_000L

        fun sha256(file: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(1 shl 20)
                while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
