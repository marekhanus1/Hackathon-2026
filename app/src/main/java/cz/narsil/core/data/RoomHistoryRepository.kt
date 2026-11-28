package cz.narsil.core.data

import cz.narsil.core.History
import cz.narsil.core.HistoryDay
import cz.narsil.core.HistoryIntervention
import cz.narsil.core.HistoryRating
import cz.narsil.core.HistoryRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * [HistoryRepository] nad sdílenou databází (140). Čte spotřebu (121), zásahy, XP, odměny úkolů
 * a hodnocení videí; schéma ani DAO nemění. Čistý výpočet dne je v [HistoryMath].
 */
class RoomHistoryRepository(
    private val db: NarsilDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : HistoryRepository {
    override suspend fun load(days: Int): History = runCatching {
        val zone = clock.zone
        val today = DayKey.of(clock.millis(), zone)
        val first = today.minusDays(days.coerceAtLeast(1) - 1L)
        val from = DayKey.startMillis(first, zone)
        val to = DayKey.startMillis(today.plusDays(1), zone)
        val consumption = db.consumptionDao().between(from, to)
        val interventions = db.interventionDao().between(from, to)
        val dayList = (0 until days.coerceAtLeast(1)).map { first.plusDays(it.toLong()) }.map { date ->
            val key = date.toString()
            HistoryMath.day(
                date = date,
                xp = db.xpDao().forDate(key).map { it.source to it.xp },
                consumption = consumption.filter { DayKey.of(it.startedAtMillis, zone) == date },
                taskRewardMinutes = db.ledgerDao().sumForDate(key, LedgerKind.TASK_REWARD),
            )
        }
        History(
            days = dayList,
            interventions = interventions.map { HistoryIntervention(it.atMillis, it.packageName, it.outcome, it.feeling) },
            ratings = db.videoRatingDao().observeAll().first()
                .filter { it.atMillis >= from }
                .map { HistoryRating(it.atMillis, it.title, it.intent, it.rating) },
        )
    }.getOrDefault(History())
}

/** Výpočet dne historie z řádků databáze. Čistý Kotlin (testy). */
object HistoryMath {
    private const val CLOSED = "CLOSED"
    private val UNLOCKS = setOf("PAID_STEPS", "SQUATS", "BORROWED")
    private val ENDLESS = setOf("FEED", "REELS", "SHORTS")
    const val RECLAIMED_PER_CLOSE = 15
    const val FEED_MINUTES_PER_UNLOCK = 10
    const val SQUATS_PER_UNLOCK = 20
    const val SQUATS_PER_JOKER = 50

    /** [xp] = (zdroj, XP) z `xp_events` toho dne; reset dne z demo panelu zahodí události před ním. */
    fun day(
        date: LocalDate,
        xp: List<Pair<String, Int>>,
        consumption: List<ConsumptionEventEntity>,
        taskRewardMinutes: Int,
    ): HistoryDay {
        val effective = xp.drop(xp.indexOfLast { it.first == "DAY_RESET" } + 1)
        val closes = effective.count { it.first == CLOSED }
        val unlocks = effective.count { it.first in UNLOCKS }
        val feedEvents = consumption.filter { !it.planned && it.context == "HOME" && it.surface in ENDLESS }
        val feed = if (consumption.isNotEmpty()) (feedEvents.sumOf { it.durationMillis } / 60_000L).toInt()
        else unlocks * FEED_MINUTES_PER_UNLOCK
        return HistoryDay(
            date = date,
            feedMinutes = feed,
            reclaimedMinutes = closes * RECLAIMED_PER_CLOSE + taskRewardMinutes.coerceAtLeast(0),
            closes = closes,
            squats = effective.count { it.first == "SQUATS" } * SQUATS_PER_UNLOCK +
                effective.count { it.first == "JOKER" } * SQUATS_PER_JOKER,
            borrows = effective.count { it.first == "BORROWED" },
            contractFulfilled = effective.any { it.first == "CONTRACT" },
            bossSurvived = effective.any { it.first == "BOSS" },
            hasData = effective.isNotEmpty() || consumption.isNotEmpty() || taskRewardMinutes > 0,
        )
    }
}
