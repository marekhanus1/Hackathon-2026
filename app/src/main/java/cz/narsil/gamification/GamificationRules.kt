package cz.narsil.gamification

import cz.narsil.core.AvatarState
import cz.narsil.core.MovementRates

/** Čísla gamifikace (docs/GAMIFICATION.md, SPEC 8). Ladí 230. */
object GamificationRules {
    const val XP_CLOSED = 50
    const val XP_SQUATS = 20
    const val XP_PAID_STEPS = 10
    const val XP_BORROWED = 0
    const val XP_TASK = 30
    const val XP_CONTRACT = 100
    const val XP_VIDEO_RATED = 5
    const val XP_BOSS = 150

    const val XP_PER_LEVEL = 500

    /** CLOSED dá XP nejvýš 5× denně a s odstupem ≥ 10 min od posledního odměněného zavření. */
    const val CLOSED_MAX_REWARDED_PER_DAY = 5
    const val CLOSED_MIN_GAP_MILLIS = 10 * 60_000L

    const val VIDEO_RATED_MAX_PER_DAY = 10

    /** Souboj s algoritmem: odhad ušetřené relace za jedno CLOSED. */
    const val RECLAIMED_MINUTES_PER_CLOSE = 15

    /** Záloha minut feedu za odemknutí, dokud detekce (121) nedodává data. */
    const val FEED_MINUTES_PER_UNLOCK = MovementRates.MINUTES_PER_UNLOCK

    /** Žolík za 50 dřepů navíc, nejvýš 3 v zásobě. */
    const val SQUATS_PER_JOKER = 50
    const val MAX_JOKERS = 3

    /** Boss kritické hodiny (MVP pevně 21:00–22:00 místního času). */
    const val BOSS_START_HOUR = 21
    const val BOSS_END_HOUR = 22

    const val AVATAR_WINDOW_DAYS = 7
    const val NO_DEBT_DAYS = 7
    const val SQUATS_BADGE_TOTAL = 1_000
    const val STREAK_BADGE_DAYS = 3

    fun level(xp: Int): Int = 1 + xp.coerceAtLeast(0) / XP_PER_LEVEL

    fun xpInLevel(xp: Int): Int = xp.coerceAtLeast(0) % XP_PER_LEVEL

    /** Postava podle dní se splněnou smlouvou za posledních 7 dní (SPEC 8). */
    fun avatarFor(fulfilledDays: Int): AvatarState = when {
        fulfilledDays <= 1 -> AvatarState.EXHAUSTED
        fulfilledDays == 2 -> AvatarState.TIRED
        fulfilledDays <= 4 -> AvatarState.OK
        fulfilledDays <= 6 -> AvatarState.FIT
        else -> AvatarState.THRIVING
    }
}

/** Hodnoty [XpRecord.source] = `XpEventEntity.source`. Volby Ozvěny = názvy `InterventionOutcome`. */
object XpSource {
    const val CLOSED = "CLOSED"
    const val PAID_STEPS = "PAID_STEPS"
    const val SQUATS = "SQUATS"
    const val BORROWED = "BORROWED"
    const val PLANNED_FREE = "PLANNED_FREE"
    const val TASK = "TASK"
    const val CONTRACT = "CONTRACT"
    const val VIDEO_RATED = "VIDEO_RATED"
    const val BOSS = "BOSS"
    const val DEMO = "DEMO"

    /** Získaný žolík (50 dřepů navíc), 0 XP. */
    const val JOKER = "JOKER"

    /** Volby, které jsou zásahem Ozvěny (boss, skóre). */
    val INTERVENTIONS = setOf(CLOSED, PAID_STEPS, SQUATS, BORROWED)

    /** Volby, které odemknou obsah (záloha minut feedu). */
    val UNLOCKS = setOf(PAID_STEPS, SQUATS, BORROWED)
}

/** Id odznaků (docs/GAMIFICATION.md sekce 8) = `BadgeEntity.id`. */
object Badges {
    const val FIRST_CLOSE = "FIRST_CLOSE"
    const val CLOSE_10 = "CLOSE_10"
    const val NO_DEBT_WEEK = "NO_DEBT_WEEK"
    const val SQUATS_1000 = "SQUATS_1000"
    const val PLAN_STREAK_3 = "PLAN_STREAK_3"

    val ALL = listOf(FIRST_CLOSE, CLOSE_10, NO_DEBT_WEEK, SQUATS_1000, PLAN_STREAK_3)
}
