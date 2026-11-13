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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    data class Failed(val message: String) : DownloadUi
    data object Done : DownloadUi
}

/** Starts, cancels and observes the AI model download job. */
@Singleton
class ModelDownloads @Inject constructor(@ApplicationContext private val context: Context) {
    private val wm get() = WorkManager.getInstance(context)

    init {
        // The v2.2 Gemma job (token-based) is obsolete; its worker would fail anyway.
        runCatching { wm.cancelUniqueWork(LEGACY_UNIQUE_NAME) }
    }

    val state: Flow<DownloadUi> = wm.getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.UNIQUE_NAME).map { infos ->
        toUi(infos.lastOrNull())
    }

    fun start(model: TutorModel) {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(ModelDownloadWorker.KEY_MODEL_ID to model.id))
            .addTag(ModelDownloadWorker.TAG_PREFIX + model.id)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        wm.enqueueUniqueWork(ModelDownloadWorker.UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Stops the download and frees the partial file. */
    fun cancel() {
        wm.cancelUniqueWork(ModelDownloadWorker.UNIQUE_NAME)
        ModelDownloadWorker.downloadsDir(context).listFiles()?.forEach { it.delete() }
    }

    /** Forget finished jobs once the UI has seen the result. */
    fun clearFinished() { wm.pruneWork() }

    fun isMetered(): Boolean = runCatching {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
    }.getOrDefault(false)

    companion object {
        private const val LEGACY_UNIQUE_NAME = "studin-gemma-download"

        /** The catalog model a job downloads, read from its tag. */
        fun modelOf(info: WorkInfo): TutorModel? = info.tags.firstNotNullOfOrNull { tag ->
            if (tag.startsWith(ModelDownloadWorker.TAG_PREFIX)) ModelCatalog.byId(tag.removePrefix(ModelDownloadWorker.TAG_PREFIX)) else null
        }

        fun toUi(info: WorkInfo?): DownloadUi {
            val total = info?.let { modelOf(it)?.sizeBytes } ?: 0L
            return when (info?.state) {
                null, WorkInfo.State.CANCELLED -> DownloadUi.Idle
                WorkInfo.State.SUCCEEDED -> DownloadUi.Done
                WorkInfo.State.FAILED -> DownloadUi.Failed(info.outputData.getString(ModelDownloadWorker.KEY_ERROR) ?: "Download failed")
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadUi.Running(
                    info.progress.getLong(ModelDownloadWorker.KEY_BYTES, 0), total, waiting = true,
                )
                WorkInfo.State.RUNNING -> DownloadUi.Running(
                    info.progress.getLong(ModelDownloadWorker.KEY_BYTES, 0),
                    info.progress.getLong(ModelDownloadWorker.KEY_TOTAL, total), waiting = false,
                )
            }
        }
    }
}
