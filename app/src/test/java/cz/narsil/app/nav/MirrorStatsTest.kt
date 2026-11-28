package cz.narsil.app.nav

import cz.narsil.core.InterventionOutcome
import cz.narsil.core.data.ConsumptionEventEntity
import cz.narsil.core.data.DayKey
import cz.narsil.core.data.InterventionEntity
import cz.narsil.ui.screens.mirror.MirrorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MirrorStatsTest {
    private val zone = ZoneId.of("Europe/Prague")
    private val from = LocalDate.of(2026, 11, 8)
    private val to = from.plusDays(20)

    private fun at(d: LocalDate, hour: Int) = DayKey.startMillis(d, zone) + (hour - 4) * 3_600_000L

    private fun feed(d: LocalDate, minutes: Int, surface: String = "REELS", planned: Boolean = false) =
        ConsumptionEventEntity(packageName = "ig", surface = surface, startedAtMillis = at(d, 20), durationMillis = minutes * 60_000L, planned = planned, context = "HOME")

    private fun intervention(d: LocalDate, o: InterventionOutcome) =
        InterventionEntity(atMillis = at(d, 21), packageName = "ig", reason = "ENDLESS_SCROLL", outcome = o.name)

    @Test
    fun weeklyTrendFromRoom() {
        val consumption = listOf(
            feed(from, 60), feed(from.plusDays(1), 40),
            feed(from.plusDays(1), 30, surface = "VIDEO"), // video se do feedu nepočítá
            feed(from.plusDays(2), 30, planned = true), // plánované taky ne
            feed(to, 20),
        )
        val interventions = listOf(
            intervention(from, InterventionOutcome.CLOSED),
            intervention(to, InterventionOutcome.SQUATS),
            intervention(to, InterventionOutcome.PAID_STEPS),
        )
        val days = MirrorStats.days(consumption, interventions, { if (it == to) 10 else 0 }, from, to, zone)
        assertEquals(21, days.size)
        assertEquals(60, days[0].feedMinutes)
        assertEquals(15, days[0].reclaimedMinutes)
        assertEquals(10, days[20].reclaimedMinutes)
        assertEquals(20, days[20].squats)

        val ui = MirrorStats.apply(MirrorUiState(), days)
        assertEquals(listOf(100, 0, 20), ui.weeklyTrend.map { it.feedMinutes })
        assertEquals(listOf(15, 0, 10), ui.weeklyTrend.map { it.reclaimedMinutes })
        assertEquals(25, ui.totalReclaimedMinutes)
        assertEquals(80, ui.feedDropPercent)
        assertEquals(0, ui.averageDailySquats) // 20 / 21
    }

    @Test
    fun emptyRoomKeepsPreparedData() {
        val base = MirrorUiState()
        assertSame(base, MirrorStats.apply(base, MirrorStats.days(emptyList(), emptyList(), { 0 }, from, to, zone)))
    }
}
