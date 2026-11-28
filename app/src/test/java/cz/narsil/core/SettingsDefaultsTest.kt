package cz.narsil.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 133: výchozí nastavení = čísla ze SPEC, aby ho uživatel nemusel měnit. */
class SettingsDefaultsTest {
    private val d = Settings()

    @Test
    fun defaultsMatchSpec() {
        assertEquals(5, d.toleranceMinutes) // SPEC 3: 5 min
        assertEquals(5 * 60_000L, d.toleranceMillis())
        assertEquals(3, d.videoChainLimit) // SPEC 3: 3. video
        assertEquals(20, d.squatsPerUnlock) // SPEC 4: 20 dřepů = 10 min
        assertEquals(MovementRates.PUSHUPS_PER_UNLOCK, d.pushupsPerUnlock)
        assertEquals(30, d.debtLimitMinutes) // SPEC 5: limit dluhu 30 min
        assertEquals(TrackedApp.entries.toSet(), d.trackedApps)
        assertTrue(d.partnerAlerts)
        assertTrue(d.showScrollProgress)
        assertEquals(null, d.protectionPausedUntilMillis)
        assertEquals(LlmProvider.GEMINI, d.llmProvider)
        assertEquals(Exercise.SQUATS, d.exercise)
        assertFalse(d.demoMode)
    }

    @Test
    fun defaultsAreAmongOptions() {
        assertTrue(d.toleranceMinutes in SettingsDefaults.TOLERANCE_OPTIONS)
        assertTrue(d.videoChainLimit in SettingsDefaults.VIDEO_CHAIN_OPTIONS)
        assertTrue(d.squatsPerUnlock in SettingsDefaults.SQUATS_OPTIONS)
        assertTrue(d.pushupsPerUnlock in SettingsDefaults.PUSHUPS_OPTIONS)
        assertTrue(d.debtLimitMinutes in SettingsDefaults.DEBT_LIMIT_OPTIONS)
    }

    @Test
    fun demoThresholdWinsOverTolerance() {
        assertEquals(20_000L, d.copy(demoThresholdSec = 20, toleranceMinutes = 15).toleranceMillis())
        assertEquals(10 * 60_000L, d.copy(toleranceMinutes = 10).toleranceMillis())
    }

    @Test
    fun protectionPauseAndHelpers() {
        assertTrue(d.isProtectionActive(1_000))
        val paused = d.copy(protectionPausedUntilMillis = 5_000)
        assertFalse(paused.isProtectionActive(4_999))
        assertTrue(paused.isProtectionActive(5_000))
        assertTrue(d.isTracked("TIKTOK"))
        assertFalse(d.copy(trackedApps = setOf(TrackedApp.YOUTUBE)).isTracked("TIKTOK"))
        assertFalse(d.isTracked("WHATSAPP"))
        assertEquals(15, d.copy(squatsPerUnlock = 15).repsPerUnlock())
        assertEquals(5, d.copy(exercise = Exercise.PUSHUPS, pushupsPerUnlock = 5).repsPerUnlock())
    }

    /** Názvy musí sedět na detekci, jinak by [Settings.isTracked] nic nenašlo. */
    @Test
    fun trackedAppNamesMatchDetection() {
        assertEquals(
            cz.narsil.detection.accessibility.WatchedApp.entries.map { it.name }.toSet() - cz.narsil.core.OTHER_APPS, // OTHER má vlastní přepínač (149)
            TrackedApp.entries.map { it.name }.toSet(),
        )
    }
}
