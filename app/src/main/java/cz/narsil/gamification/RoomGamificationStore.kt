package cz.narsil.gamification

import cz.narsil.core.Surface
import cz.narsil.core.data.BadgeDao
import cz.narsil.core.data.BadgeEntity
import cz.narsil.core.data.ConsumptionDao
import cz.narsil.core.data.DayKey
import cz.narsil.core.data.LedgerDao
import cz.narsil.core.data.LedgerKind
import cz.narsil.core.data.XpDao
import cz.narsil.core.data.XpEventEntity
import java.time.Clock
import java.time.LocalDate

/**
 * Historie gamifikace ve sdílené databázi (220): `xp_events`, `badges`, minuty odměn z `ledger`.
 *
 * `XpDao` nemá dotaz „všechny události“, proto se historie načte po dnech za posledních
 * [historyDays] dní (jen jednou při startu). Poznámka pro Marka: `XpDao.all()` by to zjednodušil.
 */
class RoomGamificationStore(
    private val xpDao: XpDao,
    private val badgeDao: BadgeDao,
    private val ledgerDao: LedgerDao,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val historyDays: Int = 90,
) : GamificationStore {

    override suspend fun load(): GamificationHistory {
        val today = DayKey.of(clock.millis(), clock.zone)
        val records = mutableListOf<XpRecord>()
        for (back in historyDays.toLong() downTo 0L) {
            val date = today.minusDays(back)
            val key = date.toString()
            val events = runCatching { xpDao.forDate(key) }.getOrDefault(emptyList()).sortedWith(compareBy({ it.atMillis }, { it.id }))
            if (events.isEmpty()) continue
            // Minuty odměn úkolů nejsou v xp_events – vezmou se z ledgeru a připíšou prvnímu TASK záznamu dne.
            var taskMinutes = if (events.any { it.source == XpSource.TASK }) {
                runCatching { ledgerDao.sumForDate(key, LedgerKind.TASK_REWARD) }.getOrDefault(0)
            } else {
                0
            }
            for (e in events) {
                val minutes = if (e.source == XpSource.TASK) taskMinutes.also { taskMinutes = 0 } else 0
                records += XpRecord(date, e.atMillis, e.source, e.xp, minutes)
            }
        }
        val badges = runCatching { badgeDao.all() }.getOrDefault(emptyList()).associate { it.id to it.unlockedAtMillis }
        return GamificationHistory(records, badges)
    }

    override suspend fun append(record: XpRecord) {
        xpDao.insert(XpEventEntity(date = record.date.toString(), atMillis = record.atMillis, source = record.source, xp = record.xp))
    }

    override suspend fun unlockBadge(id: String, atMillis: Long) {
        badgeDao.insert(BadgeEntity(id, atMillis))
    }
}

/** Minuty neplánovaného nekonečného obsahu doma za den (souboj s algoritmem, SPEC 8; data 121). */
class RoomFeedMinutes(
    private val consumptionDao: ConsumptionDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend fun forDate(date: LocalDate): Int? = runCatching {
        val from = DayKey.startMillis(date, clock.zone)
        val to = DayKey.startMillis(date.plusDays(1), clock.zone)
        val endless = Surface.entries.filter { it.isEndless }.map { it.name }.toSet()
        val events = consumptionDao.between(from, to)
        if (events.isEmpty()) return null // detekce za ten den nic nemá → záloha z odemknutí
        (events.filter { !it.planned && it.context == "HOME" && it.surface in endless }.sumOf { it.durationMillis } / 60_000L).toInt()
    }.getOrNull()
}
