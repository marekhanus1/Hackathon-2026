package cz.narsil.overlay

import cz.narsil.core.Exercise
import cz.narsil.core.MovementRates
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseTextTest {
    @Test
    fun pushupsUnlockWithTenAndCountAsTwentySquats() {
        assertEquals(10, ExerciseText.target(Exercise.PUSHUPS))
        assertEquals(MovementRates.SQUATS_PER_UNLOCK, ExerciseText.squatEquivalent(Exercise.PUSHUPS, 10))
        assertEquals(24, ExerciseText.squatEquivalent(Exercise.PUSHUPS, 12))
    }

    @Test
    fun squatsStayUnchanged() {
        assertEquals(20, ExerciseText.target(Exercise.SQUATS))
        assertEquals(23, ExerciseText.squatEquivalent(Exercise.SQUATS, 23))
    }

    @Test
    fun textsFollowExercise() {
        assertEquals("Udělat 20 dřepů hned", ExerciseText.echoOption(Exercise.SQUATS))
        assertEquals("Udělat 10 kliků hned", ExerciseText.echoOption(Exercise.PUSHUPS))
        assertEquals("10 kliků hotovo!", ExerciseText.doneTitle(Exercise.PUSHUPS))
    }
}
