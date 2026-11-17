package com.example.ui.navigation

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.core.NetworkMonitor
import com.example.core.ThemeMode
import com.example.core.ThemePreferences
import com.example.data.ai.SmartStudyAiService
import com.example.data.local.LocalStudyStore
import com.example.data.model.ChatConversation
import com.example.data.model.StudyCatalog
import com.example.data.model.StudyToolType
import com.example.data.repository.StudyRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MathSolverScreen
import com.example.ui.screens.QuestionSolverScreen
import com.example.ui.screens.StudyToolsScreen
import com.example.ui.screens.SubjectTutorScreen
import com.example.ui.screens.ToolDetailScreen
import com.example.ui.study.StudyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object Routes {
    const val CHAT = "chat"
    const val HOME = "home"
    const val SOLVER = "solver"
    const val MATH = "math"
    const val TOOLS = "tools"
    const val SUBJECT_TUTOR = "subject_tutor/{subjectId}"
    const val TOOL_DETAIL = "tool_detail/{toolId}?subject={subject}&topic={topic}"

    fun subjectTutor(subjectId: String) = "subject_tutor/$subjectId"
    fun toolDetail(toolId: String, subject: String = "General", topic: String = ""): String {
        val encSubj = Uri.encode(subject)
        val encTopic = Uri.encode(topic)
        return "tool_detail/$toolId?subject=$encSubj&topic=$encTopic"
    }
}

