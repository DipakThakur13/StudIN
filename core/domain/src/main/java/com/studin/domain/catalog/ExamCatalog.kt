package com.studin.domain.catalog

import com.studin.domain.importer.ParsedCard
import com.studin.domain.model.ExamType

data class CatalogTopic(val title: String, val priority: Int = 2, val aliases: List<String> = emptyList())
data class CatalogSubject(val name: String, val topics: List<CatalogTopic>)

/** Built-in syllabus packs and validated starter flashcards (offline, versioned). */
object ExamCatalog {
    const val VERSION = "catalog-2026.11"

    private fun t(title: String, p: Int = 2, vararg aliases: String) = CatalogTopic(title, p, aliases.toList())

    private val physicsJee = CatalogSubject("Physics", listOf(
        t("Units & Measurements", 1), t("Kinematics", 3, "motion", "projectile", "gati"),
        t("Laws of Motion", 3, "newton", "friction"), t("Work, Energy & Power", 3, "urja"),
        t("Rotational Motion", 3, "torque", "moment of inertia"), t("Gravitation", 2, "gurutvakarshan"),
        t("Properties of Matter", 2, "elasticity", "fluid", "viscosity"), t("Thermodynamics", 3, "heat", "ushma"),
        t("Kinetic Theory of Gases", 1), t("Oscillations & Waves", 3, "shm", "sound"),
        t("Electrostatics", 3, "coulomb", "capacitor", "electric field"), t("Current Electricity", 3, "ohm", "kirchhoff"),
        t("Magnetic Effects of Current", 3, "biot savart", "ampere"), t("Electromagnetic Induction & AC", 3, "faraday", "lenz"),
        t("Ray & Wave Optics", 3, "lens", "mirror", "interference"), t("Modern Physics", 3, "photoelectric", "atom", "nuclei"),
        t("Semiconductors", 2, "diode", "transistor"),
    ))
    private val chemistry = CatalogSubject("Chemistry", listOf(
        t("Mole Concept", 3, "stoichiometry"), t("Atomic Structure", 2, "quantum numbers"),
        t("Chemical Bonding", 3, "vsepr", "hybridisation"), t("Thermodynamics (Chem)", 2, "enthalpy", "entropy"),
        t("Chemical Equilibrium", 3, "le chatelier"), t("Ionic Equilibrium", 3, "ph", "buffer"),
        t("Redox & Electrochemistry", 3, "nernst", "cell"), t("Chemical Kinetics", 2, "rate law"),
        t("Periodic Table", 2, "periodicity"), t("Coordination Compounds", 3, "ligand", "cft"),
        t("p-Block Elements", 2), t("d & f Block", 2), t("GOC & Isomerism", 3, "organic chemistry", "isomers"),
        t("Hydrocarbons", 2, "alkane", "alkene"), t("Alcohols, Phenols & Ethers", 2),
        t("Aldehydes, Ketones & Acids", 3, "carbonyl"), t("Amines & Biomolecules", 2),
    ))
    private val mathsJee = CatalogSubject("Mathematics", listOf(
        t("Sets, Relations & Functions", 2), t("Complex Numbers", 3, "iota", "argand"),
        t("Quadratic Equations", 2, "roots"), t("Sequences & Series", 2, "ap", "gp"),
        t("Permutations & Combinations", 3, "pnc"), t("Binomial Theorem", 2),
        t("Matrices & Determinants", 3), t("Limits & Continuity", 2), t("Differentiation & AOD", 3, "derivative", "maxima"),
        t("Integral Calculus", 3, "integration", "definite integral", "area under curve"),
        t("Differential Equations", 2), t("Coordinate Geometry", 3, "straight line", "circle", "parabola", "ellipse", "hyperbola"),
        t("Vectors & 3D Geometry", 3), t("Probability", 3, "bayes"), t("Trigonometry", 2, "trigo"),
        t("Statistics", 1, "mean", "variance"),
    ))
    private val biology = CatalogSubject("Biology", listOf(
        t("Cell: Structure & Function", 3, "cell biology", "organelle"), t("Cell Cycle & Division", 2, "mitosis", "meiosis"),
        t("Biomolecules", 2, "enzymes", "protein"), t("Plant Physiology", 3, "photosynthesis", "respiration in plants"),
        t("Human Physiology", 3, "digestion", "circulation", "neural control"), t("Reproduction", 3, "reproductive health"),
        t("Genetics & Inheritance", 3, "mendel", "genetics"), t("Molecular Basis of Inheritance", 3, "dna", "replication", "transcription"),
        t("Evolution", 2, "darwin"), t("Human Health & Disease", 2, "immunity"), t("Biotechnology", 3, "pcr", "rdna"),
        t("Ecology & Environment", 3, "ecosystem", "biodiversity"), t("Plant Kingdom", 2, "botany"), t("Animal Kingdom", 2, "zoology"),
        t("Structural Organisation", 1, "morphology", "anatomy"),
    ))
    private val physicsNeet = physicsJee.copy(topics = physicsJee.topics.map { if (it.title == "Rotational Motion") it.copy(priority = 2) else it })

