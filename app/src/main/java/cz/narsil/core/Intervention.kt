package cz.narsil.core

import kotlinx.coroutines.flow.Flow

/** Proč detekce zasáhla. */
enum class TriggerReason {
    /** Nekonečný feed/Reels/Shorts déle než práh (5 min, demo 20 s). */
    ENDLESS_SCROLL,

    /** Řetězení neplánovaných videí na YouTube. */
    UNPLANNED_VIDEO_CHAIN,

    /** Vynuceno z demo panelu. */
    DEMO,
}

/** Událost „teď zasáhnout“ z detekce (111, 120). Spotřebitel: překryvné okno (113–115). */
data class InterventionTrigger(
    val event: ConsumptionEvent,
    val reason: TriggerReason,
    val atMillis: Long,
)

/** Zdroj zásahů. Implementuje detekce (111); fake umožní vyvolat zásah ručně. */
interface InterventionTriggerSource {
    /** Hot stream; každý prvek = ukázat Ozvěnu. Bez replay. */
    val triggers: Flow<InterventionTrigger>
}

/**
 * Jak zásah skončil (114). Jde do [GamificationEngine.onOutcome].
 * PLANNED_FREE = obsah byl v plánu, Ozvěna se neukázala.
 */
enum class InterventionOutcome { CLOSED, PAID_STEPS, SQUATS, BORROWED, PLANNED_FREE }
