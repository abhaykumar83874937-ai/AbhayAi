package com.example.data.offline

import com.example.data.model.QuizQuestion
import com.example.data.model.StudyToolType

data class OfflineSubjectPack(
    val subjectName: String,
    val quickFormulasAndRules: List<String>,
    val chapterNotesEnglish: String,
    val chapterNotesHindi: String,
    val quizQuestions: List<QuizQuestion>
)

data class OfflineDictionaryEntry(
    val word: String,
    val partOfSpeech: String,
    val englishMeaning: String,
    val hindiMeaning: String,
    val synonyms: String,
    val example: String
)

object OfflineStudyBank {

    val subjectPacks: Map<String, OfflineSubjectPack> = listOf(
        OfflineSubjectPack(
            subjectName = "Mathematics",
            quickFormulasAndRules = listOf(
                "Quadratic Formula: x = (-b ± √(b² - 4ac)) / 2a",
                "Pythagoras Theorem: a² + b² = c²",
                "Trig Identity: sin²θ + cos²θ = 1; tan θ = sin θ / cos θ",
                "Power Rule: d/dx(xⁿ) = n·xⁿ⁻¹ | ∫xⁿ dx = xⁿ⁺¹/(n+1) + C",
                "Mean = Σx / n | P(E) = n(E) / n(S)"
            ),
            chapterNotesEnglish = """
                ### Mathematics Offline Revision Notes
                - **Algebra Identities:** `(a + b)² = a² + 2ab + b²`, `(a - b)² = a² - 2ab + b²`, `a² - b² = (a + b)(a - b)`.
                - **Quadratic Equations:** Discriminant `Δ = b² - 4ac`. If `Δ > 0`, 2 real roots; if `Δ = 0`, 1 equal root; if `Δ < 0`, complex roots.
                - **Trigonometry Ratios:** `sin θ = Opp/Hyp`, `cos θ = Adj/Hyp`, `tan θ = Opp/Adj`. Standard values: `sin 30° = 1/2`, `cos 60° = 1/2`, `tan 45° = 1`.
                - **Statistics & Probability:** Median is the middle sorted value; Mode is the most frequent value; Probability always lies between `0` and `1`.
            """.trimIndent(),
            chapterNotesHindi = """
                ### गणित ऑफलाइन रिवीजन नोट्स
                - **बीजगणित सर्वसमिकाएँ:** `(a + b)² = a² + 2ab + b²`, `a² - b² = (a + b)(a - b)`.
                - **द्विघात समीकरण:** विविक्तकर `Δ = b² - 4ac`. यदि `Δ > 0` तो दो वास्तविक मूल, `Δ = 0` तो समान मूल।
                - **त्रिकोणमिति:** `sin²θ + cos²θ = 1`, `sin 30° = 1/2`, `tan 45° = 1`.
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "What is the discriminant of the quadratic equation 2x² - 7x + 3 = 0?",
                    options = listOf("25", "49", "24", "13"),
                    correctIndex = 0,
                    explanation = "Δ = b² - 4ac = (-7)² - 4(2)(3) = 49 - 24 = 25."
                ),
                QuizQuestion(
                    question = "If sin(θ) = 3/5 for an acute angle θ, what is cos(θ)?",
                    options = listOf("4/5", "3/4", "5/4", "1/5"),
                    correctIndex = 0,
                    explanation = "Using sin²θ + cos²θ = 1, cos²θ = 1 - 9/25 = 16/25, so cos θ = 4/5."
                ),
                QuizQuestion(
                    question = "What is the derivative of f(x) = 3x² + 2x - 1?",
                    options = listOf("6x + 2", "3x + 2", "6x - 1", "x³ + x²"),
                    correctIndex = 0,
                    explanation = "By the power rule d/dx(xⁿ) = n·xⁿ⁻¹, d/dx(3x² + 2x - 1) = 6x + 2."
                ),
                QuizQuestion(
                    question = "Find the median of the data set: 12, 15, 12, 18, 23.",
                    options = listOf("15", "12", "16", "18"),
                    correctIndex = 0,
                    explanation = "Sorted order is 12, 12, 15, 18, 23. The middle (3rd) value is 15."
                ),
                QuizQuestion(
                    question = "What is the probability of getting an even number on a single roll of a fair 6-sided die?",
                    options = listOf("1/2", "1/3", "1/6", "2/3"),
                    correctIndex = 0,
                    explanation = "Even outcomes are {2, 4, 6} (3 outcomes out of 6), so P = 3/6 = 1/2."
                )
            )
        ),
        OfflineSubjectPack(
            subjectName = "Physics",
            quickFormulasAndRules = listOf(
                "Newton's 2nd Law: F = m × a",
                "Equations of Motion: v = u + at | s = ut + ½at² | v² = u² + 2as",
                "Ohm's Law: V = I × R | Power: P = V × I = I²R",
                "Kinetic Energy: KE = ½mv² | Potential Energy: PE = mgh",
                "Mirror/Lens Formula: 1/f = 1/v + 1/u (mirror) | 1/f = 1/v - 1/u (lens)"
            ),
            chapterNotesEnglish = """
                ### Physics Offline Revision Notes
                - **Newton's Laws:** (1) Law of Inertia, (2) `F = ma` (rate of change of momentum), (3) Every action has an equal and opposite reaction.
                - **Electricity & Magnetism:** Potential difference `V = IR` (SI unit: Volt). Resistance increases with length and decreases with cross-sectional area.
                - **Work, Power & Energy:** Work `W = F·d·cosθ` (Joule). Total mechanical energy `KE + PE` remains conserved in the absence of friction.
            """.trimIndent(),
            chapterNotesHindi = """
                ### भौतिक विज्ञान ऑफलाइन नोट्स
                - **न्यूटन के नियम:** जड़त्व का नियम, `F = m × a`, तथा क्रिया-प्रतिक्रिया का नियम।
                - **ओम का नियम:** स्थिर ताप पर `V = I × R` (विभवांतर = धारा × प्रतिरोध)।
                - **गतिज ऊर्जा:** `KE = ½mv²`, स्थितिज ऊर्जा `PE = mgh`.
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "Which SI unit measures electric resistance?",
                    options = listOf("Ohm (Ω)", "Volt (V)", "Ampere (A)", "Watt (W)"),
                    correctIndex = 0,
                    explanation = "By Ohm's law R = V/I, resistance is measured in Ohms (Ω)."
                ),
                QuizQuestion(
                    question = "A 2 kg object accelerates at 4 m/s². What net force acts on it?",
                    options = listOf("8 N", "2 N", "6 N", "16 N"),
                    correctIndex = 0,
                    explanation = "F = m × a = 2 kg × 4 m/s² = 8 N."
                ),
                QuizQuestion(
                    question = "Which equation gives the kinetic energy of a moving body?",
                    options = listOf("½mv²", "mgh", "mv", "F × d"),
                    correctIndex = 0,
                    explanation = "Kinetic energy equals half the mass times velocity squared (½mv²)."
                )
            )
        ),
        OfflineSubjectPack(
            subjectName = "Chemistry",
            quickFormulasAndRules = listOf(
                "Ideal Gas Law: PV = nRT",
                "Molarity (M) = moles of solute / volume of solution (L)",
                "pH Scale: pH = -log₁₀[H⁺] | Neutral water pH = 7",
                "Avogadro's Number: Nₐ = 6.022 × 10²³ particles/mol",
                "OIL RIG: Oxidation Is Loss (of electrons), Reduction Is Gain"
            ),
            chapterNotesEnglish = """
                ### Chemistry Offline Revision Notes
                - **Chemical Bonding:** Ionic bonds transfer electrons (metal + non-metal); Covalent bonds share electron pairs (non-metals).
                - **Acids, Bases & Salts:** Acids have `pH < 7` and donate `H⁺`; Bases have `pH > 7` and accept `H⁺` or release `OH⁻`. Neutralization: `Acid + Base → Salt + Water`.
                - **Periodic Trends:** Across a period (left to right), atomic radius decreases and electronegativity increases.
            """.trimIndent(),
            chapterNotesHindi = """
                ### रसायन विज्ञान ऑफलाइन नोट्स
                - **रासायनिक बंधन:** आयनिक बंध में इलेक्ट्रॉनों का स्थानांतरण होता है, सहसंयोजक बंध में साझेदारी।
                - **pH मान:** अम्ल `pH < 7`, उदासीन `pH = 7`, क्षार `pH > 7`.
                - **अवोगाद्रो संख्या:** `6.022 × 10²³` कण प्रति मोल।
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "What is the pH of a neutral aqueous solution at 25°C?",
                    options = listOf("7", "0", "14", "1"),
                    correctIndex = 0,
                    explanation = "Pure neutral water at 25°C has equal [H⁺] and [OH⁻] ions, giving pH = 7."
                ),
                QuizQuestion(
                    question = "Which type of bond involves sharing pairs of electrons between atoms?",
                    options = listOf("Covalent bond", "Ionic bond", "Hydrogen bond", "Metallic bond"),
                    correctIndex = 0,
                    explanation = "Covalent bonds form when non-metal atoms share electron pairs."
                ),
                QuizQuestion(
                    question = "In redox reactions, what does Oxidation mean?",
                    options = listOf("Loss of electrons", "Gain of electrons", "Gain of protons", "Decrease in oxidation state"),
                    correctIndex = 0,
                    explanation = "Remember OIL RIG: Oxidation Is Loss of electrons, Reduction Is Gain."
                )
            )
        ),
        OfflineSubjectPack(
            subjectName = "Biology",
            quickFormulasAndRules = listOf(
                "Photosynthesis: 6CO₂ + 6H₂O + Light → C₆H₁₂O₆ + 6O₂",
                "Aerobic Respiration: C₆H₁₂O₆ + 6O₂ → 6CO₂ + 6H₂O + ~38 ATP",
                "DNA Base Pairing: Adenine–Thymine (A=T), Guanine–Cytosine (G≡C)",
                "Cell Division: Mitosis = 2 identical diploid cells | Meiosis = 4 haploid gametes"
            ),
            chapterNotesEnglish = """
                ### Biology Offline Revision Notes
                - **Cell Organelles:** Mitochondria (powerhouse, ATP), Chloroplasts (photosynthesis in plants), Ribosomes (protein synthesis), Nucleus (genetic DNA).
                - **Mitosis vs Meiosis:** Mitosis is for growth and repair (maintains chromosome number `2n`); Meiosis creates reproductive gametes (`n`).
                - **Human Circulatory System:** 4-chambered heart pumps oxygenated blood via aorta and receives deoxygenated blood via vena cava.
            """.trimIndent(),
            chapterNotesHindi = """
                ### जीव विज्ञान ऑफलाइन नोट्स
                - **कोशिकांग:** माइटोकॉन्ड्रिया (ऊर्जा गृह/ATP), हरितलवक (प्रकाश संश्लेषण), राइबोसोम (प्रोटीन निर्माण)।
                - **DNA क्षार युग्मन:** एडेनिन-थायमिन (A-T) और ग्वानिन-साइटोसिन (G-C)।
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "Which organelle is responsible for ATP energy production in eukaryotic cells?",
                    options = listOf("Mitochondria", "Golgi apparatus", "Lysosome", "Vacuole"),
                    correctIndex = 0,
                    explanation = "Mitochondria carry out aerobic cellular respiration to produce ATP."
                ),
                QuizQuestion(
                    question = "Which gas is released as a byproduct during photosynthesis?",
                    options = listOf("Oxygen (O₂)", "Carbon dioxide (CO₂)", "Nitrogen (N₂)", "Methane (CH₄)"),
                    correctIndex = 0,
                    explanation = "Plants split water molecules using light energy and release O₂ gas."
                ),
                QuizQuestion(
                    question = "In DNA, Adenine (A) always pairs with which nitrogenous base?",
                    options = listOf("Thymine (T)", "Cytosine (C)", "Guanine (G)", "Uracil (U)"),
                    correctIndex = 0,
                    explanation = "In double-stranded DNA, A pairs with T and G pairs with C."
                )
            )
        ),
        OfflineSubjectPack(
            subjectName = "English",
            quickFormulasAndRules = listOf(
                "Active → Passive: Object + be-verb + V3 (past participle) + by + Subject",
                "Subject-Verb Agreement: Singular subject takes singular verb (She writes, They write)",
                "Essay Structure: Hook & Thesis → 2–3 Body Paragraphs (PEEL) → Conclusion",
                "Figures of Speech: Simile uses 'like/as'; Metaphor makes a direct comparison"
            ),
            chapterNotesEnglish = """
                ### English Grammar & Writing Offline Notes
                - **Tenses Quick Guide:** Simple Present (`writes`), Present Continuous (`is writing`), Present Perfect (`has written`), Simple Past (`wrote`).
                - **Active & Passive Voice:** *Active:* "The teacher praised Riya." → *Passive:* "Riya was praised by the teacher."
                - **Direct vs Indirect Speech:** Shift tenses back in reported speech when the reporting verb is past (e.g. `"I am ready"` → `He said that he was ready`).
            """.trimIndent(),
            chapterNotesHindi = """
                ### अंग्रेज़ी ग्रामर ऑफलाइन नोट्स
                - **Active से Passive:** Object को आगे लाकर `be + V3` का प्रयोग करें।
                - **Subject-Verb Agreement:** एकवचन कर्ता के साथ एकवचन क्रिया (`He plays`, `They play`).
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "Choose the passive voice of: 'She writes a letter.'",
                    options = listOf(
                        "A letter is written by her.",
                        "A letter was written by her.",
                        "A letter is writing by her.",
                        "A letter has been written by she."
                    ),
                    correctIndex = 0,
                    explanation = "Simple present active ('writes') becomes 'is + V3 (written)' in passive voice."
                ),
                QuizQuestion(
                    question = "Which figure of speech compares two unlike things using 'like' or 'as'?",
                    options = listOf("Simile", "Metaphor", "Personification", "Alliteration"),
                    correctIndex = 0,
                    explanation = "A simile explicitly uses 'like' or 'as' (e.g., 'brave as a lion')."
                ),
                QuizQuestion(
                    question = "Select the grammatically correct sentence:",
                    options = listOf(
                        "Neither of the boys was present.",
                        "Neither of the boys were present.",
                        "Neither of the boy was present.",
                        "Neither of the boys are present."
                    ),
                    correctIndex = 0,
                    explanation = "'Neither of + plural noun' takes a singular verb ('was')."
                )
            )
        ),
        OfflineSubjectPack(
            subjectName = "Hindi",
            quickFormulasAndRules = listOf(
                "समास के मुख्य 6 भेद: अव्ययीभाव, तत्पुरुष, कर्मधारय, द्विगु, द्वंद्व, बहुव्रीहि",
                "संधि के 3 प्रकार: स्वर संधि, व्यंजन संधि, विसर्ग संधि",
                "अलंकार: अनुप्रास (वर्ण आवृत्ति), यमक (शब्द आवृत्ति भिन्न अर्थ), उपमा (तुलना)",
                "निबंध लेखन: प्रस्तावना → मुख्य विषय (लाभ/हानि/कारण) → उपसंहार"
            ),
            chapterNotesEnglish = """
                ### Hindi Grammar (हिन्दी व्याकरण) Offline Notes
                - **समास (Samas):** दो या दो से अधिक शब्दों के मेल से बने नए शब्द को समास कहते हैं (जैसे: *नीलकमल* = कर्मधारय, *माता-पिता* = द्वंद्व, *चौराहा* = द्विगु)।
                - **संधि (Sandhi):** दो निकटवर्ती वर्णों के मेल से होने वाले विकार को संधि कहते हैं (जैसे: *विद्या + आलय = विद्यालय* — दीर्घ स्वर संधि)।
                - **अलंकार:** काव्य की शोभा बढ़ाने वाले तत्व अलंकार कहलाते हैं।
            """.trimIndent(),
            chapterNotesHindi = """
                ### हिन्दी व्याकरण ऑफलाइन नोट्स
                - **समास:** द्विगु समास में पहला पद संख्यावाचक होता है (जैसे *त्रिभुज*), द्वंद्व में दोनों पद प्रधान होते हैं (*दिन-रात*)।
                - **रस:** काव्य को पढ़ने से उत्पन्न आनंद को रस कहते हैं (प्रमुख रस: शृंगार, वीर, करुण, हास्य, शांत)।
                - **मुहावरा:** 'आँख का तारा' = अत्यंत प्रिय, 'नौ दो ग्यारह होना' = भाग जाना।
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "'माता-पिता' शब्द में कौन-सा समास है?",
                    options = listOf("द्वंद्व समास", "द्विगु समास", "तत्पुरुष समास", "अव्ययीभाव समास"),
                    correctIndex = 0,
                    explanation = "जिस समास के दोनों पद प्रधान हों और योजक चिह्न या 'और' का लोप हो, वह द्वंद्व समास है।"
                ),
                QuizQuestion(
                    question = "'विद्या + आलय' किस संधि का उदाहरण है?",
                    options = listOf("दीर्घ स्वर संधि", "गुण स्वर संधि", "व्यंजन संधि", "विसर्ग संधि"),
                    correctIndex = 0,
                    explanation = "आ + आ = आ बनने के कारण यह दीर्घ स्वर संधि है (विद्यालय)।"
                ),
                QuizQuestion(
                    question = "'चौराहा' में कौन-सा समास है?",
                    options = listOf("द्विगु समास", "कर्मधारय समास", "बहुव्रीहि समास", "द्वंद्व समास"),
                    correctIndex = 0,
                    explanation = "पहला पद संख्यावाचक ('चौ' = चार) होने के कारण यह द्विगु समास है।"
                )
            )
        ),
        OfflineSubjectPack(
            subjectName = "History",
            quickFormulasAndRules = listOf(
                "Industrial Revolution: Started in Britain (~1760) — steam engine, factories, railways",
                "1857 Revolt: First major Indian uprising against British East India Company",
                "1919–1947 Freedom Milestones: Non-Cooperation (1920), Dandi March (1930), Quit India (1942)",
                "World Wars: WWI (1914–1918), WWII (1939–1945) → Formation of United Nations (1945)"
            ),
            chapterNotesEnglish = """
                ### History Offline Revision Notes
                - **Industrial Revolution:** Shifted production from hand tools at home to steam-powered machines in factories, accelerating urbanization and global trade.
                - **Indian National Movement:** Led by mass satyagraha campaigns—Champaran (1917), Non-Cooperation (1920), Civil Disobedience / Dandi Salt March (1930), Quit India (1942), Independence (15 Aug 1947).
                - **Modern World History:** Treaty of Versailles (1919) reshaped Europe after WWI; United Nations was founded in 1945 to maintain international peace.
            """.trimIndent(),
            chapterNotesHindi = """
                ### इतिहास ऑफलाइन नोट्स
                - **औद्योगिक क्रांति:** 18वीं सदी में ब्रिटेन से शुरुआत, भाप इंजन व कारखानों का विकास।
                - **भारतीय स्वतंत्रता संग्राम:** 1857 की क्रांति, असहयोग आंदोलन (1920), दांडी मार्च (1930), भारत छोड़ो आंदोलन (1942)।
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "In which year did the Dandi Salt March (Civil Disobedience Movement) begin?",
                    options = listOf("1930", "1920", "1942", "1919"),
                    correctIndex = 0,
                    explanation = "Mahatma Gandhi led the historic Dandi March in March–April 1930."
                ),
                QuizQuestion(
                    question = "Where did the Industrial Revolution first begin in the 18th century?",
                    options = listOf("Great Britain", "France", "Germany", "United States"),
                    correctIndex = 0,
                    explanation = "Britain led the Industrial Revolution due to coal, iron, capital, and steam inventions."
                ),
                QuizQuestion(
                    question = "When was the United Nations (UN) officially founded?",
                    options = listOf("1945", "1919", "1939", "1950"),
                    correctIndex = 0,
                    explanation = "The United Nations was established in October 1945 after World War II."
                )
            )
        ),
        OfflineSubjectPack(
            subjectName = "Geography",
            quickFormulasAndRules = listOf(
                "Atmosphere Layers (bottom → top): Troposphere, Stratosphere, Mesosphere, Thermosphere, Exosphere",
                "Earth Interior: Crust → Mantle → Outer Core (liquid) → Inner Core (solid)",
                "Weathering vs Erosion: Weathering breaks rocks in place; Erosion transports sediments",
                "Monsoon Mechanism: Seasonal wind reversal driven by differential heating of land & ocean"
            ),
            chapterNotesEnglish = """
                ### Geography Offline Revision Notes
                - **Atmosphere:** Troposphere contains weather phenomena; Stratosphere holds the protective Ozone (`O₃`) layer.
                - **Monsoon Winds:** In summer, low pressure over heated landmass pulls moist winds from the Indian Ocean (Southwest Monsoon).
                - **Soil & Landforms:** Alluvial soil is most fertile for agriculture; Black (Regur) soil retains moisture and suits cotton.
            """.trimIndent(),
            chapterNotesHindi = """
                ### भूगोल ऑफलाइन नोट्स
                - **वायुमंडल की परतें:** क्षोभमंडल (मौसम), समतापमंडल (ओजोन परत), मध्यमंडल, आयनमंडल, बहिर्मंडल।
                - **अपक्षय व अपरदन:** चट्टानों का अपने स्थान पर टूटना अपक्षय और कणों का बहकर जाना अपरदन है।
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "Which layer of Earth's atmosphere contains the protective Ozone layer?",
                    options = listOf("Stratosphere", "Troposphere", "Mesosphere", "Thermosphere"),
                    correctIndex = 0,
                    explanation = "The ozone layer lies mostly in the Stratosphere and absorbs harmful UV radiation."
                ),
                QuizQuestion(
                    question = "Which process breaks down rocks in place without transporting the fragments?",
                    options = listOf("Weathering", "Erosion", "Deposition", "Subduction"),
                    correctIndex = 0,
                    explanation = "Weathering disintegrates rocks in situ, whereas erosion moves the particles."
                ),
                QuizQuestion(
                    question = "Which soil type in India is also known as Regur soil and is ideal for cotton?",
                    options = listOf("Black soil", "Alluvial soil", "Laterite soil", "Arid soil"),
                    correctIndex = 0,
                    explanation = "Black (Regur) soil has high moisture-holding capacity and is famous for cotton cultivation."
                )
            )
        ),
        OfflineSubjectPack(
            subjectName = "Political Science",
            quickFormulasAndRules = listOf(
                "3 Organs of Government: Legislature (makes laws), Executive (enforces), Judiciary (interprets)",
                "Indian Constitution Fundamental Rights: Articles 12–35 (Part III)",
                "Federalism: Division of powers across Union, State, and Concurrent Lists",
                "Parliamentary Democracy: Executive is accountable to the elected Legislature"
            ),
            chapterNotesEnglish = """
                ### Political Science Offline Revision Notes
                - **Separation of Powers:** Prevents concentration of authority—Legislature passes laws, Executive implements policies, and an independent Judiciary safeguards the Constitution.
                - **Fundamental Rights:** Right to Equality, Right to Freedom, Right against Exploitation, Freedom of Religion, Cultural & Educational Rights, Constitutional Remedies (Article 32).
                - **Democracy:** Guarantees universal adult franchise, periodic free elections, rule of law, and protection of minority rights.
            """.trimIndent(),
            chapterNotesHindi = """
                ### राजनीति विज्ञान ऑफलाइन नोट्स
                - **सरकार के तीन अंग:** विधायिका (कानून बनाना), कार्यपालिका (लागू करना), न्यायपालिका (न्याय व संविधान रक्षा)।
                - **मौलिक अधिकार (अनुच्छेद 12-35):** समानता, स्वतंत्रता, शोषण के विरुद्ध, धार्मिक स्वतंत्रता, शिक्षा-संस्कृति व संवैधानिक उपचार (अनुच्छेद 32)।
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "Which organ of government is responsible for interpreting laws and safeguarding the Constitution?",
                    options = listOf("Judiciary", "Legislature", "Executive", "Election Commission"),
                    correctIndex = 0,
                    explanation = "The independent Judiciary interprets laws and enforces constitutional limits."
                ),
                QuizQuestion(
                    question = "Which Article of the Indian Constitution provides the Right to Constitutional Remedies?",
                    options = listOf("Article 32", "Article 14", "Article 21", "Article 370"),
                    correctIndex = 0,
                    explanation = "Dr. B.R. Ambedkar called Article 32 the heart and soul of the Constitution."
                ),
                QuizQuestion(
                    question = "What does Federalism mean in governance?",
                    options = listOf(
                        "Division of powers between central and regional governments",
                        "Rule by a single central monarch",
                        "Elimination of state legislatures",
                        "Military control of parliament"
                    ),
                    correctIndex = 0,
                    explanation = "Federalism constitutionally divides powers between national and state levels."
                )
            )
        ),
        OfflineSubjectPack(
            subjectName = "Computer Science",
            quickFormulasAndRules = listOf(
                "Time Complexity Order: O(1) < O(log n) < O(n) < O(n log n) < O(n²)",
                "Binary Search: Requires sorted array; O(log n) time",
                "Stack = LIFO (Last In First Out) | Queue = FIFO (First In First Out)",
                "SQL Commands: SELECT, INSERT, UPDATE, DELETE | ACID = Atomicity, Consistency, Isolation, Durability"
            ),
            chapterNotesEnglish = """
                ### Computer Science Offline Revision Notes
                - **Data Structures:** Arrays give `O(1)` random index access; Linked Lists allow `O(1)` insertion at head; HashMaps provide average `O(1)` key lookup.
                - **Algorithms:** Binary Search halves the search space each step (`O(log n)`). Merge Sort & Quick Sort run in `O(n log n)` average time.
                - **Databases:** SQL databases use structured tables with schemas and ACID transactions; NoSQL stores flexible documents/key-values for horizontal scaling.
            """.trimIndent(),
            chapterNotesHindi = """
                ### कंप्यूटर साइंस ऑफलाइन नोट्स
                - **डेटा स्ट्रक्चर:** Stack (LIFO - अंतिम अंदर, पहला बाहर) और Queue (FIFO - पहला अंदर, पहला बाहर)।
                - **बाइनरी सर्च:** क्रमबद्ध (sorted) सूची में `O(log n)` समय में खोज करता है।
                - **SQL बनाम NoSQL:** SQL में तालिकाएँ (tables) व स्कीमा होते हैं, NoSQL में लचीले दस्तावेज़/कुंजी-मान।
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "What is the worst-case time complexity of Binary Search on a sorted array of n elements?",
                    options = listOf("O(log n)", "O(n)", "O(1)", "O(n²)"),
                    correctIndex = 0,
                    explanation = "Each comparison halves the remaining search interval, giving O(log₂ n)."
                ),
                QuizQuestion(
                    question = "Which data structure follows the LIFO (Last In, First Out) principle?",
                    options = listOf("Stack", "Queue", "Linked List", "Binary Tree"),
                    correctIndex = 0,
                    explanation = "A Stack pushes and pops elements from the top, following LIFO."
                ),
                QuizQuestion(
                    question = "In databases, what does the 'A' in ACID stand for?",
                    options = listOf("Atomicity", "Availability", "Authentication", "Aggregation"),
                    correctIndex = 0,
                    explanation = "Atomicity ensures a transaction either completes fully or rolls back completely."
                )
            )
        ),
        OfflineSubjectPack(
            subjectName = "Economics",
            quickFormulasAndRules = listOf(
                "GDP (Expenditure Method): GDP = C + I + G + (X - M)",
                "Law of Demand: Price ↑ ⇒ Quantity Demanded ↓ (ceteris paribus)",
                "Price Elasticity of Demand: Ed = (% Δ Quantity) / (% Δ Price)",
                "Fiscal Policy = Government Taxes & Spending | Monetary Policy = Central Bank Interest Rates & Money Supply"
            ),
            chapterNotesEnglish = """
                ### Economics Offline Revision Notes
                - **Demand & Supply:** Market equilibrium occurs where quantity demanded equals quantity supplied (`Qd = Qs`).
                - **GDP Calculation:** Measured by Expenditure (`C + I + G + X - M`), Income, or Value-Added (Product) method.
                - **Monetary vs Fiscal Policy:** Central bank uses repo rate & CRR (Monetary) to control inflation; Government adjusts budget taxes & public spending (Fiscal).
            """.trimIndent(),
            chapterNotesHindi = """
                ### अर्थशास्त्र ऑफलाइन नोट्स
                - **माँग का नियम:** अन्य बातें समान रहने पर कीमत बढ़ने से माँग घटती है।
                - **GDP सूत्र:** `GDP = C + I + G + (X - M)` (उपभोग + निवेश + सरकारी व्यय + शुद्ध निर्यात)।
                - **मौद्रिक बनाम राजकोषीय नीति:** केंद्रीय बैंक ब्याज दर नियंत्रित करता है (मौद्रिक); सरकार कर व व्यय तय करती है (राजकोषीय)।
            """.trimIndent(),
            quizQuestions = listOf(
                QuizQuestion(
                    question = "In the expenditure formula GDP = C + I + G + (X - M), what does (X - M) represent?",
                    options = listOf("Net Exports", "National Income", "Government Deficit", "Net Investment"),
                    correctIndex = 0,
                    explanation = "X stands for Exports and M stands for Imports, so (X - M) is Net Exports."
                ),
                QuizQuestion(
                    question = "According to the Law of Demand, when the price of a normal good rises (other things equal), quantity demanded:",
                    options = listOf("Decreases", "Increases", "Stays unchanged", "Becomes infinite"),
                    correctIndex = 0,
                    explanation = "Price and quantity demanded have an inverse relationship under the Law of Demand."
                ),
                QuizQuestion(
                    question = "Who formulates Monetary Policy in a country?",
                    options = listOf("The Central Bank", "The Parliament only", "Private commercial banks", "Municipal corporations"),
                    correctIndex = 0,
                    explanation = "The Central Bank (e.g., RBI / Federal Reserve) manages money supply and policy interest rates."
                )
            )
        )
    ).associateBy { it.subjectName }

    val offlineDictionary: List<OfflineDictionaryEntry> = listOf(
        OfflineDictionaryEntry("Algorithm", "Noun", "A step-by-step procedure for solving a problem or computation.", "कलन विधि / चरणबद्ध समाधान नियम", "procedure, formula, method", "Binary search is an efficient sorting algorithm."),
        OfflineDictionaryEntry("Analyze", "Verb", "To examine methodically by separating into parts.", "विश्लेषण करना", "examine, inspect, evaluate", "Analyze the graph before answering the question."),
        OfflineDictionaryEntry("Catalyst", "Noun", "A substance that speeds up a chemical reaction without being consumed.", "उत्प्रेरक", "accelerator, stimulus, trigger", "Enzymes act as biological catalysts in cells."),
        OfflineDictionaryEntry("Democracy", "Noun", "A system of government by the whole population through elected representatives.", "लोकतंत्र / प्रजातंत्र", "republic, self-government", "Free elections are essential to a healthy democracy."),
        OfflineDictionaryEntry("Ecosystem", "Noun", "A biological community of interacting organisms and their physical environment.", "पारिस्थितिकी तंत्र", "biome, habitat, environment", "Wetlands form a vital coastal ecosystem."),
        OfflineDictionaryEntry("Hypothesis", "Noun", "A testable explanation proposed as a starting point for investigation.", "परिकल्पना", "theory, assumption, proposition", "The scientist tested her hypothesis with a controlled experiment."),
        OfflineDictionaryEntry("Inflation", "Noun", "A general rise in prices and fall in the purchasing value of money.", "मुद्रास्फीति / महँगाई", "price rise, escalation", "Central banks raise interest rates to curb high inflation."),
        OfflineDictionaryEntry("Metaphor", "Noun", "A figure of speech comparing two unlike things directly without 'like' or 'as'.", "रूपक अलंकार", "analogy, allegory, symbol", "'Time is a thief' is a classic metaphor."),
        OfflineDictionaryEntry("Mitosis", "Noun", "Cell division resulting in two genetically identical daughter cells.", "समसूत्री विभाजन", "cell division, replication", "Skin cells divide by mitosis to heal a wound."),
        OfflineDictionaryEntry("Momentum", "Noun", "The quantity of motion of a moving body, measured as mass times velocity (p = mv).", "संवेग", "impetus, force, drive", "A heavy truck moving fast has large momentum."),
        OfflineDictionaryEntry("Osmosis", "Noun", "Movement of water molecules across a semipermeable membrane from high to low water concentration.", "परासरण", "diffusion, permeation", "Plant roots absorb soil water through osmosis."),
        OfflineDictionaryEntry("Photosynthesis", "Noun", "Process by which green plants convert sunlight, water, and CO₂ into glucose and oxygen.", "प्रकाश संश्लेषण", "carbon fixation", "Chlorophyll captures sunlight during photosynthesis."),
        OfflineDictionaryEntry("Resilient", "Adjective", "Able to recover quickly from difficulties or return to original shape.", "लचीला / सहनशील", "adaptable, tough, flexible", "Students who practice regularly become resilient problem solvers."),
        OfflineDictionaryEntry("Sovereignty", "Noun", "Supreme authority of a state to govern itself without external interference.", "संप्रभुता", "autonomy, independence, self-rule", "The preamble declares India a sovereign republic."),
        OfflineDictionaryEntry("Velocity", "Noun", "The speed of an object in a specific direction (vector quantity).", "वेग", "speed, pace, rate", "The car moved north at a velocity of 60 km/h.")
    )

    fun getPack(subjectName: String): OfflineSubjectPack {
        return subjectPacks[subjectName] ?: subjectPacks.getValue("Mathematics")
    }

    fun getOfflineQuiz(subjectName: String, count: Int): List<QuizQuestion> {
        val pack = subjectPacks[subjectName]
        val pool = if (pack != null) {
            pack.quizQuestions
        } else {
            subjectPacks.values.flatMap { it.quizQuestions }
        }
        return pool.take(count.coerceIn(1, pool.size))
    }

    fun searchOfflineStudyContent(
        query: String,
        subject: String,
        language: String
    ): String {
        val q = query.trim().lowercase()

        // 1. Dictionary exact/partial match
        val dictMatch = offlineDictionary.firstOrNull {
            q.contains(it.word.lowercase()) || it.word.lowercase().contains(q)
        }
        if (dictMatch != null && q.length >= 3) {
            return formatDictionaryEntry(dictMatch)
        }

        // 2. Match subject pack by subject or keywords
        val matchedPack = subjectPacks[subject]
            ?: subjectPacks.values.firstOrNull { pack ->
                q.contains(pack.subjectName.lowercase()) ||
                    pack.quickFormulasAndRules.any { it.lowercase().contains(q) } ||
                    pack.chapterNotesEnglish.lowercase().contains(q)
            }

        if (matchedPack != null) {
            val notes = if (language == "Hindi") matchedPack.chapterNotesHindi else matchedPack.chapterNotesEnglish
            return buildString {
                appendLine("### Offline Study Reference — ${matchedPack.subjectName}")
                appendLine("*Internet is currently unavailable, so here is your built-in offline study pack:*")
                appendLine()
                appendLine("**Key Formulas & Rules:**")
                matchedPack.quickFormulasAndRules.forEach { appendLine("- $it") }
                appendLine()
                appendLine(notes)
                appendLine()
                appendLine("*Tip: Reconnect to the internet anytime for custom Gemini AI step-by-step answers, photo scanning, and PDF solving.*")
            }
        }

        return buildString {
            appendLine("### Offline Mode Active (No Internet)")
            appendLine("Your device is offline. Here is what works **100% without internet** right now:")
            appendLine("- **Maths Solver:** Solves arithmetic, algebra (`2x+5=15`), quadratic equations (`2x^2-7x+3=0`), and statistics (`12, 15, 18`) step-by-step on device!")
            appendLine("- **Subject Tutors:** Built-in formulas, bilingual notes & quizzes for all 11 subjects.")
            appendLine("- **Smart Dictionary & Grammar Rules:** Instant offline word lookup & writing rules.")
            appendLine("- **Study Planner & Saved History:** All notes, history & tasks stay saved on your device.")
        }
    }

    fun runOfflineTool(
        tool: StudyToolType,
        subject: String,
        input: String,
        language: String
    ): String {
        val q = input.trim().lowercase()
        return when (tool) {
            StudyToolType.DICTIONARY -> {
                val matches = offlineDictionary.filter {
                    it.word.lowercase().contains(q) ||
                        it.englishMeaning.lowercase().contains(q) ||
                        it.hindiMeaning.contains(input.trim(), ignoreCase = true)
                }
                if (matches.isNotEmpty()) {
                    matches.take(3).joinToString("\n\n---\n\n") { formatDictionaryEntry(it) }
                } else {
                    buildString {
                        appendLine("### Offline Smart Dictionary")
                        appendLine("`\"$input\"` was not found in the compact offline word bank (internet is required to look up any rare word with AI).")
                        appendLine()
                        appendLine("**Available Offline Sample Words:**")
                        offlineDictionary.take(8).forEach {
                            appendLine("- **${it.word}** (${it.partOfSpeech}): ${it.englishMeaning} — *${it.hindiMeaning}*")
                        }
                    }
                }
            }

            StudyToolType.GRAMMAR_CHECKER -> {
                buildString {
                    appendLine("### Offline Grammar Quick Check & Rules")
                    appendLine("*Device is offline. Local basic checks + essential grammar rules:*")
                    appendLine()
                    val warnings = checkBasicGrammarLocally(input)
                    if (warnings.isEmpty()) {
                        appendLine("- **Local Check:** Capitalization and basic spacing look good!")
                    } else {
                        warnings.forEach { appendLine("- **Suggestion:** $it") }
                    }
                    appendLine()
                    appendLine("**Core Rules to Verify:**")
                    appendLine("- Check **Subject–Verb Agreement** (Singular subject → singular verb).")
                    appendLine("- Keep **Tense Consistency** within the same paragraph.")
                    appendLine("- Use **Active Voice** for clearer essays (`The student solved the sum`).")
                    appendLine("- Connect to internet for full Gemini AI sentence rewriting.")
                }
            }

            StudyToolType.ENGLISH_SPEAKING -> {
                buildString {
                    appendLine("### Offline English Speaking Practice Pack")
                    appendLine("**Topic:** `$input`")
                    appendLine()
                    appendLine("**1. Confident Starter Phrases (Practice Aloud):**")
                    appendLine("- *\"Let me share my thoughts on $input...\"*")
                    appendLine("- *\"The most important point to consider is...\"*")
                    appendLine("- *\"For example, in everyday life we often see...\"*")
                    appendLine("- *\"To summarize, I believe that...\"*")
                    appendLine()
                    appendLine("**2. Fluency Tips:**")
                    appendLine("- Speak slowly at a steady pace and pause at commas/periods.")
                    appendLine("- Emphasize key nouns and verbs for clear pronunciation.")
                }
            }

            else -> {
                val pack = getPack(subject)
                val notes = if (language == "Hindi") pack.chapterNotesHindi else pack.chapterNotesEnglish
                buildString {
                    appendLine("### Offline ${tool.title} — ${pack.subjectName}")
                    appendLine("*Running offline using built-in study pack (connect to internet for custom AI generation for \"$input\"):*")
                    appendLine()
                    appendLine("**Key Formulas & Facts:**")
                    pack.quickFormulasAndRules.forEach { appendLine("- $it") }
                    appendLine()
                    appendLine(notes)
                }
            }
        }
    }

    private fun formatDictionaryEntry(entry: OfflineDictionaryEntry): String {
        return buildString {
            appendLine("### ${entry.word} *(${entry.partOfSpeech})* — Offline Dictionary")
            appendLine("- **English Meaning:** ${entry.englishMeaning}")
            appendLine("- **Hindi Meaning (हिन्दी अर्थ):** ${entry.hindiMeaning}")
            appendLine("- **Synonyms:** ${entry.synonyms}")
            appendLine("- **Example:** *\"${entry.example}\"*")
        }
    }

    private fun checkBasicGrammarLocally(text: String): List<String> {
        val trimmed = text.trim()
        val tips = mutableListOf<String>()
        if (trimmed.isNotEmpty() && trimmed.first().isLowerCase()) {
            tips.add("Start your sentence with a capital letter (`${trimmed.first().uppercaseChar()}...`).")
        }
        if (trimmed.isNotEmpty() && trimmed.last() !in listOf('.', '!', '?')) {
            tips.add("End your sentence with proper punctuation (`.`, `!`, or `?`).")
        }
        if (trimmed.contains("  ")) {
            tips.add("Remove extra double spaces between words.")
        }
        if (Regex("""\bi\b""").containsMatchIn(trimmed)) {
            tips.add("Always capitalize the pronoun `I`.")
        }
        return tips
    }
}