    private val subjects: Map<ExamType, List<CatalogSubject>> = mapOf(
        ExamType.JEE to listOf(physicsJee, chemistry, mathsJee),
        ExamType.NEET to listOf(physicsNeet, chemistry, biology),
        ExamType.UPSC to listOf(
            CatalogSubject("Polity", listOf(t("Constitution & Preamble", 3, "samvidhan", "संविधान"), t("Fundamental Rights & DPSP", 3, "fundamental duties"),
                t("Parliament & State Legislature", 3, "lok sabha", "rajya sabha"), t("Executive & Judiciary", 3, "supreme court", "president"),
                t("Federalism & Local Government", 2, "panchayati raj"), t("Constitutional Bodies", 2, "election commission", "cag"))),
            CatalogSubject("History", listOf(t("Ancient India", 2, "harappan", "maurya", "itihas"), t("Medieval India", 2, "mughal", "delhi sultanate"),
                t("Modern India & Freedom Struggle", 3, "national movement", "gandhi"), t("Art & Culture", 3, "architecture", "dance forms"),
                t("World History", 1))),
            CatalogSubject("Geography", listOf(t("Physical Geography", 3, "geomorphology", "climatology", "bhugol"), t("Indian Geography", 3, "monsoon", "rivers"),
                t("World Geography & Mapping", 2), t("Human & Economic Geography", 2))),
            CatalogSubject("Economy", listOf(t("Macroeconomics & GDP", 3, "inflation", "national income"), t("Money, Banking & RBI", 3, "monetary policy"),
                t("Budget & Fiscal Policy", 3, "taxation"), t("External Sector", 2, "balance of payments"), t("Agriculture & Industry", 2))),
            CatalogSubject("Environment & S&T", listOf(t("Ecology & Biodiversity", 3, "national parks"), t("Climate Change", 3, "cop", "unfccc"),
                t("Science & Technology", 2, "space", "isro", "biotech"))),
            CatalogSubject("Ethics & Essay", listOf(t("Ethics, Integrity & Aptitude", 3, "gs4"), t("Essay Writing", 2, "answer writing"))),
        ),
        ExamType.GATE_CS to listOf(
            CatalogSubject("Core CS", listOf(t("Data Structures", 3, "dsa", "trees", "graphs", "heap"), t("Algorithms", 3, "dynamic programming", "greedy", "complexity"),
                t("Theory of Computation", 3, "toc", "automata", "dfa", "turing machine"), t("Compiler Design", 2, "parsing", "lexical analysis"),
                t("Operating Systems", 3, "scheduling", "deadlock", "paging"), t("Databases", 3, "dbms", "sql", "normalization", "transactions"),
                t("Computer Networks", 3, "tcp", "routing", "osi"), t("Computer Organization", 3, "coa", "pipelining", "cache"),
                t("Digital Logic", 2, "k map", "flip flop"), t("Programming in C", 2, "pointers", "recursion"))),
            CatalogSubject("Mathematics", listOf(t("Discrete Mathematics", 3, "graph theory", "set theory", "logic"), t("Linear Algebra", 2, "eigenvalues"),
                t("Calculus", 1), t("Probability & Statistics", 2))),
            CatalogSubject("Aptitude", listOf(t("General Aptitude", 2, "verbal", "numerical ability"))),
        ),
        ExamType.CAT to listOf(
            CatalogSubject("Quant", listOf(t("Arithmetic", 3, "percentages", "profit loss", "time speed distance"), t("Algebra", 3, "equations", "inequalities"),
                t("Geometry & Mensuration", 2), t("Number System", 2), t("Modern Maths", 2, "pnc", "probability"))),
            CatalogSubject("VARC", listOf(t("Reading Comprehension", 3, "rc"), t("Para Jumbles & Summary", 2, "parajumble"), t("Vocabulary & Grammar", 1))),
            CatalogSubject("DILR", listOf(t("Data Interpretation", 3, "di", "charts"), t("Logical Reasoning", 3, "lr", "arrangements", "puzzles"))),
        ),
        ExamType.COLLEGE_CS to listOf(
            CatalogSubject("Programming", listOf(t("Programming Fundamentals", 2, "python", "java", "c++"), t("Object Oriented Design", 2, "oop", "design patterns"),
                t("Data Structures & Algorithms", 3, "dsa", "leetcode", "competitive programming"))),
            CatalogSubject("Systems", listOf(t("Operating Systems", 3), t("Computer Networks", 2), t("Database Systems", 3, "sql"), t("Computer Architecture", 2))),
            CatalogSubject("Theory & Maths", listOf(t("Discrete Mathematics", 2), t("Theory of Computation", 2), t("Linear Algebra", 1), t("Probability", 2))),
            CatalogSubject("Applied", listOf(t("Machine Learning", 2, "ml", "neural networks"), t("Web & Mobile Development", 1, "android", "kotlin"), t("Software Engineering", 1))),
        ),
        ExamType.BOARDS to listOf(
            CatalogSubject("Science", listOf(t("Chemical Reactions", 2), t("Acids, Bases & Salts", 2), t("Life Processes", 3), t("Electricity", 3),
                t("Light – Reflection & Refraction", 3), t("Heredity & Evolution", 2), t("Magnetic Effects", 2))),
            CatalogSubject("Mathematics", listOf(t("Real Numbers", 1), t("Polynomials", 2), t("Linear Equations", 2), t("Quadratic Equations", 2),
                t("Trigonometry", 3), t("Coordinate Geometry", 2), t("Statistics & Probability", 2), t("Circles & Constructions", 2))),
            CatalogSubject("English", listOf(t("Reading & Comprehension", 2), t("Writing Skills", 2, "letter writing", "essay"), t("Literature", 3))),
            CatalogSubject("Social Science", listOf(t("History", 2), t("Geography", 2), t("Civics", 2), t("Economics", 2))),
        ),
    )

