package com.studin.domain

import com.studin.domain.ai.LiteAi
import com.studin.domain.classifier.ContentClassifier
import com.studin.domain.classifier.ExplicitLists
import com.studin.domain.classifier.TextNormalizer
import com.studin.domain.focus.FocusEvent
import com.studin.domain.focus.FocusPolicyEngine
import com.studin.domain.focus.FocusSnapshot
import com.studin.domain.focus.FocusStateMachine
import com.studin.domain.focus.IllegalFocusTransition
import com.studin.domain.importer.CsvDeckParser
import com.studin.domain.importer.CsvImportException
import com.studin.domain.model.FocusState
import com.studin.domain.model.Grade
import com.studin.domain.model.RuleMode
import com.studin.domain.model.Verdict
import com.studin.domain.plan.PlannerTopic
import com.studin.domain.plan.StudyPlanner
import com.studin.domain.quiz.QuizGenerator
import com.studin.domain.quiz.QuizSource
import com.studin.domain.srs.CardSchedule
import com.studin.domain.srs.Sm2Scheduler
import com.studin.domain.stats.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FocusStateMachineTest {
    private fun prepared() = FocusStateMachine.reduce(FocusSnapshot(), FocusEvent.Prepare(1, 25 * 60_000L, 0, null, "Kinematics"))

    @Test fun fullLifecycle() {
        var s = prepared()
        assertEquals(FocusState.PREPARING, s.state)
        s = FocusStateMachine.reduce(s, FocusEvent.Start(1_000))
        s = FocusStateMachine.reduce(s, FocusEvent.Pause(61_000))
        assertEquals(60_000, s.accumulatedMs)
        s = FocusStateMachine.reduce(s, FocusEvent.Resume(500_000))
        assertEquals(60_000 + 10_000, s.focusedMs(510_000))
        s = FocusStateMachine.reduce(s, FocusEvent.Tick(500_000 + 24 * 60_000L))
        assertEquals(FocusState.COMPLETED, s.state)
        assertEquals(25 * 60_000L, s.accumulatedMs)
    }

    @Test(expected = IllegalFocusTransition::class) fun cannotPauseWhenIdle() {
        FocusStateMachine.reduce(FocusSnapshot(), FocusEvent.Pause(0))
    }

    @Test fun abortKeepsReason() {
        var s = FocusStateMachine.reduce(prepared(), FocusEvent.Start(0))
        s = FocusStateMachine.reduce(s, FocusEvent.Abort(30_000, "Emergency exit"))
        assertEquals(FocusState.ABORTED, s.state); assertEquals("Emergency exit", s.endReason); assertEquals(30_000, s.accumulatedMs)
    }

    @Test fun reconcileAfterRebootPauses() {
        val s = FocusStateMachine.reduce(prepared(), FocusEvent.Start(900_000))
        val r = FocusStateMachine.reconcile(s, 5_000, rebooted = true)
        assertEquals(FocusState.PAUSED, r.state); assertEquals(0, r.accumulatedMs)
    }

    @Test fun randomEventSequencesNeverCorrupt() {
        val rnd = Random(42)
        repeat(500) {
            var s = prepared(); var now = 0L
            repeat(30) {
                now += rnd.nextLong(0, 120_000)
                val e = when (rnd.nextInt(6)) { 0 -> FocusEvent.Start(now); 1 -> FocusEvent.Pause(now); 2 -> FocusEvent.Resume(now)
                    3 -> FocusEvent.Tick(now); 4 -> FocusEvent.Finish(now); else -> FocusEvent.Abort(now, "x") }
                s = try { FocusStateMachine.reduce(s, e) } catch (_: IllegalFocusTransition) { s }
                assertTrue(s.focusedMs(now) in 0..s.plannedMs.coerceAtLeast(s.focusedMs(now)))
                assertTrue(s.accumulatedMs >= 0)
            }
        }
    }
}

