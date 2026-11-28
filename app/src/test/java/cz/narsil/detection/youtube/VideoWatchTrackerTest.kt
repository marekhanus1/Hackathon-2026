package cz.narsil.detection.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VideoWatchTrackerTest {
    private val ff = "org.mozilla.firefox"
    private val yt = "com.google.android.youtube"
    private val sec = 1_000L

    private fun play(key: String?, title: String? = null, ended: Boolean = false) = VideoPlayback(key, ended = ended, title = title)

    @Test
    fun endedVideoReportedOnceWithTitle() {
        val t = VideoWatchTracker()
        assertNull(t.observe(true, ff, play("AAAAAAAAAAA"), 0))
        assertNull(t.observe(true, ff, play("AAAAAAAAAAA", "Rust Async"), 30 * sec))
        val f = t.observe(true, ff, play("AAAAAAAAAAA", ended = true), 90 * sec)
        assertNotNull(f)
        assertEquals("Rust Async", f!!.title)
        assertEquals(90 * sec, f.watchedMillis)
        assertEquals(true, f.ended)
        assertNull(t.observe(true, ff, play("AAAAAAAAAAA", ended = true), 95 * sec)) // jen jednou
        assertNull(t.observe(false, ff, null, 100 * sec)) // odchod po nahlášeném konci nic
    }

    @Test
    fun shortEndedVideoIgnored() {
        val t = VideoWatchTracker()
        t.observe(true, ff, play("AAAAAAAAAAA"), 0)
        assertNull(t.observe(true, ff, play("AAAAAAAAAAA", ended = true), 30 * sec))
    }

    @Test
    fun leavingAfterTwoMinutesReported() {
        val t = VideoWatchTracker()
        t.observe(true, ff, play("AAAAAAAAAAA", "Kočky"), 0)
        t.observe(true, ff, play("AAAAAAAAAAA"), 150 * sec)
        val f = t.observe(false, ff, null, 160 * sec)
        assertEquals("Kočky", f?.title)
        assertEquals(false, f?.ended)
        assertEquals(150 * sec, f?.watchedMillis)
    }

    @Test
    fun leavingEarlyNotReported() {
        val t = VideoWatchTracker()
        t.observe(true, ff, play("AAAAAAAAAAA"), 0)
        t.observe(true, ff, play("AAAAAAAAAAA"), 60 * sec)
        assertNull(t.observe(false, ff, null, 70 * sec))
    }

    @Test
    fun otherVideoFinishesPrevious() {
        val t = VideoWatchTracker()
        t.observe(true, ff, play("AAAAAAAAAAA", "První"), 0)
        t.observe(true, ff, play("AAAAAAAAAAA"), 180 * sec)
        val f = t.observe(true, ff, play("BBBBBBBBBBB", "Druhé"), 185 * sec)
        assertEquals("První", f?.title)
    }

    @Test
    fun appWithoutKeyReplayAfterEndIsNewVideo() {
        val t = VideoWatchTracker()
        t.observe(true, yt, play(null), 0)
        assertNotNull(t.observe(true, yt, play(null, ended = true), 120 * sec))
        assertNull(t.observe(true, yt, play(null), 125 * sec)) // autoplay dalšího videa
        assertNotNull(t.observe(true, yt, play(null, ended = true), 200 * sec))
    }
}
