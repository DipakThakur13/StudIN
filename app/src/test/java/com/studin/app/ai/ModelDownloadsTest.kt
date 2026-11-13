package com.studin.app.ai

import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.workDataOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

/** The download card reads the model (and so the total size) from the job's tag. */
class ModelDownloadsTest {
    private fun info(state: WorkInfo.State, model: TutorModel?, progress: Data = Data.EMPTY, output: Data = Data.EMPTY) =
        WorkInfo(UUID.randomUUID(), state, setOfNotNull(model?.let { ModelDownloadWorker.TAG_PREFIX + it.id }, "other"), output, progress)

    @Test fun runningUsesTheTaggedModelsSize() {
        val ui = ModelDownloads.toUi(info(WorkInfo.State.ENQUEUED, ModelCatalog.QWEN3_4B, workDataOf(ModelDownloadWorker.KEY_BYTES to 10L)))
        assertEquals(DownloadUi.Running(10L, ModelCatalog.QWEN3_4B.sizeBytes, waiting = true), ui)
        assertEquals(ModelCatalog.QWEN25_1_5B, ModelDownloads.modelOf(info(WorkInfo.State.RUNNING, ModelCatalog.QWEN25_1_5B)))
    }

    @Test fun finishedStates() {
        assertEquals(DownloadUi.Idle, ModelDownloads.toUi(null))
        assertEquals(DownloadUi.Idle, ModelDownloads.toUi(info(WorkInfo.State.CANCELLED, ModelCatalog.QWEN3_4B)))
        assertEquals(DownloadUi.Done, ModelDownloads.toUi(info(WorkInfo.State.SUCCEEDED, ModelCatalog.QWEN3_4B)))
        assertEquals(
            DownloadUi.Failed("Not enough free storage"),
            ModelDownloads.toUi(info(WorkInfo.State.FAILED, ModelCatalog.QWEN3_4B, output = workDataOf(ModelDownloadWorker.KEY_ERROR to "Not enough free storage"))),
        )
    }
}
