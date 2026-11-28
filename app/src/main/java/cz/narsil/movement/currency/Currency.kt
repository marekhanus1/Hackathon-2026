package cz.narsil.movement.currency

import cz.narsil.core.MovementRates
import cz.narsil.core.data.LedgerKind

/** Jeden pohyb minut (= `LedgerEntryEntity` bez id). [kind] = [LedgerKind], [minutes] ≥ 0. */
data class MinuteEntry(val kind: String, val minutes: Int, val atMillis: Long = 0)

/** Součty minut za jeden den (od 4:00). */
data class DayMinutes(
    val taskRewards: Int = 0,
    val paid: Int = 0,
    val borrowed: Int = 0,
    val carriedDebt: Int = 0,
    /** Přenos dluhu z předchozího dne už proběhl (existuje záznam DEBT_CARRY, i s 0). */
    val carryDone: Boolean = false,
) {
    companion object {
        /** Reset dne z demo panelu (222): pohyby minut do posledního resetu se nepočítají. */
        const val DAY_RESET = "DAY_RESET"

        fun of(all: List<MinuteEntry>): DayMinutes {
            // Podle pořadí zápisu (store vrací záznamy dne v pořadí vložení).
            val resetIndex = all.indexOfLast { it.kind == DAY_RESET }
            val lastReset = if (resetIndex >= 0) resetIndex else null
            val entries = if (lastReset == null) all else all.drop(resetIndex + 1)
            return DayMinutes(
                taskRewards = entries.sumFor(LedgerKind.TASK_REWARD),
                paid = entries.sumFor(LedgerKind.PAID),
                borrowed = entries.sumFor(LedgerKind.BORROW),
                carriedDebt = entries.sumFor(LedgerKind.DEBT_CARRY),
                // Po resetu se dluh ze včerejška znovu nepřenáší.
                carryDone = lastReset != null || entries.any { it.kind == LedgerKind.DEBT_CARRY },
            )
        }

        private fun List<MinuteEntry>.sumFor(kind: String) = filter { it.kind == kind }.sumOf { it.minutes.coerceAtLeast(0) }
    }
}

/**
 * Čistá pravidla měny (SPEC 5, úkol 203):
 * `zůstatek = floor(kroky / 100) + Σ odměny úkolů − zaplacené − přenesený dluh`, nikdy pod 0.
 * Dřepy do zůstatku nejdou, odemykají hned. Minuty od Parťáka (163, dřív půjčka) platí jen
 * pro dnešek a do dalšího dne se nepřenáší; [DayMinutes.carriedDebt] zůstává jen pro staré záznamy.
 */
object Currency {
    fun stepMinutes(steps: Int): Int = steps.coerceAtLeast(0) / MovementRates.STEPS_PER_MINUTE

    fun balance(stepsToday: Int, day: DayMinutes): Int =
        (stepMinutes(stepsToday) + day.taskRewards - day.paid - day.carriedDebt).coerceAtLeast(0)

    /** Přenesený dluh, který dnešní kroky a odměny (po platbách) ještě nepokryly. */
    fun unpaidCarriedDebt(stepsToday: Int, day: DayMinutes): Int {
        val earned = (stepMinutes(stepsToday) + day.taskRewards - day.paid).coerceAtLeast(0)
        return (day.carriedDebt - earned).coerceAtLeast(0)
    }

    /** Platba projde, jen když zůstatek stačí (nikdy pod 0). */
    fun canPay(stepsToday: Int, day: DayMinutes, minutes: Int): Boolean =
        minutes > 0 && balance(stepsToday, day) >= minutes

    /** Minuty od Parťáka nepřekročí denní limit (SPEC 5). */
    fun canBorrow(debtMinutes: Int, minutes: Int, limit: Int = MovementRates.DEFAULT_DEBT_LIMIT_MINUTES): Boolean =
        debtMinutes + minutes <= limit

    /** Kolik minut dluhu přejde z [yesterday] do dalšího dne: nic, minuty od Parťáka se nesplácí (163). */
    @Suppress("UNUSED_PARAMETER")
    fun debtToCarry(yesterday: DayMinutes): Int = 0

    /** Minuty za dřepy podle kurzu (20 dřepů = 10 min), jen celé dávky. */
    fun squatMinutes(count: Int): Int =
        count.coerceAtLeast(0) / MovementRates.SQUATS_PER_UNLOCK * MovementRates.MINUTES_PER_UNLOCK
}