    fun subjectsFor(exam: ExamType): List<CatalogSubject> = subjects.getValue(exam)

    private val physicsCards = listOf(
        ParsedCard("SI unit of electric charge?", "Coulomb (C) = 1 ampere × 1 second"),
        ParsedCard("Range of a projectile on level ground?", "R = u² sin 2θ / g, maximum at θ = 45°"),
        ParsedCard("Newton's second law (general form)?", "F = dp/dt; for constant mass F = ma"),
        ParsedCard("Work–energy theorem?", "Net work done on a body equals its change in kinetic energy"),
        ParsedCard("Escape velocity from Earth's surface?", "vₑ = √(2gR) ≈ 11.2 km/s"),
        ParsedCard("Time period of a simple pendulum?", "T = 2π √(L/g)"),
        ParsedCard("Coulomb's law?", "F = (1/4πε₀) · q₁q₂ / r²"),
        ParsedCard("Equivalent resistance of resistors in parallel?", "1/R = 1/R₁ + 1/R₂ + …"),
        ParsedCard("Einstein's photoelectric equation?", "hν = φ + KEmax"),
        ParsedCard("Lens formula?", "1/f = 1/v − 1/u"),
    )
    private val chemCards = listOf(
        ParsedCard("Avogadro's number?", "6.022 × 10²³ per mole"),
        ParsedCard("Hybridisation of carbon in methane?", "sp³ (tetrahedral, 109.5°)"),
        ParsedCard("pH of a neutral solution at 25 °C?", "7"),
        ParsedCard("Le Chatelier's principle?", "A system at equilibrium shifts to counteract an imposed change"),
        ParsedCard("Nernst equation (298 K)?", "E = E° − (0.0591/n) log Q"),
        ParsedCard("Order of reaction with half-life independent of concentration?", "First order (t½ = 0.693/k)"),
    )
    private val mathCards = listOf(
        ParsedCard("Derivative of sin x?", "cos x"),
        ParsedCard("∫ 1/x dx?", "ln|x| + C"),
        ParsedCard("Number of ways to arrange n distinct objects?", "n!"),
        ParsedCard("Sum of first n natural numbers?", "n(n + 1)/2"),
        ParsedCard("Modulus of complex number a + ib?", "√(a² + b²)"),
        ParsedCard("Bayes' theorem?", "P(A|B) = P(B|A)·P(A) / P(B)"),
    )
    private val bioCards = listOf(
        ParsedCard("Powerhouse of the cell?", "Mitochondria – site of aerobic respiration (ATP)"),
        ParsedCard("Where does the light reaction of photosynthesis occur?", "Thylakoid membranes of the chloroplast"),
        ParsedCard("Mendel's law of segregation?", "Allele pairs separate during gamete formation"),
        ParsedCard("Enzyme used to join DNA fragments?", "DNA ligase"),
        ParsedCard("Functional unit of the kidney?", "Nephron"),
        ParsedCard("Universal donor blood group?", "O negative"),
    )

