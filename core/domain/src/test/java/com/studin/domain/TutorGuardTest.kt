package com.studin.domain

import com.studin.domain.ai.ReplyGuard
import com.studin.domain.ai.TutorGuard
import com.studin.domain.ai.TutorGuard.Intent
import com.studin.domain.classifier.ContentClassifier
import com.studin.domain.model.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorGuardRoutingTest {
    private val classifier = ContentClassifier()
    private val distraction: (String) -> Boolean = { classifier.classify(it).verdict == Verdict.BLOCK }
    private fun r(q: String) = TutorGuard.route(q, distraction)

    @Test fun identityNeverReachesTheModel() {
        listOf("who are you", "Who are you?", "what's your name", "are you ChatGPT", "tum kaun ho", "what can you do")
            .forEach { assertEquals(it, Intent.IDENTITY, r(it)) }
    }

    @Test fun greetingsAndThanks() {
        listOf("hi", "Hello!", "hey there", "namaste", "good morning").forEach { assertEquals(it, Intent.GREETING, r(it)) }
        listOf("thanks", "thank you", "ok thanks", "shukriya").forEach { assertEquals(it, Intent.THANKS, r(it)) }
        assertEquals(Intent.STUDY, r("hi, explain Newton's third law with an example"))
    }

    @Test fun planningUsesRealPlanner() {
        listOf("What should I study today?", "make a plan for today", "aaj kya padhu", "revision plan please").forEach { assertEquals(it, Intent.PLAN, r(it)) }
    }

    @Test fun quizWellbeingSafetyOffTopic() {
        assertEquals(Intent.QUIZ, r("quiz me on electrostatics"))
        assertEquals(Intent.WELLBEING, r("I can't focus and feel stressed"))
        assertEquals(Intent.SAFETY, r("I want to die, exams are too much"))
        assertEquals(Intent.SAFETY, r("mujhe marna chahta hu"))
        assertEquals(Intent.OFF_TOPIC, r("best BGMI gameplay funny moments prank"))
    }

    @Test fun studyQuestionsAndSummaries() {
        listOf("give me python code for print function", "Explain Coulomb's law", "What is photosynthesis?", "derive v = u + at")
            .forEach { assertEquals(it, Intent.STUDY, r(it)) }
        assertEquals(Intent.SUMMARIZE, r("Summarise these notes: mitochondria make ATP"))
        assertEquals(Intent.SUMMARIZE, r("x".repeat(400)))
    }

    @Test fun followUpDetection() {
        assertTrue(TutorGuard.isFollowUp("give an example"))
        assertTrue(TutorGuard.isFollowUp("why?"))
        assertTrue(TutorGuard.isFollowUp("explain it in simpler words"))
        assertFalse(TutorGuard.isFollowUp("What is the difference between mitosis and meiosis in plant cells during growth phase"))
    }
}

class TutorPromptTest {
    @Test fun rulesAreFirstAndSurviveAnyLength() {
        val p = TutorGuard.buildPrompt("q ".repeat(5_000), Intent.STUDY, "JEE", notes = List(5) { "note $it ".repeat(40) },
            previous = TutorGuard.Turn("old question", "old answer ".repeat(100)), maxChars = 2_000)
        assertTrue(p.length <= 2_000)
        assertTrue(p.startsWith("You are StudIN, a careful study tutor for a JEE student."))
        assertTrue("never-invent rule kept", p.contains("Never invent numbers"))
        assertFalse("follow-up dropped first", p.contains("old question"))
        assertTrue(p.contains("Question: q q"))
    }

    @Test fun noFakeDialogueAndQuestionLast() {
        val p = TutorGuard.buildPrompt("who discovered gravity", Intent.STUDY, null, notes = listOf("Newton → laws of motion"))
        assertFalse(p.contains("Student:")); assertFalse(p.contains("Tutor:"))
        assertTrue(p.trimEnd().endsWith("Question: who discovered gravity"))
        assertTrue(p.contains("- Newton → laws of motion"))
    }

    @Test fun summaryPromptOnlyUsesNotes() {
        val p = TutorGuard.buildPrompt("Mitochondria produce ATP. Ribosomes make proteins.", Intent.SUMMARIZE, "NEET")
        assertTrue(p.contains("Use only information that is in the notes"))
        assertTrue(p.trimEnd().endsWith("Notes:\nMitochondria produce ATP. Ribosomes make proteins."))
    }
}

class ReplyGuardTest {
    @Test fun stripsSpecialTokensAndFabricatedTurns() {
        val raw = "model\nThe print() function shows text.<end_of_turn>\nStudent: who are you\nTutor: I'm a robot"
        assertEquals("The print() function shows text.", ReplyGuard.clean(raw))
    }

    @Test fun cutsRepetitionLoops() {
        val loop = "Gravity pulls objects toward Earth. " + "Gravity is a force that attracts masses. ".repeat(6)
        val c = ReplyGuard.clean(loop)
        assertTrue(c, c.split("Gravity is a force").size <= 3)
        assertTrue(ReplyGuard.shouldStop(loop, 500))
    }

    @Test fun stopsAtSentenceEndAfterCap() {
        val text = "word ".repeat(120).trim() + "."
        assertTrue(ReplyGuard.shouldStop(text, 120))
        assertFalse(ReplyGuard.shouldStop("word ".repeat(60), 120))
        assertTrue(ReplyGuard.shouldStop("word ".repeat(160), 120)) // hard cap even mid-sentence
    }

    @Test fun finishEndsOnCompleteSentenceAndClosesCode() {
        assertEquals("First sentence is complete. Second one too.", ReplyGuard.finish("First sentence is complete. Second one too. Third one is cut of", truncated = true))
        assertTrue(ReplyGuard.finish("Here:\n```python\nprint(\"hi\")", truncated = true).trimEnd().endsWith("```"))
        assertEquals("Fine.", ReplyGuard.finish("Fine.", truncated = false))
    }
}
