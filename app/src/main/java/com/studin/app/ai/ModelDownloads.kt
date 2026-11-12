package com.studin.app.ai

import android.content.Context
import android.net.ConnectivityManager
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** What the AI Tutor screen shows about the model download. */
sealed interface DownloadUi {
    data object Idle : DownloadUi
    /** [waiting] = queued until a network is available. */
    data class Running(val bytes: Long, val total: Long, val waiting: Boolean) : DownloadUi {
        val fraction: Float get() = if (total > 0) (bytes.toFloat() / total).coerceIn(0f, 1f) else 0f
    }
    data class Failed(val message: String, val needsToken: Boolean) : DownloadUi
    data object Done : DownloadUi
}

/** Starts, cancels and observes the Gemma download job. */
@Singleton
class ModelDownloads @Inject constructor(@ApplicationContext private val context: Context) {
    private val wm get() = WorkManager.getInstance(context)

    val state: Flow<DownloadUi> = wm.getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.UNIQUE_NAME).map { infos ->
        toUi(infos.lastOrNull())
    }

    /** Validates the token against Hugging Face (fast, specific errors) before starting. */
    suspend fun verifyAccess(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { ModelDownloader().checkAccess(GemmaModel.DOWNLOAD_URL, token) }
    }

    fun start(token: String) {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(ModelDownloadWorker.KEY_TOKEN to token.trim()))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        wm.enqueueUniqueWork(ModelDownloadWorker.UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Stops the download and frees the partial file. */
    fun cancel() {
        wm.cancelUniqueWork(ModelDownloadWorker.UNIQUE_NAME)
        ModelDownloadWorker.partFile(context).delete()
    }

    /** Forget finished jobs (their input held the token) once the UI has seen the result. */
    fun clearFinished() { wm.pruneWork() }

    fun isMetered(): Boolean = runCatching {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
    }.getOrDefault(false)

    companion object {
        fun toUi(info: WorkInfo?): DownloadUi = when (info?.state) {
            null, WorkInfo.State.CANCELLED -> DownloadUi.Idle
            WorkInfo.State.SUCCEEDED -> DownloadUi.Done
            WorkInfo.State.FAILED -> DownloadUi.Failed(
                info.outputData.getString(ModelDownloadWorker.KEY_ERROR) ?: "Download failed",
                info.outputData.getBoolean(ModelDownloadWorker.KEY_NEEDS_TOKEN, false),
            )
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadUi.Running(
                info.progress.getLong(ModelDownloadWorker.KEY_BYTES, 0), GemmaModel.SIZE_BYTES, waiting = true,
            )
            WorkInfo.State.RUNNING -> DownloadUi.Running(
                info.progress.getLong(ModelDownloadWorker.KEY_BYTES, 0),
                info.progress.getLong(ModelDownloadWorker.KEY_TOTAL, GemmaModel.SIZE_BYTES), waiting = false,
            )
        }
    }
}
