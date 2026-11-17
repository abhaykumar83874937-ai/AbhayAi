package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.ThemeMode
import com.example.data.model.SolvedQuestion
import com.example.data.model.StudyCatalog
import com.example.data.model.StudyToolType
import com.example.data.model.SubjectInfo
import com.example.data.model.UserStudyProfile
import com.example.ui.components.FormattedStudyMarkdown
import com.example.ui.study.FirestoreUiState
import com.example.ui.theme.WarningAmber

@Composable
fun HomeDashboardScreen(
    displayName: String,
    isGuest: Boolean,
    profileState: FirestoreUiState<UserStudyProfile?>,
    recentQuestionsState: FirestoreUiState<List<SolvedQuestion>>,
    themeMode: ThemeMode,
    isWideScreen: Boolean,
    onCycleTheme: () -> Unit,
    onSignOutOrSignIn: () -> Unit,
    onAskAi: (String) -> Unit,
    onOpenSolver: () -> Unit,
    onOpenMathSolver: () -> Unit,
    onOpenHomeworkHelper: () -> Unit,
    onOpenSubject: (SubjectInfo) -> Unit,
    onDeleteQuestion: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var subjectFilter by remember { mutableStateOf("") }
    var selectedHistoryItem by remember { mutableStateOf<SolvedQuestion?>(null) }

    val profile = (profileState as? FirestoreUiState.Success)?.data
    val recentQuestions = (recentQuestionsState as? FirestoreUiState.Success)?.data.orEmpty()

    val filteredSubjects = remember(subjectFilter) {
        if (subjectFilter.isBlank()) StudyCatalog.allSubjects
        else StudyCatalog.allSubjects.filter {
            it.name.contains(subjectFilter, ignoreCase = true) ||
                it.subtitle.contains(subjectFilter, ignoreCase = true)
        }
    }

    val firstName = displayName.substringBefore(" ").ifBlank { "Student" }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Welcome Header & Theme / Sign-out actions
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_study_hero),
                    contentDescription = "Smart Study dashboard banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xD90F172A), Color(0x731E3A8A))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (isGuest) "Abhay GPT • Guest Mode" else "Abhay GPT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = onCycleTheme,
                                    modifier = Modifier.testTag("theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (themeMode == ThemeMode.DARK) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                        contentDescription = "Toggle theme (${themeMode.label})",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = onSignOutOrSignIn,
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = if (isGuest) Icons.AutoMirrored.Rounded.Login else Icons.AutoMirrored.Rounded.Logout,
                                        contentDescription = if (isGuest) "Sign in with Google" else "Sign out",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "Welcome back, $firstName!",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Ready to master today's lessons and solve tough doubts?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }
        }

        // Ask AI Anything Search Box
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Ask AI anything... e.g. Explain Ohm's Law or solve 2x+5=15") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        val q = searchQuery
                                        searchQuery = ""
                                        onAskAi(q)
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = "Ask AI",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = onOpenSolver) {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = "Scan question",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    val q = searchQuery
                                    searchQuery = ""
                                    onAskAi(q)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ask_ai_search_input")
                    )
                }
            }
        }

        // Daily Study Progress Card
        item {
            DailyProgressCard(
                minutesStudied = profile?.todayMinutesStudied?.toInt() ?: 0,
                goalMinutes = profile?.dailyGoalMinutes?.toInt() ?: 45,
                questionsSolved = profile?.todayQuestionsSolved?.toInt() ?: recentQuestions.size,
                streakDays = profile?.streakDays?.toInt() ?: 1
            )
        }

        // Quick Action Highlights: Maths Solver & AI Homework Helper & Photo Solver
        item {
            Text(
                text = "Quick Study Launchers",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickHighlightCard(
                    title = "Maths Solver",
                    subtitle = "Algebra, Calculus & 9 topics",
                    icon = Icons.Rounded.Calculate,
                    gradientColors = listOf(Color(0xFF2563EB), Color(0xFF4F46E5)),
                    onClick = onOpenMathSolver,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_math_solver_card")
                )
                QuickHighlightCard(
                    title = "Homework Helper",
                    subtitle = "Guided step-by-step help",
                    icon = Icons.Rounded.School,
                    gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF6366F1)),
                    onClick = onOpenHomeworkHelper,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_homework_helper_card")
                )
            }
        }

        // All Subjects Section + Filter Search Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Subjects (${filteredSubjects.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = subjectFilter,
                onValueChange = { subjectFilter = it },
                placeholder = { Text("Filter subjects (Maths, Physics, Hindi...)") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (subjectFilter.isNotEmpty()) {
                        IconButton(onClick = { subjectFilter = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear filter")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_filter_input")
            )
        }

        // Subject Cards Grid (2 columns on phone, 3 columns on tablet/desktop)
        val columns = if (isWideScreen) 3 else 2
        val chunkedSubjects = filteredSubjects.chunked(columns)
        items(chunkedSubjects) { rowSubjects ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowSubjects.forEach { subject ->
                    SubjectGridCard(
                        subject = subject,
                        onClick = { onOpenSubject(subject) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subject_card_${subject.id}")
                    )
                }
                repeat(columns - rowSubjects.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        // Recent Questions Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Recent Questions",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        if (recentQuestions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_tutor_empty),
                            contentDescription = "No recent questions yet",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No solved questions yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Ask a doubt, scan a textbook page, or try Maths Solver—your history syncs automatically!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(recentQuestions.take(6), key = { it.id }) { item ->
                RecentQuestionCard(
                    item = item,
                    onClick = { selectedHistoryItem = item },
                    onDelete = { onDeleteQuestion(item.id) }
                )
            }
        }
    }

    selectedHistoryItem?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedHistoryItem = null },
            title = {
                Column {
                    Text(text = item.subject, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(text = item.questionText, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            },
            text = {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    item {
                        FormattedStudyMarkdown(text = item.answerSummary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedHistoryItem = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DailyProgressCard(
    minutesStudied: Int,
    goalMinutes: Int,
    questionsSolved: Int,
    streakDays: Int
) {
    val progress = (minutesStudied.toFloat() / goalMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Study Progress",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$minutesStudied of $goalMinutes mins goal • $questionsSolved solved today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = WarningAmber.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Whatshot,
                            contentDescription = "Streak",
                            tint = WarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${streakDays}d streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarningAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun QuickHighlightCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.86f)
            )
        }
    }
}

@Composable
private fun SubjectGridCard(
    subject: SubjectInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(subject.accentColor, subject.secondaryColor))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = subject.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subject.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentQuestionCard(
    item: SolvedQuestion,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.subject,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.language,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.questionText,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.answerSummary.replace("**", "").replace("#", ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete question",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
