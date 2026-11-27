package cz.narsil.movement.squat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SquatRepCounterTest {
    private fun SquatRepCounter.feed(angles: List<Double>, startMillis: Long = 0, stepMillis: Long = 100): Long {
        var t = startMillis
        for (a in angles) {
            onAngle(a, t)
            t += stepMillis
        }
        return t
    }

    private val oneSquat = List(5) { 172.0 } + List(5) { 85.0 } + List(5) { 172.0 }

    @Test
    fun angleAtKnee() {
        val straight = SquatRepCounter.angleAt(Joint(0f, 0f), Joint(0f, 1f), Joint(0f, 2f))
        assertEquals(180.0, straight, 0.01)
        val right = SquatRepCounter.angleAt(Joint(0f, 0f), Joint(0f, 1f), Joint(1f, 1f))
        assertEquals(90.0, right, 0.01)
    }

    @Test
    fun countsFullSquats() {
        val counter = SquatRepCounter()
        var t = 0L
        repeat(20) { t = counter.feed(oneSquat, t) }
        assertEquals(20, counter.count)
    }

    @Test
    fun halfSquatDoesNotCount() {
        val counter = SquatRepCounter()
        counter.feed(List(5) { 172.0 } + List(5) { 120.0 } + List(5) { 172.0 })
        assertEquals(0, counter.count)
    }

    @Test
    fun singleFrameGlitchIsIgnored() {
        val counter = SquatRepCounter(SquatThresholds(smoothing = 1.0))
        counter.feed(List(5) { 172.0 } + listOf(80.0) + List(5) { 172.0 })
        assertEquals(0, counter.count)
    }

    @Test
    fun tooFastRepIsIgnored() {
        val counter = SquatRepCounter(SquatThresholds(smoothing = 1.0, minRepMillis = 600))
        // dva dřepy během 400 ms → druhý se nepočítá
        counter.feed(List(2) { 172.0 } + List(2) { 85.0 } + List(2) { 172.0 } + List(2) { 85.0 } + List(2) { 172.0 }, stepMillis = 40)
        assertEquals(1, counter.count)
    }

    @Test
    fun startingDownCountsOnlyAfterStandingUp() {
        val counter = SquatRepCounter()
        counter.feed(List(5) { 85.0 } + List(5) { 172.0 })
        assertEquals(1, counter.count)
    }

    @Test
    fun legsWithLowLikelihoodAreIgnored() {
        val counter = SquatRepCounter()
        val hidden = Leg(Joint(0f, 0f, 0.1f), Joint(0f, 1f, 0.1f), Joint(0f, 2f, 0.1f))
        assertNull(counter.kneeAngle(hidden, null))
        val visible = Leg(Joint(0f, 0f), Joint(0f, 1f), Joint(1f, 1f))
        assertEquals(90.0, counter.kneeAngle(hidden, visible)!!, 0.01)
    }

    @Test
    fun resetClearsCount() {
        val counter = SquatRepCounter()
        counter.feed(oneSquat)
        counter.reset()
        assertEquals(0, counter.count)
    }
}
