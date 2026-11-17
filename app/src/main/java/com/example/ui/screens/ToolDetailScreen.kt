package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.StudyPlanTask
import com.example.data.model.StudyToolType
import com.example.ui.components.AiSolutionResultCard
import com.example.ui.components.LanguageSelectorRow
import com.example.ui.components.SubjectChipRow
import com.example.ui.study.AiStreamState
import com.example.ui.study.FirestoreUiState
import com.example.ui.study.QuizUiState
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailScreen(
    tool: StudyToolType,
    initialSubject: String,
    initialTopic: String,
    selectedLanguage: String,
    toolAiState: AiStreamState,
    quizState: QuizUiState,
    studyPlansState: FirestoreUiState<List<StudyPlanTask>>,
    onLanguageChange: (String) -> Unit,
    onRunTool: (subject: String, input: String, language: String) -> Unit,
    onGenerateQuiz: (subject: String, topic: String, difficulty: String, count: Int, language: String) -> Unit,
    onGenerateOfflineQuiz: (subject: String, count: Int) -> Unit,
    onSelectQuizAnswer: (Int, Int) -> Unit,
    onSubmitQuiz: () -> Unit,
    onResetQuiz: () -> Unit,
    onAddStudyPlan: (subject: String, title: String, scheduledDate: String, durationMinutes: Int) -> Unit,
    onToggleStudyPlan: (StudyPlanTask) -> Unit,
    onDeleteStudyPlan: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedSubject by remember(tool, initialSubject) {
        mutableStateOf(initialSubject.ifBlank { "General" })
    }
    var userInput by remember(tool, initialTopic) { mutableStateOf(initialTopic) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool.title) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tool_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (tool) {
            StudyToolType.QUIZ_GENERATOR -> {
                QuizGeneratorContent(
                    selectedSubject = selectedSubject,
                    topic = userInput,
                    selectedLanguage = selectedLanguage,
                    quizState = quizState,
                    onSubjectChange = { selectedSubject = it },
                    onTopicChange = { userInput = it },
                    onLanguageChange = onLanguageChange,
                    onGenerateQuiz = { diff, count ->
                        onGenerateQuiz(selectedSubject, userInput, diff, count, selectedLanguage)
                    },
                    onGenerateOfflineQuiz = { count ->
                        onGenerateOfflineQuiz(selectedSubject, count)
                    },
                    onSelectAnswer = onSelectQuizAnswer,
                    onSubmitQuiz = onSubmitQuiz,
                    onResetQuiz = onResetQuiz,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            StudyToolType.STUDY_PLANNER -> {
                StudyPlannerContent(
                    selectedSubject = selectedSubject,
                    selectedLanguage = selectedLanguage,
                    toolAiState = toolAiState,
                    studyPlansState = studyPlansState,
                    onSubjectChange = { selectedSubject = it },
                    onLanguageChange = onLanguageChange,
                    onAddPlan = onAddStudyPlan,
                    onTogglePlan = onToggleStudyPlan,
                    onDeletePlan = onDeleteStudyPlan,
                    onGenerateAiPlan = { goal ->
                        onRunTool(selectedSubject, goal, selectedLanguage)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            else -> {
                GenericAiToolContent(
                    tool = tool,
                    selectedSubject = selectedSubject,
                    userInput = userInput,
                    selectedLanguage = selectedLanguage,
                    toolAiState = toolAiState,
                    onSubjectChange = { selectedSubject = it },
                    onInputChange = { userInput = it },
                    onLanguageChange = onLanguageChange,
                    onRun = { onRunTool(selectedSubject, userInput, selectedLanguage) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun GenericAiToolContent(
    tool: StudyToolType,
    selectedSubject: String,
    userInput: String,
    selectedLanguage: String,
    toolAiState: AiStreamState,
    onSubjectChange: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = tool.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Subject", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    SubjectChipRow(
                        selectedSubject = selectedSubject,
                        includeGeneral = true,
                        onSelect = onSubjectChange
                    )

                    Spacer(Modifier.height(10.dp))
                    Text("Language", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    LanguageSelectorRow(
                        selectedLanguage = selectedLanguage,
                        onSelect = onLanguageChange
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = onInputChange,
                        label = { Text(tool.inputLabel) },
                        placeholder = { Text(tool.placeholder) },
                        minLines = 3,
                        maxLines = 7,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tool_input_field")
                    )

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onRun,
                        enabled = toolAiState !is AiStreamState.Loading && toolAiState !is AiStreamState.Streaming,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("tool_generate_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate with AI")
                    }
                }
            }
        }

        item {
            AiSolutionResultCard(
                state = toolAiState,
                title = "${tool.title} Result"
            )
        }
    }
}

@Composable
private fun QuizGeneratorContent(
    selectedSubject: String,
    topic: String,
    selectedLanguage: String,
    quizState: QuizUiState,
    onSubjectChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onGenerateQuiz: (difficulty: String, count: Int) -> Unit,
    onGenerateOfflineQuiz: (count: Int) -> Unit,
    onSelectAnswer: (Int, Int) -> Unit,
    onSubmitQuiz: () -> Unit,
    onResetQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    var difficulty by remember { mutableStateOf("Medium") }
    var count by remember { mutableIntStateOf(5) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Subject", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    SubjectChipRow(selectedSubject = selectedSubject, includeGeneral = false, onSelect = onSubjectChange)

                    Spacer(Modifier.height(10.dp))
                    Text("Difficulty & Questions", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Easy", "Medium", "Hard").forEach { diff ->
                            FilterChip(
                                selected = diff == difficulty,
                                onClick = { difficulty = diff },
                                label = { Text(diff) }
                            )
                        }
                        listOf(3, 5).forEach { qCount ->
                            FilterChip(
                                selected = count == qCount,
                                onClick = { count = qCount },
                                label = { Text("${qCount}Q") }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    LanguageSelectorRow(selectedLanguage = selectedLanguage, onSelect = onLanguageChange)

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = topic,
                        onValueChange = onTopicChange,
                        label = { Text("Quiz Topic") },
                        placeholder = { Text("e.g. Quadratic Equations, Photosynthesis, World War II") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_topic_input")
                    )

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onGenerateOfflineQuiz(count) },
                            enabled = quizState !is QuizUiState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("quiz_offline_button")
                        ) {
                            Text("Offline Quiz")
                        }
                        Button(
                            onClick = { onGenerateQuiz(difficulty, count) },
                            enabled = quizState !is QuizUiState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("quiz_generate_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("AI Quiz")
                        }
                    }
                }
            }
        }

        when (quizState) {
            QuizUiState.Idle -> Unit
            QuizUiState.Loading -> {
                item {
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Crafting your $count-question quiz...")
                        }
                    }
                }
            }

            is QuizUiState.Error -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = quizState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is QuizUiState.Ready -> {
                if (quizState.submitted) {
                    val score = quizState.questions.indices.count { i ->
                        quizState.selectedAnswers[i] == quizState.questions[i].correctIndex
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Score: $score / ${quizState.questions.size}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Saved to your study history",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                OutlinedButton(onClick = onResetQuiz) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("New")
                                }
                            }
                        }
                    }
                }

                items(quizState.questions.indices.toList()) { qIdx ->
                    val q = quizState.questions[qIdx]
                    val chosen = quizState.selectedAnswers[qIdx]
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Q${qIdx + 1}. ${q.question}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(10.dp))

                            q.options.forEachIndexed { optIdx, optText ->
                                val isSelected = chosen == optIdx
                                val isCorrect = optIdx == q.correctIndex
                                val borderColor = when {
                                    quizState.submitted && isCorrect -> SuccessGreen
                                    quizState.submitted && isSelected && !isCorrect -> ErrorRed
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                }
                                val bgColor = when {
                                    quizState.submitted && isCorrect -> SuccessGreen.copy(alpha = 0.1f)
                                    quizState.submitted && isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.1f)
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.surface
                                }

                                Surface(
                                    color = bgColor,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, borderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable(enabled = !quizState.submitted) {
                                            onSelectAnswer(qIdx, optIdx)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = borderColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = optText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            if (quizState.submitted) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Explanation: ${q.explanation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (!quizState.submitted) {
                    item {
                        Button(
                            onClick = onSubmitQuiz,
                            enabled = quizState.selectedAnswers.size == quizState.questions.size,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("quiz_submit_button")
                        ) {
                            Text("Submit Quiz (${quizState.selectedAnswers.size}/${quizState.questions.size} answered)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyPlannerContent(
    selectedSubject: String,
    selectedLanguage: String,
    toolAiState: AiStreamState,
    studyPlansState: FirestoreUiState<List<StudyPlanTask>>,
    onSubjectChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onAddPlan: (subject: String, title: String, scheduledDate: String, durationMinutes: Int) -> Unit,
    onTogglePlan: (StudyPlanTask) -> Unit,
    onDeletePlan: (String) -> Unit,
    onGenerateAiPlan: (goal: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var taskTitle by remember { mutableStateOf("") }
    var taskTime by remember { mutableStateOf("Today 6:00 PM") }
    var duration by remember { mutableIntStateOf(30) }
    var aiGoal by remember { mutableStateOf("") }

    val tasks = (studyPlansState as? FirestoreUiState.Success)?.data.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Study Task", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    SubjectChipRow(selectedSubject = selectedSubject, includeGeneral = false, onSelect = onSubjectChange)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Chapter or Task") },
                        placeholder = { Text("e.g. Solve 15 Trigonometry problems") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("planner_task_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = taskTime,
                            onValueChange = { taskTime = it },
                            label = { Text("When") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(25, 45).forEach { mins ->
                                FilterChip(
                                    selected = duration == mins,
                                    onClick = { duration = mins },
                                    label = { Text("${mins}m") }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                onAddPlan(selectedSubject, taskTitle.trim(), taskTime.trim(), duration)
                                taskTitle = ""
                            }
                        },
                        enabled = taskTitle.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("planner_add_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add to Schedule")
                    }
                }
            }
        }

        item {
            Text("Your Study Schedule (${tasks.size})", style = MaterialTheme.typography.titleLarge)
        }

        if (tasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No tasks scheduled yet. Add a study block above or generate an AI study schedule below!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.completed,
                            onCheckedChange = { onTogglePlan(task) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                )
                            )
                            Text(
                                text = "${task.subject} • ${task.scheduledDate} • ${task.durationMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeletePlan(task.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete task")
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI Smart Study Plan Generator", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    LanguageSelectorRow(selectedLanguage = selectedLanguage, onSelect = onLanguageChange)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiGoal,
                        onValueChange = { aiGoal = it },
                        label = { Text("Exam Goal or Syllabus") },
                        placeholder = { Text("e.g. Prepare Science & Maths chapters in 5 days") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onGenerateAiPlan(aiGoal) },
                        enabled = aiGoal.isNotBlank() && toolAiState !is AiStreamState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Generate AI Timetable")
                    }
                }
            }
        }

        item {
            AiSolutionResultCard(state = toolAiState, title = "AI Study Timetable")
        }
    }
}
