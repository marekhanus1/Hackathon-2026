package cz.narsil.movement.squat

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    /**
     * Syntetický člověk čelem ke kameře: rameno nahoře, kyčel klesá ke kolenu, kolena a kotníky na místě.
     * [depth] 0 = stojí, 1 = stehna vodorovně. Úhel v koleni se v 2D skoro nemění (nohy rovně pod sebou).
     */
    private fun frontal(depth: Double, scale: Float = 1f, noise: Float = 0f, rnd: Random = Random(1)): PoseFrame {
        fun n() = if (noise == 0f) 0f else (rnd.nextFloat() - 0.5f) * 2 * noise
        val kneeY = 600f * scale
        val thigh = 200f * scale * (1 - depth.toFloat() * 0.85f)
        val hipY = kneeY - thigh
        val shoulderY = hipY - 220f * scale
        fun j(x: Float, y: Float) = Joint(x * scale + n(), y + n(), 0.9f)
        return PoseFrame(
            leftShoulder = j(180f, shoulderY), rightShoulder = j(300f, shoulderY),
            leftHip = j(200f, hipY), rightHip = j(280f, hipY),
            leftKnee = j(200f, kneeY), rightKnee = j(280f, kneeY),
            leftAnkle = j(200f, kneeY + 200f * scale), rightAnkle = j(280f, kneeY + 200f * scale),
        )
    }

    private fun SquatRepCounter.feedFrames(depths: List<Double>, start: Long = 0, step: Long = 100, scale: Float = 1f, noise: Float = 0f): Long {
        val rnd = Random(42)
        var t = start
        for (d in depths) {
            onFrame(frontal(d, scale, noise, rnd), t)
            t += step
        }
        return t
    }

    private val frontalSquat = List(5) { 0.0 } + List(6) { 0.9 } + List(5) { 0.0 }

    @Test
    fun angleAtKnee() {
        assertEquals(180.0, SquatRepCounter.angleAt(Joint(0f, 0f), Joint(0f, 1f), Joint(0f, 2f)), 0.01)
        assertEquals(90.0, SquatRepCounter.angleAt(Joint(0f, 0f), Joint(0f, 1f), Joint(1f, 1f)), 0.01)
    }

    @Test
    fun countsFullSquatsFromSide() {
        val counter = SquatRepCounter()
        var t = 0L
        repeat(20) { t = counter.feed(oneSquat, t) }
        assertEquals(20, counter.count)
    }

    @Test
    fun tolerantThresholdCountsShallowerSquat() {
        // 120° v koleni: tolerantní prahy (231) počítají, přísné (210) ne.
        val shallow = List(5) { 172.0 } + List(6) { 118.0 } + List(5) { 172.0 }
        assertEquals(1, SquatRepCounter().apply { feed(shallow) }.count)
        assertEquals(0, SquatRepCounter(SquatThresholds.STRICT).apply { feed(shallow) }.count)
    }

    @Test
    fun halfSquatDoesNotCount() {
        val counter = SquatRepCounter()
        counter.feed(List(5) { 172.0 } + List(6) { 140.0 } + List(5) { 172.0 })
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
    fun frontalViewCountsByThigh() {
        // Čelem ke kameře je úhel v koleni pořád ~180°, počítá se ze zkrácení stehna.
        val counter = SquatRepCounter()
        var t = 0L
        repeat(20) { t = counter.feedFrames(frontalSquat, t) }
        assertEquals(20, counter.count)
        assertTrue(counter.lastAngle!! > 170)
    }

    @Test
    fun frontalViewWithNoiseAndDifferentDistance() {
        // Tři „lidé“: blízko, daleko, se šumem detekce ±8 px – vždy 20 z 20.
        for ((scale, noise) in listOf(1.3f to 0f, 0.6f to 0f, 1f to 8f)) {
            val counter = SquatRepCounter()
            var t = 0L
            repeat(20) { t = counter.feedFrames(frontalSquat, t, scale = scale, noise = noise) }
            assertEquals("scale=$scale noise=$noise", 20, counter.count)
        }
    }

    @Test
    fun frontalShallowBobDoesNotCount() {
        val counter = SquatRepCounter()
        counter.feedFrames(List(5) { 0.0 } + List(6) { 0.3 } + List(5) { 0.0 })
        assertEquals(0, counter.count)
    }

    @Test
    fun countsWithoutAnklesInFrame() {
        // Kotníky mimo záběr (telefon blízko): úhel nejde, stehno ano.
        val counter = SquatRepCounter()
        var t = 0L
        repeat(5) {
            for (d in frontalSquat) {
                counter.onFrame(frontal(d).copy(leftAnkle = null, rightAnkle = null), t)
                t += 100
            }
        }
        assertEquals(5, counter.count)
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
    fun resetClearsCountAndCalibration() {
        val counter = SquatRepCounter()
        counter.feedFrames(frontalSquat)
        counter.reset()
        assertEquals(0, counter.count)
        assertNull(counter.standingThighRatio)
    }
}
