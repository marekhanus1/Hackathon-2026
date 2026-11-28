package cz.narsil.ui.screens.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetRateLimiterTest {

    private class InMemoryPetMemoryStore : PetMemoryStore {
        private val ints = mutableMapOf<String, Int>()
        private val longs = mutableMapOf<String, Long>()

        override fun getInt(key: String, def: Int): Int = ints[key] ?: def
        override fun putInt(key: String, v: Int) {
            ints[key] = v
        }

        override fun getLong(key: String, def: Long): Long = longs[key] ?: def
        override fun putLong(key: String, v: Long) {
            longs[key] = v
        }
    }

    @Test
    fun `enforces minimum interval between api calls`() {
        val limiter = PetRateLimiter(InMemoryPetMemoryStore(), minIntervalMillis = 6_000L)
        val day = 20_000L

        assertTrue(limiter.canCallApi(10_000L, day))
        limiter.recordCall(10_000L, day)

        assertFalse(limiter.canCallApi(15_999L, day))
        assertTrue(limiter.canCallApi(16_000L, day))
    }

    @Test
    fun `enforces daily cap and resets on next day`() {
        val limiter = PetRateLimiter(
            InMemoryPetMemoryStore(),
            minIntervalMillis = 0L,
            maxDailyCalls = 2,
        )
        val day1 = 20_000L
        val day2 = 20_001L

        assertEquals(2, limiter.remainingToday(day1))
        assertTrue(limiter.canCallApi(1_000L, day1))
        limiter.recordCall(1_000L, day1)
        assertEquals(1, limiter.remainingToday(day1))

        assertTrue(limiter.canCallApi(2_000L, day1))
        limiter.recordCall(2_000L, day1)
        assertEquals(0, limiter.remainingToday(day1))
        assertFalse(limiter.canCallApi(3_000L, day1))

        assertEquals(2, limiter.remainingToday(day2))
        assertTrue(limiter.canCallApi(4_000L, day2))
    }

    @Test
    fun `applies 60s cooldown after HTTP 429 rate limit`() {
        val limiter = PetRateLimiter(
            InMemoryPetMemoryStore(),
            minIntervalMillis = 0L,
            rateLimitCooldownMillis = 60_000L,
        )
        val day = 20_000L

        limiter.recordError(nowMillis = 10_000L, isRateLimit = true)
        assertFalse(limiter.canCallApi(69_999L, day))
        assertTrue(limiter.canCallApi(70_000L, day))
    }

    @Test
    fun `applies 20s cooldown after transient api error`() {
        val limiter = PetRateLimiter(
            InMemoryPetMemoryStore(),
            minIntervalMillis = 0L,
            errorCooldownMillis = 20_000L,
        )
        val day = 20_000L

        limiter.recordError(nowMillis = 10_000L, isRateLimit = false)
        assertFalse(limiter.canCallApi(29_999L, day))
        assertTrue(limiter.canCallApi(30_000L, day))
    }
}
