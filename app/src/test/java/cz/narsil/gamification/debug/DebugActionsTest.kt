package cz.narsil.gamification.debug

import cz.narsil.core.AvatarState
import cz.narsil.core.GamificationState
import cz.narsil.core.fake.FakeGamificationEngine
import cz.narsil.core.fake.FakeMovementRepository
import cz.narsil.gamification.RulesGamificationEngine
import cz.narsil.movement.StepsMovementRepository
import cz.narsil.movement.currency.CurrencyBook
import cz.narsil.movement.debug.MovementDebug
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugActionsTest {
    @Test
    fun addStepsOnFakeAndReal() = runTest {
        val fake = FakeMovementRepository(stepsToday = 0)
        assertTrue(MovementDebug.addSteps(fake))
        assertEquals(1_000, fake.state.value.stepsToday)

        val real = StepsMovementRepository({ 2_000 }, CurrencyBook())
        real.refresh()
        assertTrue(MovementDebug.addSteps(real))
        assertEquals(3_000, real.state.value.stepsToday)
        assertEquals(30, real.state.value.balanceMinutes)
    }

    @Test
    fun addXpAndAvatarOnFakeAndReal() = runTest {
        val fake = FakeGamificationEngine(GamificationState())
        assertTrue(GamificationDebug.addXp(fake))
        assertEquals(2, fake.state.value.level)

        val real = RulesGamificationEngine()
        assertTrue(GamificationDebug.addXp(real, 1_000))
        assertEquals(3, real.state.value.level)
        assertTrue(GamificationDebug.setAvatar(real, AvatarState.THRIVING))
        assertEquals(AvatarState.THRIVING, real.state.value.avatar)
        assertEquals(AvatarState.EXHAUSTED, GamificationDebug.nextAvatar(real))
        assertEquals(AvatarState.TIRED, GamificationDebug.nextAvatar(fake.also { it.setAvatar(AvatarState.EXHAUSTED) }))
    }
}

class ResetDayTest {
    @Test
    fun gamificationResetDropsTodayOnly() = kotlinx.coroutines.test.runTest {
        val clock = cz.narsil.gamification.TestClock(java.time.LocalDateTime.of(2026, 11, 28, 10, 0))
        val store = cz.narsil.gamification.InMemoryGamificationStore()
        val engine = RulesGamificationEngine(store, clock)
        engine.onContractFulfilled() // včera
        clock.set(java.time.LocalDateTime.of(2026, 11, 29, 10, 0))
        engine.onOutcome(cz.narsil.core.InterventionOutcome.CLOSED)
        engine.setAvatar(AvatarState.THRIVING)
        assertEquals(300, engine.state.value.xp) // smlouva 100 + včerejší boss 150 + CLOSED 50
        assertTrue(GamificationDebug.resetDay(engine))
        assertEquals(250, engine.state.value.xp) // včerejšek (smlouva + boss) zůstal
        assertEquals(0, engine.state.value.today.reclaimedMinutes)
        assertEquals(AvatarState.OK, engine.state.value.avatar)
        assertEquals(50, engine.onOutcome(cz.narsil.core.InterventionOutcome.CLOSED)) // strop CLOSED od nuly
        val restarted = RulesGamificationEngine(store, clock)
        restarted.refresh()
        assertEquals(300, restarted.state.value.xp)
    }

    @Test
    fun movementResetClearsMinutesAndSetsSteps() = kotlinx.coroutines.test.runTest {
        val repo = StepsMovementRepository({ 9_000 }, CurrencyBook())
        repo.refresh()
        repo.pay(30)
        repo.borrow(20)
        assertTrue(MovementDebug.resetDay(repo))
        val s = repo.state.value
        assertEquals(4_200, s.stepsToday)
        assertEquals(42, s.balanceMinutes)
        assertEquals(0, s.debtMinutes)

        val fake = FakeMovementRepository(stepsToday = 0, debtMinutes = 20)
        assertTrue(MovementDebug.resetDay(fake))
        assertEquals(4_200, fake.state.value.stepsToday)
        assertEquals(0, fake.state.value.debtMinutes)
    }
}
