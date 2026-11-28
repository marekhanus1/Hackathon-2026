package cz.narsil.overlay

import cz.narsil.core.Exercise
import cz.narsil.core.GamificationState
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.MovementState
import cz.narsil.core.Settings
import cz.narsil.core.fake.FakeGamificationEngine
import cz.narsil.core.fake.FakeInterventionTriggerSource
import cz.narsil.core.fake.FakeMovementRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 136: Ozvěna bere počet opakování z Nastavení (`Settings.repsPerUnlock`). */
class RepsPerUnlockTest {
    private val trigger = FakeInterventionTriggerSource.demoTrigger(1_000_000L)

    @Test
    fun echoAndRewardTextsUseSettings() {
        val s = Settings(squatsPerUnlock = 30, pushupsPerUnlock = 15)
        val squats = EchoStateBuilder.build(trigger, MovementState(), null, null, Exercise.SQUATS, s.repsPerUnlock(Exercise.SQUATS))
        assertEquals("Udělat 30 dřepů hned", squats.ui.exerciseOptionTitle)
        val pushups = EchoStateBuilder.build(trigger, MovementState(), null, null, Exercise.PUSHUPS, s.repsPerUnlock(Exercise.PUSHUPS))
        assertEquals("Udělat 15 kliků hned", pushups.ui.exerciseOptionTitle)
        val reward = RewardBuilder.build(InterventionOutcome.SQUATS, GamificationState(xp = 0), GamificationState(xp = 20), Exercise.SQUATS, 30)
        assertEquals("30 dřepů hotovo!", reward?.reasonTitle)
        // Bez nastavení výchozí hodnoty SPEC.
        assertEquals("Udělat 20 dřepů hned", EchoStateBuilder.build(trigger, MovementState(), null, null).ui.exerciseOptionTitle)
    }

    @Test
    fun squatsRecordedFromSettings() = runTest {
        val movement = FakeMovementRepository()
        val handler = EchoOutcomeHandler(
            movement, FakeGamificationEngine(GamificationState()), UnlockTimer { 0L },
            goHome = {},
            squatsForUnlock = { ExerciseText.squatEquivalent(Exercise.PUSHUPS, 15) },
        )
        handler.onChoice(trigger, InterventionOutcome.SQUATS, null)
        assertEquals(30, movement.squatsTotal) // 15 kliků = 30 dřepů
    }
}
