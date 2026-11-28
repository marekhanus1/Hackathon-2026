package cz.narsil.app.nav

import cz.narsil.core.PlanTask
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.ui.screens.verify.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** 144: „Zkusit znovu“ po zamítnutí vrátí formulář důkazu. */
class VerifyStatusTest {
    private val rejected = PlanTask(id = 1, type = TaskType.SCHOOL, title = "Úkol", status = TaskStatus.REJECTED, attempts = 1)

    @Test
    fun retryShowsProofFormAgain() {
        assertEquals(VerificationStatus.REJECTED, verifyStatus(rejected, running = false, retrying = false))
        assertEquals(VerificationStatus.PENDING, verifyStatus(rejected, running = false, retrying = true))
        assertEquals(VerificationStatus.VERIFYING, verifyStatus(rejected, running = true, retrying = true))
    }

    @Test
    fun noRetryWithoutAttemptsLeft() {
        val spent = rejected.copy(attempts = 3)
        assertEquals(VerificationStatus.REJECTED, verifyStatus(spent, running = false, retrying = true))
    }
}
