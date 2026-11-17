package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.SavedStudyNote
import com.example.data.model.SolvedQuestion
import com.example.data.model.StudyPlanTask
import com.example.data.model.UserStudyProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun todayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun observeProfile(userId: String): Flow<UserStudyProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserStudyProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfile(userId: String, displayName: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (uid != userId) {
            return Result.failure(IllegalStateException("Authenticated user mismatch."))
        }
        val docRef = db.collection("users").document(uid)
        val today = todayDateString()
        val cleanName = displayName.trim().ifBlank { "Student" }.take(80)
        return try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "dailyGoalMinutes" to 45L,
                    "todayMinutesStudied" to 0L,
                    "todayQuestionsSolved" to 0L,
                    "streakDays" to 1L,
                    "preferredLanguage" to "English",
                    "lastStudyDate" to today,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val lastDate = existing.getString("lastStudyDate").orEmpty()
                if (lastDate != today) {
                    val currentStreak = existing.getLong("streakDays") ?: 1L
                    docRef.update(
                        mapOf(
                            "todayMinutesStudied" to 0L,
                            "todayQuestionsSolved" to 0L,
                            "streakDays" to (currentStreak + 1L).coerceAtMost(3650L),
                            "lastStudyDate" to today,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordStudyActivity(
        userId: String,
        minutesAdded: Int,
        questionsAdded: Int
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        val today = todayDateString()
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to (auth.currentUser?.displayName?.take(80) ?: "Student"),
                    "dailyGoalMinutes" to 45L,
                    "todayMinutesStudied" to minutesAdded.toLong().coerceIn(0L, 1440L),
                    "todayQuestionsSolved" to questionsAdded.toLong().coerceIn(0L, 1000L),
                    "streakDays" to 1L,
                    "preferredLanguage" to "English",
                    "lastStudyDate" to today,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val lastDate = snap.getString("lastStudyDate").orEmpty()
                val prevMinutes = if (lastDate == today) (snap.getLong("todayMinutesStudied") ?: 0L) else 0L
                val prevSolved = if (lastDate == today) (snap.getLong("todayQuestionsSolved") ?: 0L) else 0L
                val prevStreak = snap.getLong("streakDays") ?: 1L
                val nextStreak = if (lastDate == today) prevStreak else (prevStreak + 1L).coerceAtMost(3650L)

                docRef.update(
                    mapOf(
                        "todayMinutesStudied" to (prevMinutes + minutesAdded).coerceIn(0L, 1440L),
                        "todayQuestionsSolved" to (prevSolved + questionsAdded).coerceIn(0L, 1000L),
                        "streakDays" to nextStreak,
                        "lastStudyDate" to today,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        userId: String,
        dailyGoalMinutes: Int,
        preferredLanguage: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        val validLang = if (preferredLanguage in listOf("English", "Hindi", "Bilingual")) {
            preferredLanguage
        } else {
            "English"
        }
        return try {
            docRef.update(
                mapOf(
                    "dailyGoalMinutes" to dailyGoalMinutes.toLong().coerceIn(5L, 600L),
                    "preferredLanguage" to validLang,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeRecentQuestions(userId: String): Flow<List<SolvedQuestion>> = flow {
        val colRef = db.collection("users").document(userId).collection("questions")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SolvedQuestion::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveSolvedQuestion(
        subject: String,
        topic: String,
        questionText: String,
        answerSummary: String,
        mode: String,
        language: String,
        hasAttachment: Boolean
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("questions").document()
        val validMode = if (mode in listOf("solver", "math", "tutor", "tool")) mode else "solver"
        val validLang = if (language in listOf("English", "Hindi", "Bilingual")) language else "English"
        val payload = mapOf(
            "userId" to uid,
            "subject" to subject.trim().ifBlank { "General" }.take(60),
            "topic" to topic.trim().take(100),
            "questionText" to questionText.trim().ifBlank { "Image/PDF Question" }.take(4000),
            "answerSummary" to answerSummary.trim().ifBlank { "Solved" }.take(16000),
            "mode" to validMode,
            "language" to validLang,
            "hasAttachment" to hasAttachment,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteQuestion(questionId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("questions").document(questionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeStudyPlans(userId: String): Flow<List<StudyPlanTask>> = flow {
        val colRef = db.collection("users").document(userId).collection("studyPlans")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyPlanTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun addStudyPlan(
        subject: String,
        title: String,
        scheduledDate: String,
        durationMinutes: Int
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("studyPlans").document()
        val payload = mapOf(
            "userId" to uid,
            "subject" to subject.trim().ifBlank { "General" }.take(60),
            "title" to title.trim().ifBlank { "Study Session" }.take(160),
            "scheduledDate" to scheduledDate.trim().ifBlank { "Today" }.take(40),
            "durationMinutes" to durationMinutes.toLong().coerceIn(5L, 360L),
            "completed" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleStudyPlanCompleted(task: StudyPlanTask): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("studyPlans").document(task.id)
        return try {
            docRef.update(
                mapOf(
                    "completed" to !task.completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudyPlan(planId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("studyPlans").document(planId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeSavedNotes(userId: String): Flow<List<SavedStudyNote>> = flow {
        val colRef = db.collection("users").document(userId).collection("savedNotes")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavedStudyNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveStudyNote(
        subject: String,
        toolType: String,
        title: String,
        content: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("savedNotes").document()
        val validType = if (toolType in listOf("notes", "quiz", "exam_prep", "grammar", "dictionary", "speaking")) {
            toolType
        } else {
            "notes"
        }
        val payload = mapOf(
            "userId" to uid,
            "subject" to subject.trim().ifBlank { "General" }.take(60),
            "toolType" to validType,
            "title" to title.trim().ifBlank { "Study Note" }.take(160),
            "content" to content.trim().ifBlank { "Saved content" }.take(24000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedNote(noteId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("savedNotes").document(noteId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
