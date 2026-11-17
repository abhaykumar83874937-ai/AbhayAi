package com.example

import com.example.data.offline.OfflineMathEngine
import com.example.data.offline.OfflineStudyBank
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineEngineTest {

    @Test
    fun solvesArithmeticStepByStep() {
        val result = OfflineMathEngine.trySolveStepByStep("Calculate 4,875 + 2,396 - 1,548 step by step")
        assertNotNull(result)
        assertTrue(result!!.contains("5723"))
    }

    @Test
    fun solvesLinearEquationWithVerification() {
        val result = OfflineMathEngine.trySolveStepByStep("2x + 5 = 15", "Algebra")
        assertNotNull(result)
        assertTrue(result!!.contains("x = 5"))
    }

    @Test
    fun solvesQuadraticEquationWithDiscriminant() {
        val result = OfflineMathEngine.trySolveStepByStep("2x^2 - 7x + 3 = 0", "Quadratic Equations")
        assertNotNull(result)
        assertTrue(result!!.contains("3") && result.contains("0.5"))
    }

    @Test
    fun solvesStatisticsMeanMedianMode() {
        val result = OfflineMathEngine.trySolveStepByStep("12, 15, 12, 18, 23", "Statistics")
        assertNotNull(result)
        assertTrue(result!!.contains("16")) // mean = 80 / 5 = 16
        assertTrue(result.contains("15")) // median = 15
    }

    @Test
    fun offlineStudyBankCoversAllElevenSubjects() {
        assertTrue(OfflineStudyBank.subjectPacks.size == 11)
        OfflineStudyBank.subjectPacks.values.forEach { pack ->
            assertTrue(pack.quickFormulasAndRules.isNotEmpty())
            assertTrue(pack.quizQuestions.size >= 3)
        }
    }
}
