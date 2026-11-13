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

    @Test fun inlineMarkdownFromTheBugReportRendersWithoutRawMarkers() {
        // Output Gemma produced in the field: literal ** and ` showed on screen.
        val spans = parseInline("**Print Statement:** The `print()` function lets you display *text*.")
        assertEquals(listOf(
            InlineSpan("Print Statement:", bold = true), InlineSpan(" The "), InlineSpan("print()", code = true),
            InlineSpan(" function lets you display "), InlineSpan("text", italic = true), InlineSpan("."),
        ), spans)
        assertTrue(spans.none { "*" in it.text || "`" in it.text })
    }

    @Test fun unbalancedMarkersAreHiddenWhileStreaming() {
        assertEquals(listOf(InlineSpan("What you")), parseInline("**What you"))
        assertEquals(listOf(InlineSpan("2 * 3 = 6")), parseInline("2 * 3 = 6"))
    }

    @Test fun headingsAndPlainCopy() {
        val blocks = formatReply("## Newton's laws\n- **First:** inertia\n```\nF = ma\n```")
        assertEquals(ReplyBlock.Heading("Newton's laws"), blocks[0])
        assertEquals("Newton's laws\n• First: inertia\nF = ma", plainReply("## Newton's laws\n- **First:** inertia\n```\nF = ma\n```"))
    }

    @Test fun emptyBulletsAreNotRendered() {
        val blocks = formatReply("Intro\n-\n- Real point\n•\n3.\n1. First")
        assertEquals(
            listOf(ReplyBlock.Paragraph("Intro"), ReplyBlock.Bullet("Real point"), ReplyBlock.Numbered("1", "First")),
            blocks,
        )
    }

    @Test fun readyStatusSaysGpuOrCpu() {
        assertTrue(engineStatus(LlmState.Ready("Qwen3 4B", 1, onGpu = true), useLlm = true).detail.endsWith("GPU"))
        assertTrue(engineStatus(LlmState.Ready("Qwen2.5 1.5B", 1, onGpu = false), useLlm = true).detail.endsWith("CPU"))
    }
}
