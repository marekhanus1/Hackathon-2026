package cz.narsil.verify

import cz.narsil.core.DailyContract
import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.core.VerificationResult
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.core.fake.FakeTaskVerifier
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskVerificationServiceTest {
    private val day = LocalDate.of(2026, 11, 28)

    private suspend fun setup(): Pair<FakeContractRepository, PlanTask> {
        val repo = FakeContractRepository(clock = { day }, withDemoContract = false)
        repo.save(DailyContract(day, 6_000, tasks = listOf(PlanTask(type = TaskType.SCHOOL, title = "Lineární algebra"))))
        return repo to repo.today.value!!.tasks.single()
    }

    @Test
    fun photoVerifiesSchoolTaskAndRewards() = runTest {
        val (repo, task) = setup()
        val rewarded = mutableListOf<PlanTask>()
        val service = TaskVerificationService(repo, FakeTaskVerifier(nextResult = VerificationResult.YES), onVerified = { rewarded += it })
        val updated = service.submit(task, Proof.Photo("content://zapisky.jpg"))
        assertEquals(TaskStatus.VERIFIED, updated.status)
        assertEquals(1, updated.attempts)
        assertEquals("content://zapisky.jpg", updated.proofUri)
        assertEquals(TaskStatus.VERIFIED, repo.today.value!!.tasks.single().status)
        assertEquals(listOf(updated.id), rewarded.map { it.id })
        assertEquals("Ověřeno", service.events.value[task.id]!!.reason)
    }

    @Test
    fun rejectedAtMostThreeAttempts() = runTest {
        val (repo, task) = setup()
        val service = TaskVerificationService(repo, FakeTaskVerifier(nextResult = VerificationResult.NO))
        var t = task
        repeat(5) { t = service.submit(t, Proof.Text("krátké")) }
        assertEquals(TaskStatus.REJECTED, t.status)
        assertEquals(3, t.attempts)
        assertFalse(service.canSubmit(t))
    }

    @Test
    fun laterIsRetriedOnNetworkAtMostThreeTimes() = runTest {
        val (repo, task) = setup()
        val verifier = FakeTaskVerifier(nextResult = VerificationResult.LATER)
        val service = TaskVerificationService(repo, verifier)
        val later = service.submit(task, Proof.Text("Spočítal jsem příklady 1–10"))
        assertEquals(TaskStatus.VERIFY_LATER, later.status)
        assertEquals(0, later.attempts) // LATER se do pokusů nepočítá
        assertEquals("text:Spočítal jsem příklady 1–10", later.proofUri)

        repeat(3) { assertEquals(1, service.retryPending().size) }
        assertTrue(service.retryPending().isEmpty()) // automatické pokusy došly

        verifier.nextResult = VerificationResult.YES
        val manual = service.retryNow(repo.today.value!!.tasks.single())
        assertEquals(TaskStatus.VERIFIED, manual.status)
    }

    @Test
    fun verifiedTaskIsNotReverified() = runTest {
        val (repo, task) = setup()
        var rewards = 0
        val service = TaskVerificationService(repo, FakeTaskVerifier(nextResult = VerificationResult.YES), onVerified = { rewards++ })
        val v = service.submit(task, Proof.Photo("content://a"))
        service.submit(v, Proof.Photo("content://b"))
        assertEquals(1, rewards)
    }

    @Test
    fun pendingFromAllDays() = runTest {
        val (repo, task) = setup()
        val yesterday = task.copy(id = 99, status = TaskStatus.VERIFY_LATER, proofUri = "content://vcera")
        val verifier = FakeTaskVerifier(nextResult = VerificationResult.YES)
        val service = TaskVerificationService(repo, verifier, pendingTasks = { listOf(yesterday) })
        val done = service.retryPending()
        assertEquals(listOf(99L), done.map { it.id })
        assertEquals(TaskStatus.VERIFIED, done.single().status)
    }

    @Test
    fun proofCodecRoundTrip() {
        assertEquals(Proof.Strava, ProofCodec.decode(ProofCodec.encode(Proof.Strava)))
        assertEquals(Proof.Text("ahoj"), ProofCodec.decode(ProofCodec.encode(Proof.Text("ahoj"))))
        assertEquals(Proof.Photo("content://x"), ProofCodec.decode(ProofCodec.encode(Proof.Photo("content://x"))))
        assertEquals(null, ProofCodec.decode(null))
    }
}
