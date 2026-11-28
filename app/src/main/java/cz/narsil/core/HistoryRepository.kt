package cz.narsil.core

import java.time.LocalDate

/**
 * Den historie pro Zrcadlo a odznaky (140).
 *
 * @param feedMinutes minuty neplánovaného nekonečného obsahu doma (události spotřeby 121; bez nich
 *   záloha 10 min za každé odemknutí, stejně jako souboj s algoritmem)
 * @param reclaimedMinutes „vzato zpátky“: Zavřít × 15 + odměny ověřených úkolů (SPEC 8)
 * @param squats dřepy z odemykání a žolíků
 */
data class HistoryDay(
    val date: LocalDate,
    val feedMinutes: Int = 0,
    val reclaimedMinutes: Int = 0,
    val closes: Int = 0,
    val squats: Int = 0,
    val borrows: Int = 0,
    val contractFulfilled: Boolean = false,
    val bossSurvived: Boolean = false,
    /** Den má nějaký záznam (zásah, XP, spotřebu) – dny před instalací se do průměrů nepočítají. */
    val hasData: Boolean = false,
) {
    val battleWon: Boolean get() = reclaimedMinutes > feedMinutes
}

/** Zásah Ozvěny z historie. [outcome] = název `InterventionOutcome`. */
data class HistoryIntervention(val atMillis: Long, val packageName: String, val outcome: String, val feeling: String?)

/** Ohodnocené video (mapa hodnoty, 120/321). */
data class HistoryRating(val atMillis: Long, val title: String, val intent: String?, val rating: Int)

/** Historie posledních dní; [days] od nejstaršího po dnešek. */
data class History(
    val days: List<HistoryDay> = emptyList(),
    val interventions: List<HistoryIntervention> = emptyList(),
    val ratings: List<HistoryRating> = emptyList(),
)

/**
 * Čtení historie ze sdílené databáze (140): jen čte, nic nezapisuje. Implementace
 * `cz.narsil.core.data.RoomHistoryRepository`, fake `cz.narsil.core.fake.FakeHistoryRepository`.
 */
interface HistoryRepository {
    /** Posledních [days] dní včetně dneška (den od 4:00). Chyba = prázdná historie. */
    suspend fun load(days: Int = DEFAULT_DAYS): History

    companion object {
        const val DEFAULT_DAYS = 21
    }
}
