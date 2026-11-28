package cz.narsil.core.data

import cz.narsil.core.Settings
import cz.narsil.core.TrackedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackedAppsMigrationTest {
    @Test
    fun newNetworksDefaultOnAfterUpdate() {
        // Uloženo před 149 bez TikToku: TikTok zůstane vypnutý, nové sítě (X…) zapnuté.
        val apps = trackedAppsFrom(setOf("INSTAGRAM", "YOUTUBE", "FIREFOX"), null)
        assertFalse(TrackedApp.TIKTOK in apps)
        assertTrue(TrackedApp.X in apps)
        assertTrue(TrackedApp.REDDIT in apps)
        // Uloženo po 149 s vypnutým X: zůstane vypnutý.
        val known = TrackedApp.entries.map { it.name }.toSet()
        assertFalse(TrackedApp.X in trackedAppsFrom(known - "X", known))
        // Nic uloženo: všechno.
        assertEquals(TrackedApp.entries.toSet(), trackedAppsFrom(null, null))
    }

    @Test
    fun otherAppsSwitch() {
        assertFalse(Settings().isTracked("OTHER"))
        assertTrue(Settings(trackOtherFeedApps = true).isTracked("OTHER"))
        assertTrue(Settings().isTracked("X"))
    }
}
