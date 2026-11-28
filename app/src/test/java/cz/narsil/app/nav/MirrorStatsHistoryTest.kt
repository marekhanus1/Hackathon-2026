package cz.narsil.app.nav

import cz.narsil.core.History
import cz.narsil.core.HistoryDay
import cz.narsil.core.HistoryIntervention
import cz.narsil.core.HistoryRating
import cz.narsil.ui.screens.mirror.MirrorUiState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** 143: boss a mapa hodnoty z databáze. */
class MirrorStatsHistoryTest {
    private val base = MirrorUiState()
    private val start = LocalDate.of(2026, 11, 8)
    private val ratings = listOf(
        HistoryRating(1, "Když je pondělí #312", null, 2),
        HistoryRating(2, "Když je pondělí #318", null, 1),
        HistoryRating(3, "Ownership za 20 minut", "pochopit borrow checker", 5),
        HistoryRating(4, "Kdo lže?", null, 1),
        HistoryRating(5, "Kočka vs. okurka", null, 3),
    )

    @Test
    fun emptyHistoryKeepsPreparedNumbers() {
        assertSame(base, MirrorStats.applyHistory(base, History()))
        val beforeInstall = History(days = listOf(HistoryDay(start), HistoryDay(start.plusDays(1))))
        assertSame(base, MirrorStats.applyHistory(base, beforeInstall))
    }

    @Test
    fun bossAndValueMapFromHistory() {
        val days = (0 until 21).map { HistoryDay(start.plusDays(it.toLong()), bossSurvived = it % 3 == 0, hasData = true) }
        val s = MirrorStats.applyHistory(base, History(days = days, ratings = ratings))
        assertEquals(7, s.bossSurvivedDays)
        assertEquals("21:00 – 22:00", s.criticalBossHour)
        val first = s.valueEntries.first()
        assertEquals("Když je pondělí", first.channelOrFormat)
        assertEquals(2, first.videoCount)
        assertEquals(1.5f, first.averageRating)
        assertTrue(first.isLowValueTrap)
        assertTrue(s.valueEntries.any { it.channelOrFormat == "Se záměrem: pochopit borrow checker" && !it.isLowValueTrap })
        assertTrue(s.valueEntries.any { it.channelOrFormat == "Bez záměru · hodnoceno nízko" && it.isLowValueTrap })
        assertTrue(s.valueEntries.any { it.channelOrFormat == "Bez záměru · ostatní" })
        // pasti nahoře
        assertEquals(s.valueEntries.sortedByDescending { it.isLowValueTrap }.map { it.isLowValueTrap }, s.valueEntries.map { it.isLowValueTrap })
    }

    @Test
    fun dataWithoutRatingsShowsEmptyValueMap() {
        val h = History(
            days = listOf(HistoryDay(start, hasData = true)),
            interventions = listOf(HistoryIntervention(0, "tiktok", "CLOSED", null)),
        )
        val s = MirrorStats.applyHistory(base, h)
        assertTrue(s.valueEntries.isEmpty())
        assertEquals(0, s.bossSurvivedDays)
    }

    @Test
    fun valueMapLimit() {
        val many = ("abcdefghijklmnopqrst").map { HistoryRating(it.code.toLong(), "Video $it", "záměr $it", 4) }
        assertEquals(6, MirrorStats.valueEntries(many).size)
    }
}
