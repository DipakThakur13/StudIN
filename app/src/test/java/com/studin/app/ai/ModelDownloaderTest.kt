package com.studin.app.ai

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Exercises the real download code against a local server that behaves like Hugging Face:
 * gated resolve URL (401 GatedRepo) → 302 to a signed CDN URL that must NOT receive credentials,
 * Range support for resume, plus the whoami token check.
 */
class ModelDownloaderTest {
    @get:Rule val tmp = TemporaryFolder()

    private val payload = Random(42).nextBytes(3_500_000)
    private val sha = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }
    private val goodToken = "hf_validToken123"
    private val licensedTokens = mutableSetOf(goodToken)
    private val cdnHits = AtomicInteger()
    private var cdnLeakedAuth = false
    private var lastRange: String? = null
    private lateinit var server: ServerSocket
    private lateinit var base: String

    /** Minimal HTTP/1.1 server (android.jar has no com.sun.net.httpserver). */
    private class Req(val path: String, val headers: Map<String, String>) { fun header(n: String) = headers[n.lowercase()] }
    private class Resp(val code: Int, val body: ByteArray = ByteArray(0), val headers: Map<String, String> = emptyMap())

    private fun route(r: Req): Resp {
        val bearer = r.header("Authorization")?.removePrefix("Bearer ")
        return when {
            r.path.startsWith("/api/whoami-v2") ->
                if (bearer in setOf(goodToken, "hf_noLicence999")) Resp(200, """{"name":"student"}""".toByteArray())
                else Resp(401, """{"error":"Invalid username or password."}""".toByteArray())
            r.path.startsWith("/repo/resolve/rev/model.task") ->
                if (bearer !in licensedTokens) Resp(401, headers = mapOf("X-Error-Code" to "GatedRepo"))
                // Real HF redirects to a different host (signed CDN); "localhost" ≠ "127.0.0.1" models that.
                else Resp(302, headers = mapOf("Location" to "http://localhost:${server.localPort}/cdn/signed/model.task?sig=abc"))
            r.path.startsWith("/cdn/signed/model.task") -> {
                cdnHits.incrementAndGet()
                if (r.header("Authorization") != null) cdnLeakedAuth = true
                val range = r.header("Range"); lastRange = range
                val from = range?.removePrefix("bytes=")?.substringBefore('-')?.toLongOrNull() ?: 0L
                val to = range?.substringAfter('-')?.toLongOrNull() ?: (payload.size - 1L)
                val body = payload.copyOfRange(from.toInt(), (to + 1).toInt())
                if (range != null) Resp(206, body, mapOf("Content-Range" to "bytes $from-$to/${payload.size}")) else Resp(200, body)
            }
            else -> Resp(404)
        }
    }

    @Before fun start() {
        server = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
        base = "http://127.0.0.1:${server.localPort}"
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val sock = runCatching { server.accept() }.getOrNull() ?: break
                thread(isDaemon = true) {
                    sock.use { s ->
                        val input = s.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                        val line = input.readLine() ?: return@use
                        val headers = generateSequence { input.readLine()?.takeIf { it.isNotEmpty() } }
                            .associate { it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim() }
                        val resp = route(Req(line.split(' ')[1], headers))
                        val out = s.getOutputStream()
                        val head = buildString {
                            append("HTTP/1.1 ${resp.code} X\r\n")
                            resp.headers.forEach { (k, v) -> append("$k: $v\r\n") }
                            append("Content-Length: ${resp.body.size}\r\nConnection: close\r\n\r\n")
                        }
                        out.write(head.toByteArray(Charsets.ISO_8859_1)); out.write(resp.body); out.flush()
                    }
                }
            }
        }
    }

    @After fun stop() = server.close()

    private val url get() = "$base/repo/resolve/rev/model.task"
    private val dl = ModelDownloader()

    @Test fun downloadsFollowsRedirectWithoutLeakingTokenAndVerifies() {
        val part = tmp.root.resolve("m.part")
        var lastProgress = 0L
        dl.download(url, goodToken, part, payload.size.toLong(), sha, onProgress = { b, _ -> lastProgress = b })
        assertEquals(payload.size.toLong(), part.length())
        assertEquals(payload.size.toLong(), lastProgress)
        assertFalse("token must never reach the CDN", cdnLeakedAuth)
        assertEquals(sha, ModelDownloader.sha256(part))
    }

    @Test fun resumesPartialFileWithRange() {
        val part = tmp.root.resolve("m.part").apply { writeBytes(payload.copyOfRange(0, 1_000_000)) }
        dl.download(url, goodToken, part, payload.size.toLong(), sha)
        assertEquals("bytes=1000000-", lastRange)
        assertEquals(sha, ModelDownloader.sha256(part))
    }

    @Test fun corruptPartialFileFailsChecksumAndIsDeleted() {
        val part = tmp.root.resolve("m.part").apply { writeBytes(ByteArray(1_000_000) { 7 }) }
        try { dl.download(url, goodToken, part, payload.size.toLong(), sha); fail("expected checksum failure") }
        catch (e: DownloadFailure.Checksum) { assertFalse(part.exists()) }
    }

    @Test fun gatedWithoutTokenOrWithBadToken() {
        val part = tmp.root.resolve("m.part")
        try { dl.download(url, null, part, payload.size.toLong(), sha); fail() } catch (e: DownloadFailure) { assertTrue(e is DownloadFailure.TokenRequired) }
        try { dl.download(url, "hf_wrong", part, payload.size.toLong(), sha); fail() } catch (e: DownloadFailure) { assertTrue(e is DownloadFailure.LicenceNotAccepted || e is DownloadFailure.TokenRejected) }
        assertEquals(0, cdnHits.get())
    }

    @Test fun accessCheckTellsWrongTokenFromLicenceNotAccepted() {
        val who = "$base/api/whoami-v2"
        dl.checkAccess(url, goodToken, who) // ok
        try { dl.checkAccess(url, "hf_wrong", who); fail() } catch (e: DownloadFailure) { assertTrue(e.toString(), e is DownloadFailure.TokenRejected) }
        try { dl.checkAccess(url, "hf_noLicence999", who); fail() } catch (e: DownloadFailure) { assertTrue(e.toString(), e is DownloadFailure.LicenceNotAccepted) }
        try { dl.checkAccess(url, "", who); fail() } catch (e: DownloadFailure) { assertTrue(e is DownloadFailure.TokenRequired) }
        assertFalse(cdnLeakedAuth)
    }

    @Test fun pinnedModelConstantsAreConsistent() {
        for (m in ModelCatalog.all) {
            assertTrue(m.downloadUrl, m.downloadUrl.startsWith("https://huggingface.co/${m.repo}/resolve/${m.revision}/"))
            assertTrue(m.downloadUrl.endsWith(m.fileName))
            assertEquals(m.id, 40, m.revision.length)
            assertTrue(m.id, Regex("^[0-9a-f]{64}$").matches(m.sha256))
            assertEquals(m.fileName, m.runtime, ModelCatalog.runtimeFor(m.fileName))
        }
        assertEquals("1.6 GB", ModelCatalog.QWEN25_1_5B.sizeLabel)
        assertEquals("2.3 GB", ModelCatalog.QWEN3_4B.sizeLabel)
        assertEquals(ModelCatalog.all.size, ModelCatalog.all.map { it.id }.toSet().size)
    }
}
