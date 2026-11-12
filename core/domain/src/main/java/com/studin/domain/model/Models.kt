package com.studin.domain.model

/** Exams StudIN ships syllabus packs for. */
enum class ExamType(val displayName: String, val tagline: String) {
    JEE("JEE Main / Advanced", "Physics · Chemistry · Maths"),
    NEET("NEET UG", "Physics · Chemistry · Biology"),
    UPSC("UPSC CSE", "GS · Polity · History · Economy"),
    GATE_CS("GATE CS/IT", "Algorithms · OS · DBMS · Networks"),
    CAT("CAT", "Quant · VARC · DILR"),
    COLLEGE_CS("College CS", "Programming · DSA · Systems"),
    BOARDS("Class 10 / 12 Boards", "Science · Maths · English"),
}

/** Focus session lifecycle from the roadmap: IDLE → PREPARING → ACTIVE ⇄ PAUSED → COMPLETED / ABORTED. */
enum class FocusState { IDLE, PREPARING, ACTIVE, PAUSED, COMPLETED, ABORTED;
    val isRunning: Boolean get() = this == ACTIVE || this == PAUSED || this == PREPARING
}

/** How an app is treated while a focus session is running. */
enum class RuleMode {
    /** Never interrupted. */
    ALLOW,
    /** Full block during focus (gate screen shown). */
    BLOCK,
    /** Syllabus-aware: only distracting content is gated; study content passes. */
    SMART,
}

/** Classifier / policy verdict. BLOCK is only produced at high confidence. */
enum class Verdict { ALLOW, WARN, BLOCK, UNKNOWN }

/** Spaced-repetition answer grade. */
enum class Grade(val label: String) { AGAIN("Again"), HARD("Hard"), GOOD("Good"), EASY("Easy") }
