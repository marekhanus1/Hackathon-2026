package cz.narsil.core.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryMathTest {
    private val date = LocalDate.of(2026, 11, 28)
    private fun feed(min: Int, surface: String = "FEED", planned: Boolean = false, context: String = "HOME") =
        ConsumptionEventEntity(packageName = "p", surface = surface, startedAtMillis = 0, durationMillis = min * 60_000L, planned = planned, context = context)

    @Test
    fun dayFromRows() {
        val d = HistoryMath.day(
            date,
            xp = listOf("CLOSED" to 50, "CLOSED" to 50, "SQUATS" to 20, "BORROWED" to 0, "JOKER" to 0, "CONTRACT" to 100, "BOSS" to 150),
            consumption = listOf(feed(30), feed(10, "REELS"), feed(20, planned = true), feed(15, "MESSAGES"), feed(40, context = "AWAY")),
            taskRewardMinutes = 20,
        )
        assertEquals(40, d.feedMinutes) // jen neplánovaný nekonečný obsah doma
        assertEquals(2 * 15 + 20, d.reclaimedMinutes)
        assertEquals(2, d.closes)
        assertEquals(20 + 50, d.squats)
        assertEquals(1, d.borrows)
        assertTrue(d.contractFulfilled && d.bossSurvived && d.hasData)
        assertTrue(d.battleWon)
    }

    @Test
    fun withoutConsumptionFeedFallsBackToUnlocks() {
        val d = HistoryMath.day(date, listOf("PAID_STEPS" to 10, "SQUATS" to 20), emptyList(), 0)
        assertEquals(20, d.feedMinutes)
        val empty = HistoryMath.day(date, emptyList(), emptyList(), 0)
        assertFalse(empty.hasData)
    }

    @Test
    fun dayResetDropsEarlierEvents() {
        val d = HistoryMath.day(date, listOf("CLOSED" to 50, "DAY_RESET" to 0, "SQUATS" to 20), emptyList(), 0)
        assertEquals(0, d.closes)
        assertEquals(20, d.squats)
    }
}
