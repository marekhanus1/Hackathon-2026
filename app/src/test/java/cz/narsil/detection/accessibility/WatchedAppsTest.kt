package cz.narsil.detection.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchedAppsTest {
    @Test
    fun recognizesWatchedAndIgnoresChats() {
        assertEquals(WatchedApp.TIKTOK, WatchedApp.of("com.zhiliaoapp.musically"))
        assertEquals(WatchedApp.CHROME, WatchedApp.of("com.android.chrome"))
        assertNull(WatchedApp.of("com.whatsapp"))
        assertTrue(WatchedApp.isIgnored("com.facebook.orca"))
        assertFalse(WatchedApp.isIgnored("com.instagram.android"))
    }

    @Test
    fun throttleLetsWindowChangesThrough() {
        val t = SnapshotThrottle(minIntervalMillis = 1_000)
        assertTrue(t.shouldSnapshot("a", SnapshotKind.SCROLLED, 0))
        assertFalse(t.shouldSnapshot("a", SnapshotKind.SCROLLED, 500))
        assertTrue(t.shouldSnapshot("a", SnapshotKind.WINDOW_CHANGED, 600))
        assertTrue(t.shouldSnapshot("b", SnapshotKind.SCROLLED, 700))
        assertFalse(t.shouldSnapshot("b", SnapshotKind.CONTENT_CHANGED, 1_000))
        assertTrue(t.shouldSnapshot("b", SnapshotKind.CONTENT_CHANGED, 1_700))
    }
}
