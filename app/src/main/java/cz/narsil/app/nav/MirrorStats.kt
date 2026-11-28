package cz.narsil.app.nav

import cz.narsil.core.InterventionOutcome
import cz.narsil.core.MovementRates
import cz.narsil.core.Surface
import cz.narsil.core.data.ConsumptionEventEntity
import cz.narsil.core.data.DayKey
import cz.narsil.core.data.InterventionEntity
import cz.narsil.gamification.GamificationRules
import cz.narsil.core.History
import cz.narsil.core.HistoryRating
import cz.narsil.ui.screens.mirror.ContentValueEntry
import cz.narsil.ui.screens.mirror.DailyTrendPoint
import cz.narsil.ui.screens.mirror.MirrorUiState
import java.time.LocalDate
import java.time.ZoneId

/**
 * Zrcadlo (322) ze skutečných dat v Room (121): minuty feedu z detekce, získané minuty jako v souboji
 * (Zavřít × [GamificationRules.RECLAIMED_MINUTES_PER_CLOSE] + minuty za úkoly), dřepy ze zásahů.
 * Čistý Kotlin kvůli testům. Boss a mapa hodnoty z [HistoryRepository] (143, viz [applyHistory]).
 */
object MirrorStats {
    const val DAYS = 21

    data class Day(val date: LocalDate, val feedMinutes: Int, val reclaimedMinutes: Int, val squats: Int)

    /** Dny [from]..[to] (včetně), den od 4:00 ([DayKey]). */
    fun days(
        consumption: List<ConsumptionEventEntity>,
        interventions: List<InterventionEntity>,
        taskMinutes: (LocalDate) -> Int,
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Day> {
        val endless = Surface.entries.filter { it.isEndless }.map { it.name }.toSet()
        val feed = consumption
            .filter { !it.planned && it.context == "HOME" && it.surface in endless }
            .groupBy { DayKey.of(it.startedAtMillis, zone) }
            .mapValues { (_, l) -> (l.sumOf { it.durationMillis } / 60_000L).toInt() }
        val byDay = interventions.groupBy { DayKey.of(it.atMillis, zone) }
        return generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.map { d ->
            val dayInterventions = byDay[d].orEmpty()
            val closes = dayInterventions.count { it.outcome == InterventionOutcome.CLOSED.name }
            Day(
                date = d,
                feedMinutes = feed[d] ?: 0,
                reclaimedMinutes = closes * GamificationRules.RECLAIMED_MINUTES_PER_CLOSE + taskMinutes(d),
                squats = dayInterventions.count { it.outcome == InterventionOutcome.SQUATS.name } * MovementRates.SQUATS_PER_UNLOCK,
            )
        }.toList()
    }

    /** Přepíše čísla v [base] skutečnými; bez dat (prázdná Room) vrátí [base] beze změny. */
    fun apply(base: MirrorUiState, days: List<Day>): MirrorUiState {
        if (days.all { it.feedMinutes == 0 && it.reclaimedMinutes == 0 && it.squats == 0 }) return base
        val weeks = days.chunked(7).mapIndexed { i, w ->
            DailyTrendPoint("Týden ${i + 1}", reclaimedMinutes = w.sumOf { it.reclaimedMinutes }, feedMinutes = w.sumOf { it.feedMinutes })
        }
        val first = weeks.first().feedMinutes
        val last = weeks.last().feedMinutes
        return base.copy(
            totalReclaimedMinutes = days.sumOf { it.reclaimedMinutes },
            feedDropPercent = if (first > 0) ((first - last) * 100 / first).coerceAtLeast(0) else 0,
            averageDailySquats = days.sumOf { it.squats } / days.size.coerceAtLeast(1),
            weeklyTrend = weeks,
        )
    }

    private val TRAILING_NUMBER = Regex("""\s*#?\d+\s*$""")

    /**
     * Boss a mapa hodnoty z historie (143). Prázdná historie (žádný den s daty, zásah ani hodnocení)
     * = [base] beze změny, jako u [apply]. Boss má pevné okno 21:00–22:00 (SPEC 8), počítají se dny,
     * kdy ho gamifikace připsala. Bez hodnocení zůstane mapa prázdná (obrazovka vysvětlí proč).
     */
    fun applyHistory(base: MirrorUiState, history: History): MirrorUiState {
        val empty = history.days.none { it.hasData } && history.interventions.isEmpty() && history.ratings.isEmpty()
        if (empty) return base
        return base.copy(
            criticalBossHour = "%d:00 – %d:00".format(GamificationRules.BOSS_START_HOUR, GamificationRules.BOSS_END_HOUR),
            bossSurvivedDays = history.days.count { it.bossSurvived },
            valueEntries = valueEntries(history.ratings),
        )
    }

    /**
     * Mapa hodnoty: videa podle názvu bez pořadového čísla („Když je pondělí #312“ → „Když je pondělí“);
     * osamocená videa podle záměru, nebo do dvou skupin bez záměru podle hodnocení. Past = průměr ≤ 2.
     * Kanál ani minuty sledování se neukládají (`video_ratings` má jen název, záměr a hodnocení).
     */
    fun valueEntries(ratings: List<HistoryRating>, limit: Int = 6): List<ContentValueEntry> {
        if (ratings.isEmpty()) return emptyList()
        val byStem = ratings.groupBy { it.title.replace(TRAILING_NUMBER, "").trim().ifEmpty { it.title } }
        val groups = linkedMapOf<String, List<HistoryRating>>()
        byStem.forEach { (stem, list) ->
            val key = when {
                list.size > 1 -> stem
                list.single().intent != null -> "Se záměrem: ${list.single().intent}"
                list.single().rating <= 2 -> "Bez záměru · hodnoceno nízko"
                else -> "Bez záměru · ostatní"
            }
            groups[key] = groups[key].orEmpty() + list
        }
        return groups.map { (name, list) ->
            val avg = list.map { it.rating }.average().toFloat()
            ContentValueEntry(
                channelOrFormat = name,
                averageRating = Math.round(avg * 10) / 10f,
                videoCount = list.size,
                isLowValueTrap = avg <= 2f,
            )
        }.sortedWith(compareByDescending<ContentValueEntry> { it.isLowValueTrap }.thenByDescending { it.videoCount }.thenBy { it.channelOrFormat })
            .take(limit)
    }
}
