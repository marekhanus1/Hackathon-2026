package cz.narsil.core.fake

import cz.narsil.core.ComfortContext
import cz.narsil.core.DailyContract
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.InterventionTrigger
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakesTest {

    @Test
    fun contextOverrideWinsOverDetection() = runTest {
        val repo = FakeContextRepository(ComfortContext.AWAY)
        repo.setManualOverride(ComfortContext.HOME)
        repo.setDetected(ComfortContext.AWAY)
        assertEquals(ComfortContext.HOME, repo.context.value)
        repo.setManualOverride(null)
        assertEquals(ComfortContext.AWAY, repo.context.value)
    }

    @Test
    fun movementPayAndBorrow() = runTest {
        val repo = FakeMovementRepository(stepsToday = 1_500)
        assertEquals(15, repo.state.value.balanceMinutes)
        assertTrue(repo.pay(10))
        assertEquals(5, repo.state.value.balanceMinutes)
        assertFalse(repo.pay(10))
        assertEquals(5, repo.state.value.balanceMinutes)
        repo.addSteps(1_000)
        assertEquals(15, repo.state.value.balanceMinutes)
        repeat(4) { repo.borrow(10) }
        assertEquals(40, repo.state.value.debtMinutes)
        assertTrue(repo.state.value.isOverDebtLimit)
        assertEquals(10, repo.recordSquats(20))
    }

    @Test
    fun gamificationRewardsClosingMost() = runTest {
        val engine = FakeGamificationEngine(cz.narsil.core.GamificationState())
        assertEquals(50, engine.onOutcome(InterventionOutcome.CLOSED))
        assertEquals(0, engine.onOutcome(InterventionOutcome.BORROWED))
        assertEquals(10, engine.state.value.today.reclaimedMinutes)
        assertEquals(10, engine.state.value.today.feedMinutes)
        engine.addXp(500)
        assertEquals(550, engine.state.value.xp)
        assertEquals(2, engine.state.value.level)
        assertEquals(50, engine.state.value.xpInLevel)
    }

    @Test
    fun contractSavedForToday() = runTest {
        val day = LocalDate.of(2026, 11, 28)
        val repo = FakeContractRepository(clock = { day }, withDemoContract = false)
        assertNull(repo.today.value)
        repo.save(DailyContract(day.plusDays(1), stepGoal = 5_000))
        assertNull(repo.today.value)
        repo.save(DailyContract(day, stepGoal = 6_000, plannedContent = listOf("přednáška o Rustu")))
        assertEquals(6_000, repo.today.value?.stepGoal)
        assertEquals(5_000, repo.forDate(day.plusDays(1))?.stepGoal)
    }

    @Test
    fun matcherFindsPlannedContent() = runTest {
        val m = FakeContentMatcher()
        val plan = listOf("přednáška o Rustu")
        assertTrue(m.isPlanned("Rust ownership – přednáška FIT", plan).planned)
        assertTrue(m.isPlanned("Learning Rust in 1 hour", plan).planned)
        assertFalse(m.isPlanned("Top 10 fails", plan).planned)
    }

    @Test
    fun squatCounterSimulatesUntilTarget() = runTest {
        val counter = FakeSquatCounter(scope = backgroundScope, intervalMillis = 100, target = 3)
        counter.start()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(3, counter.count.value)
        counter.stop()
        assertFalse(counter.isRunning.value)
    }

    @Test
    fun triggerSourceEmitsFired() = runTest {
        val source = FakeInterventionTriggerSource()
        val received = async { source.triggers.first() }
        runCurrent()
        source.fire()
        val t: InterventionTrigger = received.await()
        assertTrue(t.event.surface.isEndless)
    }
}
