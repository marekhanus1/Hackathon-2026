package cz.narsil.core

import kotlinx.coroutines.flow.StateFlow

/** Budoucí já: 5 stavů podle posledních 7 dní, od nejhoršího po nejlepší. */
enum class AvatarState { EXHAUSTED, TIRED, OK, FIT, THRIVING }

/** Souboj s algoritmem: získané minuty proti minutám feedu. */
data class DailyScore(
    val reclaimedMinutes: Int = 0,
    val feedMinutes: Int = 0,
) {
    val isWon: Boolean get() = reclaimedMinutes > feedMinutes
}

/**
 * @param xp celkové XP
 * @param level úroveň od 1 (každých 500 XP)
 * @param xpInLevel XP v rámci aktuální úrovně (pro progress bar)
 * @param xpPerLevel XP potřebné na úroveň
 * @param streakDays série dnů se splněnou smlouvou
 * @param jokers žolíky chránící sérii
 * @param avatar stav postavy
 * @param today dnešní skóre souboje
 * @param wonDays vyhrané dny (body ze souboje)
 * @param badges id získaných odznaků (seznam v docs/GAMIFICATION.md)
 */
data class GamificationState(
    val xp: Int = 0,
    val level: Int = 1,
    val xpInLevel: Int = 0,
    val xpPerLevel: Int = 500,
    val streakDays: Int = 0,
    val jokers: Int = 0,
    val avatar: AvatarState = AvatarState.OK,
    val today: DailyScore = DailyScore(),
    val wonDays: Int = 0,
    val badges: List<String> = emptyList(),
)

/** Gamifikace (202, 220). Nikdy netrestá: XP se jen přičítají. Pravidla: docs/GAMIFICATION.md. */
interface GamificationEngine {
    val state: StateFlow<GamificationState>

    /** Výsledek Ozvěny. Vrací přidané XP. */
    suspend fun onOutcome(outcome: InterventionOutcome): Int

    /** Splněná večerní smlouva (+100, prodlouží sérii). Vrací přidané XP. */
    suspend fun onContractFulfilled(): Int

    /** Ověřený úkol plánu (+30, volá [RewardService]). Vrací přidané XP. */
    suspend fun onTaskVerified(task: PlanTask): Int

    /** Ohodnocené video (+5). Vrací přidané XP. */
    suspend fun onVideoRated(rating: Int): Int
}
