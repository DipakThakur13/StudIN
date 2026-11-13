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
 * Downloads a catalog model ([ModelCatalog]) in a user-visible foreground job (survives leaving
 * the app), resumes partial downloads, verifies SHA-256 + size, installs it and switches the
 * tutor to it. The Qwen repositories are public: no account or token is involved.
 */
@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferences,
    private val llm: OnDeviceLlm,
) : CoroutineWorker(context, params) {

    /** Required for expedited work on Android 11 and older. */
    private val model: TutorModel? get() = ModelCatalog.byId(inputData.getString(KEY_MODEL_ID))

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val m = model ?: ModelCatalog.QWEN25_1_5B
        return foregroundInfo(m, partFile(applicationContext, m).length(), m.sizeBytes)
    }

    override suspend fun doWork(): Result {
        val model = model?.takeIf { it in ModelCatalog.downloadable }
        if (model == null) {
            // e.g. a Gemma download queued by v2.2 that resumed after the update.
            downloadsDir(applicationContext).listFiles()?.forEach { it.delete() }
            return failure("Unknown AI model")
        }
        val part = partFile(applicationContext, model)
        // Partial files of other models (an older or switched download) only waste space.
        downloadsDir(applicationContext).listFiles()?.filter { it != part }?.forEach { it.delete() }
        val needed = model.sizeBytes - part.length() + 100_000_000L
        if (part.parentFile!!.apply { mkdirs() }.usableSpace < needed) {
            return failure("Not enough free storage. StudIN needs about ${(needed + 500_000) / 1_000_000} MB free for ${model.displayName}.")
        }
        runCatching { setForeground(foregroundInfo(model, part.length(), model.sizeBytes)) }

        return try {
            var lastPercent = -1
            withContext(Dispatchers.IO) {
                ModelDownloader().download(
                    url = model.downloadUrl, token = null, part = part,
                    expectedSize = model.sizeBytes, expectedSha256 = model.sha256,
                    onProgress = { bytes, total ->
                        val pct = (bytes * 100 / total).toInt()
                        if (pct != lastPercent) {
                            lastPercent = pct
                            setProgressAsync(workDataOf(KEY_BYTES to bytes, KEY_TOTAL to total))
                            runCatching { setForegroundAsync(foregroundInfo(model, bytes, total)) }
                        }
                    },
                    isCancelled = { isStopped },
                )
            }
            install(model, part)
            Result.success()
        } catch (f: DownloadFailure) {
            if (f.retryable && runAttemptCount < MAX_ATTEMPTS) Result.retry() else failure(f.message ?: "Download failed")
        } catch (e: java.io.IOException) {
            // Network drop etc.: keep the partial file and let WorkManager retry with backoff.
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else failure("Network problem: ${e.message ?: "connection lost"}. Try again on Wi-Fi.")
        }
    }

    /** Move the verified file into the model directory and make it the active tutor engine. */
    private suspend fun install(model: TutorModel, part: File) {
        llm.unload()
        val dir = File(applicationContext.filesDir, "models").apply { mkdirs() }
        // Replaces any older model (e.g. Gemma 3 1B) only now that the new one is verified.
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, model.fileName)
        if (!part.renameTo(target)) { part.copyTo(target, overwrite = true); part.delete() }
        prefs.setModel(model.fileName, model.sha256)
        prefs.setUseLlm(true)
        llm.refreshState()
    }

    private fun failure(message: String): Result = Result.failure(workDataOf(KEY_ERROR to message))

    private fun foregroundInfo(model: TutorModel, bytes: Long, total: Long): ForegroundInfo {
        val pct = if (total > 0) (bytes * 100 / total).toInt() else 0
        val n: Notification = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_stat_studin)
            .setContentTitle("Downloading AI Tutor (${model.displayName})")
            .setContentText("${bytes / 1_000_000} of ${total / 1_000_000} MB · $pct%")
            .setProgress(100, pct, bytes == 0L)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setContentIntent(Notifications.openApp(applicationContext, Routes.AI, requestCode = 7))
            .build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(Notifications.ID_MODEL_DOWNLOAD, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(Notifications.ID_MODEL_DOWNLOAD, n)
    }

    companion object {
        const val UNIQUE_NAME = "studin-model-download"
        const val KEY_MODEL_ID = "modelId"
        const val TAG_PREFIX = "model:"
        const val KEY_BYTES = "bytes"
        const val KEY_TOTAL = "total"
        const val KEY_ERROR = "error"
        private const val MAX_ATTEMPTS = 5

        fun downloadsDir(context: Context) = File(context.filesDir, "downloads")
        fun partFile(context: Context, model: TutorModel) = File(downloadsDir(context), model.fileName + ".part")
    }
}
