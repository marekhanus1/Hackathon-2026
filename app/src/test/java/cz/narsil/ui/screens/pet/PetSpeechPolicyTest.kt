package cz.narsil.ui.screens.pet

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetSpeechPolicyTest {

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
    fun `allows at most two unprompted messages per day and resets next day`() {
        val policy = PetSpeechPolicy(InMemoryPetMemoryStore(), maxUnsolicitedPerDay = 2)
        val day1 = 20_000L
        val day2 = 20_001L

        assertTrue(policy.canSpeakUnprompted(day1))
        policy.markSpoken(day1)
        assertTrue(policy.canSpeakUnprompted(day1))
        policy.markSpoken(day1)
        assertFalse(policy.canSpeakUnprompted(day1))

        assertTrue(policy.canSpeakUnprompted(day2))
    }

    @Test
    fun `muteUntilTomorrow applies only to that day`() {
        val policy = PetSpeechPolicy(InMemoryPetMemoryStore(), maxUnsolicitedPerDay = 2)
        val day1 = 20_000L
        val day2 = 20_001L

        policy.muteUntilTomorrow(day1)
        assertTrue(policy.isMuted(day1))
        assertFalse(policy.canSpeakUnprompted(day1))

        assertFalse(policy.isMuted(day2))
        assertTrue(policy.canSpeakUnprompted(day2))
    }

    @Test
    fun `petDayKey shifts day boundary to 4am`() {
        val zone = ZoneId.of("Europe/Prague")
        val before4am = LocalDateTime.of(2026, 11, 28, 3, 30).atZone(zone).toInstant().toEpochMilli()
        val after4am = LocalDateTime.of(2026, 11, 28, 4, 5).atZone(zone).toInstant().toEpochMilli()

        assertEquals(petDayKey(before4am, zone) + 1L, petDayKey(after4am, zone))
    }
}
