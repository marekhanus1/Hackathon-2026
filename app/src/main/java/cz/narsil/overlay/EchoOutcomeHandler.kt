package cz.narsil.overlay

import cz.narsil.core.GamificationEngine
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.MovementRates
import cz.narsil.core.MovementRepository
import cz.narsil.ui.screens.echo.EchoFeeling

/** Co se stalo po volbě (pro log, Room 121 a testy). */
data class EchoResult(
    val trigger: InterventionTrigger,
    val outcome: InterventionOutcome,
    /** Volba prošla; false = zůstatek nestačí / Parťák nad denním limitem (nic se nestrhlo, okno jen zavřeno). */
    val accepted: Boolean,
    val unlockedMinutes: Int,
    val xp: Int,
    val feeling: EchoFeeling?,
)

/**
 * Volby v Ozvěně (114, SPEC 4):
 * - CLOSED: návrat na plochu, nová relace od nuly; XP čekající na potvrzení (155: po 10 min bez feedu).
 * - PAID_STEPS: strhne 10 min ze zůstatku, odemkne 10 min.
 * - SQUATS: (po 20 dřepech) zapíše dřepy, odemkne 10 min; do zůstatku se nepřičítají.
 * - BORROWED: Parťák dá 10 min na dnešek (163, dřív půjčka do zítřka); nad denním limitem neprojde.
 * Odemknutí: [timer] + [onDetectionOutcome] (`AccessibilityTriggerSource.onOutcome`). Nic nehází.
 */
class EchoOutcomeHandler(
    private val movement: MovementRepository,
    private val gamification: GamificationEngine,
    private val timer: UnlockTimer,
    private val goHome: () -> Unit,
    private val onDetectionOutcome: suspend (InterventionOutcome, Int) -> Unit = { _, _ -> },
    /** Dřepy (v dřepovém ekvivalentu) zapsané za odemčení cvikem; z Nastavení (136). */
    private val squatsForUnlock: () -> Int = { MovementRates.SQUATS_PER_UNLOCK },
    private val onResult: suspend (EchoResult) -> Unit = {},
) : EchoChoiceHandler {

    override suspend fun onChoice(trigger: InterventionTrigger, outcome: InterventionOutcome, feeling: EchoFeeling?): Int {
        val minutes = MovementRates.MINUTES_PER_UNLOCK
        val accepted = when (outcome) {
            InterventionOutcome.CLOSED -> {
                goHome()
                timer.clear()
                true
            }
            InterventionOutcome.PAID_STEPS -> movement.pay(minutes)
            InterventionOutcome.SQUATS -> {
                movement.recordSquats(squatsForUnlock())
                true
            }
            InterventionOutcome.BORROWED -> canBorrow(minutes).also { if (it) movement.borrow(minutes) }
            InterventionOutcome.PLANNED_FREE -> true
        }
        val unlocks = accepted && outcome in UNLOCKING
        if (unlocks) timer.unlock(minutes)
        val xp = if (accepted) runCatching { gamification.onOutcome(outcome) }.getOrDefault(0) else 0
        // Detekce: odemknuto / zavřeno / zamítnuto (zamítnutí = jen zavřené okno, zásah může přijít znovu).
        val detectionOutcome = if (accepted) outcome else InterventionOutcome.PLANNED_FREE
        runCatching { onDetectionOutcome(detectionOutcome, if (unlocks) minutes else 0) }
        val result = EchoResult(trigger, outcome, accepted, if (unlocks) minutes else 0, xp, feeling)
        runCatching { onResult(result) }
        return xp
    }

    /** Prosba projde, jen když minuty od Parťáka nepřekročí denní limit (SPEC 5). */
    fun canBorrow(minutes: Int = MovementRates.MINUTES_PER_UNLOCK): Boolean {
        val s = movement.state.value
        return s.debtMinutes + minutes <= s.debtLimitMinutes
    }

    private companion object {
        val UNLOCKING = setOf(InterventionOutcome.PAID_STEPS, InterventionOutcome.SQUATS, InterventionOutcome.BORROWED)
    }
}
