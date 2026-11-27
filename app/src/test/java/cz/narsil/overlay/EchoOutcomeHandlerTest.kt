package cz.narsil.overlay

import cz.narsil.core.GamificationState
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.fake.FakeGamificationEngine
import cz.narsil.core.fake.FakeInterventionTriggerSource
import cz.narsil.core.fake.FakeMovementRepository
import cz.narsil.ui.screens.echo.EchoFeeling
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoOutcomeHandlerTest {
    private var now = 1_000_000L
    private val trigger = FakeInterventionTriggerSource.demoTrigger(now)

    private class Setup(steps: Int = 4_200, debt: Int = 0, clock: () -> Long) {
        val movement = FakeMovementRepository(stepsToday = steps, debtMinutes = debt)
        val engine = FakeGamificationEngine(GamificationState())
        val timer = UnlockTimer(clock)
        var home = 0
        val detection = mutableListOf<Pair<InterventionOutcome, Int>>()
        val results = mutableListOf<EchoResult>()
        val handler = EchoOutcomeHandler(
            movement, engine, timer,
            goHome = { home++ },
            onDetectionOutcome = { o, m -> detection += o to m },
            onResult = { results += it },
        )
    }

    @Test
    fun closedGoesHomeWithoutUnlock() = runTest {
        val s = Setup { now }
        s.handler.onChoice(trigger, InterventionOutcome.CLOSED, EchoFeeling.NUDA)
        assertEquals(1, s.home)
        assertFalse(s.timer.isUnlocked())
        assertEquals(InterventionOutcome.CLOSED to 0, s.detection.single())
        assertEquals(50, s.results.single().xp)
        assertEquals(EchoFeeling.NUDA, s.results.single().feeling)
    }

    @Test
    fun paidStepsUnlocksTenMinutesOrIsRejected() = runTest {
        val s = Setup(steps = 1_500) { now }
        s.handler.onChoice(trigger, InterventionOutcome.PAID_STEPS, null)
        assertEquals(5, s.movement.state.value.balanceMinutes)
        assertTrue(s.timer.isUnlocked())
        assertEquals(10 * 60_000L, s.timer.remainingMillis())
        assertEquals(InterventionOutcome.PAID_STEPS to 10, s.detection.last())
        s.handler.onChoice(trigger, InterventionOutcome.PAID_STEPS, null) // zbývá 5 min, nestačí
        assertFalse(s.results.last().accepted)
        assertEquals(0, s.results.last().xp)
        assertEquals(5, s.movement.state.value.balanceMinutes)
        assertEquals(InterventionOutcome.PLANNED_FREE to 0, s.detection.last())
    }

    @Test
    fun borrowRespectsDebtLimit() = runTest {
        val s = Setup(debt = 20) { now }
        s.handler.onChoice(trigger, InterventionOutcome.BORROWED, null)
        assertEquals(30, s.movement.state.value.debtMinutes)
        assertTrue(s.results.last().accepted)
        s.handler.onChoice(trigger, InterventionOutcome.BORROWED, null)
        assertEquals(30, s.movement.state.value.debtMinutes)
        assertFalse(s.results.last().accepted)
    }

    @Test
    fun squatsUnlockWithoutTouchingBalance() = runTest {
        val s = Setup { now }
        s.handler.onChoice(trigger, InterventionOutcome.SQUATS, null)
        assertEquals(42, s.movement.state.value.balanceMinutes)
        assertEquals(20, s.movement.squatsTotal)
        assertEquals(20, s.results.single().xp)
        assertTrue(s.timer.isUnlocked())
    }

    @Test
    fun unlockExtendsRemainingTime() {
        val timer = UnlockTimer { now }
        timer.unlock(10)
        now += 4 * 60_000
        timer.unlock(10)
        assertEquals(16 * 60_000L, timer.remainingMillis())
    }
}
