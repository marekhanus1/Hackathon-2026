package cz.narsil.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollProgressColorsTest {
    @Test
    fun endpointsAndClamp() {
        assertEquals(0x5926A69A, ScrollProgressColors.color(0))
        assertEquals(0xFFE53935.toInt(), ScrollProgressColors.color(100))
        assertEquals(ScrollProgressColors.color(0), ScrollProgressColors.color(-5))
        assertEquals(ScrollProgressColors.color(100), ScrollProgressColors.color(150))
        assertEquals(0xA6FFB300.toInt(), ScrollProgressColors.color(60))
    }

    @Test
    fun opacityGrowsTowardsLimit() {
        var prev = -1
        for (p in 0..100) {
            val a = ScrollProgressColors.alpha(ScrollProgressColors.color(p))
            assertTrue("krytí klesá v $p %", a >= prev)
            prev = a
        }
        assertEquals(255, prev)
    }

    @Test
    fun lerpMidpoint() {
        assertEquals(0x80808080.toInt(), ScrollProgressColors.lerp(0x00000000, 0xFFFFFFFF.toInt(), 0.5f))
    }
}
