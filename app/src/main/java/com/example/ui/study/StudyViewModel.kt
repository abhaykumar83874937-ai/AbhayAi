package com.example.ui.study

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.NetworkConnectivityProvider
import com.example.data.ai.StudyAiGateway
import com.example.data.local.LocalStudySnapshot
import com.example.data.local.LocalStudyStore
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.MathTopic
import com.example.data.model.QuizQuestion
import com.example.data.model.SavedStudyNote
import com.example.data.model.SolvedQuestion
import com.example.data.model.StudyCatalog
import com.example.data.model.StudyPlanTask
import com.example.data.model.StudyToolType
import com.example.data.model.UserStudyProfile
import com.example.data.offline.OfflineMathEngine
import com.example.data.offline.OfflineStudyBank
import com.example.data.repository.StudyRepository
import com.example.util.LoadedAttachment
import com.example.util.MediaUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

sealed interface AiStreamState {
    data object Idle : AiStreamState
    data object Loading : AiStreamState
    data class Streaming(val text: String) : AiStreamState
    data class Success(val text: String, val savedToHistory: Boolean = false) : AiStreamState
    data class Error(val message: String) : AiStreamState
}

sealed interface QuizUiState {
    data object Idle : QuizUiState
    data object Loading : QuizUiState
    data class Ready(
        val topic: String,
        val subject: String,
        val questions: List<QuizQuestion>,
        val selectedAnswers: Map<Int, Int> = emptyMap(),
        val submitted: Boolean = false,
        val isOfflinePack: Boolean = false
    ) : QuizUiState
    data class Error(val message: String) : QuizUiState
}

sealed interface FirestoreUiState<out T> {
    data object Loading : FirestoreUiState<Nothing>
    data class Success<T>(val data: T) : FirestoreUiState<T>
    data class Error(val message: String) : FirestoreUiState<Nothing>
}

