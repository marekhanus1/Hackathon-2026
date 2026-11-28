package cz.narsil.movement.pushup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushupRepCounterTest {

    private fun settleFlat(counter: PushupRepCounter, startMillis: Long = 0L): Long {
        var t = startMillis
        repeat(6) {
            counter.onGyroscope(0f, 0f, 0f, t)
            counter.onAccelerometer(0f, 0f, 9.81f, t)
            t += 100L
        }
        return t
    }

    private fun feedPushups(
        counter: PushupRepCounter,
        reps: Int,
        startMillis: Long,
        downFrame: PushupFrame,
        upFrame: PushupFrame,
    ): Long {
        var t = startMillis
        repeat(reps) {
            repeat(5) {
                counter.onAccelerometer(0f, 0f, 9.81f, t)
                counter.onFrame(upFrame, t)
                t += 100L
            }
            repeat(5) {
                counter.onAccelerometer(0f, 0f, 9.81f, t)
                counter.onFrame(downFrame, t)
                t += 100L
            }
            repeat(5) {
                counter.onAccelerometer(0f, 0f, 9.81f, t)
                counter.onFrame(upFrame, t)
                t += 100L
            }
        }
        return t
    }

    @Test
    fun countsPushupsByUpperBodySpan() {
        val counter = PushupRepCounter()
        val t0 = settleFlat(counter)
        val up = PushupFrame(upperBodySpan = 120.0, averageLuma = 140.0, personVisible = true)
        val down = PushupFrame(upperBodySpan = 210.0, averageLuma = 140.0, personVisible = true)
        feedPushups(counter, reps = 10, startMillis = t0, downFrame = down, upFrame = up)
        assertEquals(10, counter.count)
    }

    @Test
    fun countsPushupsByProximityAndLumaOcclusion() {
        val counter = PushupRepCounter()
        val t0 = settleFlat(counter)
        val up = PushupFrame(upperBodySpan = 120.0, averageLuma = 150.0, proximityNear = false, personVisible = true)
        val down = PushupFrame(upperBodySpan = null, averageLuma = 20.0, proximityNear = true, personVisible = false)
        feedPushups(counter, reps = 10, startMillis = t0, downFrame = down, upFrame = up)
        assertEquals(10, counter.count)
    }

    @Test
    fun countsHybridPushupsWithOnlyLumaDrop() {
        val counter = PushupRepCounter()
        val t0 = settleFlat(counter)
        val up = PushupFrame(averageLuma = 160.0, personVisible = true)
        val down = PushupFrame(averageLuma = 55.0, personVisible = true)
        feedPushups(counter, reps = 5, startMillis = t0, downFrame = down, upFrame = up)
        assertEquals(5, counter.count)
    }

    @Test
    fun verticalPhoneDoesNotCount() {
        // Uživatel drží telefon svisle před sebou (az ≈ 0, ay ≈ 9.81) -> anti-cheat blokuje.
        val counter = PushupRepCounter()
        var t = 0L
        repeat(10) {
            counter.onAccelerometer(0f, 9.81f, 0f, t)
            counter.onFrame(PushupFrame(proximityNear = true, personVisible = true), t)
            t += 100L
            counter.onAccelerometer(0f, 9.81f, 0f, t)
            counter.onFrame(PushupFrame(proximityNear = false, personVisible = true), t)
            t += 100L
        }
        assertEquals(PhonePlacement.NOT_FLAT, counter.placement)
        assertEquals(0, counter.count)
    }

    @Test
    fun faceDownPhoneLyingOnBackDoesNotCount() {
        val counter = PushupRepCounter()
        var t = 0L
        repeat(8) {
            counter.onAccelerometer(0f, 0f, -9.81f, t)
            counter.onFrame(PushupFrame(proximityNear = true, personVisible = true), t)
            t += 100L
        }
        assertEquals(PhonePlacement.FACE_DOWN, counter.placement)
        assertEquals(0, counter.count)
    }

    @Test
    fun movingPhoneByHandToChestCancelsRep() {
        // Leží na zádech na zemi, ale telefon si přisune rukou k hrudi (gyroskop + akcelerometr).
        val counter = PushupRepCounter()
        var t = settleFlat(counter)
        val up = PushupFrame(upperBodySpan = 120.0, averageLuma = 150.0, personVisible = true)
        val down = PushupFrame(upperBodySpan = 220.0, averageLuma = 30.0, proximityNear = true, personVisible = true)

        repeat(6) {
            counter.onFrame(up, t)
            t += 100L
        }
        // Pohyb telefonu rukou při přitažení k hrudi -> zruší probíhající opakování
        counter.onGyroscope(1.6f, 0.3f, 0.1f, t)
        counter.onAccelerometer(0.2f, 0.1f, 12.6f, t)
        counter.onFrame(down, t)
        t += 100L
        counter.onGyroscope(0f, 0f, 0f, t)
        counter.onAccelerometer(0f, 0f, 9.81f, t)
        repeat(5) {
            counter.onFrame(up, t)
            t += 100L
        }
        assertEquals(0, counter.count)
    }

    @Test
    fun emptyTableHandWaveWithoutPersonDoesNotCount() {
        // Telefon leží na stole, uživatel nad ním jen mává rukou a nikdo není před kamerou.
        val counter = PushupRepCounter()
        val t0 = settleFlat(counter)
        val up = PushupFrame(averageLuma = 160.0, proximityNear = false, personVisible = false)
        val down = PushupFrame(averageLuma = 20.0, proximityNear = true, personVisible = false)
        feedPushups(counter, reps = 5, startMillis = t0, downFrame = down, upFrame = up)
        assertEquals(0, counter.count)
    }

    @Test
    fun shallowBobDoesNotCount() {
        val counter = PushupRepCounter()
        val t0 = settleFlat(counter)
        val up = PushupFrame(upperBodySpan = 120.0, averageLuma = 150.0, personVisible = true)
        val shallow = PushupFrame(upperBodySpan = 135.0, averageLuma = 138.0, personVisible = true)
        feedPushups(counter, reps = 5, startMillis = t0, downFrame = shallow, upFrame = up)
        assertEquals(0, counter.count)
    }

    @Test
    fun resetClearsStateAndCalibration() {
        val counter = PushupRepCounter()
        val t0 = settleFlat(counter)
        val up = PushupFrame(upperBodySpan = 120.0, averageLuma = 150.0, personVisible = true)
        val down = PushupFrame(upperBodySpan = 210.0, averageLuma = 40.0, proximityNear = true, personVisible = true)
        feedPushups(counter, reps = 3, startMillis = t0, downFrame = down, upFrame = up)
        counter.reset()
        assertEquals(0, counter.count)
        assertNull(counter.standingSpan)
        assertNull(counter.standingLuma)
    }
}