@Composable
fun SmartStudyRoot(
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { ThemePreferences(context.applicationContext) }
    val networkMonitor = remember(context) { NetworkMonitor(context.applicationContext) }
    val isOnline by networkMonitor.isOnlineFlow.collectAsStateWithLifecycle(
        initialValue = networkMonitor.isCurrentlyOnline()
    )
    val isGuestMode by prefs.isGuestMode.collectAsStateWithLifecycle(initialValue = false)

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    when {
        user != null -> {
            key(user.uid) {
                AbhayGptApp(
                    isGuest = false,
                    currentUserId = user.uid,
                    displayName = user.displayName ?: "Student",
                    themeMode = themeMode,
                    onCycleTheme = onCycleTheme,
                    onExitGuest = {}
                )
            }
        }
        isGuestMode -> {
            key("guest") {
                AbhayGptApp(
                    isGuest = true,
                    currentUserId = "guest",
                    displayName = "Guest User",
                    themeMode = themeMode,
                    onCycleTheme = onCycleTheme,
                    onExitGuest = { scope.launch { prefs.setGuestMode(false) } }
                )
            }
        }
        else -> {
            AuthScreen(
                isOnline = isOnline,
                onAuthSuccess = { scope.launch { prefs.setGuestMode(false) } },
                onContinueAsGuest = { scope.launch { prefs.setGuestMode(true) } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbhayGptApp(
    isGuest: Boolean,
    currentUserId: String,
    displayName: String,
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    onExitGuest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: StudyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val repo = if (isGuest) {
                    null
                } else {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    StudyRepository(db)
                }
                StudyViewModel(
                    repository = repo,
                    aiService = SmartStudyAiService(),
                    localStore = LocalStudyStore(app),
                    networkMonitor = NetworkMonitor(app),
                    currentUserId = currentUserId,
                    displayName = displayName,
                    isGuest = isGuest
                )
            }
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val recentQuestionsState by viewModel.recentQuestionsState.collectAsStateWithLifecycle()
    val studyPlansState by viewModel.studyPlansState.collectAsStateWithLifecycle()
    val savedNotesState by viewModel.savedNotesState.collectAsStateWithLifecycle()

    val solverQuestion by viewModel.solverQuestion.collectAsStateWithLifecycle()
    val solverSubject by viewModel.solverSubject.collectAsStateWithLifecycle()
    val solverLanguage by viewModel.solverLanguage.collectAsStateWithLifecycle()
    val solverAttachment by viewModel.solverAttachment.collectAsStateWithLifecycle()
    val attachmentLoading by viewModel.attachmentLoading.collectAsStateWithLifecycle()
    val solverAiState by viewModel.solverAiState.collectAsStateWithLifecycle()

    val selectedMathTopic by viewModel.selectedMathTopic.collectAsStateWithLifecycle()
    val mathProblem by viewModel.mathProblem.collectAsStateWithLifecycle()
    val mathAttachment by viewModel.mathAttachment.collectAsStateWithLifecycle()
    val mathAiState by viewModel.mathAiState.collectAsStateWithLifecycle()

    val tutorAiState by viewModel.tutorAiState.collectAsStateWithLifecycle()
    val toolAiState by viewModel.toolAiState.collectAsStateWithLifecycle()
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatSubject by viewModel.chatSubject.collectAsStateWithLifecycle()
    val chatAttachment by viewModel.chatAttachment.collectAsStateWithLifecycle()
    val chatSending by viewModel.chatSending.collectAsStateWithLifecycle()

    val navigateToRoute: (String) -> Unit = { route ->
        scope.launch { drawerState.close() }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute == Routes.CHAT,
        drawerContent = {
            AbhayGptSidebarDrawer(
                displayName = displayName,
                isGuest = isGuest,
                conversations = conversations,
                activeConversationId = activeConversationId,
                onNewChat = {
                    viewModel.startNewChat()
                    scope.launch { drawerState.close() }
                    if (currentRoute != Routes.CHAT) {
                        navController.popBackStack(Routes.CHAT, inclusive = false)
                    }
                },
                onSelectConversation = { convId ->
                    viewModel.selectConversation(convId)
                    scope.launch { drawerState.close() }
                    if (currentRoute != Routes.CHAT) {
                        navController.popBackStack(Routes.CHAT, inclusive = false)
                    }
                },
                onDeleteConversation = viewModel::deleteConversation,
                onOpenSubjectsHub = { navigateToRoute(Routes.HOME) },
                onOpenMathSolver = { navigateToRoute(Routes.MATH) },
                onOpenPhotoSolver = { navigateToRoute(Routes.SOLVER) },
                onOpenStudyTools = { navigateToRoute(Routes.TOOLS) },
                onSignOutOrSignIn = {
                    scope.launch { drawerState.close() }
                    if (isGuest) {
                        onExitGuest()
                    } else {
                        signOutUser(credentialManager, scope)
                    }
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isOnline) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WifiOff,
                                contentDescription = "Offline mode",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Offline Mode • Math Engine, Notes, Quizzes & Planner work without internet",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val isWideScreen = maxWidth >= 600.dp
                    NavHost(
                        navController = navController,
                        startDestination = Routes.CHAT,
                        modifier = Modifier
                            .widthIn(max = 960.dp)
                            .fillMaxSize()
                    ) {
                        composable(Routes.CHAT) {
                            AiChatScreen(
                                messages = chatMessages,
                                selectedSubject = chatSubject,
                                selectedLanguage = solverLanguage,
                                attachment = chatAttachment,
                                attachmentLoading = attachmentLoading,
                                isSending = chatSending,
                                isOnline = isOnline,
                                themeMode = themeMode,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onNewChat = viewModel::startNewChat,
                                onCycleTheme = onCycleTheme,
                                onSubjectChange = viewModel::updateChatSubject,
                                onLanguageChange = viewModel::updateSolverLanguage,
                                onLoadImage = { uri -> viewModel.loadChatImage(context, uri) },
                                onLoadPdf = { uri -> viewModel.loadChatPdf(context, uri) },
                                onClearAttachment = viewModel::clearChatAttachment,
                                onSendMessage = viewModel::sendChatMessage,
                                onStopGeneration = viewModel::stopChatGeneration,
                                onRetryLast = viewModel::retryLastChatMessage,
                                onOpenMathSolver = { navigateToRoute(Routes.MATH) },
                                onOpenStudyHub = { navigateToRoute(Routes.HOME) }
                            )
                        }

                        composable(Routes.HOME) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                SubScreenBackBar(
                                    title = "Subjects & Study Hub",
                                    onBack = { navController.popBackStack() }
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    HomeDashboardScreen(
                                        displayName = displayName,
                                        isGuest = isGuest,
                                        profileState = profileState,
                                        recentQuestionsState = recentQuestionsState,
                                        themeMode = themeMode,
                                        isWideScreen = isWideScreen,
                                        onCycleTheme = onCycleTheme,
                                        onSignOutOrSignIn = {
                                            if (isGuest) onExitGuest() else signOutUser(credentialManager, scope)
                                        },
                                        onAskAi = { query ->
                                            navController.popBackStack(Routes.CHAT, inclusive = false)
                                            viewModel.startNewChat()
                                            viewModel.sendChatMessage(query)
                                        },
                                        onOpenSolver = { navigateToRoute(Routes.SOLVER) },
                                        onOpenMathSolver = { navigateToRoute(Routes.MATH) },
                                        onOpenHomeworkHelper = {
                                            viewModel.resetToolState()
                                            navController.navigate(
                                                Routes.toolDetail(StudyToolType.HOMEWORK_HELPER.id)
                                            )
                                        },
                                        onOpenSubject = { subject ->
                                            viewModel.resetTutorState()
                                            navController.navigate(Routes.subjectTutor(subject.id))
                                        },
                                        onDeleteQuestion = viewModel::deleteQuestion
                                    )
                                }
                            }
                        }

                        composable(Routes.SOLVER) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                SubScreenBackBar(
                                    title = "Photo & PDF Solver",
                                    onBack = { navController.popBackStack() }
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    QuestionSolverScreen(
                                        questionText = solverQuestion,
                                        selectedSubject = solverSubject,
                                        selectedLanguage = solverLanguage,
                                        attachment = solverAttachment,
                                        attachmentLoading = attachmentLoading,
                                        aiState = solverAiState,
                                        onQuestionChange = viewModel::updateSolverQuestion,
                                        onSubjectChange = viewModel::updateSolverSubject,
                                        onLanguageChange = viewModel::updateSolverLanguage,
                                        onLoadImage = { uri -> viewModel.loadSolverImage(context, uri) },
                                        onLoadPdf = { uri -> viewModel.loadSolverPdf(context, uri) },
                                        onClearAttachment = viewModel::clearSolverAttachment,
                                        onSolve = { viewModel.solveQuestion() }
                                    )
                                }
                            }
                        }

                        composable(Routes.MATH) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                SubScreenBackBar(
                                    title = "Maths Solver",
                                    onBack = { navController.popBackStack() }
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    MathSolverScreen(
                                        selectedTopic = selectedMathTopic,
                                        problemText = mathProblem,
                                        selectedLanguage = solverLanguage,
                                        attachment = mathAttachment,
                                        aiState = mathAiState,
                                        onSelectTopic = viewModel::selectMathTopic,
                                        onProblemChange = viewModel::updateMathProblem,
                                        onLanguageChange = viewModel::updateSolverLanguage,
                                        onLoadImage = { uri -> viewModel.loadMathImage(context, uri) },
                                        onClearAttachment = viewModel::clearMathAttachment,
                                        onSolve = viewModel::solveMathProblem,
                                        onSolveOffline = viewModel::solveMathOffline
                                    )
                                }
                            }
                        }

                        composable(Routes.TOOLS) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                SubScreenBackBar(
                                    title = "Study Tools & Quizzes",
                                    onBack = { navController.popBackStack() }
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    StudyToolsScreen(
                                        savedNotesState = savedNotesState,
                                        isWideScreen = isWideScreen,
                                        onOpenTool = { tool ->
                                            viewModel.resetToolState()
                                            if (tool == StudyToolType.QUIZ_GENERATOR) viewModel.resetQuiz()
                                            navController.navigate(Routes.toolDetail(tool.id))
                                        },
                                        onDeleteNote = viewModel::deleteSavedNote
                                    )
                                }
                            }
                        }

                        composable(
                            route = Routes.SUBJECT_TUTOR,
                            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
                        ) { entry ->
                            val subjectId = entry.arguments?.getString("subjectId")
                            val subject = StudyCatalog.allSubjects.firstOrNull { it.id == subjectId }
                                ?: StudyCatalog.allSubjects.first()
                            SubjectTutorScreen(
                                subject = subject,
                                selectedLanguage = solverLanguage,
                                tutorState = tutorAiState,
                                onLanguageChange = viewModel::updateSolverLanguage,
                                onGenerateTutorContent = { topic, mode, lang ->
                                    viewModel.runSubjectTutor(subject.name, topic, mode, lang)
                                },
                                onLaunchSubjectQuiz = { topic ->
                                    viewModel.resetQuiz()
                                    navController.navigate(
                                        Routes.toolDetail(
                                            toolId = StudyToolType.QUIZ_GENERATOR.id,
                                            subject = subject.name,
                                            topic = topic
                                        )
                                    )
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Routes.TOOL_DETAIL,
                            arguments = listOf(
                                navArgument("toolId") { type = NavType.StringType },
                                navArgument("subject") {
                                    type = NavType.StringType
                                    defaultValue = "General"
                                },
                                navArgument("topic") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { entry ->
                            val toolId = entry.arguments?.getString("toolId")
                            val tool = StudyToolType.entries.firstOrNull { it.id == toolId }
                                ?: StudyToolType.NOTES_MAKER
                            val subj = entry.arguments?.getString("subject").orEmpty().ifBlank { "General" }
                            val topic = entry.arguments?.getString("topic").orEmpty()

                            ToolDetailScreen(
                                tool = tool,
                                initialSubject = subj,
                                initialTopic = topic,
                                selectedLanguage = solverLanguage,
                                toolAiState = toolAiState,
                                quizState = quizState,
                                studyPlansState = studyPlansState,
                                onLanguageChange = viewModel::updateSolverLanguage,
                                onRunTool = { s, input, lang ->
                                    viewModel.runStudyTool(tool, s, input, lang)
                                },
                                onGenerateQuiz = viewModel::generateQuiz,
                                onGenerateOfflineQuiz = viewModel::generateOfflineQuiz,
                                onSelectQuizAnswer = viewModel::selectQuizAnswer,
                                onSubmitQuiz = viewModel::submitQuiz,
                                onResetQuiz = viewModel::resetQuiz,
                                onAddStudyPlan = viewModel::addStudyPlanTask,
                                onToggleStudyPlan = viewModel::toggleStudyPlan,
                                onDeleteStudyPlan = viewModel::deleteStudyPlan,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubScreenBackBar(
    title: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("subscreen_back_button")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Abhay GPT")
            }
        }
    )
}

@Composable
private fun AbhayGptSidebarDrawer(
    displayName: String,
    isGuest: Boolean,
    conversations: List<ChatConversation>,
    activeConversationId: String,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onOpenSubjectsHub: () -> Unit,
    onOpenMathSolver: () -> Unit,
    onOpenPhotoSolver: () -> Unit,
    onOpenStudyTools: () -> Unit,
    onSignOutOrSignIn: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredChats = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 310.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Abhay GPT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onNewChat,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("drawer_new_chat_button")
            ) {
                Icon(Icons.Rounded.EditNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New Chat")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Study Modes & Tools",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            NavigationDrawerItem(
                label = { Text("Maths Solver") },
                icon = { Icon(Icons.Rounded.Calculate, contentDescription = null) },
                selected = false,
                onClick = onOpenMathSolver,
                modifier = Modifier.height(48.dp)
            )
            NavigationDrawerItem(
                label = { Text("Photo & PDF Solver") },
                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                selected = false,
                onClick = onOpenPhotoSolver,
                modifier = Modifier.height(48.dp)
            )
            NavigationDrawerItem(
                label = { Text("All 11 Subjects") },
                icon = { Icon(Icons.Rounded.School, contentDescription = null) },
                selected = false,
                onClick = onOpenSubjectsHub,
                modifier = Modifier.height(48.dp)
            )
            NavigationDrawerItem(
                label = { Text("Quizzes, Notes & Tools") },
                icon = { Icon(Icons.Rounded.Construction, contentDescription = null) },
                selected = false,
                onClick = onOpenStudyTools,
                modifier = Modifier.height(48.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Recent Chats (${filteredChats.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (filteredChats.isEmpty()) {
                    item {
                        Text(
                            text = "No saved conversations yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(filteredChats, key = { it.id }) { conv ->
                        val isSelected = conv.id == activeConversationId
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectConversation(conv.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = conv.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDeleteConversation(conv.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete chat",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onSignOutOrSignIn)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isGuest) "Guest Mode • Tap to Sign in with Google" else "Signed in • Tap to Sign out",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (isGuest) Icons.AutoMirrored.Rounded.Login else Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = if (isGuest) "Sign in with Google" else "Sign out",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
