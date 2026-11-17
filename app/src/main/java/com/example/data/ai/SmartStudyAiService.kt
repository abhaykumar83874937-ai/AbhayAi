package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyToolType
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

interface StudyAiGateway {
    fun sendChatConversationStream(
        priorMessages: List<ChatMessage>,
        userMessage: String,
        subject: String,
        language: String,
        attachments: List<Bitmap> = emptyList()
    ): Flow<String>

    fun solveQuestionStream(
        question: String,
        subject: String,
        language: String,
        attachments: List<Bitmap>
    ): Flow<String>

    fun solveMathStepByStepStream(
        problem: String,
        topic: String,
        language: String,
        attachments: List<Bitmap>
    ): Flow<String>

    fun generateTutorContentStream(
        subject: String,
        topic: String,
        modeLabel: String,
        language: String
    ): Flow<String>

    suspend fun generateQuiz(
        subject: String,
        topic: String,
        difficulty: String,
        questionCount: Int,
        language: String
    ): List<QuizQuestion>

    fun runStudyToolStream(
        tool: StudyToolType,
        subject: String,
        userInput: String,
        language: String
    ): Flow<String>
}

class SmartStudyAiService : StudyAiGateway {

    private val aiClient by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
    }

    private val primaryModel by lazy {
        buildGeneralModel(PRIMARY_MODEL)
    }

    private val secondaryModel by lazy {
        buildGeneralModel(SECONDARY_MODEL)
    }

    private val fallbackModel by lazy {
        buildGeneralModel(FALLBACK_MODEL)
    }

    private fun buildGeneralModel(modelName: String): GenerativeModel {
        return aiClient.generativeModel(
            modelName = modelName,
            systemInstruction = content {
                text(
                    "You are Abhay GPT, a smart, helpful, ChatGPT-style AI assistant and expert study tutor. " +
                        "Answer any question clearly, accurately, and directly in a friendly conversational style. " +
                        "Respond fluently in Hindi (Devanagari script) when asked in Hindi or when Hindi is selected, " +
                        "and in English when asked in English. " +
                        "For math, science, homework, or coding questions, show clear step-by-step reasoning, " +
                        "formulas, and final answers. Use clean markdown headings, bullet points, and code blocks."
                )
            }
        )
    }

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(description = "Clear multiple choice question"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 answer options"
                        ),
                        "correctIndex" to Schema.integer(description = "0-based index of correct option (0-3)"),
                        "explanation" to Schema.string(description = "Brief explanation of why the answer is correct")
                    )
                ),
                description = "List of practice quiz questions"
            )
        )
    )

    private val quizModels by lazy {
        listOf(PRIMARY_MODEL, SECONDARY_MODEL, FALLBACK_MODEL).map { modelName ->
            aiClient.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = quizSchema
                }
            )
        }
    }

    /**
     * Multi-stage streaming + unary resilient pipeline:
     * 1) Attempts streaming on PRIMARY_MODEL (gemini-3.8-flash) with a 22s first-token deadline.
     * 2) If streaming emits 0 tokens or fails, tries unary generateContent on PRIMARY_MODEL.
     * 3) Falls back across SECONDARY_MODEL (gemini-3.5-flash) and FALLBACK_MODEL (gemini-3.5-flash-lite).
     */
    private fun streamWithResilience(inputs: List<Content>): Flow<String> = channelFlow {
        val streamCandidates = listOf(
            PRIMARY_MODEL to primaryModel,
            SECONDARY_MODEL to secondaryModel
        )
        var lastError: Throwable? = null

        for ((modelName, model) in streamCandidates) {
            var emittedAny = false
            var stageError: Throwable? = null
            var blockedReason: String? = null
            val firstTokenOrEnd = CompletableDeferred<Unit>()

            val streamJob = launch {
                try {
                    model.generateContentStream(inputs).collect { chunk ->
                        val text = extractResponseText(chunk)
                        if (text.isNotEmpty()) {
                            emittedAny = true
                            firstTokenOrEnd.complete(Unit)
                            send(text)
                        } else {
                            blockedReason = blockedReason ?: extractBlockedReason(chunk)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    stageError = e
                    Log.w(TAG, "Stream attempt failed on $modelName: ${e.message}", e)
                } finally {
                    firstTokenOrEnd.complete(Unit)
                }
            }

            withTimeoutOrNull(FIRST_TOKEN_TIMEOUT_MS) {
                firstTokenOrEnd.await()
            }

            if (emittedAny) {
                streamJob.join()
                return@channelFlow
            }

            // Stream stalled with no tokens or failed cleanly; cancel child stream and try unary/next model
            streamJob.cancelAndJoin()
            if (stageError != null) {
                lastError = stageError
            } else if (blockedReason != null) {
                lastError = IllegalStateException("Response blocked by safety filter ($blockedReason). Try rephrasing your question.")
            }

            // Try unary generateContent on the same model if streaming failed or yielded no chunks
            try {
                val unaryResp = withTimeout(UNARY_TIMEOUT_MS) {
                    model.generateContent(inputs)
                }
                val unaryText = extractResponseText(unaryResp)
                if (unaryText.isNotBlank()) {
                    send(unaryText)
                    return@channelFlow
                }
                val unaryBlock = extractBlockedReason(unaryResp)
                if (unaryBlock != null) {
                    lastError = IllegalStateException("Response blocked by safety filter ($unaryBlock). Try rephrasing your question.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Unary fallback failed on $modelName: ${e.message}", e)
            }
        }

        // Final fast fallback model (gemini-3.5-flash-lite)
        try {
            val liteResp = withTimeout(UNARY_TIMEOUT_MS) {
                fallbackModel.generateContent(inputs)
            }
            val liteText = extractResponseText(liteResp)
            if (liteText.isNotBlank()) {
                send(liteText)
                return@channelFlow
            }
            val block = extractBlockedReason(liteResp)
            if (block != null) {
                throw IllegalStateException("Response blocked by safety filter ($block). Try rephrasing your question.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lastError = lastError ?: e
            Log.e(TAG, "Final fallback failed on $FALLBACK_MODEL: ${e.message}", e)
        }

        throw lastError ?: IllegalStateException("AI server returned an empty response. Please tap Retry.")
    }

    override fun sendChatConversationStream(
        priorMessages: List<ChatMessage>,
        userMessage: String,
        subject: String,
        language: String,
        attachments: List<Bitmap>
    ): Flow<String> {
        val langInstruction = buildLanguageInstruction(language, userMessage)
        val contextNote = if (subject == "General" || subject == "All Subjects") {
            "Language preference: $langInstruction"
        } else {
            "Subject focus: $subject. Language preference: $langInstruction"
        }

        val normalizedHistory = normalizeConversationHistory(priorMessages)
        val contents = mutableListOf<Content>()
        for (msg in normalizedHistory) {
            val role = if (msg.isUser) "user" else "model"
            contents.add(content(role = role) { text(msg.text) })
        }

        val finalPrompt = if (userMessage.isNotBlank()) {
            "[$contextNote]\n$userMessage"
        } else {
            "[$contextNote]\nPlease analyze the attached image/document and answer or solve every question clearly step by step."
        }

        contents.add(
            content(role = "user") {
                attachments.forEach { bitmap -> image(bitmap) }
                text(finalPrompt)
            }
        )

        return streamWithResilience(contents)
    }

    override fun solveQuestionStream(
        question: String,
        subject: String,
        language: String,
        attachments: List<Bitmap>
    ): Flow<String> {
        val langInstruction = buildLanguageInstruction(language, question)
        val promptText = buildString {
            appendLine("Subject: $subject")
            appendLine("Language preference: $langInstruction")
            appendLine("Task: Solve the student's question accurately.")
            appendLine("Format your response clearly with:")
            appendLine("1. **Problem / Concept Identified**")
            appendLine("2. **Given & Key Formula / Principle**")
            appendLine("3. **Step-by-Step Solution** (explain why each step works)")
            appendLine("4. **Final Answer**")
            appendLine("5. **Quick Study Tip**")
            if (question.isNotBlank()) {
                appendLine()
                appendLine("Student's Question: $question")
            } else {
                appendLine()
                appendLine("Please read all text, equations, or diagrams from the attached image/document pages and solve every question step by step.")
            }
        }

        val inputContent: Content = content {
            attachments.forEach { bitmap -> image(bitmap) }
            text(promptText)
        }

        return streamWithResilience(listOf(inputContent))
    }

    override fun solveMathStepByStepStream(
        problem: String,
        topic: String,
        language: String,
        attachments: List<Bitmap>
    ): Flow<String> {
        val langInstruction = buildLanguageInstruction(language, problem)
        val promptText = buildString {
            appendLine("You are an expert Mathematics Solver in Abhay GPT.")
            appendLine("Math Topic: $topic")
            appendLine("Language preference: $langInstruction")
            appendLine("Requirements:")
            appendLine("- State the relevant formula(s) and identities first.")
            appendLine("- Show every algebraic, arithmetic, or calculus transformation step clearly on separate lines.")
            appendLine("- Explain the reasoning behind each step so a student learns how the answer was calculated.")
            appendLine("- Include a brief verification/check of the final answer.")
            if (problem.isNotBlank()) {
                appendLine()
                appendLine("Math Problem: $problem")
            } else {
                appendLine()
                appendLine("Read the math problem from the attached image(s) carefully and solve it step by step.")
            }
        }

        val inputContent: Content = content {
            attachments.forEach { bitmap -> image(bitmap) }
            text(promptText)
        }

        return streamWithResilience(listOf(inputContent))
    }

    override fun generateTutorContentStream(
        subject: String,
        topic: String,
        modeLabel: String,
        language: String
    ): Flow<String> {
        val langInstruction = buildLanguageInstruction(language, topic)
        val taskInstruction = when (modeLabel) {
            "Simple Explanation" -> "Explain this topic in very simple, everyday student-friendly language with relatable examples."
            "Short Answer" -> "Write a crisp, high-scoring short exam answer (around 60-90 words) with key terms highlighted."
            "Long Answer" -> "Write a comprehensive, well-structured long exam answer (with introduction, numbered points, examples, and conclusion)."
            "Chapter Notes" -> "Create structured revision chapter notes with definitions, formulas/dates, key concepts, and quick summary bullets."
            "Textbook Q&A" -> "Provide model textbook-style questions and their complete step-by-step answers on this topic."
            "Exam Questions" -> "List 5 most important exam questions (2-mark, 3-mark, and 5-mark) with concise model solutions and marking tips."
            else -> "Teach this topic clearly with examples."
        }
        val prompt = """
            Subject: $subject
            Topic: $topic
            Mode: $modeLabel
            Language: $langInstruction
            
            $taskInstruction
        """.trimIndent()

        return streamWithResilience(listOf(content { text(prompt) }))
    }

    override suspend fun generateQuiz(
        subject: String,
        topic: String,
        difficulty: String,
        questionCount: Int,
        language: String
    ): List<QuizQuestion> {
        val langInstruction = buildLanguageInstruction(language, topic)
        val prompt = """
            Generate $questionCount multiple-choice practice quiz questions for a student.
            Subject: $subject
            Topic: $topic
            Difficulty: $difficulty
            Language: $langInstruction
            Each question must have exactly 4 distinct options and a valid correctIndex (0 to 3).
        """.trimIndent()

        var lastError: Throwable? = null
        var responseText: String? = null
        for (model in quizModels) {
            try {
                val response = withTimeout(25_000L) {
                    model.generateContent(prompt)
                }
                val candidate = extractResponseText(response)
                if (candidate.isNotBlank()) {
                    responseText = candidate
                    break
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Quiz generation attempt failed", e)
            }
        }

        val jsonText = responseText
            ?: throw (lastError ?: IllegalStateException("AI returned an empty quiz."))
        val root = JSONObject(jsonText)
        val arr = root.getJSONArray("questions")
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val optsJson = item.getJSONArray("options")
                val options = (0 until optsJson.length()).map { idx -> optsJson.getString(idx) }
                val safeIndex = item.optInt("correctIndex", 0).coerceIn(0, (options.size - 1).coerceAtLeast(0))
                add(
                    QuizQuestion(
                        question = item.getString("question"),
                        options = options,
                        correctIndex = safeIndex,
                        explanation = item.getString("explanation")
                    )
                )
            }
        }
    }

    override fun runStudyToolStream(
        tool: StudyToolType,
        subject: String,
        userInput: String,
        language: String
    ): Flow<String> {
        val langInstruction = buildLanguageInstruction(language, userInput)
        val prompt = when (tool) {
            StudyToolType.NOTES_MAKER -> """
                Create comprehensive, exam-ready Study Notes on "$userInput" for $subject.
                Language: $langInstruction
                Include: Core Overview, Key Definitions, Important Formulas/Facts, Real-Life Example, and Quick Revision Checklist.
            """.trimIndent()

            StudyToolType.HOMEWORK_HELPER -> """
                Help a student complete this $subject homework assignment without just giving away a bare answer—guide them clearly and show the complete worked solution.
                Language: $langInstruction
                Homework: $userInput
            """.trimIndent()

            StudyToolType.EXAM_PREP -> """
                Prepare an Exam Revision Kit for $subject on "$userInput".
                Language: $langInstruction
                Include: High-weightage concepts, 5 likely exam questions with model answers, common mistakes to avoid, and a 20-minute revision strategy.
            """.trimIndent()

            StudyToolType.DICTIONARY -> """
                Act as a student Smart Dictionary & Vocabulary Builder.
                Word/Phrase: "$userInput"
                Subject context: $subject
                Provide:
                1. Part of speech & pronunciation guide
                2. Simple English definition
                3. Hindi meaning (हिन्दी अर्थ)
                4. 3 Synonyms & 2 Antonyms
                5. Two example sentences (everyday + academic)
            """.trimIndent()

            StudyToolType.ENGLISH_SPEAKING -> """
                Act as an encouraging English Speaking Coach for students.
                Topic / Student Input: "$userInput"
                Provide:
                1. Natural, confident spoken dialogue or improved response
                2. Pronunciation & tone tips for key words
                3. 4 useful conversational phrases to practice aloud
                4. A follow-up speaking prompt for the student to practice next
            """.trimIndent()

            StudyToolType.GRAMMAR_CHECKER -> """
                Act as a precise English Grammar Checker & Writing Tutor.
                Student Text: "$userInput"
                Provide:
                1. **Corrected Version** (polished and natural)
                2. **Grammar & Spelling Fixes** (bullet list explaining each error and the grammar rule)
                3. **Style & Clarity Tip**
            """.trimIndent()

            StudyToolType.STUDY_PLANNER -> """
                Create a realistic, motivating day-by-day Study Plan for a student.
                Subject Focus: $subject
                Goal: "$userInput"
                Language: $langInstruction
                Break it into actionable 30-45 minute study blocks with short breaks and active recall practice.
            """.trimIndent()

            StudyToolType.QUIZ_GENERATOR -> """
                Generate a 5-question practice quiz with answers and explanations on "$userInput" ($subject).
                Language: $langInstruction
            """.trimIndent()
        }
        return streamWithResilience(listOf(content { text(prompt) }))
    }

    private fun extractResponseText(response: GenerateContentResponse): String {
        val direct = response.text
        if (!direct.isNullOrEmpty()) return direct
        val fromParts = response.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.filterIsInstance<TextPart>()
            ?.joinToString(separator = "") { it.text }
            .orEmpty()
        return fromParts
    }

    private fun extractBlockedReason(response: GenerateContentResponse): String? {
        val reason = response.candidates.firstOrNull()?.finishReason?.name
        return if (reason != null && reason !in setOf("STOP", "MAX_TOKENS")) {
            reason
        } else {
            null
        }
    }

    internal companion object {
        const val TAG = "AbhayGptAi"
        const val PRIMARY_MODEL = "gemini-3.8-flash"
        const val SECONDARY_MODEL = "gemini-3.5-flash"
        const val FALLBACK_MODEL = "gemini-3.5-flash-lite"
        private const val FIRST_TOKEN_TIMEOUT_MS = 22_000L
        private const val UNARY_TIMEOUT_MS = 20_000L

        internal fun containsDevanagari(text: String): Boolean {
            return text.any { it in '\u0900'..'\u097F' }
        }

        internal fun buildLanguageInstruction(language: String, userText: String): String {
            return when (language) {
                "Hindi" -> "Respond clearly in Hindi (Devanagari script), keeping standard mathematical/scientific symbols readable."
                "Bilingual" -> "Respond in Bilingual Hindi + English (clear English with simple Hindi explanations alongside key points)."
                else -> if (containsDevanagari(userText)) {
                    "Respond clearly in Hindi (Devanagari script) matching the user's question language."
                } else {
                    "Respond in clear, simple English (or in Hindi if the user writes in Hindi)."
                }
            }
        }

        /**
         * Ensures multi-turn history sent before the new user prompt always:
         * 1) Excludes streaming/error/blank messages.
         * 2) Starts with a "user" message.
         * 3) Strictly alternates user -> model -> user -> model.
         * 4) Ends on a "model" message (never leaves a dangling "user" turn right before the new user turn).
         */
        internal fun normalizeConversationHistory(
            priorMessages: List<ChatMessage>,
            maxTurns: Int = 12
        ): List<ChatMessage> {
            val clean = priorMessages
                .filter { !it.isStreaming && !it.isError && it.text.isNotBlank() }
                .takeLast(maxTurns)

            val alternating = mutableListOf<ChatMessage>()
            for (msg in clean) {
                if (alternating.isEmpty()) {
                    if (msg.isUser) alternating.add(msg)
                } else if (alternating.last().isUser != msg.isUser) {
                    alternating.add(msg)
                } else if (!msg.isUser) {
                    // Replace consecutive model message with the latest one
                    alternating[alternating.lastIndex] = msg
                } else {
                    // Replace consecutive user message with the latest one
                    alternating[alternating.lastIndex] = msg
                }
            }

            if (alternating.lastOrNull()?.isUser == true) {
                alternating.removeAt(alternating.lastIndex)
            }
            return alternating
        }
    }
}
