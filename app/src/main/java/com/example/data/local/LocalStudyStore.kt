package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.SavedStudyNote
import com.example.data.model.SolvedQuestion
import com.example.data.model.StudyPlanTask
import com.example.data.model.UserStudyProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LocalStudySnapshot(
    val profile: UserStudyProfile = UserStudyProfile(
        id = "guest",
        userId = "guest",
        displayName = "Guest Student",
        dailyGoalMinutes = 45L,
        todayMinutesStudied = 0L,
        todayQuestionsSolved = 0L,
        streakDays = 1L,
        preferredLanguage = "English"
    ),
    val questions: List<SolvedQuestion> = emptyList(),
    val plans: List<StudyPlanTask> = emptyList(),
    val notes: List<SavedStudyNote> = emptyList(),
    val conversations: List<ChatConversation> = emptyList()
)

class LocalStudyStore(context: Context) {
    private val storeFile = File(context.applicationContext.filesDir, "smart_study_offline_store.json")
    private val mutex = Mutex()

    suspend fun load(): LocalStudySnapshot = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!storeFile.exists()) return@withLock LocalStudySnapshot()
            runCatching {
                val root = JSONObject(storeFile.readText())
                val pJson = root.optJSONObject("profile")
                val profile = if (pJson != null) {
                    UserStudyProfile(
                        id = pJson.optString("id", "guest"),
                        userId = pJson.optString("userId", "guest"),
                        displayName = pJson.optString("displayName", "Guest Student"),
                        dailyGoalMinutes = pJson.optLong("dailyGoalMinutes", 45L),
                        todayMinutesStudied = pJson.optLong("todayMinutesStudied", 0L),
                        todayQuestionsSolved = pJson.optLong("todayQuestionsSolved", 0L),
                        streakDays = pJson.optLong("streakDays", 1L),
                        preferredLanguage = pJson.optString("preferredLanguage", "English")
                    )
                } else {
                    LocalStudySnapshot().profile
                }

                val qArr = root.optJSONArray("questions") ?: JSONArray()
                val questions = (0 until qArr.length()).map { i ->
                    val o = qArr.getJSONObject(i)
                    SolvedQuestion(
                        id = o.optString("id"),
                        userId = o.optString("userId", "guest"),
                        subject = o.optString("subject", "General"),
                        topic = o.optString("topic", ""),
                        questionText = o.optString("questionText", ""),
                        answerSummary = o.optString("answerSummary", ""),
                        mode = o.optString("mode", "solver"),
                        language = o.optString("language", "English"),
                        hasAttachment = o.optBoolean("hasAttachment", false)
                    )
                }

                val plArr = root.optJSONArray("plans") ?: JSONArray()
                val plans = (0 until plArr.length()).map { i ->
                    val o = plArr.getJSONObject(i)
                    StudyPlanTask(
                        id = o.optString("id"),
                        userId = o.optString("userId", "guest"),
                        subject = o.optString("subject", "Mathematics"),
                        title = o.optString("title", ""),
                        scheduledDate = o.optString("scheduledDate", "Today"),
                        durationMinutes = o.optLong("durationMinutes", 30L),
                        completed = o.optBoolean("completed", false)
                    )
                }

                val nArr = root.optJSONArray("notes") ?: JSONArray()
                val notes = (0 until nArr.length()).map { i ->
                    val o = nArr.getJSONObject(i)
                    SavedStudyNote(
                        id = o.optString("id"),
                        userId = o.optString("userId", "guest"),
                        subject = o.optString("subject", "General"),
                        toolType = o.optString("toolType", "notes"),
                        title = o.optString("title", ""),
                        content = o.optString("content", "")
                    )
                }

                val cArr = root.optJSONArray("conversations") ?: JSONArray()
                val conversations = (0 until cArr.length()).map { i ->
                    val co = cArr.getJSONObject(i)
                    val mArr = co.optJSONArray("messages") ?: JSONArray()
                    val msgs = (0 until mArr.length()).map { j ->
                        val mo = mArr.getJSONObject(j)
                        ChatMessage(
                            id = mo.optString("id"),
                            isUser = mo.optBoolean("isUser", false),
                            text = mo.optString("text", ""),
                            subject = mo.optString("subject", "General"),
                            isStreaming = false,
                            isError = mo.optBoolean("isError", false),
                            isOfflineAnswer = mo.optBoolean("isOfflineAnswer", false),
                            attachmentLabel = mo.optString("attachmentLabel", "").ifBlank { null }
                        )
                    }
                    ChatConversation(
                        id = co.optString("id"),
                        title = co.optString("title", "New Chat"),
                        subject = co.optString("subject", "General"),
                        messages = msgs,
                        updatedAtMillis = co.optLong("updatedAtMillis", System.currentTimeMillis())
                    )
                }

                LocalStudySnapshot(profile, questions, plans, notes, conversations)
            }.onFailure {
                Log.e(TAG, "Failed to read local study store", it)
            }.getOrDefault(LocalStudySnapshot())
        }
    }

    suspend fun save(snapshot: LocalStudySnapshot) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val root = JSONObject().apply {
                    put("profile", JSONObject().apply {
                        put("id", snapshot.profile.id)
                        put("userId", snapshot.profile.userId)
                        put("displayName", snapshot.profile.displayName)
                        put("dailyGoalMinutes", snapshot.profile.dailyGoalMinutes)
                        put("todayMinutesStudied", snapshot.profile.todayMinutesStudied)
                        put("todayQuestionsSolved", snapshot.profile.todayQuestionsSolved)
                        put("streakDays", snapshot.profile.streakDays)
                        put("preferredLanguage", snapshot.profile.preferredLanguage)
                    })
                    put("questions", JSONArray().apply {
                        snapshot.questions.take(100).forEach { q ->
                            put(JSONObject().apply {
                                put("id", q.id)
                                put("userId", q.userId)
                                put("subject", q.subject)
                                put("topic", q.topic)
                                put("questionText", q.questionText)
                                put("answerSummary", q.answerSummary)
                                put("mode", q.mode)
                                put("language", q.language)
                                put("hasAttachment", q.hasAttachment)
                            })
                        }
                    })
                    put("plans", JSONArray().apply {
                        snapshot.plans.take(100).forEach { p ->
                            put(JSONObject().apply {
                                put("id", p.id)
                                put("userId", p.userId)
                                put("subject", p.subject)
                                put("title", p.title)
                                put("scheduledDate", p.scheduledDate)
                                put("durationMinutes", p.durationMinutes)
                                put("completed", p.completed)
                            })
                        }
                    })
                    put("notes", JSONArray().apply {
                        snapshot.notes.take(100).forEach { n ->
                            put(JSONObject().apply {
                                put("id", n.id)
                                put("userId", n.userId)
                                put("subject", n.subject)
                                put("toolType", n.toolType)
                                put("title", n.title)
                                put("content", n.content)
                            })
                        }
                    })
                    put("conversations", JSONArray().apply {
                        snapshot.conversations.take(40).forEach { conv ->
                            put(JSONObject().apply {
                                put("id", conv.id)
                                put("title", conv.title)
                                put("subject", conv.subject)
                                put("updatedAtMillis", conv.updatedAtMillis)
                                put("messages", JSONArray().apply {
                                    conv.messages.takeLast(80).forEach { m ->
                                        put(JSONObject().apply {
                                            put("id", m.id)
                                            put("isUser", m.isUser)
                                            put("text", m.text)
                                            put("subject", m.subject)
                                            put("isError", m.isError)
                                            put("isOfflineAnswer", m.isOfflineAnswer)
                                            put("attachmentLabel", m.attachmentLabel.orEmpty())
                                        })
                                    }
                                })
                            })
                        }
                    })
                }
                storeFile.writeText(root.toString())
            }.onFailure {
                Log.e(TAG, "Failed to save local study store", it)
            }
        }
    }

    private companion object {
        const val TAG = "LocalStudyStore"
    }
}
