package cz.narsil.gamification

import java.time.LocalDate

/**
 * Jedna přidaná událost gamifikace (= `XpEventEntity`).
 *
 * @param date den aplikace (od 4:00, `DayKey`)
 * @param source [XpSource]
 * @param xp přidané XP (může být 0 – volba se zapíše i bez odměny)
 * @param minutes získané minuty pro souboj s algoritmem (jen [XpSource.TASK]: odměna úkolu)
 */
data class XpRecord(
    val date: LocalDate,
    val atMillis: Long,
    val source: String,
    val xp: Int,
    val minutes: Int = 0,
)

/** Celá historie gamifikace; stav se z ní vždy přepočítá ([GamificationCalculator]). */
data class GamificationHistory(
    val records: List<XpRecord> = emptyList(),
    /** id odznaku → čas odemknutí */
    val badges: Map<String, Long> = emptyMap(),
)

/** Perzistence gamifikace. V paměti [InMemoryGamificationStore], v Room `RoomGamificationStore` (220). */
interface GamificationStore {
    suspend fun load(): GamificationHistory
    suspend fun append(record: XpRecord)

    /** Odznak se odemkne jen jednou (další volání nic nezmění). */
    suspend fun unlockBadge(id: String, atMillis: Long)
}

/** Úložiště v paměti (testy, náhledy, záloha). */
class InMemoryGamificationStore(initial: GamificationHistory = GamificationHistory()) : GamificationStore {
    private val records = initial.records.toMutableList()
    private val badges = LinkedHashMap(initial.badges)

    override suspend fun load(): GamificationHistory = GamificationHistory(records.toList(), LinkedHashMap(badges))

    override suspend fun append(record: XpRecord) {
        records += record
    }

    override suspend fun unlockBadge(id: String, atMillis: Long) {
        badges.putIfAbsent(id, atMillis)
    }
}
