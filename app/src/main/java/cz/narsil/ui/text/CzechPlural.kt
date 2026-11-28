package cz.narsil.ui.text

import kotlin.math.abs

/**
 * České plurály pro texty s čísly (145): 1 den / 2 dny / 5 dní, 1 hodinu / 2 hodiny / 5 hodin.
 * Jediné sdílené místo – na obrazovkách nepsat vlastní `when`.
 */
object CzechPlural {
    /** Tvar podle [n]: 1 → [one], 2–4 → [few], jinak (0, 5+, záporná) → [many]. */
    fun word(n: Int, one: String, few: String, many: String): String = when (abs(n)) {
        1 -> one
        in 2..4 -> few
        else -> many
    }

    /** Číslo i se slovem, např. `count(2, "den", "dny", "dní")` → „2 dny“. */
    fun count(n: Int, one: String, few: String, many: String): String = "$n ${word(n, one, few, many)}"

    fun days(n: Int): String = count(n, "den", "dny", "dní")

    /** 4. pád: „vzal zpět 1 hodinu“. */
    fun hoursAccusative(n: Int): String = count(n, "hodinu", "hodiny", "hodin")

    /** 4. pád: „vzal zpět 1 minutu“. */
    fun minutesAccusative(n: Int): String = count(n, "minutu", "minuty", "minut")

    fun squats(n: Int): String = count(n, "dřep", "dřepy", "dřepů")

    fun tasks(n: Int): String = count(n, "úkol", "úkoly", "úkolů")

    fun wins(n: Int): String = count(n, "výhra", "výhry", "výher")

    fun steps(n: Int): String = count(n, "krok", "kroky", "kroků")
}
