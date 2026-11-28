package cz.narsil.ui.screens.mirror

import cz.narsil.core.History
import cz.narsil.core.HistoryRating
import java.time.Instant
import java.time.ZoneId

/** Historie z databáze → stav Zrcadla a mapy hodnoty (140). Čistý Kotlin. */
object MirrorMapper {
    /** Pevná kritická hodina bosse (SPEC 8), když z dat nejde nic vyčíst. */
    private const val DEFAULT_CRITICAL_HOUR = 21
    private val TRAILING_NUMBER = Regex("""\s*#?\d+\s*$""")

    fun map(history: History, activeTab: MirrorTab = MirrorTab.MIRROR, zone: ZoneId = ZoneId.systemDefault()): MirrorUiState {
        val days = history.days.dropWhile { !it.hasData }
        val withData = days.count { it.hasData }
        val weeks = days.chunked(7)
        val firstWeek = weeks.firstOrNull().orEmpty().filter { it.hasData }
        val lastWeek = weeks.lastOrNull().orEmpty().filter { it.hasData }
        val firstAvg = firstWeek.map { it.feedMinutes }.average().takeIf { !it.isNaN() } ?: 0.0
        val lastAvg = lastWeek.map { it.feedMinutes }.average().takeIf { !it.isNaN() } ?: 0.0
        val drop = if (weeks.size < 2 || firstAvg <= 0.0) 0 else (((firstAvg - lastAvg) / firstAvg) * 100).toInt().coerceIn(0, 100)
        val hour = history.interventions
            .groupingBy { Instant.ofEpochMilli(it.atMillis).atZone(zone).hour }
            .eachCount()
            .maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            ?.key ?: DEFAULT_CRITICAL_HOUR
        return MirrorUiState(
            activeTab = activeTab,
            totalReclaimedMinutes = days.sumOf { it.reclaimedMinutes },
            feedDropPercent = drop,
            averageDailySquats = if (withData == 0) 0 else days.sumOf { it.squats } / withData,
            criticalBossHour = "%d:00 – %d:00".format(hour, (hour + 1) % 24),
            bossSurvivedDays = days.count { it.bossSurvived },
            weeklyTrend = weeks.mapIndexed { i, week ->
                DailyTrendPoint("Týden ${i + 1}", week.sumOf { it.reclaimedMinutes }, week.sumOf { it.feedMinutes })
            },
            valueEntries = valueEntries(history.ratings),
            isLoaded = true,
            daysWithData = withData,
        )
    }

    /**
     * Mapa hodnoty: videa podle názvu bez pořadového čísla („Když je pondělí #312“ → „Když je pondělí“),
     * osamocená videa bez záměru do dvou skupin podle hodnocení. Past = průměr ≤ 2, nejdřív pasti.
     */
    fun valueEntries(ratings: List<HistoryRating>, limit: Int = 6): List<ContentValueEntry> {
        if (ratings.isEmpty()) return emptyList()
        val byStem = ratings.groupBy { it.title.replace(TRAILING_NUMBER, "").trim().ifEmpty { it.title } }
        val groups = mutableMapOf<String, List<HistoryRating>>()
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