class StudyViewModel(
    private val repository: StudyRepository?,
    private val aiService: StudyAiGateway,
    private val localStore: LocalStudyStore,
    private val networkMonitor: NetworkConnectivityProvider,
    val currentUserId: String,
    val displayName: String,
    val isGuest: Boolean = repository == null
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = networkMonitor.isCurrentlyOnline()
        )

    private val _guestProfile = MutableStateFlow<UserStudyProfile?>(
        UserStudyProfile(
            id = "guest",
            userId = "guest",
            displayName = displayName,
            dailyGoalMinutes = 45L,
            todayMinutesStudied = 0L,
            todayQuestionsSolved = 0L,
            streakDays = 1L,
            preferredLanguage = "English"
        )
    )
    private val _guestQuestions = MutableStateFlow<List<SolvedQuestion>>(emptyList())
    private val _guestPlans = MutableStateFlow<List<StudyPlanTask>>(emptyList())
    private val _guestNotes = MutableStateFlow<List<SavedStudyNote>>(emptyList())

    val profileState: StateFlow<FirestoreUiState<UserStudyProfile?>> =
        if (repository == null) {
            _guestProfile
                .map<UserStudyProfile?, FirestoreUiState<UserStudyProfile?>> { FirestoreUiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FirestoreUiState.Loading)
        } else {
            repository.observeProfile(currentUserId)
                .map<UserStudyProfile?, FirestoreUiState<UserStudyProfile?>> { FirestoreUiState.Success(it) }
                .catch { error ->
                    val root = unwrapFirestoreError(error) ?: error
                    if (root is CancellationException && unwrapFirestoreError(error) == null) throw error
                    Log.w(TAG, "Error observing profile", root)
                    emit(FirestoreUiState.Error(root.message ?: "Failed to load study profile"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = FirestoreUiState.Loading
                )
        }

    val recentQuestionsState: StateFlow<FirestoreUiState<List<SolvedQuestion>>> =
        if (repository == null) {
            _guestQuestions
                .map<List<SolvedQuestion>, FirestoreUiState<List<SolvedQuestion>>> { FirestoreUiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FirestoreUiState.Loading)
        } else {
            repository.observeRecentQuestions(currentUserId)
                .map<List<SolvedQuestion>, FirestoreUiState<List<SolvedQuestion>>> { FirestoreUiState.Success(it) }
                .catch { error ->
                    val root = unwrapFirestoreError(error) ?: error
                    if (root is CancellationException && unwrapFirestoreError(error) == null) throw error
                    Log.w(TAG, "Error observing questions", root)
                    emit(FirestoreUiState.Error(root.message ?: "Failed to load recent questions"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = FirestoreUiState.Loading
                )
        }

    val studyPlansState: StateFlow<FirestoreUiState<List<StudyPlanTask>>> =
        if (repository == null) {
            _guestPlans
                .map<List<StudyPlanTask>, FirestoreUiState<List<StudyPlanTask>>> { FirestoreUiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FirestoreUiState.Loading)
        } else {
            repository.observeStudyPlans(currentUserId)
                .map<List<StudyPlanTask>, FirestoreUiState<List<StudyPlanTask>>> { FirestoreUiState.Success(it) }
                .catch { error ->
                    val root = unwrapFirestoreError(error) ?: error
                    if (root is CancellationException && unwrapFirestoreError(error) == null) throw error
                    Log.w(TAG, "Error observing study plans", root)
                    emit(FirestoreUiState.Error(root.message ?: "Failed to load study planner"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = FirestoreUiState.Loading
                )
        }

    val savedNotesState: StateFlow<FirestoreUiState<List<SavedStudyNote>>> =
        if (repository == null) {
            _guestNotes
                .map<List<SavedStudyNote>, FirestoreUiState<List<SavedStudyNote>>> { FirestoreUiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FirestoreUiState.Loading)
        } else {
            repository.observeSavedNotes(currentUserId)
                .map<List<SavedStudyNote>, FirestoreUiState<List<SavedStudyNote>>> { FirestoreUiState.Success(it) }
                .catch { error ->
                    val root = unwrapFirestoreError(error) ?: error
                    if (root is CancellationException && unwrapFirestoreError(error) == null) throw error
                    Log.w(TAG, "Error observing saved notes", root)
                    emit(FirestoreUiState.Error(root.message ?: "Failed to load saved notes"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = FirestoreUiState.Loading
                )
        }

    // Question Solver state
    private val _solverQuestion = MutableStateFlow("")
    val solverQuestion: StateFlow<String> = _solverQuestion.asStateFlow()

    private val _solverSubject = MutableStateFlow("General")
    val solverSubject: StateFlow<String> = _solverSubject.asStateFlow()

    private val _solverLanguage = MutableStateFlow("English")
    val solverLanguage: StateFlow<String> = _solverLanguage.asStateFlow()

    private val _solverAttachment = MutableStateFlow<LoadedAttachment?>(null)
    val solverAttachment: StateFlow<LoadedAttachment?> = _solverAttachment.asStateFlow()

    private val _attachmentLoading = MutableStateFlow(false)
    val attachmentLoading: StateFlow<Boolean> = _attachmentLoading.asStateFlow()

    private val _solverAiState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val solverAiState: StateFlow<AiStreamState> = _solverAiState.asStateFlow()

    // Math Solver state
    private val _selectedMathTopic = MutableStateFlow(StudyCatalog.mathTopics.first())
    val selectedMathTopic: StateFlow<MathTopic> = _selectedMathTopic.asStateFlow()

    private val _mathProblem = MutableStateFlow("")
    val mathProblem: StateFlow<String> = _mathProblem.asStateFlow()

    private val _mathAttachment = MutableStateFlow<LoadedAttachment?>(null)
    val mathAttachment: StateFlow<LoadedAttachment?> = _mathAttachment.asStateFlow()

    private val _mathAiState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val mathAiState: StateFlow<AiStreamState> = _mathAiState.asStateFlow()

    // Subject Tutor state
    private val _tutorAiState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val tutorAiState: StateFlow<AiStreamState> = _tutorAiState.asStateFlow()

    // Study Tools state
    private val _toolAiState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val toolAiState: StateFlow<AiStreamState> = _toolAiState.asStateFlow()

    private val _quizState = MutableStateFlow<QuizUiState>(QuizUiState.Idle)
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    // Abhay GPT (ChatGPT-style) conversations & state
    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _activeConversationId = MutableStateFlow(UUID.randomUUID().toString())
    val activeConversationId: StateFlow<String> = _activeConversationId.asStateFlow()

    private val _chatSubject = MutableStateFlow("General")
    val chatSubject: StateFlow<String> = _chatSubject.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatAttachment = MutableStateFlow<LoadedAttachment?>(null)
    val chatAttachment: StateFlow<LoadedAttachment?> = _chatAttachment.asStateFlow()

    private val _chatSending = MutableStateFlow(false)
    val chatSending: StateFlow<Boolean> = _chatSending.asStateFlow()

    private var chatJob: Job? = null
    private var solverJob: Job? = null
    private var mathJob: Job? = null
    private var tutorJob: Job? = null
    private var toolJob: Job? = null
    private var quizJob: Job? = null

    init {
        viewModelScope.launch {
            val snap = localStore.load()
            _conversations.value = snap.conversations
            if (repository == null) {
                _guestProfile.value = snap.profile.copy(displayName = displayName)
                _guestQuestions.value = snap.questions
                _guestPlans.value = snap.plans
                _guestNotes.value = snap.notes
                _solverLanguage.value = snap.profile.preferredLanguage
            } else {
                repository.ensureProfile(currentUserId, displayName)
            }
        }
    }

    private suspend fun saveLocalSnapshot() {
        val currentSnap = if (repository == null) {
            LocalStudySnapshot(
                profile = _guestProfile.value ?: LocalStudySnapshot().profile,
                questions = _guestQuestions.value,
                plans = _guestPlans.value,
                notes = _guestNotes.value,
                conversations = _conversations.value
            )
        } else {
            val existing = localStore.load()
            existing.copy(conversations = _conversations.value)
        }
        localStore.save(currentSnap)
    }

    fun updateSolverQuestion(value: String) {
        _solverQuestion.value = value
    }

    fun updateSolverSubject(subject: String) {
        _solverSubject.value = subject
    }

    fun updateSolverLanguage(language: String) {
        _solverLanguage.value = language
    }

    fun clearSolverAttachment() {
        _solverAttachment.value = null
    }

    fun loadSolverImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _attachmentLoading.value = true
            try {
                _solverAttachment.value = MediaUtils.loadImageAttachment(context, uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _solverAiState.value = AiStreamState.Error(e.localizedMessage ?: "Failed to load image.")
            } finally {
                _attachmentLoading.value = false
            }
        }
    }

    fun loadSolverPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            _attachmentLoading.value = true
            try {
                _solverAttachment.value = MediaUtils.loadPdfAttachment(context, uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _solverAiState.value = AiStreamState.Error(e.localizedMessage ?: "Failed to load PDF.")
            } finally {
                _attachmentLoading.value = false
            }
        }
    }

    fun loadChatImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _attachmentLoading.value = true
            try {
                _chatAttachment.value = MediaUtils.loadImageAttachment(context, uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendChatSystemError("Could not load image: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                _attachmentLoading.value = false
            }
        }
    }

    fun loadChatPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            _attachmentLoading.value = true
            try {
                _chatAttachment.value = MediaUtils.loadPdfAttachment(context, uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendChatSystemError("Could not load PDF: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                _attachmentLoading.value = false
            }
        }
    }

    fun clearChatAttachment() {
        _chatAttachment.value = null
    }

    private fun appendChatSystemError(message: String) {
        _chatMessages.value = _chatMessages.value + ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            text = message,
            subject = _chatSubject.value,
            isError = true
        )
    }

    fun solveQuestion(
        customPrompt: String? = null,
        customSubject: String? = null
    ) {
        if (_solverAiState.value is AiStreamState.Loading || _solverAiState.value is AiStreamState.Streaming) {
            return
        }
        val question = (customPrompt ?: _solverQuestion.value).trim()
        val subject = customSubject ?: _solverSubject.value
        val language = _solverLanguage.value
        val attachmentSnapshot = _solverAttachment.value
        val pages = attachmentSnapshot?.pages?.map { it.bitmap } ?: emptyList()

        if (customPrompt != null) _solverQuestion.value = customPrompt
        if (customSubject != null) _solverSubject.value = customSubject

        if (question.isBlank() && pages.isEmpty()) {
            _solverAiState.value = AiStreamState.Error("Please type a question or attach an image/PDF.")
            return
        }

        val currentlyOnline = isOnline.value || networkMonitor.isCurrentlyOnline()
        _solverAiState.value = AiStreamState.Loading
        solverJob?.cancel()
        solverJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                if (!currentlyOnline) {
                    if (pages.isNotEmpty() && question.isBlank()) {
                        _solverAiState.value = AiStreamState.Error(
                            "Internet is required to read textbook photos and PDFs. For offline help, type a math equation (like 2x+5=15) or select a Subject for built-in notes & quizzes."
                        )
                        return@launch
                    }
                    val offlineAnswer = OfflineMathEngine.trySolveStepByStep(question, subject, language)
                        ?: OfflineStudyBank.searchOfflineStudyContent(question, subject, language)
                    _solverAiState.value = AiStreamState.Success(offlineAnswer, savedToHistory = true)
                    viewModelScope.launch {
                        runCatching {
                            persistQuestion(
                                subject = subject,
                                topic = "Offline Study Solver",
                                questionText = question,
                                answerSummary = offlineAnswer,
                                mode = "offline",
                                language = language,
                                hasAttachment = false
                            )
                            recordActivity(minutesAdded = 5, questionsAdded = 1)
                        }
                    }
                    return@launch
                }

                withTimeout(75_000L) {
                    aiService.solveQuestionStream(question, subject, language, pages).collect { chunk ->
                        accumulated.append(chunk)
                        _solverAiState.value = AiStreamState.Streaming(accumulated.toString())
                    }
                }
                val finalAnswer = accumulated.toString().trim()
                if (finalAnswer.isBlank()) {
                    _solverAiState.value = AiStreamState.Error("Model returned an empty solution. Please try again.")
                    return@launch
                }
                _solverAiState.value = AiStreamState.Success(finalAnswer, savedToHistory = true)
                viewModelScope.launch {
                    runCatching {
                        withTimeout(10_000L) {
                            persistQuestion(
                                subject = subject,
                                topic = if (pages.isNotEmpty()) (attachmentSnapshot?.displayName ?: "Attachment") else "Question Solver",
                                questionText = question.ifBlank { "Uploaded ${attachmentSnapshot?.displayName ?: "Question"}" },
                                answerSummary = finalAnswer,
                                mode = "solver",
                                language = language,
                                hasAttachment = pages.isNotEmpty()
                            )
                            recordActivity(minutesAdded = 5, questionsAdded = 1)
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Question solver timed out", e)
                _solverAiState.value = AiStreamState.Error("Request timed out. Please check your internet connection and tap Solve again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Question solver failed", e)
                _solverAiState.value = AiStreamState.Error(formatAiError(e))
            } finally {
                val cur = _solverAiState.value
                if (cur is AiStreamState.Loading || cur is AiStreamState.Streaming) {
                    _solverAiState.value = AiStreamState.Error("Request stopped before completing. Please try again.")
                }
            }
        }
    }

    fun selectMathTopic(topic: MathTopic) {
        _selectedMathTopic.value = topic
    }

    fun updateMathProblem(value: String) {
        _mathProblem.value = value
    }

    fun loadMathImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _attachmentLoading.value = true
            try {
                _mathAttachment.value = MediaUtils.loadImageAttachment(context, uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mathAiState.value = AiStreamState.Error(e.localizedMessage ?: "Failed to load math image.")
            } finally {
                _attachmentLoading.value = false
            }
        }
    }

    fun clearMathAttachment() {
        _mathAttachment.value = null
    }

    fun solveMathOffline() {
        val problem = _mathProblem.value.trim()
        val topic = _selectedMathTopic.value.name
        val language = _solverLanguage.value

        if (problem.isBlank()) {
            _mathAiState.value = AiStreamState.Error("Type a math equation or expression (e.g., 2x+5=15, 2x^2-7x+3=0, or 12, 15, 18) to solve offline.")
            return
        }

        mathJob?.cancel()
        mathJob = viewModelScope.launch {
            val localSolution = OfflineMathEngine.trySolveStepByStep(problem, topic, language)
            if (localSolution != null) {
                val saved = persistQuestion(
                    subject = "Mathematics",
                    topic = "$topic (Offline)",
                    questionText = problem,
                    answerSummary = localSolution,
                    mode = "math_offline",
                    language = language,
                    hasAttachment = false
                )
                recordActivity(minutesAdded = 5, questionsAdded = 1)
                _mathAiState.value = AiStreamState.Success(localSolution, savedToHistory = saved)
            } else {
                _mathAiState.value = AiStreamState.Error(
                    "Offline Math Engine supports arithmetic (e.g. 4875 + 2396 - 1548), linear equations (2x+5=15), quadratic equations (2x^2-7x+3=0), trig/sqrt (sqrt(144), sin(30)), and statistics lists (12, 15, 12, 18, 23). Connect to internet for word problems or calculus proofs."
                )
            }
        }
    }

    fun solveMathProblem() {
        if (_mathAiState.value is AiStreamState.Loading || _mathAiState.value is AiStreamState.Streaming) {
            return
        }
        val problem = _mathProblem.value.trim()
        val topic = _selectedMathTopic.value.name
        val language = _solverLanguage.value
        val bitmaps: List<Bitmap> = _mathAttachment.value?.pages?.map { it.bitmap } ?: emptyList()

        if (problem.isBlank() && bitmaps.isEmpty()) {
            _mathAiState.value = AiStreamState.Error("Enter a math equation/problem or upload a photo.")
            return
        }

        val currentlyOnline = isOnline.value || networkMonitor.isCurrentlyOnline()
        if (!currentlyOnline) {
            if (bitmaps.isNotEmpty() && problem.isBlank()) {
                _mathAiState.value = AiStreamState.Error("Photo math scanning requires internet. Type your equation above to solve step-by-step offline!")
                return
            }
            solveMathOffline()
            return
        }

        _mathAiState.value = AiStreamState.Loading
        mathJob?.cancel()
        mathJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(75_000L) {
                    aiService.solveMathStepByStepStream(problem, topic, language, bitmaps).collect { chunk ->
                        accumulated.append(chunk)
                        _mathAiState.value = AiStreamState.Streaming(accumulated.toString())
                    }
                }
                val finalAnswer = accumulated.toString().trim()
                if (finalAnswer.isBlank()) {
                    _mathAiState.value = AiStreamState.Error("AI returned an empty math solution. Please try again.")
                    return@launch
                }
                _mathAiState.value = AiStreamState.Success(finalAnswer, savedToHistory = true)
                viewModelScope.launch {
                    runCatching {
                        withTimeout(10_000L) {
                            persistQuestion(
                                subject = "Mathematics",
                                topic = topic,
                                questionText = problem.ifBlank { "Math Photo Problem ($topic)" },
                                answerSummary = finalAnswer,
                                mode = "math",
                                language = language,
                                hasAttachment = bitmaps.isNotEmpty()
                            )
                            recordActivity(minutesAdded = 5, questionsAdded = 1)
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Math solver timed out", e)
                _mathAiState.value = AiStreamState.Error("Math solver timed out. Please retry or use Offline Solve.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Math solver failed", e)
                _mathAiState.value = AiStreamState.Error(formatAiError(e))
            } finally {
                val cur = _mathAiState.value
                if (cur is AiStreamState.Loading || cur is AiStreamState.Streaming) {
                    _mathAiState.value = AiStreamState.Error("Math solver did not finish. Please try again.")
                }
            }
        }
    }

    fun runSubjectTutor(subject: String, topic: String, modeLabel: String, language: String) {
        if (_tutorAiState.value is AiStreamState.Loading || _tutorAiState.value is AiStreamState.Streaming) {
            return
        }
        if (topic.isBlank()) {
            _tutorAiState.value = AiStreamState.Error("Enter a topic or chapter name to learn.")
            return
        }
        val currentlyOnline = isOnline.value || networkMonitor.isCurrentlyOnline()
        _tutorAiState.value = AiStreamState.Loading
        tutorJob?.cancel()
        tutorJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                if (!currentlyOnline) {
                    val pack = OfflineStudyBank.getPack(subject)
                    val notes = if (language == "Hindi") pack.chapterNotesHindi else pack.chapterNotesEnglish
                    val offlineText = buildString {
                        appendLine("### Offline $subject Study Pack ($modeLabel)")
                        appendLine("*Offline Mode active — showing built-in revision notes & key formulas:*")
                        appendLine()
                        appendLine("**Key Formulas & Rules:**")
                        pack.quickFormulasAndRules.forEach { appendLine("- $it") }
                        appendLine()
                        appendLine(notes)
                    }
                    _tutorAiState.value = AiStreamState.Success(offlineText, savedToHistory = true)
                    viewModelScope.launch {
                        runCatching {
                            persistQuestion(
                                subject = subject,
                                topic = "Offline $modeLabel: ${topic.trim().take(60)}",
                                questionText = "$modeLabel for ${topic.trim()}",
                                answerSummary = offlineText,
                                mode = "tutor_offline",
                                language = language,
                                hasAttachment = false
                            )
                            recordActivity(minutesAdded = 5, questionsAdded = 1)
                        }
                    }
                    return@launch
                }

                withTimeout(75_000L) {
                    aiService.generateTutorContentStream(subject, topic.trim(), modeLabel, language).collect { chunk ->
                        accumulated.append(chunk)
                        _tutorAiState.value = AiStreamState.Streaming(accumulated.toString())
                    }
                }
                val text = accumulated.toString().trim()
                if (text.isBlank()) {
                    _tutorAiState.value = AiStreamState.Error("Tutor returned an empty response. Please try again.")
                    return@launch
                }
                _tutorAiState.value = AiStreamState.Success(text, savedToHistory = true)
                viewModelScope.launch {
                    runCatching {
                        withTimeout(10_000L) {
                            persistQuestion(
                                subject = subject,
                                topic = "$modeLabel: ${topic.trim().take(60)}",
                                questionText = "$modeLabel for ${topic.trim()}",
                                answerSummary = text,
                                mode = "tutor",
                                language = language,
                                hasAttachment = false
                            )
                            recordActivity(minutesAdded = 6, questionsAdded = 1)
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Subject tutor timed out", e)
                _tutorAiState.value = AiStreamState.Error("Tutor response timed out. Please check your connection and try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Subject tutor failed", e)
                _tutorAiState.value = AiStreamState.Error(formatAiError(e))
            } finally {
                val cur = _tutorAiState.value
                if (cur is AiStreamState.Loading || cur is AiStreamState.Streaming) {
                    _tutorAiState.value = AiStreamState.Error("Tutor request did not finish. Please try again.")
                }
            }
        }
    }

    fun resetTutorState() {
        _tutorAiState.value = AiStreamState.Idle
        tutorJob?.cancel()
    }

    fun runStudyTool(tool: StudyToolType, subject: String, input: String, language: String) {
        if (_toolAiState.value is AiStreamState.Loading || _toolAiState.value is AiStreamState.Streaming) {
            return
        }
        if (input.isBlank()) {
            _toolAiState.value = AiStreamState.Error("Please enter a topic or text first.")
            return
        }
        val currentlyOnline = isOnline.value || networkMonitor.isCurrentlyOnline()
        _toolAiState.value = AiStreamState.Loading
        toolJob?.cancel()
        toolJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                if (!currentlyOnline) {
                    val offlineResult = OfflineStudyBank.runOfflineTool(tool, subject, input.trim(), language)
                    _toolAiState.value = AiStreamState.Success(offlineResult, savedToHistory = true)
                    viewModelScope.launch {
                        runCatching {
                            persistNote(
                                subject = subject,
                                toolType = tool.firestoreType,
                                title = "${tool.title} (Offline): ${input.trim().take(70)}",
                                content = offlineResult
                            )
                            recordActivity(minutesAdded = 5, questionsAdded = 1)
                        }
                    }
                    return@launch
                }

                withTimeout(75_000L) {
                    aiService.runStudyToolStream(tool, subject, input.trim(), language).collect { chunk ->
                        accumulated.append(chunk)
                        _toolAiState.value = AiStreamState.Streaming(accumulated.toString())
                    }
                }
                val text = accumulated.toString().trim()
                if (text.isBlank()) {
                    _toolAiState.value = AiStreamState.Error("AI returned an empty result. Please try again.")
                    return@launch
                }
                _toolAiState.value = AiStreamState.Success(text, savedToHistory = true)
                viewModelScope.launch {
                    runCatching {
                        withTimeout(10_000L) {
                            persistNote(
                                subject = subject,
                                toolType = tool.firestoreType,
                                title = "${tool.title}: ${input.trim().take(80)}",
                                content = text
                            )
                            recordActivity(minutesAdded = 5, questionsAdded = 1)
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Study tool timed out", e)
                _toolAiState.value = AiStreamState.Error("Request timed out. Please check your connection and try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Study tool failed", e)
                _toolAiState.value = AiStreamState.Error(formatAiError(e))
            } finally {
                val cur = _toolAiState.value
                if (cur is AiStreamState.Loading || cur is AiStreamState.Streaming) {
                    _toolAiState.value = AiStreamState.Error("Study tool request did not finish. Please try again.")
                }
            }
        }
    }

    fun resetToolState() {
        _toolAiState.value = AiStreamState.Idle
        toolJob?.cancel()
    }

    fun generateOfflineQuiz(subject: String, count: Int) {
        val questions = OfflineStudyBank.getOfflineQuiz(subject, count)
        _quizState.value = QuizUiState.Ready(
            topic = "$subject Offline Practice",
            subject = subject,
            questions = questions,
            isOfflinePack = true
        )
    }

    fun generateQuiz(subject: String, topic: String, difficulty: String, count: Int, language: String) {
        if (_quizState.value is QuizUiState.Loading) return
        val currentlyOnline = isOnline.value || networkMonitor.isCurrentlyOnline()
        if (!currentlyOnline) {
            generateOfflineQuiz(subject, count)
            return
        }
        if (topic.isBlank()) {
            _quizState.value = QuizUiState.Error("Enter a topic to generate a practice quiz (or tap Offline Quiz).")
            return
        }
        _quizState.value = QuizUiState.Loading
        quizJob?.cancel()
        quizJob = viewModelScope.launch {
            try {
                val questions = withTimeout(75_000L) {
                    aiService.generateQuiz(subject, topic.trim(), difficulty, count, language)
                }
                if (questions.isEmpty()) {
                    _quizState.value = QuizUiState.Error("Could not generate quiz questions. Try a clearer topic.")
                } else {
                    _quizState.value = QuizUiState.Ready(
                        topic = topic.trim(),
                        subject = subject,
                        questions = questions
                    )
                    viewModelScope.launch {
                        runCatching { recordActivity(minutesAdded = 5, questionsAdded = 1) }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Quiz generation timed out", e)
                _quizState.value = QuizUiState.Error("Quiz generation timed out. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Quiz generation failed", e)
                _quizState.value = QuizUiState.Error(formatAiError(e))
            } finally {
                if (_quizState.value is QuizUiState.Loading) {
                    _quizState.value = QuizUiState.Error("Quiz request did not complete. Please try again.")
                }
            }
        }
    }

    fun selectQuizAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _quizState.value as? QuizUiState.Ready ?: return
        if (current.submitted) return
        _quizState.value = current.copy(
            selectedAnswers = current.selectedAnswers + (questionIndex to optionIndex)
        )
    }

    fun submitQuiz() {
        val current = _quizState.value as? QuizUiState.Ready ?: return
        val updated = current.copy(submitted = true)
        _quizState.value = updated
        val score = updated.questions.indices.count { idx ->
            updated.selectedAnswers[idx] == updated.questions[idx].correctIndex
        }
        viewModelScope.launch {
            val summary = buildString {
                appendLine("Score: $score / ${updated.questions.size}")
                updated.questions.forEachIndexed { i, q ->
                    appendLine("${i + 1}. ${q.question}")
                    appendLine("Answer: ${q.options.getOrElse(q.correctIndex) { "" }}")
                    appendLine("Why: ${q.explanation}")
                    appendLine()
                }
            }
            persistNote(
                subject = updated.subject,
                toolType = "quiz",
                title = "Quiz (${score}/${updated.questions.size}): ${updated.topic}",
                content = summary
            )
            recordActivity(minutesAdded = 5, questionsAdded = score)
        }
    }

    fun resetQuiz() {
        _quizState.value = QuizUiState.Idle
        quizJob?.cancel()
    }

    fun updateChatSubject(subject: String) {
        _chatSubject.value = subject
    }

    fun startNewChat() {
        chatJob?.cancel()
        _chatSending.value = false
        _activeConversationId.value = UUID.randomUUID().toString()
        _chatMessages.value = emptyList()
        _chatAttachment.value = null
    }

    fun selectConversation(conversationId: String) {
        val conv = _conversations.value.firstOrNull { it.id == conversationId } ?: return
        chatJob?.cancel()
        _chatSending.value = false
        _activeConversationId.value = conv.id
        _chatSubject.value = conv.subject
        _chatMessages.value = conv.messages
        _chatAttachment.value = null
    }

    fun deleteConversation(conversationId: String) {
        _conversations.value = _conversations.value.filterNot { it.id == conversationId }
        if (_activeConversationId.value == conversationId) {
            startNewChat()
        }
        viewModelScope.launch { saveLocalSnapshot() }
    }

    fun stopChatGeneration() {
        chatJob?.cancel()
        _chatSending.value = false
        _chatMessages.value = _chatMessages.value.map { msg ->
            if (msg.isStreaming) {
                msg.copy(
                    text = msg.text.ifBlank { "Generation stopped." },
                    isStreaming = false
                )
            } else {
                msg
            }
        }
        viewModelScope.launch { persistActiveConversation() }
    }

    fun retryLastChatMessage() {
        if (_chatSending.value) return
        val lastUser = _chatMessages.value.lastOrNull { it.isUser } ?: return
        // Drop trailing error/partial assistant message after the last user message
        val lastUserIdx = _chatMessages.value.indexOfLast { it.id == lastUser.id }
        val trimmedHistory = _chatMessages.value.take(lastUserIdx)
        _chatMessages.value = trimmedHistory
        sendChatMessageInternal(lastUser.text, emptyList(), lastUser.attachmentLabel)
    }

    fun sendChatMessage(messageText: String) {
        val clean = messageText.trim()
        val attachment = _chatAttachment.value
        val bitmaps = attachment?.pages?.map { it.bitmap } ?: emptyList()
        if ((clean.isBlank() && bitmaps.isEmpty()) || _chatSending.value) return
        _chatAttachment.value = null
        sendChatMessageInternal(clean, bitmaps, attachment?.displayName)
    }

    private fun sendChatMessageInternal(
        clean: String,
        bitmaps: List<Bitmap>,
        attachmentLabel: String?
    ) {
        if (_chatSending.value) return
        val subject = _chatSubject.value
        val language = _solverLanguage.value
        val priorHistory = _chatMessages.value

        val userDisplayText = clean.ifBlank { "Solve / explain the attached file: ${attachmentLabel ?: "Attachment"}" }
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = userDisplayText,
            subject = subject,
            attachmentLabel = attachmentLabel
        )

        val currentlyOnline = isOnline.value || networkMonitor.isCurrentlyOnline()
        if (!currentlyOnline) {
            val offlineReply = if (bitmaps.isNotEmpty() && clean.isBlank()) {
                "Your device is offline. Photo and PDF scanning require an internet connection.\n\nType any math equation (like `2x + 5 = 15`) or subject topic and I can help offline right away!"
            } else {
                OfflineMathEngine.trySolveStepByStep(clean, subject, language)
                    ?: OfflineStudyBank.searchOfflineStudyContent(clean, subject, language)
            }
            val botMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = offlineReply,
                subject = subject,
                isStreaming = false,
                isError = bitmaps.isNotEmpty() && clean.isBlank(),
                isOfflineAnswer = !(bitmaps.isNotEmpty() && clean.isBlank())
            )
            _chatMessages.value = priorHistory + userMsg + botMsg
            viewModelScope.launch {
                runCatching {
                    persistActiveConversation()
                    recordActivity(minutesAdded = 3, questionsAdded = 1)
                }
            }
            return
        }

        val botId = UUID.randomUUID().toString()
        val placeholderBot = ChatMessage(
            id = botId,
            isUser = false,
            text = "",
            subject = subject,
            isStreaming = true
        )
        _chatSending.value = true
        _chatMessages.value = priorHistory + userMsg + placeholderBot

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(75_000L) {
                    aiService.sendChatConversationStream(
                        priorMessages = priorHistory,
                        userMessage = clean,
                        subject = subject,
                        language = language,
                        attachments = bitmaps
                    ).collect { chunk ->
                        accumulated.append(chunk)
                        _chatMessages.value = _chatMessages.value.map { msg ->
                            if (msg.id == botId) msg.copy(text = accumulated.toString()) else msg
                        }
                    }
                }
                val finalReply = accumulated.toString().trim()
                if (finalReply.isBlank()) {
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == botId) {
                            msg.copy(
                                text = "Abhay GPT received an empty response from the server. Tap Retry below to ask again.",
                                isStreaming = false,
                                isError = true
                            )
                        } else msg
                    }
                } else {
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == botId) msg.copy(text = finalReply, isStreaming = false, isError = false) else msg
                    }
                    viewModelScope.launch {
                        runCatching {
                            withTimeout(10_000L) {
                                persistQuestion(
                                    subject = subject,
                                    topic = "Abhay GPT Chat",
                                    questionText = userDisplayText,
                                    answerSummary = finalReply,
                                    mode = "chat",
                                    language = language,
                                    hasAttachment = bitmaps.isNotEmpty()
                                )
                                recordActivity(minutesAdded = 3, questionsAdded = 1)
                            }
                        }
                    }
                }
                viewModelScope.launch { runCatching { persistActiveConversation() } }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat timed out", e)
                val partial = accumulated.toString().trim()
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == botId) {
                        if (partial.isNotBlank()) {
                            msg.copy(
                                text = "$partial\n\n*(Response stopped due to timeout — tap Retry for full answer.)*",
                                isStreaming = false,
                                isError = false
                            )
                        } else {
                            msg.copy(
                                text = "Request timed out after waiting for Gemini server. Please check your internet connection and tap Retry.",
                                isStreaming = false,
                                isError = true
                            )
                        }
                    } else msg
                }
                viewModelScope.launch { runCatching { persistActiveConversation() } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat failed", e)
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == botId) {
                        msg.copy(
                            text = formatAiError(e),
                            isStreaming = false,
                            isError = true
                        )
                    } else msg
                }
                viewModelScope.launch { runCatching { persistActiveConversation() } }
            } finally {
                _chatSending.value = false
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == botId && msg.isStreaming) {
                        if (msg.text.isBlank()) {
                            msg.copy(
                                text = "Response did not complete. Please tap Retry.",
                                isStreaming = false,
                                isError = true
                            )
                        } else {
                            msg.copy(isStreaming = false)
                        }
                    } else {
                        msg
                    }
                }
            }
        }
    }

    private suspend fun persistActiveConversation() {
        val msgs = _chatMessages.value.map { it.copy(isStreaming = false) }
        if (msgs.isEmpty()) return
        val firstUserText = msgs.firstOrNull { it.isUser }?.text.orEmpty().ifBlank { "New Chat" }
        val title = firstUserText.lineSequence().firstOrNull().orEmpty().take(48).ifBlank { "New Chat" }
        val conv = ChatConversation(
            id = _activeConversationId.value,
            title = title,
            subject = _chatSubject.value,
            messages = msgs,
            updatedAtMillis = System.currentTimeMillis()
        )
        val updatedList = listOf(conv) + _conversations.value.filterNot { it.id == conv.id }
        _conversations.value = updatedList.take(40)
        saveLocalSnapshot()
    }

    fun addStudyPlanTask(subject: String, title: String, scheduledDate: String, durationMinutes: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            if (repository != null) {
                repository.addStudyPlan(subject, title, scheduledDate, durationMinutes)
            } else {
                val task = StudyPlanTask(
                    id = UUID.randomUUID().toString(),
                    userId = "guest",
                    subject = subject,
                    title = title.trim(),
                    scheduledDate = scheduledDate.trim().ifBlank { "Today" },
                    durationMinutes = durationMinutes.toLong(),
                    completed = false
                )
                _guestPlans.value = listOf(task) + _guestPlans.value
                saveLocalSnapshot()
            }
        }
    }

    fun toggleStudyPlan(task: StudyPlanTask) {
        viewModelScope.launch {
            if (repository != null) {
                repository.toggleStudyPlanCompleted(task)
            } else {
                _guestPlans.value = _guestPlans.value.map {
                    if (it.id == task.id) it.copy(completed = !it.completed) else it
                }
                saveLocalSnapshot()
            }
            if (!task.completed) {
                recordActivity(
                    minutesAdded = task.durationMinutes.toInt().coerceIn(5, 60),
                    questionsAdded = 0
                )
            }
        }
    }

    fun deleteStudyPlan(planId: String) {
        viewModelScope.launch {
            if (repository != null) {
                repository.deleteStudyPlan(planId)
            } else {
                _guestPlans.value = _guestPlans.value.filterNot { it.id == planId }
                saveLocalSnapshot()
            }
        }
    }

    fun deleteQuestion(questionId: String) {
        viewModelScope.launch {
            if (repository != null) {
                repository.deleteQuestion(questionId)
            } else {
                _guestQuestions.value = _guestQuestions.value.filterNot { it.id == questionId }
                saveLocalSnapshot()
            }
        }
    }

    fun deleteSavedNote(noteId: String) {
        viewModelScope.launch {
            if (repository != null) {
                repository.deleteSavedNote(noteId)
            } else {
                _guestNotes.value = _guestNotes.value.filterNot { it.id == noteId }
                saveLocalSnapshot()
            }
        }
    }

    fun updateStudyGoal(goalMinutes: Int, preferredLanguage: String) {
        viewModelScope.launch {
            if (repository != null) {
                repository.updateProfilePreferences(currentUserId, goalMinutes, preferredLanguage)
            } else {
                _guestProfile.value = _guestProfile.value?.copy(
                    dailyGoalMinutes = goalMinutes.toLong().coerceIn(5L, 600L),
                    preferredLanguage = preferredLanguage
                )
                saveLocalSnapshot()
            }
            _solverLanguage.value = preferredLanguage
        }
    }

    private suspend fun persistQuestion(
        subject: String,
        topic: String,
        questionText: String,
        answerSummary: String,
        mode: String,
        language: String,
        hasAttachment: Boolean
    ): Boolean {
        return if (repository != null) {
            repository.saveSolvedQuestion(
                subject = subject,
                topic = topic,
                questionText = questionText,
                answerSummary = answerSummary,
                mode = mode,
                language = language,
                hasAttachment = hasAttachment
            ).isSuccess
        } else {
            val item = SolvedQuestion(
                id = UUID.randomUUID().toString(),
                userId = "guest",
                subject = subject,
                topic = topic,
                questionText = questionText,
                answerSummary = answerSummary,
                mode = mode,
                language = language,
                hasAttachment = hasAttachment
            )
            _guestQuestions.value = listOf(item) + _guestQuestions.value
            saveLocalSnapshot()
            true
        }
    }

    private suspend fun persistNote(
        subject: String,
        toolType: String,
        title: String,
        content: String
    ): Boolean {
        return if (repository != null) {
            repository.saveStudyNote(
                subject = subject,
                toolType = toolType,
                title = title,
                content = content
            ).isSuccess
        } else {
            val note = SavedStudyNote(
                id = UUID.randomUUID().toString(),
                userId = "guest",
                subject = subject,
                toolType = toolType,
                title = title,
                content = content
            )
            _guestNotes.value = listOf(note) + _guestNotes.value
            saveLocalSnapshot()
            true
        }
    }

    private suspend fun recordActivity(minutesAdded: Int, questionsAdded: Int) {
        if (repository != null) {
            repository.recordStudyActivity(currentUserId, minutesAdded, questionsAdded)
        } else {
            _guestProfile.value = _guestProfile.value?.let { current ->
                current.copy(
                    todayMinutesStudied = (current.todayMinutesStudied + minutesAdded).coerceAtMost(1440L),
                    todayQuestionsSolved = (current.todayQuestionsSolved + questionsAdded).coerceAtMost(1000L)
                )
            }
            saveLocalSnapshot()
        }
    }

    private fun isNetworkException(error: Throwable): Boolean {
        return generateSequence(error) { it.cause }.any {
            it is UnknownHostException ||
                it is ConnectException ||
                it is SocketTimeoutException ||
                it is IOException
        }
    }

    private fun formatAiError(error: Throwable): String {
        val raw = error.localizedMessage ?: error.message ?: "Unable to get a response right now."
        return when {
            raw.contains("403", ignoreCase = true) || raw.contains("App Check", ignoreCase = true) ->
                "App Check / Firebase authorization error (403): $raw"
            raw.contains("429", ignoreCase = true) || raw.contains("quota", ignoreCase = true) || raw.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                "AI rate limit reached right now. Please wait a few seconds and tap Retry."
            isNetworkException(error) ->
                "Network error: unable to reach Gemini server ($raw). Check your internet connection and tap Retry."
            else -> raw
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private companion object {
        const val TAG = "AbhayGptVM"
    }
}
