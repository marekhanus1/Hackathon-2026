package cz.narsil.overlay

import cz.narsil.core.Exercise
import cz.narsil.core.GamificationState
import cz.narsil.core.InterventionOutcome
import cz.narsil.ui.screens.reward.RewardUiState

/**
 * Obrazovka odměny (321) po volbě v Ozvěně: z XP a úrovně před a po. Čistý Kotlin kvůli testům.
 * null = žádné XP (půjčka, zamítnutá volba) → nic neukazovat.
 */
object RewardBuilder {
    fun build(
        outcome: InterventionOutcome,
        before: GamificationState?,
        after: GamificationState?,
        exercise: Exercise = Exercise.SQUATS,
        /** Opakování na odemčení z Nastavení (136). */
        reps: Int = ExerciseText.target(exercise),
    ): RewardUiState? {
        if (before == null || after == null) return null
        val gained = after.xp - before.xp
        if (gained <= 0) return null
        val (title, subtitle) = when (outcome) {
            InterventionOutcome.CLOSED -> "Odešel jsi od feedu sám!" to "Tohle je ta nejtěžší volba. Budoucí já ti děkuje."
            InterventionOutcome.SQUATS -> ExerciseText.doneTitle(exercise, reps) to "Pohyb místo scrollu. Tělo si to pamatuje."
            InterventionOutcome.PAID_STEPS -> "Zaplaceno kroky" to "Čas sis vyšlapal. Užij si ho vědomě."
            else -> "Dobrá volba" to "Každé rozhodnutí se počítá."
        }
        return RewardUiState(
            gainedXp = gained,
            totalXpAfter = after.xp,
            previousLevel = before.level,
            newLevel = after.level,
            newLevelTitle = levelTitle(after.level),
            reasonTitle = title,
            encouragingSubtitle = subtitle,
        )
    }

    fun levelTitle(level: Int): String = when {
        level <= 1 -> "Začátek cesty"
        level == 2 -> "Pozorovatel"
        level == 3 -> "Kovář vůle"
        level == 4 -> "Strážce pozornosti"
        level == 5 -> "Znovu ukovaný"
        else -> "Mistr klidu"
    }
}
