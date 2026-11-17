package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.core.NetworkConnectivityProvider
import com.example.core.ThemeMode
import com.example.data.ai.SmartStudyAiService
import com.example.data.ai.StudyAiGateway
import com.example.data.local.LocalStudyStore
import com.example.data.model.ChatMessage
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyToolType
import com.example.ui.screens.AiChatScreen
import com.example.ui.study.StudyViewModel
import com.example.ui.theme.SmartStudyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatFlowViewModelTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeNetworkMonitor(initialOnline: Boolean = true) : NetworkConnectivityProvider {
        private val state = MutableStateFlow(initialOnline)
        override val isOnlineFlow: Flow<Boolean> = state
        override fun isCurrentlyOnline(): Boolean = state.value
    }

    private class FakeAiGateway(
        private val streamHandler: suspend (prior: List<ChatMessage>, prompt: String, lang: String) -> List<String>
    ) : StudyAiGateway {
        var callCount = 0
        var lastPriorMessages: List<ChatMessage> = emptyList()
        var lastUserPrompt: String = ""

        override fun sendChatConversationStream(
            priorMessages: List<ChatMessage>,
            userMessage: String,
            subject: String,
            language: String,
            attachments: List<Bitmap>
        ): Flow<String> = flow {
            callCount++
            lastPriorMessages = priorMessages
            lastUserPrompt = userMessage
            val chunks = streamHandler(priorMessages, userMessage, language)
            for (chunk in chunks) {
                emit(chunk)
            }
        }

        override fun solveQuestionStream(
            question: String,
            subject: String,
            language: String,
            attachments: List<Bitmap>
        ): Flow<String> = flow { emit("Solved: $question") }

        override fun solveMathStepByStepStream(
            problem: String,
            topic: String,
            language: String,
            attachments: List<Bitmap>
        ): Flow<String> = flow { emit("Math: $problem") }

        override fun generateTutorContentStream(
            subject: String,
            topic: String,
            modeLabel: String,
            language: String
        ): Flow<String> = flow { emit("Tutor: $topic") }

        override suspend fun generateQuiz(
            subject: String,
            topic: String,
            difficulty: String,
            questionCount: Int,
            language: String
        ): List<QuizQuestion> = emptyList()

        override fun runStudyToolStream(
            tool: StudyToolType,
            subject: String,
            userInput: String,
            language: String
        ): Flow<String> = flow { emit("Tool: $userInput") }
    }

    private fun createViewModel(gateway: StudyAiGateway, online: Boolean = true): StudyViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return StudyViewModel(
            repository = null,
            aiService = gateway,
            localStore = LocalStudyStore(context),
            networkMonitor = FakeNetworkMonitor(online),
            currentUserId = "test-user",
            displayName = "Test Student",
            isGuest = true
        )
    }

    private fun waitForChatIdle(vm: StudyViewModel, timeoutMs: Long = 5_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper(25, TimeUnit.MILLISECONDS)
            val done = !vm.chatSending.value && vm.chatMessages.value.lastOrNull()?.isStreaming == false
            if (done) return
            Thread.sleep(10)
        }
        throw AssertionError("Timed out waiting for chat to finish. chatSending=${vm.chatSending.value}, last=${vm.chatMessages.value.lastOrNull()}")
    }

    @Test
    fun chatUi_handlesAllFourUserPromptsAndStopsLoadingCleanly() {
        val gateway = FakeAiGateway { _, prompt, _ ->
            delay(40)
            when (prompt) {
                "Hello" -> listOf("Hello! ", "How can I help you study today?")
                "What is Artificial Intelligence?" -> listOf("Artificial Intelligence (AI) is the simulation of human intelligence by machines.")
                "भारत की राजधानी क्या है?" -> listOf("भारत की राजधानी **नई दिल्ली** है।")
                "Explain photosynthesis." -> listOf("Photosynthesis is the process by which green plants convert sunlight, water, and CO2 into glucose and oxygen.")
                else -> listOf("Response for: $prompt")
            }
        }
        val vm = createViewModel(gateway)

        val prompts = listOf(
            "Hello",
            "What is Artificial Intelligence?",
            "भारत की राजधानी क्या है?",
            "Explain photosynthesis."
        )

        for (prompt in prompts) {
            vm.sendChatMessage(prompt)
            waitForChatIdle(vm)
            val lastBot = vm.chatMessages.value.last()
            assertFalse("Bot reply must not be user", lastBot.isUser)
            assertFalse("Bot reply must not be error for '$prompt'", lastBot.isError)
            assertTrue("Bot reply must not be blank for '$prompt'", lastBot.text.isNotBlank())
        }

        assertEquals(4, gateway.callCount)
        assertEquals(8, vm.chatMessages.value.size)
        assertTrue(vm.chatMessages.value[1].text.contains("How can I help"))
        assertTrue(vm.chatMessages.value[3].text.contains("Artificial Intelligence"))
        assertTrue(vm.chatMessages.value[5].text.contains("नई दिल्ली"))
        assertTrue(vm.chatMessages.value[7].text.contains("Photosynthesis"))
    }

    @Test
    fun errorResponse_resetsLoadingAndDisplaysClearErrorMessage() {
        val gateway = FakeAiGateway { _, _, _ ->
            delay(20)
            throw IllegalStateException("App Check verification failed (403)")
        }
        val vm = createViewModel(gateway)

        vm.sendChatMessage("What is Artificial Intelligence?")
        waitForChatIdle(vm)

        assertFalse(vm.chatSending.value)
        val errorMsg = vm.chatMessages.value.last()
        assertTrue(errorMsg.isError)
        assertFalse(errorMsg.isStreaming)
        assertTrue(errorMsg.text.contains("403"))
    }

    @Test
    fun timeoutException_resetsLoadingInFinallyAndShowsTimeoutError() {
        val gateway = FakeAiGateway { _, _, _ ->
            withTimeout(10L) {
                delay(500L)
            }
            emptyList()
        }
        val vm = createViewModel(gateway)

        vm.sendChatMessage("Explain photosynthesis.")
        waitForChatIdle(vm)

        assertFalse(vm.chatSending.value)
        val msg = vm.chatMessages.value.last()
        assertTrue(msg.isError)
        assertFalse(msg.isStreaming)
        assertTrue(msg.text.contains("timed out", ignoreCase = true))
    }

    @Test
    fun doubleClickSend_onlySendsOneRequest() {
        val gateway = FakeAiGateway { _, _, _ ->
            delay(150)
            listOf("Single response")
        }
        val vm = createViewModel(gateway)

        vm.sendChatMessage("Hello")
        vm.sendChatMessage("Hello")

        waitForChatIdle(vm)

        assertEquals(1, gateway.callCount)
        assertEquals(2, vm.chatMessages.value.size)
    }

    @Test
    fun normalizeConversationHistory_alternatesUserAndModelEvenAfterErrors() {
        val history = listOf(
            ChatMessage(id = "0", isUser = false, text = "Dangling initial model msg"),
            ChatMessage(id = "1", isUser = true, text = "Hello"),
            ChatMessage(id = "2", isUser = false, text = "Hi there!"),
            ChatMessage(id = "3", isUser = true, text = "Broken prompt"),
            ChatMessage(id = "4", isUser = false, text = "Network error", isError = true),
            ChatMessage(id = "5", isUser = true, text = "Unanswered user turn")
        )
        val normalized = SmartStudyAiService.normalizeConversationHistory(history)
        assertEquals(2, normalized.size)
        assertTrue(normalized[0].isUser)
        assertEquals("Hello", normalized[0].text)
        assertFalse(normalized[1].isUser)
        assertEquals("Hi there!", normalized[1].text)
    }

    @Test
    fun languageInstruction_autoDetectsHindiAndEnglish() {
        val hindiInstr = SmartStudyAiService.buildLanguageInstruction("English", "भारत की राजधानी क्या है?")
        val englishInstr = SmartStudyAiService.buildLanguageInstruction("English", "What is Artificial Intelligence?")
        assertTrue(hindiInstr.contains("Hindi"))
        assertTrue(englishInstr.contains("English"))
    }

    @Test
    fun composeChatScreen_sendButtonDisplaysAiReplyAndClearsThinking() {
        val gateway = FakeAiGateway { _, prompt, _ ->
            delay(30)
            listOf("AI answer for: $prompt")
        }
        val vm = createViewModel(gateway)

        composeRule.setContent {
            val collectedMsgs = vm.chatMessages.collectAsState().value
            val collectedSending = vm.chatSending.collectAsState().value
            SmartStudyTheme(themeMode = ThemeMode.LIGHT) {
                AiChatScreen(
                    messages = collectedMsgs,
                    selectedSubject = "General",
                    selectedLanguage = "English",
                    attachment = null,
                    attachmentLoading = false,
                    isSending = collectedSending,
                    isOnline = true,
                    themeMode = ThemeMode.LIGHT,
                    onOpenDrawer = {},
                    onNewChat = {},
                    onCycleTheme = {},
                    onSubjectChange = {},
                    onLanguageChange = {},
                    onLoadImage = {},
                    onLoadPdf = {},
                    onClearAttachment = {},
                    onSendMessage = vm::sendChatMessage,
                    onStopGeneration = vm::stopChatGeneration,
                    onRetryLast = vm::retryLastChatMessage,
                    onOpenMathSolver = {},
                    onOpenStudyHub = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat_input_field").performTextInput("Hello")
        composeRule.onNodeWithTag("chat_send_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            ShadowLooper.idleMainLooper(25, TimeUnit.MILLISECONDS)
            !vm.chatSending.value && vm.chatMessages.value.lastOrNull()?.isStreaming == false
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("AI answer for: Hello").assertIsDisplayed()
    }
}
