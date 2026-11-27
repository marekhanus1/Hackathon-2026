package cz.narsil.gamification

import cz.narsil.core.GamificationState
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.fake.FakeGamificationEngine

/**
 * Ladění čísel pro 2minutové demo (230, scénář v briefu / 403).
 *
 * Ve scénáři přidávají XP jen dvě akce: **20 dřepů naživo** (`SQUATS`, +20) a případně
 * placení kroky za neplánované video (`PAID_STEPS`, +10). Demo začíná [DEMO_START_XP] = 20 XP
 * pod hranicí úrovně, takže novou úroveň (animace 321) spustí právě dřepy – vrchol dema.
 * Placení kroky potom už jen přidá XP v nové úrovni (žádná druhá animace).
 */
object DemoTuning {
    /** Úroveň, ze které demo startuje (sedí na připravenou postavu a obrazovky Jana). */
    const val DEMO_START_LEVEL = 3

    /** Volba, která v demu spustí novou úroveň. */
    val LEVEL_UP_OUTCOME = InterventionOutcome.SQUATS

    /** XP, která chybí do další úrovně na začátku dema (= XP za [LEVEL_UP_OUTCOME]). */
    const val XP_GAP_BEFORE_DEMO = GamificationRules.XP_SQUATS

    /** 1 480 XP: úroveň 3, 480 / 500. */
    const val DEMO_START_XP = DEMO_START_LEVEL * GamificationRules.XP_PER_LEVEL - XP_GAP_BEFORE_DEMO

    /** Stav fake gamifikace pro demo (jako `FakeGamificationEngine.DEMO_STATE`, jen XP těsně pod úrovní). */
    val DEMO_STATE: GamificationState = FakeGamificationEngine.DEMO_STATE.copy(
        xp = DEMO_START_XP,
        level = GamificationRules.level(DEMO_START_XP),
        xpInLevel = GamificationRules.xpInLevel(DEMO_START_XP),
    )

    /**
     * Kolik XP přidat k [currentXp], aby do další úrovně chybělo přesně [gap] XP
     * (nejméně 0; když je hranice blíž než [gap], dorovná na stejnou pozici v další úrovni).
     */
    fun xpToPrepare(currentXp: Int, gap: Int = XP_GAP_BEFORE_DEMO): Int {
        val per = GamificationRules.XP_PER_LEVEL
        val target = per - gap.coerceIn(1, per)
        val inLevel = GamificationRules.xpInLevel(currentXp)
        return if (inLevel <= target) target - inLevel else per - inLevel + target
    }

    /** Spustí přidání [xp] novou úroveň? (UI: animace jen při přechodu.) */
    fun levelsUp(xpBefore: Int, xp: Int): Boolean =
        GamificationRules.level(xpBefore + xp) > GamificationRules.level(xpBefore)
}
