package cz.narsil.ui.screens.settings

import cz.narsil.core.Settings
import cz.narsil.core.TrackedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 149: další aplikace a nové sítě v Nastavení. */
class SettingsGuardOtherAppsTest {
    @Test
    fun otherAppsOffIsLoosening() {
        val on = Settings(trackOtherFeedApps = true)
        assertTrue(SettingsGuard.loosenings(Settings(), on).isEmpty()) // zapnutí = přísnější
        assertEquals(1, SettingsGuard.loosenings(on, Settings()).size)
        val noX = SettingsGuard.loosenings(Settings(), Settings(trackedApps = TrackedApp.entries.toSet() - TrackedApp.X))
        assertTrue(noX.single().contains("X (Twitter)"))
    }

    @Test
    fun resetAppsSection() {
        val custom = Settings(trackOtherFeedApps = true, trackedApps = setOf(TrackedApp.X))
        val reset = SettingsGuard.resetSection(custom, SettingsSection.APPS)
        assertFalse(reset.trackOtherFeedApps)
        assertEquals(TrackedApp.entries.toSet(), reset.trackedApps)
    }
}
