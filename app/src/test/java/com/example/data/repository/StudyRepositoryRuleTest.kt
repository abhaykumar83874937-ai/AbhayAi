package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StudyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfile_and_saveSolvedQuestion_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = StudyRepository(firestore, auth)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureProfile(uid, "Alice Student")
        }
        assertTrue("Profile ensure should succeed: ${profileResult.exceptionOrNull()}", profileResult.isSuccess)

        val uniqueTopic = "Quadratic_${UUID.randomUUID().toString().take(8)}"
        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveSolvedQuestion(
                subject = "Mathematics",
                topic = uniqueTopic,
                questionText = "Solve x^2 - 9 = 0",
                answerSummary = "Step 1: Factor (x-3)(x+3)=0. Roots: 3, -3.",
                mode = "math",
                language = "English",
                hasAttachment = false
            )
        }
        assertTrue("Save question should succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
        val questionId = saveResult.getOrThrow()

        val questions = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeRecentQuestions(uid).first { list -> list.any { it.id == questionId } }
        }
        assertTrue(questions.any { it.id == questionId && it.topic == uniqueTopic })
    }

    @Test
    fun studyPlans_and_savedNotes_crudSucceedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = StudyRepository(firestore, auth)

        val planTitle = "Revise Optics_${UUID.randomUUID().toString().take(6)}"
        val planId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addStudyPlan("Physics", planTitle, "Tomorrow 6 PM", 45).getOrThrow()
        }
        val plans = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStudyPlans(uid).first { list -> list.any { it.id == planId } }
        }
        val createdPlan = plans.first { it.id == planId }
        assertEquals(false, createdPlan.completed)

        val toggleRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.toggleStudyPlanCompleted(createdPlan)
        }
        assertTrue(toggleRes.isSuccess)

        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveStudyNote("Biology", "notes", "Cell Notes", "Mitochondria produces ATP.").getOrThrow()
        }
        val notes = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeSavedNotes(uid).first { list -> list.any { it.id == noteId } }
        }
        assertTrue(notes.any { it.id == noteId })
    }

    @Test
    fun crossUserRead_isRejectedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudyRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureProfile(aliceUid, "Alice").getOrThrow()
            aliceRepo.saveSolvedQuestion(
                subject = "Chemistry",
                topic = "Mole Concept",
                questionText = "Define 1 mole",
                answerSummary = "6.022 x 10^23 entities",
                mode = "solver",
                language = "English",
                hasAttachment = false
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = StudyRepository(firestore, auth)
        var capturedError: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeRecentQuestions(aliceUid).first()
            }
        } catch (e: Throwable) {
            capturedError = e
        }
        assertNotNull("Expected PERMISSION_DENIED on cross-user read", capturedError)
        val firestoreException = capturedError!!.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $capturedError", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    @Test
    fun unauthenticatedObserve_isRejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = StudyRepository(firestore, auth)
        var capturedError: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeRecentQuestions("some_user_123").first()
            }
        } catch (e: Throwable) {
            capturedError = e
        }
        assertNotNull("Expected PERMISSION_DENIED when unauthenticated", capturedError)
        val firestoreException = capturedError!!.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $capturedError", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_study@test.com"
        const val BOB_EMAIL = "bob_study@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
