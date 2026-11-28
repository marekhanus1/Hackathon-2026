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

    @Test
    fun verifiedTaskStaysShownUntilContinue() {
        val done = rejected.copy(id = 1, status = TaskStatus.VERIFIED)
        val next = rejected.copy(id = 2, status = TaskStatus.PENDING, attempts = 0)
        val tasks = listOf(done, next)
        assertEquals(1L, pickVerifyTask(tasks, shownTaskId = 1)?.id) // výsledek zůstane vidět
        assertEquals(2L, pickVerifyTask(tasks, shownTaskId = null)?.id) // první vstup: první neověřený
        assertEquals(2L, pickVerifyTask(tasks, shownTaskId = 99)?.id) // smazaný úkol
        assertEquals(null, pickVerifyTask(emptyList(), null))
    }
}
