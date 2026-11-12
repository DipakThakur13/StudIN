package com.studin.app.ai

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.studin.app.R
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.platform.notifications.Notifications
import com.studin.app.ui.navigation.Routes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Downloads the Gemma model in a user-visible foreground job (survives leaving the app), resumes
 * partial downloads, verifies SHA-256 + size, installs it and switches the tutor to it.
 */
@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferences,
    private val llm: OnDeviceLlm,
) : CoroutineWorker(context, params) {

    /** Required for expedited work on Android 11 and older. */
    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo(partFile(applicationContext).length(), GemmaModel.SIZE_BYTES)

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN)
        val part = partFile(applicationContext)
        val needed = GemmaModel.SIZE_BYTES - part.length() + 50_000_000L
        if (part.parentFile!!.apply { mkdirs() }.usableSpace < needed) {
            return failure("Not enough free storage. StudIN needs about ${needed / 1_000_000} MB free for the AI Tutor.")
        }
        runCatching { setForeground(foregroundInfo(part.length(), GemmaModel.SIZE_BYTES)) }

        return try {
            var lastPercent = -1
            withContext(Dispatchers.IO) {
                ModelDownloader().download(
                    url = GemmaModel.DOWNLOAD_URL, token = token, part = part,
                    expectedSize = GemmaModel.SIZE_BYTES, expectedSha256 = GemmaModel.SHA256,
                    onProgress = { bytes, total ->
                        val pct = (bytes * 100 / total).toInt()
                        if (pct != lastPercent) {
                            lastPercent = pct
                            setProgressAsync(workDataOf(KEY_BYTES to bytes, KEY_TOTAL to total))
                            runCatching { setForegroundAsync(foregroundInfo(bytes, total)) }
                        }
                    },
                    isCancelled = { isStopped },
                )
            }
            install(part)
            Result.success()
        } catch (f: DownloadFailure) {
            if (f.retryable && runAttemptCount < MAX_ATTEMPTS) Result.retry() else failure(f.message ?: "Download failed", f)
        } catch (e: java.io.IOException) {
            // Network drop etc.: keep the partial file and let WorkManager retry with backoff.
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else failure("Network problem: ${e.message ?: "connection lost"}. Try again on Wi-Fi.")
        }
    }

    /** Move the verified file into the model directory and make it the active tutor engine. */
    private suspend fun install(part: File) {
        llm.unload()
        val dir = File(applicationContext.filesDir, "models").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, GemmaModel.FILE_NAME)
        if (!part.renameTo(target)) { part.copyTo(target, overwrite = true); part.delete() }
        prefs.setModel(GemmaModel.FILE_NAME, GemmaModel.SHA256)
        prefs.setUseLlm(true)
        llm.refreshState()
    }

    private fun failure(message: String, f: DownloadFailure? = null): Result = Result.failure(
        workDataOf(KEY_ERROR to message, KEY_NEEDS_TOKEN to (f is DownloadFailure.TokenRequired || f is DownloadFailure.TokenRejected || f is DownloadFailure.LicenceNotAccepted)),
    )

    private fun foregroundInfo(bytes: Long, total: Long): ForegroundInfo {
        val pct = if (total > 0) (bytes * 100 / total).toInt() else 0
        val n: Notification = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_stat_studin)
            .setContentTitle("Downloading AI Tutor (${GemmaModel.DISPLAY_NAME})")
            .setContentText("${bytes / 1_000_000} of ${total / 1_000_000} MB · $pct%")
            .setProgress(100, pct, bytes == 0L)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setContentIntent(Notifications.openApp(applicationContext, Routes.AI, requestCode = 7))
            .build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(Notifications.ID_MODEL_DOWNLOAD, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(Notifications.ID_MODEL_DOWNLOAD, n)
    }

    companion object {
        const val UNIQUE_NAME = "studin-gemma-download"
        const val KEY_TOKEN = "token"
        const val KEY_BYTES = "bytes"
        const val KEY_TOTAL = "total"
        const val KEY_ERROR = "error"
        const val KEY_NEEDS_TOKEN = "needsToken"
        private const val MAX_ATTEMPTS = 5

        fun partFile(context: Context) = File(File(context.filesDir, "downloads"), GemmaModel.FILE_NAME + ".part")
    }
}