class PolicyEngineTest {
    private val engine = FocusPolicyEngine()
    @Test fun emergencyWinsOverBlock() {
        assertEquals(Verdict.ALLOW, engine.decide("com.google.android.dialer", RuleMode.BLOCK, true, false).verdict)
    }
    @Test fun passBeatsBlockButNotUserAllow() {
        assertEquals(Verdict.ALLOW, engine.decide("com.x", RuleMode.BLOCK, true, passActive = true).verdict)
        assertEquals(Verdict.BLOCK, engine.decide("com.x", RuleMode.BLOCK, true, passActive = false).verdict)
    }
    @Test fun smartUnknownIsAllowed() {
        assertEquals(Verdict.UNKNOWN, engine.decide("com.google.android.youtube", RuleMode.SMART, true, false, null).verdict)
    }
    @Test fun noSessionNoBlock() {
        assertEquals(Verdict.ALLOW, engine.decide("com.x", RuleMode.BLOCK, sessionActive = false, passActive = false).verdict)
    }
}

class ClassifierTest {
    private val c = ContentClassifier(listOf("Kinematics", "Electrostatics", "Coordination Compounds"))

    @Test fun studyTitlesAllowed() {
        listOf(
            "Kinematics One Shot | JEE Main 2027 | Physics Lecture",
            "NCERT Biology Class 11 Chapter 3 revision",
            "Electrostatics PYQs with solutions",
            "Polity lecture: Fundamental Rights for UPSC prelims",
            "भौतिकी अध्याय 2 व्याख्यान",
            "Ganit ke sawal – kaksha 12 padhai",
        ).forEach { assertEquals(it, Verdict.ALLOW, c.classify(it).verdict) }
    }

    @Test fun obviousDistractionBlocked() {
        val r = c.classify("BGMI funny moments gameplay prank 😂")
        assertEquals(Verdict.BLOCK, r.verdict)
        assertTrue(r.confidence > 0.9)
    }

    @Test fun singleWeakSignalNeverBlocks() {
        assertNotEquals(Verdict.BLOCK, c.classify("lofi beats").verdict)
        assertNotEquals(Verdict.BLOCK, c.classify("cricket").verdict)
    }

    @Test fun mixedIntentNotBlocked() {
        assertNotEquals(Verdict.BLOCK, c.classify("Physics in GTA 5 – kinematics explained").verdict)
    }

    @Test fun unknownWhenNoSignals() {
        assertEquals(Verdict.UNKNOWN, c.classify("Sunday morning").verdict)
    }

    @Test fun explicitListsWin() {
        val cl = ContentClassifier(lists = ExplicitLists(allowKeywords = setOf("Physics Wallah"), blockKeywords = setOf("bigg boss")))
        assertEquals(Verdict.ALLOW, cl.classify("physics wallah funny prank").verdict)
        assertEquals(Verdict.BLOCK, cl.classify("Bigg Boss highlights").verdict)
    }

    @Test fun hostileInputIsBounded() {
        val huge = "prank ".repeat(100_000) + "\u0000\u0007"
        val start = System.nanoTime()
        c.classify(huge)
        assertTrue((System.nanoTime() - start) / 1_000_000 < 500)
        assertTrue(TextNormalizer.tokenize(huge).size <= TextNormalizer.MAX_TOKENS)
    }

    @Test fun normalizerKeepsDevanagari() {
        assertTrue(TextNormalizer.hasDevanagari("रसायन"))
        assertEquals(listOf("रसायन", "class", "12"), TextNormalizer.tokenize("रसायन — Class 12!!"))
    }
}

class Sm2Test {
    @Test fun goodGradesGrowIntervals() {
        var s = CardSchedule(); val now = 0L
        s = Sm2Scheduler.review(s, Grade.GOOD, now); assertEquals(1.0, s.intervalDays, 0.0)
        s = Sm2Scheduler.review(s, Grade.GOOD, now); assertEquals(6.0, s.intervalDays, 0.0)
        s = Sm2Scheduler.review(s, Grade.GOOD, now); assertEquals(15.0, s.intervalDays, 0.01)
    }
    @Test fun againResetsAndCountsLapse() {
        var s = CardSchedule(repetitions = 4, intervalDays = 20.0)
        s = Sm2Scheduler.review(s, Grade.AGAIN, 1000)
        assertEquals(0, s.repetitions); assertEquals(1, s.lapses); assertEquals(1000 + 600_000, s.dueAt)
    }
    @Test fun easeNeverBelowFloor() {
        var s = CardSchedule()
        repeat(50) { s = Sm2Scheduler.review(s, Grade.HARD, 0) }
        assertTrue(s.easeFactor >= 1.3)
        assertTrue(s.intervalDays <= 365.0)
    }
}

