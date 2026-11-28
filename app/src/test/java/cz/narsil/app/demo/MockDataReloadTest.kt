package cz.narsil.app.demo

import cz.narsil.gamification.GamificationHistory
import cz.narsil.gamification.GamificationStore
import cz.narsil.gamification.RulesGamificationEngine
import cz.narsil.gamification.TestClock
import cz.narsil.gamification.XpRecord
import cz.narsil.gamification.XpSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 147: nahrání mock dat smaže historii a gamifikace se přepočítá bez restartu – vždy stejný výsledek. */
class MockDataReloadTest {
    private class SwapStore : GamificationStore {
        var history = GamificationHistory()
        override suspend fun load() = history
        override suspend fun append(record: XpRecord) { history = history.copy(records = history.records + record) }
        override suspend fun unlockBadge(id: String, atMillis: Long) { history = history.copy(badges = history.badges + (id to atMillis)) }
    }

    @Test
    fun clearsEverythingButCurrentContracts() {
        val sql = MockDataLoader.clearStatements(LocalDate.of(2026, 12, 5))
        listOf("xp_events", "ledger", "interventions", "consumption_events", "video_ratings", "badges").forEach { t ->
            assertTrue(t, sql.contains("DELETE FROM $t"))
        }
        assertTrue(sql.contains("DELETE FROM contracts WHERE date < '2026-12-05'"))
        assertTrue(sql.contains("DELETE FROM plan_tasks WHERE date < '2026-12-05'"))
    }

    @Test
    fun reloadReplacesOldHistory() = runTest {
        val today = LocalDate.of(2026, 12, 5)
        val clock = TestClock(LocalDateTime.of(today, LocalTime.of(10, 0)))
        val store = SwapStore()
        val engine = RulesGamificationEngine(store = store, clock = clock)
        // Stará data z testu na telefonu: reset dne, DEMO XP, dnešní zásah.
        engine.resetToday()
        engine.addXp(480)
        engine.refresh()

        // Mock data po smazání historie (jako MockDataLoader.load).
        store.history = GamificationHistory(
            MockDataLoader.rows(today, ZoneOffset.UTC).xp.map { XpRecord(LocalDate.parse(it.date), it.atMillis, it.source, it.xp) },
        )
        engine.reload()
        val s = engine.state.value
        assertEquals(480, s.xpInLevel)
        assertEquals(4, s.streakDays)
        assertEquals(1, s.jokers)
        assertEquals(cz.narsil.core.AvatarState.FIT, s.avatar)
        assertTrue(store.history.records.none { it.source == XpSource.DEMO || it.source == XpSource.DAY_RESET })
    }
}
