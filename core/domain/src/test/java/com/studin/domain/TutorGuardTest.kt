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

    @Test fun shortNewTopicsAreNotFollowUps() {
        // Bug report: "linux os" right after "operating system" got the previous answer mixed in.
        assertFalse(TutorGuard.isFollowUp("linux os"))
        assertFalse(TutorGuard.isFollowUp("binary search"))
        assertFalse(TutorGuard.isFollowUp("photosynthesis"))
        assertFalse(TutorGuard.isFollowUp("what is gate exam"))
        assertTrue(TutorGuard.isFollowUp("but why"))
        assertTrue(TutorGuard.isFollowUp("more"))
        assertTrue(TutorGuard.isFollowUp("explain in hindi"))
        assertTrue(TutorGuard.isFollowUp("what about windows"))
    }

    @Test fun codeQuestionsGetRoomForCode() {
        assertTrue(TutorGuard.wantsCode("python print"))
        assertTrue(TutorGuard.wantsCode("Write a C++ program for binary search"))
        assertTrue(TutorGuard.wantsCode("binary search code in java"))
        assertFalse(TutorGuard.wantsCode("what is an operating system"))
        assertEquals(220, TutorGuard.maxWordsFor("python print", TutorGuard.Intent.STUDY))
        assertEquals(120, TutorGuard.maxWordsFor("linux os", TutorGuard.Intent.STUDY))
        assertEquals(150, TutorGuard.maxWordsFor("anything", TutorGuard.Intent.SUMMARIZE))
        val p = TutorGuard.buildPrompt("binary search code in python", TutorGuard.Intent.STUDY, null)
        assertTrue(p.contains("under 220 words"))
        assertTrue(p.contains("Do not include website links"))
    }

    @Test fun hidesQwenThinkingAndChatTokens() {
        assertEquals("Paris is the capital.", ReplyGuard.clean("<think>\nThe user asks…\n</think>\n\nParis is the capital.<|im_end|>"))
        // Reasoning still streaming: show nothing yet.
        assertEquals("", ReplyGuard.clean("<think>Let me think about"))
        assertEquals("Answer.", ReplyGuard.clean("<|im_start|>assistant\nAnswer.<|endoftext|>").removePrefix("assistant").trim())
    }

    @Test fun dropsInventedLinks() {
        val raw = "GATE is a national exam. Visit https://gate.example.ac.in for the schedule.\n- Check [the official site](https://gate.iitk.ac.in) often.\n- Syllabus covers maths.\n- www.fake-gate.com"
        val t = ReplyGuard.clean(raw)
        assertFalse(t, t.contains("http") || t.contains("www."))
        assertTrue(t, t.contains("GATE is a national exam."))
        assertTrue(t, t.contains("Check the official site often."))
        assertTrue(t, t.contains("- Syllabus covers maths."))
        // URLs inside code are code, not links.
        val code = "```python\nrequests.get(\"https://api.example.com\")\n```"
        assertEquals(code, ReplyGuard.clean(code))
    }

    @Test fun dropsEmptyBulletsAndKeepsFlashcardFormat() {
        assertEquals("- One\n- Two", ReplyGuard.clean("- One\n-\n•\n- Two"))
        val cards = "Q: What is OS?\nA: System software.\nQ: What is a kernel?\nA: Core of the OS."
        assertEquals(cards, ReplyGuard.clean(cards, qaFormat = true))
        assertEquals("Q: What is OS?\nA: System software.", ReplyGuard.clean(cards))
    }
}