class CsvTest {
    @Test fun parsesQuotedHeaderAndTabs() {
        val r = CsvDeckParser.parse("front,back\n\"What is 2+2?\",\"4, obviously\"\n\"Multi\nline\",\"a \"\"quote\"\"\"\n,missing\n")
        assertEquals(2, r.cards.size); assertEquals("4, obviously", r.cards[0].back)
        assertEquals("a \"quote\"", r.cards[1].back); assertEquals(1, r.skipped)
        assertEquals(1, CsvDeckParser.parse("Q\tA\nx\ty").cards.size)
    }
    @Test(expected = CsvImportException::class) fun rejectsHugeFiles() { CsvDeckParser.parse("a,b\n".repeat(300_000)) }
    @Test fun stripsControlChars() { assertEquals("ab", CsvDeckParser.clean("a\u0000b\u001B")) }
    @Test fun exportEscapesFormulas() { assertEquals("'=SUM(A1)", CsvDeckParser.escapeForExport("=SUM(A1)")) }
    @Test fun fuzzNeverCrashesUnexpectedly() {
        val rnd = Random(7); val alphabet = "ab,\"\n\r\t;=\u0000"
        repeat(2_000) {
            val s = String(CharArray(rnd.nextInt(200)) { alphabet[rnd.nextInt(alphabet.length)] })
            try { CsvDeckParser.parse(s) } catch (_: CsvImportException) {}
        }
    }
}

class QuizPlannerStreakTest {
    @Test fun quizHasFourUniqueOptionsWithCorrectAnswer() {
        val pool = (1..10L).map { QuizSource(it, "Q$it", "Answer $it") }
        val qs = QuizGenerator.generate(pool, 5, seed = 1)
        assertEquals(5, qs.size)
        qs.forEach { q -> assertEquals(4, q.options.toSet().size); assertEquals("Answer ${q.sourceId}", q.options[q.correctIndex]) }
    }
    @Test fun plannerFitsBudgetAndPrioritisesWeakHighWeightTopics() {
        val topics = listOf(
            PlannerTopic(1, "Physics", "Units", 1, 0.9, 100, 0), PlannerTopic(2, "Physics", "Electrostatics", 3, 0.1, 0, null),
            PlannerTopic(3, "Maths", "Probability", 2, 0.4, 30, 5),
        )
        val plan = StudyPlanner.planToday(topics, today = 10, examDay = 40, dailyMinutes = 120, dueCards = 0, missedYesterday = true)
        assertEquals("Easy restart", plan.first().title)
        assertEquals(2L, plan[1].topicId)
        assertTrue(plan.sumOf { it.minutes } <= 120 + 25)
    }
    @Test fun streakWithFreeze() {
        assertEquals(3, StreakCalculator.currentStreak(setOf(10, 9, 8), 10))
        assertEquals(4, StreakCalculator.currentStreak(setOf(10, 9, 7, 6), 10))
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), 10))
        assertEquals(3, StreakCalculator.bestStreak(setOf(1, 2, 3, 7, 8)))
    }
}

class LiteAiTest {
    @Test fun notesBecomeCards() {
        val notes = """
            Photosynthesis is the process by which green plants make food using sunlight.
            Mitochondria: the powerhouse of the cell where ATP is produced.
            F = ma
            The Treaty of Versailles was signed in 1919 after the First World War.
        """.trimIndent()
        val cards = LiteAi.notesToCards(notes)
        assertTrue(cards.any { it.front == "What is Photosynthesis?" })
        assertTrue(cards.joinToString { it.front }, cards.any { it.front.startsWith("What is Mitochondria") })
        assertTrue(cards.any { it.front.startsWith("Formula: F") })
        assertTrue(cards.size >= 4)
    }
    @Test fun parsesLlmQa() {
        val out = LiteAi.parseQaBlocks("1. **Q:** What is DNA?\n**A:** Deoxyribonucleic acid\n\nQ2: Unit of force?\nA2: Newton\n")
        assertEquals(2, out.size); assertEquals("Newton", out[1].back)
    }
    @Test fun retrievalRanksRelevantDocs() {
        val docs = listOf(LiteAi.Doc(1, "Coulomb's law", "force between charges", "card"), LiteAi.Doc(2, "Mitosis", "cell division", "card"))
        val hits = LiteAi.retrieve("what is the force between two charges", docs)
        assertEquals(1L, hits.first().doc.id); assertFalse(hits.any { it.doc.id == 2L })
    }
}
