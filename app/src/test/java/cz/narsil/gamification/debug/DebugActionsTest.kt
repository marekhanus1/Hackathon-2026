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
