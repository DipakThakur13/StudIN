package com.studin.domain.classifier

/**
 * Curated, versioned on-device lexicon. Weights are log-odds contributions
 * (positive = study, negative = distraction). Includes Hindi-English aliases.
 */
object Lexicon {
    const val VERSION = "lex-2026.11.1"

    /** Strong study signals that apply to every exam. */
    val STUDY: Map<String, Double> = mapOf(
        "lecture" to 2.2, "lectures" to 2.2, "class" to 1.0, "chapter" to 1.8, "ch" to 0.6,
        "ncert" to 2.6, "pyq" to 2.6, "pyqs" to 2.6, "previous year" to 2.4, "mock test" to 2.2, "mock" to 1.2,
        "one shot" to 2.0, "oneshot" to 2.0, "revision" to 2.0, "notes" to 1.6, "solutions" to 1.4,
        "solution" to 1.2, "tutorial" to 1.8, "explained" to 1.2, "concept" to 1.6, "concepts" to 1.6,
        "derivation" to 2.2, "numericals" to 2.2, "numerical" to 1.8, "practice" to 1.4, "questions" to 1.2,
        "syllabus" to 1.8, "exam" to 1.0, "preparation" to 1.4, "strategy" to 0.8, "lesson" to 1.8,
        "course" to 1.4, "crash course" to 2.0, "marathon" to 1.0, "formula" to 1.8, "formulas" to 1.8,
        "theorem" to 2.0, "proof" to 1.6, "dpp" to 2.2, "sample paper" to 2.2, "board exam" to 1.8,
        "jee" to 2.0, "neet" to 2.0, "upsc" to 2.0, "gate" to 1.4, "cat" to 0.4, "cbse" to 1.8, "icse" to 1.6,
        "iit" to 1.0, "aiims" to 1.2, "mains" to 1.2, "prelims" to 1.6, "advanced" to 0.6,
        "physics" to 1.8, "chemistry" to 1.8, "maths" to 1.6, "math" to 1.4, "mathematics" to 1.8,
        "biology" to 1.8, "botany" to 1.8, "zoology" to 1.8, "polity" to 2.0, "economy" to 1.4,
        "history" to 1.0, "geography" to 1.4, "algorithm" to 2.0, "algorithms" to 2.0, "dsa" to 2.2,
        "programming" to 1.6, "coding" to 1.0, "dbms" to 2.2, "os" to 0.6, "compiler" to 1.8, "calculus" to 2.0,
        "integration" to 1.4, "differentiation" to 1.8, "kinematics" to 2.2, "thermodynamics" to 2.2,
        "electrostatics" to 2.2, "organic" to 1.2, "inorganic" to 1.8, "genetics" to 2.0, "ecology" to 1.4,
        "quant" to 1.4, "aptitude" to 1.6, "reasoning" to 1.4, "grammar" to 1.4, "vocabulary" to 1.2,
        "editorial" to 1.2, "current affairs" to 1.6, "answer writing" to 2.2, "interview" to 0.4,
        // Hinglish / Hindi transliterations
        "padhai" to 2.0, "padho" to 1.4, "bhautiki" to 2.2, "rasayan" to 2.2, "ganit" to 2.2,
        "jeev vigyan" to 2.2, "vigyan" to 1.6, "itihas" to 2.0, "bhugol" to 2.0, "rajvyavastha" to 2.2,
        "samjhaya" to 1.0, "sawal" to 1.0, "prashn" to 1.2, "adhyay" to 2.0, "kaksha" to 1.6,
        // Devanagari
        "भौतिकी" to 2.2, "रसायन" to 2.2, "गणित" to 2.2, "जीव" to 1.0, "विज्ञान" to 1.8, "इतिहास" to 2.0,
        "भूगोल" to 2.0, "अध्याय" to 2.0, "कक्षा" to 1.6, "प्रश्न" to 1.4, "पढ़ाई" to 2.0, "व्याख्यान" to 2.2,
        "परीक्षा" to 1.4, "संविधान" to 2.0, "अर्थव्यवस्था" to 1.8,
    )

    /** Distraction signals. */
    val DISTRACTION: Map<String, Double> = mapOf(
        "prank" to -2.6, "vlog" to -2.2, "vlogs" to -2.2, "gameplay" to -2.6, "gaming" to -2.4,
        "bgmi" to -2.8, "pubg" to -2.8, "free fire" to -2.8, "freefire" to -2.8, "minecraft" to -2.0,
        "gta" to -2.4, "valorant" to -2.6, "fortnite" to -2.6, "roblox" to -2.4, "funny" to -2.0,
        "comedy" to -1.8, "meme" to -2.4, "memes" to -2.4, "roast" to -2.4, "reaction" to -1.8,
        "reacts" to -1.8, "trailer" to -1.8, "teaser" to -1.8, "song" to -1.8, "songs" to -1.8,
        "lyrics" to -1.8, "remix" to -2.0, "lofi" to -0.6, "music video" to -2.2, "dance" to -1.8,
        "movie" to -1.8, "web series" to -2.4, "episode" to -1.2, "bigg boss" to -2.8, "celebrity" to -2.0,
        "gossip" to -2.4, "unboxing" to -2.0, "haul" to -2.2, "challenge" to -1.2, "shorts" to -1.6,
        "reels" to -2.0, "reel" to -1.6, "tiktok" to -2.0, "highlights" to -1.6, "ipl" to -2.2,
        "cricket" to -1.6, "football" to -1.4, "match" to -0.8, "live stream" to -1.2, "streamer" to -2.0,
        "asmr" to -2.0, "mukbang" to -2.4, "fails" to -2.2, "satisfying" to -1.8, "dhamaal" to -1.8,
        "masti" to -1.8, "mazak" to -2.0, "full movie" to -2.8, "status video" to -2.4, "edit" to -1.0,
        "fan edit" to -2.2, "breakup" to -1.6, "dating" to -2.0, "relationship" to -1.2,
        "गाना" to -2.0, "मज़ेदार" to -2.0, "फिल्म" to -1.8,
    )

    /** Generic tokens that should not count as syllabus evidence on their own. */
    val STOP: Set<String> = setOf(
        "the", "a", "an", "of", "in", "on", "for", "to", "and", "is", "are", "with", "by", "at", "from",
        "video", "new", "best", "top", "full", "part", "how", "what", "why", "this", "that", "my", "your",
        "ka", "ki", "ke", "hai", "aur", "se", "me", "mein", "ko", "kya", "ek", "official", "hd", "4k",
    )
}
