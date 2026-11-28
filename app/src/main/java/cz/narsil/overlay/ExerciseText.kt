package cz.narsil.overlay

import cz.narsil.core.Exercise
import cz.narsil.core.MovementRates

/**
 * Texty a čísla podle zvoleného cviku (232). Pohyb a gamifikace počítají v dřepech,
 * kliky se proto převádějí na „dřepový ekvivalent“ (10 kliků = 20 dřepů = 10 minut).
 */
object ExerciseText {
    /** Výchozí počet na odemčení (SPEC). Skutečný počet z Nastavení: `Settings.repsPerUnlock()` (136). */
    fun target(e: Exercise): Int = when (e) {
        Exercise.SQUATS -> MovementRates.SQUATS_PER_UNLOCK
        Exercise.PUSHUPS -> MovementRates.PUSHUPS_PER_UNLOCK
    }

    /** Množné číslo v 2. pádě: „20 dřepů“, „10 kliků“. */
    fun plural(e: Exercise): String = when (e) {
        Exercise.SQUATS -> "dřepů"
        Exercise.PUSHUPS -> "kliků"
    }

    /** Název cviku do navigace a přepínačů. */
    fun label(e: Exercise): String = when (e) {
        Exercise.SQUATS -> "Dřepy"
        Exercise.PUSHUPS -> "Kliky"
    }

    /** Titulek volby v Ozvěně. */
    fun echoOption(e: Exercise, reps: Int = target(e)): String = "Udělat $reps ${plural(e)} hned"

    /** Titulek odměny po dokončení. */
    fun doneTitle(e: Exercise, reps: Int = target(e)): String = "$reps ${plural(e)} hotovo!"

    /** Počet opakování převedený na dřepy pro pohyb a gamifikaci. */
    fun squatEquivalent(e: Exercise, count: Int): Int = when (e) {
        Exercise.SQUATS -> count
        Exercise.PUSHUPS -> count * MovementRates.SQUATS_PER_UNLOCK / MovementRates.PUSHUPS_PER_UNLOCK
    }
}
