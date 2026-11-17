package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserStudyProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Student",
    val dailyGoalMinutes: Long = 45L,
    val todayMinutesStudied: Long = 0L,
    val todayQuestionsSolved: Long = 0L,
    val streakDays: Long = 1L,
    val preferredLanguage: String = "English",
    val lastStudyDate: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SolvedQuestion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "General",
    val topic: String = "",
    val questionText: String = "",
    val answerSummary: String = "",
    val mode: String = "solver",
    val language: String = "English",
    val hasAttachment: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyPlanTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "Mathematics",
    val title: String = "",
    val scheduledDate: String = "Today",
    val durationMinutes: Long = 30L,
    val completed: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedStudyNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "General",
    val toolType: String = "notes",
    val title: String = "",
    val content: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class SubjectInfo(
    val id: String,
    val name: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val secondaryColor: Color,
    val samplePrompts: List<String>
)

data class MathTopic(
    val id: String,
    val name: String,
    val formulaHint: String,
    val sampleProblem: String
)

enum class StudyToolType(
    val id: String,
    val firestoreType: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val inputLabel: String,
    val placeholder: String
) {
    NOTES_MAKER(
        id = "notes_maker",
        firestoreType = "notes",
        title = "AI Notes Maker",
        subtitle = "Structured chapter summaries & key points",
        icon = Icons.Rounded.EditNote,
        inputLabel = "Chapter or Topic",
        placeholder = "e.g. Photosynthesis, French Revolution, Ohm's Law"
    ),
    QUIZ_GENERATOR(
        id = "quiz_generator",
        firestoreType = "quiz",
        title = "AI Quiz Generator",
        subtitle = "Interactive practice MCQs with instant grading",
        icon = Icons.Rounded.Quiz,
        inputLabel = "Quiz Topic",
        placeholder = "e.g. Quadratic Equations, Human Heart, World War II"
    ),
    HOMEWORK_HELPER(
        id = "homework_helper",
        firestoreType = "notes",
        title = "Homework Helper",
        subtitle = "Guided hints & step-by-step homework help",
        icon = Icons.Rounded.School,
        inputLabel = "Homework Question or Assignment",
        placeholder = "Paste your homework prompt or upload an image in Solver..."
    ),
    EXAM_PREP(
        id = "exam_prep",
        firestoreType = "exam_prep",
        title = "Exam Preparation",
        subtitle = "High-yield questions, revision plan & mark schemes",
        icon = Icons.Rounded.Description,
        inputLabel = "Chapter & Exam Board / Grade",
        placeholder = "e.g. Thermodynamics Class 11 important 5-mark questions"
    ),
    DICTIONARY(
        id = "dictionary",
        firestoreType = "dictionary",
        title = "Smart Dictionary",
        subtitle = "Definitions, Hindi meanings, synonyms & usage",
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        inputLabel = "Word or Phrase",
        placeholder = "e.g. Serendipity, Mitosis, Sovereignty"
    ),
    ENGLISH_SPEAKING(
        id = "english_speaking",
        firestoreType = "speaking",
        title = "English Speaking",
        subtitle = "Fluency practice, daily dialogues & feedback",
        icon = Icons.Rounded.RecordVoiceOver,
        inputLabel = "Speaking Scenario or Your Sentence",
        placeholder = "e.g. Job interview intro, or write a paragraph to improve fluency"
    ),
    GRAMMAR_CHECKER(
        id = "grammar_checker",
        firestoreType = "grammar",
        title = "Grammar Checker",
        subtitle = "Instant corrections with clear grammar rules",
        icon = Icons.Rounded.Spellcheck,
        inputLabel = "Text to Check",
        placeholder = "Paste your essay, paragraph, or sentence here..."
    ),
    STUDY_PLANNER(
        id = "study_planner",
        firestoreType = "notes",
        title = "Study Planner",
        subtitle = "Schedule daily tasks & AI-generated study plans",
        icon = Icons.Rounded.CalendarMonth,
        inputLabel = "Exam Goal",
        placeholder = "e.g. Final exams in 7 days for Maths & Physics"
    )
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val subject: String = "General",
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val isOfflineAnswer: Boolean = false,
    val attachmentLabel: String? = null
)

