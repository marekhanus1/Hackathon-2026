package cz.narsil.detection.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoChainTrackerTest {
    private val min = 60_000L

    private fun ff(key: String, ended: Boolean = false) = VideoPlayback(key, ended = ended)

    /** Firefox: přepne na video [key] a hned vyhodnotí jako (ne)plánované. Vrací „zasáhnout“. */
    private fun VideoChainTracker.watch(key: String, now: Long, planned: Boolean = false, limit: Int = 3): Boolean {
        val v = observe(true, ff(key), now) ?: error("$key není nové video")
        return resolve(v, planned, limit, now)
    }

    @Test
    fun thirdUnplannedVideoTriggersAtItsStart() {
        val t = VideoChainTracker()
        assertFalse(t.watch("A", 0))
        // Ticky během videa A: žádné nové video.
        assertNull(t.observe(true, ff("A"), 2 * min))
        assertFalse(t.watch("B", 4 * min))
        assertTrue(t.watch("C", 8 * min))
        assertEquals(3, t.count)
        assertEquals(0L, t.chainStartedAtMillis)
    }

    @Test
    fun limitFromSettings() {
        val t = VideoChainTracker()
        assertFalse(t.watch("A", 0, limit = 2))
        assertTrue(t.watch("B", min, limit = 2))
        val five = VideoChainTracker()
        for ((i, k) in listOf("A", "B", "C", "D").withIndex()) assertFalse(five.watch(k, i * min, limit = 5))
        assertTrue(five.watch("E", 4 * min, limit = 5))
    }

    @Test
    fun plannedVideoDoesNotCountNorTrigger() {
        val t = VideoChainTracker()
        assertFalse(t.watch("A", 0))
        assertFalse(t.watch("lecture", min, planned = true))
        assertFalse(t.watch("B", 20 * min / 10))
        assertEquals(2, t.count)
        assertTrue(t.watch("C", 3 * min))
    }

    @Test
    fun pauseOverTenMinutesResets() {
        val t = VideoChainTracker()
        t.watch("A", 0)
        t.watch("B", min)
        assertNull(t.observe(false, null, 2 * min)) // feed, jiná aplikace
        assertFalse(t.watch("C", 12 * min)) // pauza > 10 min od posledního videa
        assertEquals(1, t.count)
    }

    @Test
    fun returningToSameVideoIsNotNew() {
        val t = VideoChainTracker()
        t.watch("A", 0)
        t.observe(false, null, min)
        assertNull(t.observe(true, ff("A"), 2 * min))
        assertEquals(1, t.count)
    }

    @Test
    fun skippedVideoCountsAndNextOneTriggers() {
        val t = VideoChainTracker()
        t.watch("A", 0)
        t.watch("B", min)
        // C se vyhodnotí až po přepnutí na D (pomalý ContentMatcher): na C už nikdo nekouká.
        val c = t.observe(true, ff("C"), 2 * min)!!
        val d = t.observe(true, ff("D"), 2 * min + 5_000)!!
        assertFalse(t.resolve(c, false, 3, 2 * min + 6_000))
        assertTrue(t.resolve(d, false, 3, 2 * min + 7_000))
    }

    @Test
    fun youtubeAppWithoutIdsUsesEnterAndAutoplay() {
        val t = VideoChainTracker()
        val app = VideoPlayback(null)
        val a = t.observe(true, app, 0)
        assertNotNull(a)
        assertNull(t.observe(true, app, 30_000)) // stejné video
        assertNull(t.observe(true, VideoPlayback(null, ended = true), min)) // end-screen
        val b = t.observe(true, app, min + 10_000) // autoplay: přehrává další
        assertNotNull(b)
        t.observe(false, null, 2 * min) // zpět na domovskou stránku
        val c = t.observe(true, app, 2 * min + 5_000) // klik na další video
        assertNotNull(c)
        assertFalse(t.resolve(a!!, false, 3, 0))
        assertFalse(t.resolve(b!!, false, 3, min))
        assertTrue(t.resolve(c!!, false, 3, 2 * min + 5_000))
    }

    @Test
    fun outcomesResetAndUnlockSuppresses() {
        val t = VideoChainTracker()
        t.watch("A", 0); t.watch("B", min)
        assertTrue(t.watch("C", 2 * min))
        assertFalse(t.watch("D", 3 * min)) // Ozvěna už je venku
        t.onUnlocked(15 * min)
        assertEquals(0, t.count)
        t.watch("E", 4 * min); t.watch("F", 5 * min)
        assertFalse(t.watch("G", 6 * min)) // zaplacený čas
        assertTrue(t.watch("H", 16 * min)) // po vypršení hned, řetěz pokračoval
        t.onClosed()
        assertEquals(0, t.count)
        assertFalse(t.watch("I", 17 * min))
    }
}
