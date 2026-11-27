package cz.narsil.core.fake

import cz.narsil.core.DailyContract
import cz.narsil.core.GamificationState
import cz.narsil.core.LlmProvider
import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.core.VerificationMethod
import cz.narsil.core.VerificationResult
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanFakesTest {
    private val day = LocalDate.of(2026, 11, 28)

    @Test
    fun taskTypeDefaults() {
        val t = PlanTask(type = TaskType.MOVEMENT, title = "Běh")
        assertEquals(30, t.rewardMinutes)
        assertEquals(VerificationMethod.STRAVA, t.method)
        assertEquals(10, PlanTask(type = TaskType.OTHER, title = "x").rewardMinutes)
        assertFalse(t.copy(attempts = 3).canRetry)
        assertFalse(t.copy(status = TaskStatus.VERIFIED).canRetry)
        assertTrue(t.copy(status = TaskStatus.REJECTED, attempts = 2).canRetry)
    }

    @Test
    fun demoContractHasTasksAndUpdateWorks() = runTest {
        val repo = FakeContractRepository(clock = { day })
        val today = repo.today.value!!
        assertEquals(4, today.tasks.size)
        assertTrue(today.tasks.all { it.id > 0 })
        assertFalse(today.isFulfilled(stepsToday = 10_000))

        val school = today.tasks[1]
        repo.updateTask(school.copy(status = TaskStatus.VERIFIED, attempts = 1))
        assertEquals(TaskStatus.VERIFIED, repo.today.value!!.tasks[1].status)
        assertEquals(TaskStatus.VERIFIED, repo.forDate(day)!!.tasks[1].status)
    }

    @Test
    fun contractFulfilledNeedsStepsAndAllTasks() {
        val c = DailyContract(day, 6_000, tasks = listOf(PlanTask(type = TaskType.WORK, title = "a", status = TaskStatus.VERIFIED)))
        assertTrue(c.isFulfilled(6_000))
        assertFalse(c.isFulfilled(5_999))
        assertTrue(DailyContract(day, 1_000).isFulfilled(1_000))
    }

    @Test
    fun rewardAddsMinutesAndXpOnce() = runTest {
        val movement = FakeMovementRepository(stepsToday = 1_000)
        val engine = FakeGamificationEngine(GamificationState())
        val reward = FakeRewardService(movement, engine)
        val task = PlanTask(id = 7, type = TaskType.SCHOOL, title = "Učení")
        reward.reward(task)
        reward.reward(task)
        assertEquals(10 + 20, movement.state.value.balanceMinutes)
        assertEquals(30, engine.state.value.xp)
        assertEquals(20, engine.state.value.today.reclaimedMinutes)
    }

    @Test
    fun verifierRules() = runTest {
        val strava = FakeStravaRepository()
        val v = FakeTaskVerifier(strava)
        val task = PlanTask(type = TaskType.SCHOOL, title = "Učení")
        assertEquals(VerificationResult.YES, v.verify(task, Proof.Text("Přečetl jsem 20 stran.")))
        assertEquals(VerificationResult.NO, v.verify(task, Proof.Text("hotovo")))
        assertEquals(VerificationResult.YES, v.verify(task, Proof.Photo("file://a.jpg")))
        assertEquals(VerificationResult.NO, v.verify(task, Proof.Photo("file://a.jpg")))
        assertEquals(VerificationResult.YES, v.verify(task, Proof.Strava))
        strava.setConnected(false)
        assertEquals(VerificationResult.NO, v.verify(task, Proof.Strava))
        v.nextResult = VerificationResult.LATER
        assertEquals(VerificationResult.LATER, v.verify(task, Proof.Text("cokoli dlouhého")))
    }

    @Test
    fun settingsUpdate() = runTest {
        val s = FakeSettingsRepository()
        s.update { it.copy(llmProvider = LlmProvider.GEMINI, demoMode = true) }
        assertEquals(LlmProvider.GEMINI, s.settings.value.llmProvider)
        assertTrue(s.settings.value.demoMode)
    }
}
