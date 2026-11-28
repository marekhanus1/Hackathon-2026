package cz.narsil.core

import kotlinx.coroutines.flow.StateFlow

/** Kurz pohybu na minuty (rozhodnutí 8 v briefu). Ladí se v 230. */
object MovementRates {
    /** 1 000 kroků = 10 min. */
    const val STEPS_PER_MINUTE = 100

    /** Kolik dřepů odemkne [MINUTES_PER_UNLOCK] minut. */
    const val SQUATS_PER_UNLOCK = 20

    /** Kolik kliků odemkne [MINUTES_PER_UNLOCK] minut (232; kliky jsou náročnější). */
    const val PUSHUPS_PER_UNLOCK = 10

    /** Kolik minut dá jedna platba v Ozvěně (kroky, dřepy i půjčka). */
    const val MINUTES_PER_UNLOCK = 10

    /** Nad tímto dluhem dostane zprávu Parťák. */
    const val DEFAULT_DEBT_LIMIT_MINUTES = 30
}

/**
 * @param stepsToday kroky dnes (Health Connect / senzor)
 * @param balanceMinutes zůstatek: kroky / kurz + odměny za úkoly − zaplacené − přenesený dluh (SPEC 5)
 * @param debtMinutes vypůjčené minuty, přenáší se do zítřka
 * @param debtLimitMinutes limit dluhu pro Parťáka
 * @param carriedDebtMinutes přenesený dluh ze včerejška, který dnešní kroky a odměny ještě nepokryly (SPEC 5)
 */
data class MovementState(
    val stepsToday: Int = 0,
    val balanceMinutes: Int = 0,
    val debtMinutes: Int = 0,
    val debtLimitMinutes: Int = MovementRates.DEFAULT_DEBT_LIMIT_MINUTES,
    val carriedDebtMinutes: Int = 0,
) {
    val isOverDebtLimit: Boolean get() = debtMinutes > debtLimitMinutes
}

/** Pohyb jako měna. Implementuje 211 (movement), čistá logika měny 203. */
interface MovementRepository {
    val state: StateFlow<MovementState>

    /** Zaplatí [minutes] ze zůstatku kroků. Vrací false, když zůstatek nestačí (nic se nestrhne). */
    suspend fun pay(minutes: Int = MovementRates.MINUTES_PER_UNLOCK): Boolean

    /** Půjčí si [minutes]; zvýší dluh. Vždy projde (nic tvrdě neblokujeme). */
    suspend fun borrow(minutes: Int = MovementRates.MINUTES_PER_UNLOCK)

    /** Zapíše odcvičené dřepy (statistiky, odznaky). Vrací získané minuty podle kurzu. */
    suspend fun recordSquats(count: Int): Int

    /** Odměna za ověřený úkol plánu (volá [RewardService]); zvýší zůstatek. */
    suspend fun addEarnedMinutes(minutes: Int)

    /** Znovu načte kroky (Health Connect). */
    suspend fun refresh()
}
