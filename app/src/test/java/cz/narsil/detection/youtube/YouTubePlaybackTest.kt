package cz.narsil.detection.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubePlaybackTest {
    @Test
    fun videoIdFromUrl() {
        assertEquals("dQw4w9WgXcQ", YouTubePlayback.videoIdFromUrl("m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubePlayback.videoIdFromUrl("https://www.youtube.com/watch?feature=share&v=dQw4w9WgXcQ&t=10"))
        assertEquals("dQw4w9WgXcQ", YouTubePlayback.videoIdFromUrl("youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("abcDEF12345", YouTubePlayback.videoIdFromUrl("HTTPS://M.YOUTUBE.COM/live/abcDEF12345"))
        assertNull(YouTubePlayback.videoIdFromUrl("m.youtube.com/shorts/dQw4w9WgXcQ"))
        assertNull(YouTubePlayback.videoIdFromUrl("m.youtube.com/"))
        assertNull(YouTubePlayback.videoIdFromUrl("m.youtube.com/watch"))
        assertNull(YouTubePlayback.videoIdFromUrl("example.com/watch?v=dQw4w9WgXcQ"))
        assertNull(YouTubePlayback.videoIdFromUrl(null))
        assertTrue(YouTubePlayback.isYouTubeUrl("m.youtube.com/feed/subscriptions"))
        assertFalse(YouTubePlayback.isYouTubeUrl("instagram.com/reels/x"))
    }

    @Test
    fun timeAndDuration() {
        // Čas v jednom uzlu i rozdělený do sousedních uzlů (m.youtube.com).
        val a = YouTubePlayback.parse("id", listOf("Přehrát", "0:15 / 4:32"), 1_000)
        assertEquals(15_000L, a.positionMillis)
        assertEquals(272_000L, a.durationMillis)
        assertEquals(257_000L, a.remainingMillis)
        assertFalse(a.ended)
        val b = YouTubePlayback.parse("id", listOf("Pozastavit", "12:03", "/", "1:02:00"), 0)
        assertEquals(723_000L, b.positionMillis)
        assertEquals(3_720_000L, b.durationMillis)
        // Bez času: délka neznámá.
        val c = YouTubePlayback.parse("id", listOf("Název videa", "1,2 mil. zhlédnutí"), 0)
        assertNull(c.remainingMillis)
        assertFalse(c.ended)
    }

    @Test
    fun endDetection() {
        assertTrue(YouTubePlayback.parse("id", listOf("Přehrát znovu"), 0).ended)
        assertTrue(YouTubePlayback.parse("id", listOf("Replay"), 0).ended)
        assertTrue(YouTubePlayback.parse("id", listOf("Další video za 5"), 0).ended)
        assertTrue(YouTubePlayback.parse("id", listOf("Up next in 8"), 0).ended)
        assertTrue(YouTubePlayback.parse("id", listOf("4:32 / 4:32"), 0).ended)
        // Aplikace YouTube na Nothingu: end-screen „Navrhované video · Zavřít · Přehrát“.
        assertTrue(YouTubePlayback.parse(null, listOf("Navrhované video", "Zavřít", "Přehrát"), 0).ended)
        // 154: hudební videa, angličtina; samotný nadpis bez tlačítka konec není.
        assertTrue(YouTubePlayback.parse(null, listOf("Navrhovaná hudba", "Zavřít", "Přehrát"), 0).ended)
        assertTrue(YouTubePlayback.parse(null, listOf("Suggested video", "Close", "Play"), 0).ended)
        assertFalse(YouTubePlayback.parse(null, listOf("Navrhovaná videa", "Odebírat", "1:00 / 4:32"), 0).ended)
        // Pozastavené video: „Přehrát“ bez „Zavřít“ konec není.
        assertFalse(YouTubePlayback.parse(null, listOf("Navrhovaná videa", "Přehrát", "1:00 / 4:32"), 0).ended)
        assertTrue(YouTubePlayback.parse(null, listOf("Navrhovaná hudba", "Zavřít"), 0).ended)
        // Tlačítko „Další video“ a přepínač autoplay jsou vidět i během přehrávání.
        assertFalse(YouTubePlayback.parse("id", listOf("Další video", "Automatické přehrávání je zapnuto", "1:00 / 4:32"), 0).ended)
    }

    @Test
    fun parseClock() {
        assertEquals(65_000L, YouTubePlayback.parseClock("1:05"))
        assertEquals(3_665_000L, YouTubePlayback.parseClock("1:01:05"))
        assertNull(YouTubePlayback.parseClock("1:75"))
    }
}
