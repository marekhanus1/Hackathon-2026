package cz.narsil.app.nav

import cz.narsil.core.InterventionOutcome
import cz.narsil.core.MovementRates
import cz.narsil.core.Surface
import cz.narsil.core.data.ConsumptionEventEntity
import cz.narsil.core.data.DayKey
import cz.narsil.core.data.InterventionEntity
import cz.narsil.gamification.GamificationRules
import cz.narsil.ui.screens.mirror.DailyTrendPoint
import cz.narsil.ui.screens.mirror.MirrorUiState
import java.time.LocalDate
import java.time.ZoneId

/**
 * Zrcadlo (322) ze skutečných dat v Room (121): minuty feedu z detekce, získané minuty jako v souboji
 * (Zavřít × [GamificationRules.RECLAIMED_MINUTES_PER_CLOSE] + minuty za úkoly), dřepy ze zásahů.
 * Čistý Kotlin kvůli testům; boss a mapa hodnoty zůstávají z UI (připravená data).
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
}
