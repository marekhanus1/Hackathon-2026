package cz.narsil.gamification.reward

import cz.narsil.core.GamificationState
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.core.fake.FakeGamificationEngine
import cz.narsil.core.fake.FakeMovementRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskRewardServiceTest {
    private val task = PlanTask(id = 7, type = TaskType.SCHOOL, title = "Lineární algebra", status = TaskStatus.VERIFIED)

    @Test
    fun rewardsMinutesAndXp() = runTest {
        val movement = FakeMovementRepository(stepsToday = 0)
        val gamification = FakeGamificationEngine(GamificationState())
        TaskRewardService(movement, gamification).reward(task)
        assertEquals(20, movement.state.value.balanceMinutes)
        assertEquals(30, gamification.state.value.xp)
    }

    @Test
    fun doubleCallRewardsOnce() = runTest {
        val movement = FakeMovementRepository(stepsToday = 0)
        val gamification = FakeGamificationEngine(GamificationState())
        val service = TaskRewardService(movement, gamification)
        service.reward(task)
        service.reward(task.copy(attempts = 2))
        (1..5).map { async { service.reward(task) } }.awaitAll()
        assertEquals(20, movement.state.value.balanceMinutes)
        assertEquals(30, gamification.state.value.xp)
    }

    @Test
    fun rewardSurvivesRestartThroughLog() = runTest {
        val log = InMemoryRewardLog()
        val movement = FakeMovementRepository(stepsToday = 0)
        val gamification = FakeGamificationEngine(GamificationState())
        TaskRewardService(movement, gamification, log).reward(task)
        TaskRewardService(movement, gamification, log).reward(task) // „po restartu“
        assertEquals(20, movement.state.value.balanceMinutes)
    }

    @Test
    fun differentTasksBothRewarded() = runTest {
        val movement = FakeMovementRepository(stepsToday = 0)
        val gamification = FakeGamificationEngine(GamificationState())
        val service = TaskRewardService(movement, gamification)
        service.reward(task)
        service.reward(PlanTask(id = 8, type = TaskType.MOVEMENT, title = "Běh", status = TaskStatus.VERIFIED))
        assertEquals(50, movement.state.value.balanceMinutes)
        assertEquals(60, gamification.state.value.xp)
    }
}