data class ChatConversation(
    val id: String,
    val title: String,
    val subject: String = "General",
    val messages: List<ChatMessage> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

object StudyCatalog {
    val languages = listOf("English", "Hindi", "Bilingual")

    val allSubjects = listOf(
        SubjectInfo(
            id = "math",
            name = "Mathematics",
            subtitle = "Algebra, Calculus, Geometry & more",
            icon = Icons.Rounded.Calculate,
            accentColor = Color(0xFF2563EB),
            secondaryColor = Color(0xFF6366F1),
            samplePrompts = listOf(
                "Explain the quadratic formula with an example",
                "Prove sin^2(x) + cos^2(x) = 1",
                "How do derivatives represent rate of change?"
            )
        ),
        SubjectInfo(
            id = "physics",
            name = "Physics",
            subtitle = "Mechanics, Optics, Electromagnetism",
            icon = Icons.Rounded.AutoAwesome,
            accentColor = Color(0xFF4F46E5),
            secondaryColor = Color(0xFF7C3AED),
            samplePrompts = listOf(
                "Explain Newton's three laws of motion simply",
                "Derive v^2 = u^2 + 2as step by step",
                "Difference between AC and DC current"
            )
        ),
        SubjectInfo(
            id = "chemistry",
            name = "Chemistry",
            subtitle = "Organic, Inorganic & Reactions",
            icon = Icons.Rounded.Science,
            accentColor = Color(0xFF0284C7),
            secondaryColor = Color(0xFF4F46E5),
            samplePrompts = listOf(
                "How to balance redox chemical equations?",
                "Explain covalent vs ionic bonding",
                "Periodic table trends for electronegativity"
            )
        ),
        SubjectInfo(
            id = "biology",
            name = "Biology",
            subtitle = "Genetics, Cell Biology & Ecology",
            icon = Icons.Rounded.Biotech,
            accentColor = Color(0xFF0EA5E9),
            secondaryColor = Color(0xFF6366F1),
            samplePrompts = listOf(
                "Explain mitosis vs meiosis with key stages",
                "How does photosynthesis work in chloroplasts?",
                "Structure and function of DNA"
            )
        ),
        SubjectInfo(
            id = "english",
            name = "English",
            subtitle = "Literature, Writing & Grammar",
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            accentColor = Color(0xFF6D28D9),
            secondaryColor = Color(0xFF3B82F6),
            samplePrompts = listOf(
                "How to write a strong argumentative essay?",
                "Explain active vs passive voice with examples",
                "Literary devices: metaphor, simile, irony"
            )
        ),
        SubjectInfo(
            id = "hindi",
            name = "Hindi",
            subtitle = "व्याकरण, गद्य, पद्य एवं निबंध",
            icon = Icons.Rounded.Translate,
            accentColor = Color(0xFF7C3AED),
            secondaryColor = Color(0xFF2563EB),
            samplePrompts = listOf(
                "समास के भेद और उदाहरण सरल भाषा में समझाइए",
                "रस और अलंकार की परिभाषा उदाहरण सहित लिखें",
                "प्रदूषण पर निबंध लिखने की रूपरेखा बताइए"
            )
        ),
        SubjectInfo(
            id = "history",
            name = "History",
            subtitle = "World & Modern History Timelines",
            icon = Icons.Rounded.HistoryEdu,
            accentColor = Color(0xFF3B82F6),
            secondaryColor = Color(0xFF8B5CF6),
            samplePrompts = listOf(
                "Key causes and impact of the Industrial Revolution",
                "Major milestones of India's Freedom Struggle",
                "Causes of World War I in simple bullet points"
            )
        ),
        SubjectInfo(
            id = "geography",
            name = "Geography",
            subtitle = "Climate, Landforms & Maps",
            icon = Icons.Rounded.Public,
            accentColor = Color(0xFF1D4ED8),
            secondaryColor = Color(0xFF0EA5E9),
            samplePrompts = listOf(
                "How do monsoon winds work?",
                "Difference between weathering and erosion",
                "Layers of the Earth's atmosphere"
            )
        ),
        SubjectInfo(
            id = "political_science",
            name = "Political Science",
            subtitle = "Constitution, Governance & Civics",
            icon = Icons.Rounded.Gavel,
            accentColor = Color(0xFF5B21B6),
            secondaryColor = Color(0xFF4F46E5),
            samplePrompts = listOf(
                "Explain Fundamental Rights in the Constitution",
                "How does parliamentary democracy work?",
                "Separation of powers: Legislature, Executive, Judiciary"
            )
        ),
        SubjectInfo(
            id = "computer_science",
            name = "Computer Science",
            subtitle = "Coding, Algorithms & Databases",
            icon = Icons.Rounded.Computer,
            accentColor = Color(0xFF2563EB),
            secondaryColor = Color(0xFF9333EA),
            samplePrompts = listOf(
                "Explain Big-O time complexity with examples",
                "How does binary search work step by step?",
                "Difference between SQL and NoSQL databases"
            )
        ),
        SubjectInfo(
            id = "economics",
            name = "Economics",
            subtitle = "Micro, Macro & Monetary Policy",
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            accentColor = Color(0xFF4338CA),
            secondaryColor = Color(0xFF38BDF8),
            samplePrompts = listOf(
                "Law of Demand and Supply with curve explanation",
                "How is GDP calculated using three methods?",
                "Difference between fiscal and monetary policy"
            )
        )
    )

    val mathTopics = listOf(
        MathTopic(
            id = "add_sub",
            name = "Addition & Subtraction",
            formulaHint = "a + b = c, c - b = a",
            sampleProblem = "Calculate 4,875 + 2,396 - 1,548 step by step"
        ),
        MathTopic(
            id = "mul_div",
            name = "Multiplication & Division",
            formulaHint = "Dividend = Divisor × Quotient + Remainder",
            sampleProblem = "Divide 9,432 by 24 and verify using the division algorithm"
        ),
        MathTopic(
            id = "algebra",
            name = "Algebra",
            formulaHint = "(a + b)^2 = a^2 + 2ab + b^2",
            sampleProblem = "Solve for x and y: 2x + 3y = 13 and 4x - y = 5"
        ),
        MathTopic(
            id = "geometry",
            name = "Geometry",
            formulaHint = "Pythagoras: a^2 + b^2 = c^2; Area(triangle) = 1/2 × b × h",
            sampleProblem = "Find the area and perimeter of a right triangle with legs 6 cm and 8 cm"
        ),
        MathTopic(
            id = "trigonometry",
            name = "Trigonometry",
            formulaHint = "sin^2(θ) + cos^2(θ) = 1; tan(θ) = sin(θ)/cos(θ)",
            sampleProblem = "If sin(A) = 3/5 and A is acute, find cos(A), tan(A), and sec(A)"
        ),
        MathTopic(
            id = "calculus",
            name = "Calculus",
            formulaHint = "d/dx(x^n) = n·x^(n-1); ∫x^n dx = x^(n+1)/(n+1) + C",
            sampleProblem = "Find the derivative and definite integral from 0 to 2 of f(x) = 3x^2 + 2x - 1"
        ),
        MathTopic(
            id = "statistics",
            name = "Statistics",
            formulaHint = "Mean = Σx / n; Variance = Σ(x - μ)^2 / n",
            sampleProblem = "Find the mean, median, mode, and standard deviation of: 12, 15, 12, 18, 23"
        ),
        MathTopic(
            id = "probability",
            name = "Probability",
            formulaHint = "P(E) = n(E)/n(S); P(A|B) = P(A∩B)/P(B)",
            sampleProblem = "Two fair dice are rolled. Find the probability that the sum is at least 9"
        ),
        MathTopic(
            id = "quadratic",
            name = "Quadratic Equations",
            formulaHint = "x = (-b ± √(b^2 - 4ac)) / 2a",
            sampleProblem = "Solve 2x^2 - 7x + 3 = 0 using the quadratic formula and factoring"
        )
    )
}
