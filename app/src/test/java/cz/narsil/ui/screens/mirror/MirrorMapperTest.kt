package cz.narsil.ui.screens.mirror

import cz.narsil.core.History
import cz.narsil.core.HistoryDay
import cz.narsil.core.HistoryIntervention
import cz.narsil.core.HistoryRating
import cz.narsil.core.fake.FakeHistoryRepository
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MirrorMapperTest {
    private val start = LocalDate.of(2026, 11, 8)
    private fun history(): History {
        val days = (0 until 21).map { i ->
            HistoryDay(
                date = start.plusDays(i.toLong()),
                feedMinutes = if (i < 7) 100 else if (i < 14) 70 else 40,
                reclaimedMinutes = 30,
                closes = 2,
                squats = 20,
                bossSurvived = i % 3 == 0,
                hasData = true,
            )
        }
        val at21 = start.atTime(21, 15).toInstant(ZoneOffset.UTC).toEpochMilli()
        val at19 = start.atTime(19, 5).toInstant(ZoneOffset.UTC).toEpochMilli()
        return History(
            days = days,
            interventions = listOf(at21, at21, at19).map { HistoryIntervention(it, "tiktok", "CLOSED", null) },
            ratings = listOf(
                HistoryRating(1, "Když je pondělí #312", null, 2),
                HistoryRating(2, "Když je pondělí #318", null, 1),
                HistoryRating(3, "Ownership za 20 minut", "pochopit borrow checker", 5),
                HistoryRating(4, "Kdo lže?", null, 1),
                HistoryRating(5, "Kočka vs. okurka", null, 3),
            ),
        )
    }

    @Test
    fun mapsHistory() {
        val s = MirrorMapper.map(history(), zone = ZoneOffset.UTC)
        assertTrue(s.isLoaded)
        assertEquals(21 * 30, s.totalReclaimedMinutes)
        assertEquals(60, s.feedDropPercent) // 100 → 40
        assertEquals(20, s.averageDailySquats)
        assertEquals("21:00 – 22:00", s.criticalBossHour)
        assertEquals(7, s.bossSurvivedDays)
        assertEquals(listOf("Týden 1", "Týden 2", "Týden 3"), s.weeklyTrend.map { it.dayLabel })
        assertEquals(700, s.weeklyTrend.first().feedMinutes)
        assertEquals("Za 21 dní sis vzal zpět 10 h 30 min", s.reclaimedHeadline)
    }

    @Test
    fun valueMapGroupsAndTraps() {
        val entries = MirrorMapper.valueEntries(history().ratings)
        assertEquals("Když je pondělí", entries.first().channelOrFormat)
        assertEquals(2, entries.first().videoCount)
        assertTrue(entries.first().isLowValueTrap)
        assertTrue(entries.any { it.channelOrFormat == "Se záměrem: pochopit borrow checker" && !it.isLowValueTrap })
        assertTrue(entries.any { it.channelOrFormat == "Bez záměru · hodnoceno nízko" && it.isLowValueTrap })
        assertTrue(entries.any { it.channelOrFormat == "Bez záměru · ostatní" })
        assertTrue(MirrorMapper.valueEntries(emptyList()).isEmpty())
    }

    @Test
    fun emptyHistory() {
        val s = MirrorMapper.map(History())
        assertEquals(0, s.daysWithData)
        assertEquals(0, s.totalReclaimedMinutes)
        assertEquals("Zrcadlo se naplní po prvních dnech s Narsilem", s.reclaimedHeadline)
        assertTrue(s.valueEntries.isEmpty())
        // dny před instalací se nepočítají
        val partial = History(days = listOf(HistoryDay(start), HistoryDay(start.plusDays(1), feedMinutes = 50, hasData = true)))
        assertEquals(1, MirrorMapper.map(partial).daysWithData)
        assertFalse(MirrorMapper.map(partial).trendLine.contains("kleslo"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun viewModelLoadsFromRepository() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val vm = MirrorViewModel(FakeHistoryRepository(history()))
            vm.selectTab(MirrorTab.VALUE_MAP)
            vm.refresh()
            assertTrue(vm.uiState.value.isLoaded)
            assertEquals(MirrorTab.VALUE_MAP, vm.uiState.value.activeTab)
            assertEquals(21 * 30, vm.uiState.value.totalReclaimedMinutes)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
