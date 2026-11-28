package cz.narsil.app

import cz.narsil.core.ComfortContext
import cz.narsil.core.Surface
import cz.narsil.detection.accessibility.SavedSession
import cz.narsil.detection.accessibility.ScrollSessionTracker
import cz.narsil.ui.screens.onboarding.OnboardingUiState
import cz.narsil.ui.screens.onboarding.PermissionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Přežití na Xiaomi a jednodušší průvodce: data z Redmi Note 11 (11/2026). */
class SurvivalTest {
    private val min = 60_000L

    @Test
    fun systemKillsFromRedmiLog() {
        // dumpsys activity exit-info cz.narsil na Redmi Note 11:
        assertTrue(KillWatcher.isSystemKill(13, "OneKeyClean")) // Vyčistit vše
        assertTrue(KillWatcher.isSystemKill(10, "stop cz.narsil due to AutoIdleKill")) // úsporný režim MIUI
        assertTrue(KillWatcher.isSystemKill(3, null)) // málo paměti
        // Ne: force-stop z Nastavení, pád, ANR, aktualizace aplikace.
        assertFalse(KillWatcher.isSystemKill(10, "stop cz.narsil due to from pid 1234"))
        assertFalse(KillWatcher.isSystemKill(4, null))
        assertFalse(KillWatcher.isSystemKill(6, null))
        assertFalse(KillWatcher.isSystemKill(16, null))
    }

    @Test
    fun notifyOnlyOnNewKillAndRarely() {
        val now = 100 * 60 * min
        assertTrue(KillWatcher.shouldNotify(true, exitAt = now - min, lastSeenExit = 0, lastNotice = 0, now = now, onboardingDone = true))
        assertFalse(KillWatcher.shouldNotify(true, now - min, lastSeenExit = now - min, lastNotice = 0, now = now, onboardingDone = true)) // stejné zabití
        assertFalse(KillWatcher.shouldNotify(true, now - min, 0, lastNotice = now - 2 * 60 * min, now = now, onboardingDone = true)) // před 2 h
        assertFalse(KillWatcher.shouldNotify(true, now - min, 0, 0, now, onboardingDone = false)) // ještě průvodce
        assertFalse(KillWatcher.shouldNotify(false, now - min, 0, 0, now, onboardingDone = true))
    }

    @Test
    fun sessionSurvivesProcessDeath() {
        val t = ScrollSessionTracker()
        for (m in 0L..3L) t.observe("com.instagram.android", Surface.REELS, ComfortContext.HOME, m * min, 5 * min, scrolled = true)
        val saved = t.snapshot()
        assertNotNull(saved)
        assertEquals(0L, saved!!.startedAtMillis)

        // MIUI zabije proces, služba se vrátí za 70 s: relace pokračuje a Ozvěna přijde v 5. minutě, ne v 8.
        val restarted = ScrollSessionTracker()
        restarted.restore(saved, 3 * min + 70_000)
        assertNull(restarted.observe("com.instagram.android", Surface.REELS, ComfortContext.HOME, 4 * min, 5 * min, scrolled = true))
        assertNotNull(restarted.observe("com.instagram.android", Surface.REELS, ComfortContext.HOME, 5 * min, 5 * min, scrolled = true))
    }

    @Test
    fun oldSessionIsNotRestoredButPaidTimeIs() {
        val saved = SavedSession(0, "com.instagram.android", Surface.REELS, lastEndlessAtMillis = min, unlockedUntilMillis = 20 * min)
        val t = ScrollSessionTracker()
        t.restore(saved, 10 * min) // pauza > 2 min: relace by stejně skončila
        assertNull(t.session)
        // Zaplacený čas platí: v 15. minutě bez zásahu i po dlouhém scrollu.
        t.observe("com.instagram.android", Surface.REELS, ComfortContext.HOME, 10 * min, 20_000, scrolled = true)
        assertNull(t.observe("com.instagram.android", Surface.REELS, ComfortContext.HOME, 15 * min, 20_000, scrolled = true))
    }

    @Test
    fun onboardingNeedsOnlyAccessibilityAndWifi() {
        val base = OnboardingUiState(detectedWifiSsid = "Domov", homeWifiSsid = "Domov")
        assertFalse(base.permissions.any { it.type.name == "OVERLAY" }) // překrytí oken už není krok
        val onlyRequired = base.copy(
            permissions = base.permissions.map {
                it.copy(isGranted = it.type == PermissionType.ACCESSIBILITY || it.type == PermissionType.LOCATION_FOR_WIFI)
            },
        )
        assertTrue(onlyRequired.allRequiredGranted) // kamera a kroky nepovinné
        assertEquals(listOf(PermissionType.ACCESSIBILITY, PermissionType.LOCATION_FOR_WIFI), base.permissions.filter { it.required }.map { it.type })
        assertFalse(onlyRequired.copy(homeWifiSsid = null).allRequiredGranted) // domácí Wi-Fi zůstává povinná
    }
}
