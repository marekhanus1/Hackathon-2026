package cz.narsil.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 148: puls při 50 % a 80 %, štítek se zbývajícím časem v posledních 20 %. */
class ScrollProgressStyleTest {
    @Test
    fun pulsesOnlyWhenCrossingUp() {
        assertTrue(ScrollProgressStyle.crossedPulse(49, 50))
        assertTrue(ScrollProgressStyle.crossedPulse(45, 55))
        assertTrue(ScrollProgressStyle.crossedPulse(79, 80))
        assertFalse(ScrollProgressStyle.crossedPulse(50, 51))
        assertFalse(ScrollProgressStyle.crossedPulse(81, 79)) // dolů ne
        assertFalse(ScrollProgressStyle.crossedPulse(10, 20))
        assertTrue(ScrollProgressStyle.crossedPulse(null, 60)) // overlay při prvním zobrazení nepulsuje (previous == null)
    }

    @Test
    fun labelInLastTwentyPercent() {
        assertFalse(ScrollProgressStyle.showLabel(79))
        assertTrue(ScrollProgressStyle.showLabel(80))
        assertTrue(ScrollProgressStyle.showLabel(100))
    }

    @Test
    fun remainingTimeAndFormat() {
        assertEquals(15_000L, ScrollProgressStyle.remainingMillis(85, 100_000))
        assertEquals(0L, ScrollProgressStyle.remainingMillis(100, 100_000))
        assertEquals(120_000L, ScrollProgressStyle.remainingMillis(80, 600_000)) // 10 min práh
        assertEquals(4_000L, ScrollProgressStyle.remainingMillis(80, 20_000)) // demo 20 s
        assertEquals("0:15", ScrollProgressStyle.format(15_000))
        assertEquals("0:15", ScrollProgressStyle.format(14_200)) // sekundy nahoru
        assertEquals("2:00", ScrollProgressStyle.format(120_000))
        assertEquals("0:00", ScrollProgressStyle.format(-50))
        assertEquals("Ozvěna za 1:05", ScrollProgressStyle.label(65_000))
    }
}
