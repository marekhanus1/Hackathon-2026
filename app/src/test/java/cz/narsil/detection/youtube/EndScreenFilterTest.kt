package cz.narsil.detection.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndScreenFilterTest {
    private val stale = VideoPlayback(null, ended = true)
    private val playing = VideoPlayback(null)

    @Test
    fun staleEndScreenOnOpenIsIgnoredUntilPlaybackSeen() {
        val f = EndScreenFilter()
        // Nothing 10:55:09: „Me at the zoo“ přes intent, ve stromu end-screen minulého přehrání.
        assertFalse(f.filter(true, stale)!!.ended)
        assertFalse(f.filter(true, stale)!!.ended)
        assertFalse(f.filter(true, playing)!!.ended)
        assertTrue(f.filter(true, stale)!!.ended) // teď skutečný konec
        assertNull(f.filter(true, null))
    }

    @Test
    fun leavingOrAnotherVideoRearms() {
        val f = EndScreenFilter()
        f.filter(true, playing)
        assertTrue(f.filter(true, stale)!!.ended)
        f.filter(false, null) // zpět na feed
        assertFalse(f.filter(true, stale)!!.ended) // nové otevření
        // Firefox: jiné v= je nové video.
        val g = EndScreenFilter()
        g.filter(true, VideoPlayback("A"))
        assertTrue(g.filter(true, VideoPlayback("A", ended = true))!!.ended)
        assertFalse(g.filter(true, VideoPlayback("B", ended = true))!!.ended)
        assertEquals("B", g.filter(true, VideoPlayback("B"))!!.videoKey)
        assertTrue(g.filter(true, VideoPlayback("B", ended = true))!!.ended)
    }
}