    fun starterCards(exam: ExamType): Pair<String, List<ParsedCard>> = when (exam) {
        ExamType.JEE -> "JEE Essentials" to (physicsCards + chemCards + mathCards)
        ExamType.NEET -> "NEET Essentials" to (bioCards + physicsCards.take(6) + chemCards)
        ExamType.UPSC -> "UPSC Polity & GS" to listOf(
            ParsedCard("Which Article abolishes untouchability?", "Article 17"),
            ParsedCard("Right to Constitutional Remedies is in which Article?", "Article 32 – called the 'heart and soul' by Dr. Ambedkar"),
            ParsedCard("Which amendment added 'Socialist' and 'Secular' to the Preamble?", "42nd Amendment, 1976"),
            ParsedCard("Who appoints the Chief Election Commissioner?", "The President of India"),
            ParsedCard("Money Bill is defined under which Article?", "Article 110"),
            ParsedCard("Panchayati Raj got constitutional status by?", "73rd Amendment Act, 1992"),
            ParsedCard("Repo rate is?", "Rate at which RBI lends short-term funds to commercial banks"),
            ParsedCard("Harappan site with a dockyard?", "Lothal (Gujarat)"),
            ParsedCard("Dandi March year?", "1930 – Salt Satyagraha"),
            ParsedCard("Largest river basin in India?", "Ganga basin"),
            ParsedCard("Fiscal deficit?", "Total expenditure − total receipts excluding borrowings"),
            ParsedCard("Ramsar Convention concerns?", "Conservation of wetlands"),
        )
        ExamType.GATE_CS -> "GATE CS Core" to listOf(
            ParsedCard("Time complexity of building a binary heap?", "O(n)"),
            ParsedCard("Master theorem: T(n)=2T(n/2)+n?", "Θ(n log n)"),
            ParsedCard("Necessary conditions for deadlock?", "Mutual exclusion, hold & wait, no preemption, circular wait"),
            ParsedCard("BCNF condition?", "For every non-trivial FD X → Y, X is a superkey"),
            ParsedCard("Language accepted by a PDA?", "Context-free languages"),
            ParsedCard("Is the halting problem decidable?", "No – it is undecidable (but recursively enumerable)"),
            ParsedCard("TCP handshake steps?", "SYN → SYN-ACK → ACK"),
            ParsedCard("Speedup of k-stage ideal pipeline for n tasks?", "nk / (k + n − 1)"),
            ParsedCard("Dijkstra fails with?", "Negative edge weights"),
            ParsedCard("Number of edges in a tree with n vertices?", "n − 1"),
            ParsedCard("ACID stands for?", "Atomicity, Consistency, Isolation, Durability"),
            ParsedCard("Page fault occurs when?", "A referenced page is not in main memory"),
        )
        ExamType.CAT -> "CAT Quick Recall" to listOf(
            ParsedCard("Successive discounts of a% and b% equal?", "a + b − ab/100 %"),
            ParsedCard("Average speed for equal distances at x and y?", "2xy / (x + y)"),
            ParsedCard("Sum of interior angles of an n-gon?", "(n − 2) × 180°"),
            ParsedCard("Number of factors of N = pᵃ qᵇ?", "(a + 1)(b + 1)"),
            ParsedCard("Compound interest amount?", "A = P(1 + r/100)ⁿ"),
            ParsedCard("If x% of A = y% of B then A : B?", "y : x"),
            ParsedCard("nCr formula?", "n! / (r!(n − r)!)"),
            ParsedCard("Para jumble first step?", "Find the independent opening sentence and mandatory pairs"),
        )
        ExamType.COLLEGE_CS -> "CS Fundamentals" to listOf(
            ParsedCard("Big-O of binary search?", "O(log n)"),
            ParsedCard("Difference between process and thread?", "Threads share their process's address space; processes are isolated"),
            ParsedCard("What does a hash map's load factor control?", "When the table resizes – trades memory for collision rate"),
            ParsedCard("Four pillars of OOP?", "Encapsulation, Abstraction, Inheritance, Polymorphism"),
            ParsedCard("What is a race condition?", "Outcome depends on the timing of concurrent operations on shared state"),
            ParsedCard("SQL clause to filter groups?", "HAVING"),
            ParsedCard("Stable sorting algorithm with O(n log n) worst case?", "Merge sort"),
            ParsedCard("What is overfitting?", "Model fits training noise and generalises poorly"),
            ParsedCard("HTTP status 404 means?", "Not Found"),
            ParsedCard("Kotlin keyword for a null-safe call?", "?. (safe-call operator)"),
        )
        ExamType.BOARDS -> "Board Exam Basics" to listOf(
            ParsedCard("Ohm's law?", "V = IR"),
            ParsedCard("Chemical formula of baking soda?", "NaHCO₃ (sodium hydrogen carbonate)"),
            ParsedCard("sin²θ + cos²θ = ?", "1"),
            ParsedCard("Quadratic formula?", "x = [−b ± √(b² − 4ac)] / 2a"),
            ParsedCard("Site of photosynthesis?", "Chloroplast"),
            ParsedCard("Power of a lens formula?", "P = 1/f (f in metres), unit dioptre"),
            ParsedCard("Euclid's division lemma?", "a = bq + r, 0 ≤ r < b"),
            ParsedCard("pH less than 7 indicates?", "Acidic solution"),
        )
    }
}
