package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import com.example.data.model.SubjectInfo
import com.example.data.offline.OfflineStudyBank
import com.example.ui.components.AiSolutionResultCard
import com.example.ui.components.FormattedStudyMarkdown
import com.example.ui.components.LanguageSelectorRow
import com.example.ui.study.AiStreamState

private val tutorModes = listOf(
    "Simple Explanation",
    "Short Answer",
    "Long Answer",
    "Chapter Notes",
    "Textbook Q&A",
    "Exam Questions"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectTutorScreen(
    subject: SubjectInfo,
    selectedLanguage: String,
    tutorState: AiStreamState,
    onLanguageChange: (String) -> Unit,
    onGenerateTutorContent: (topic: String, mode: String, language: String) -> Unit,
    onLaunchSubjectQuiz: (topic: String) -> Unit,
    onBack: () -> Unit
) {
    var topicInput by remember(subject.id) { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(tutorModes.first()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${subject.name} AI Tutor") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tutor_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(subject.accentColor, subject.secondaryColor)))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = subject.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = subject.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tutor Output Style", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tutorModes.forEach { mode ->
                                FilterChip(
                                    selected = mode == selectedMode,
                                    onClick = { selectedMode = mode },
                                    label = { Text(mode) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Language (Hindi / English)", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        LanguageSelectorRow(
                            selectedLanguage = selectedLanguage,
                            onSelect = onLanguageChange
                        )

                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("Chapter, topic, or textbook question") },
                            placeholder = { Text(subject.samplePrompts.first()) },
                            minLines = 2,
                            maxLines = 5,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tutor_topic_input")
                        )

                        Spacer(Modifier.height(10.dp))
                        Text("Popular Prompts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            subject.samplePrompts.forEach { prompt ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { topicInput = prompt }
                                ) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onGenerateTutorContent(topicInput, selectedMode, selectedLanguage)
                                },
                                enabled = tutorState !is AiStreamState.Loading && tutorState !is AiStreamState.Streaming,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("tutor_generate_button")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Teach Me")
                            }

                            OutlinedButton(
                                onClick = {
                                    onLaunchSubjectQuiz(topicInput.ifBlank { subject.name })
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("tutor_quiz_button")
                            ) {
                                Icon(Icons.Rounded.Quiz, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Quiz")
                            }
                        }
                    }
                }
            }

            item {
                AiSolutionResultCard(
                    state = tutorState,
                    title = "${subject.name} • $selectedMode"
                )
            }

            item {
                val offlinePack = remember(subject.name) { OfflineStudyBank.getPack(subject.name) }
                var expanded by remember(subject.id) { mutableStateOf(false) }
                val notes = if (selectedLanguage == "Hindi") {
                    offlinePack.chapterNotesHindi
                } else {
                    offlinePack.chapterNotesEnglish
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Offline Study Pack & Formulas",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Always available without internet (${selectedLanguage})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (expanded) "Collapse offline notes" else "Expand offline notes"
                            )
                        }

                        if (expanded) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Quick Formulas & Core Rules",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            offlinePack.quickFormulasAndRules.forEach { formula ->
                                Text(
                                    text = "• $formula",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            FormattedStudyMarkdown(text = notes)
                        }
                    }
                }
            }
        }
    }
}
