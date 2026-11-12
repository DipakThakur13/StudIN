package com.studin.app.feature.ai

import com.studin.app.ai.LlmState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPresentationTest {

    @Test fun engineStatusIsHonest() {
        assertEquals("Lite AI", engineStatus(LlmState.NotInstalled, useLlm = true).label)
        assertEquals("Lite AI", engineStatus(LlmState.Unsupported("needs 4 GB"), useLlm = true).label)
        assertEquals("Model loading", engineStatus(LlmState.Loading("gemma.task"), useLlm = true).label)
        assertEquals("Model error", engineStatus(LlmState.Error("boom"), useLlm = true).label)
        assertEquals("Model installed", engineStatus(LlmState.Installed("gemma.task", 1), useLlm = true).label)
        assertEquals("On-device model", engineStatus(LlmState.Ready("gemma.task", 1), useLlm = true).label)
        assertEquals("Processing", engineStatus(LlmState.Ready("gemma.task", 1), useLlm = true, generating = true).label)
    }

    @Test fun switchedOffModelNeverClaimsLlm() {
        assertEquals(EngineStatusKind.LITE, engineStatus(LlmState.Ready("g", 1), useLlm = false).kind)
        assertEquals(EngineStatusKind.LITE, engineStatus(LlmState.Installed("g", 1), useLlm = false).kind)
        // Errors and loading are still surfaced even when switched off.
        assertEquals(EngineStatusKind.ERROR, engineStatus(LlmState.Error("x"), useLlm = false).kind)
    }

    @Test fun formatsBulletsNumbersAndParagraphs() {
        val blocks = formatReply("Here's a plan:\n\n• 25 min – Kinematics\n- 10 min – Review\n1. First\n2) Second\n\nGood luck!")
        assertEquals(
            listOf(
                ReplyBlock.Paragraph("Here's a plan:"),
                ReplyBlock.Bullet("25 min – Kinematics"),
                ReplyBlock.Bullet("10 min – Review"),
                ReplyBlock.Numbered("1", "First"),
                ReplyBlock.Numbered("2", "Second"),
                ReplyBlock.Paragraph("Good luck!"),
            ),
            blocks,
        )
    }

    @Test fun keepsLineBreaksInsideParagraphs() {
        assertEquals(listOf(ReplyBlock.Paragraph("line one\nline two")), formatReply("line one\nline two"))
    }

    @Test fun codeFencesBecomeCodeBlocksVerbatim() {
        val blocks = formatReply("Try:\n```kotlin\nval x = 1\n  • not a bullet\n```\nDone")
        assertEquals(ReplyBlock.Code("val x = 1\n  • not a bullet"), blocks[1])
        assertEquals(ReplyBlock.Paragraph("Done"), blocks[2])
    }

    @Test fun unterminatedFenceStillRendersAndEmptyInputIsEmpty() {
        assertEquals(listOf(ReplyBlock.Code("partial")), formatReply("```\npartial"))
        assertTrue(formatReply("").isEmpty())
        assertTrue(formatReply("\n\n  \n").isEmpty())
    }

    @Test fun negativeNumbersAreNotBullets() {
        assertEquals(listOf(ReplyBlock.Paragraph("-5 is negative")), formatReply("-5 is negative"))
    }

    @Test fun downloadSizesAreHumanReadable() {
        assertEquals("555 MB", formatBytes(554_661_243))
        assertEquals("1.07 GB", formatBytes(1_073_765_694))
        assertEquals("0 MB", formatBytes(0))
    }
}
