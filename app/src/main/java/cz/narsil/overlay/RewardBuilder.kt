package cz.narsil.overlay

import cz.narsil.core.CloseRules
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
        /** CLOSED (155): XP, které se připíšou, když zavření vydrží 10 min bez feedu; 0 = bez odměny. */
        pendingCloseXp: Int = 0,
    ): RewardUiState? {
        if (before == null || after == null) return null
        if (outcome == InterventionOutcome.CLOSED) return closePending(pendingCloseXp, after)
        val gained = after.xp - before.xp
        if (gained <= 0) return null
        val (title, subtitle) = when (outcome) {
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

    private fun closePending(xp: Int, now: GamificationState): RewardUiState? {
        if (xp <= 0) return null
        val minutes = CloseRules.CONFIRM_MILLIS / 60_000
        return RewardUiState(
            gainedXp = xp,
            totalXpAfter = now.xp, // zatím nepřipsané
            previousLevel = now.level,
            newLevel = now.level,
            newLevelTitle = levelTitle(now.level),
            reasonTitle = "Odešel jsi od feedu sám!",
            encouragingSubtitle = "+$xp XP ti připíšu, když se $minutes minut do feedu nevrátíš. Vydrž.",
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
