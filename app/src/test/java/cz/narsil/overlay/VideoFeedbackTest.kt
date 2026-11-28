package cz.narsil.overlay

import cz.narsil.core.ConsumptionEvent
import cz.narsil.core.HistoryRating
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.Surface
import cz.narsil.core.TriggerReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFeedbackTest {
    private val day = 1_000_000L

    private fun video(title: String?) = InterventionTrigger(
        ConsumptionEvent("org.mozilla.firefox", Surface.VIDEO, day, 0, videoTitle = title), TriggerReason.UNPLANNED_VIDEO_CHAIN, day,
    )

    private fun feed() = InterventionTrigger(ConsumptionEvent("com.zhiliaoapp.musically", Surface.FEED, day, 0), TriggerReason.ENDLESS_SCROLL, day)

    private fun r(at: Long, title: String, rating: Int) = HistoryRating(day + at, title, null, rating)

    @Test
    fun stemDropsEpisodeNumber() {
        assertEquals("Když je pondělí", VideoFeedback.stem("Když je pondělí #312"))
        assertEquals("Kočky", VideoFeedback.stem("Kočky 2"))
    }

    @Test
    fun sameShowLowLastTime() {
        val ratings = listOf(r(1, "Když je pondělí #311", 2))
        assertEquals("Minule ti „Když je pondělí“ nedalo nic.", VideoFeedback.echoNote(video("Když je pondělí #312"), "Firefox", ratings, day))
    }

    @Test
    fun sameShowLowTwiceInRow() {
        val ratings = listOf(r(1, "Když je pondělí #310", 4), r(2, "Když je pondělí #311", 1), r(3, "Když je pondělí #312", 2))
        assertEquals("Už 2× za sebou ti „Když je pondělí“ nedalo nic.", VideoFeedback.echoNote(video("Když je pondělí #313"), "Firefox", ratings, day))
    }

    @Test
    fun goodLastTimeFallsBackToToday() {
        val ratings = listOf(r(1, "Přednáška", 5), r(2, "Kočky", 2), r(3, "Psi", 1))
        assertEquals("Dnes ti už 2 videa nedala nic.", VideoFeedback.echoNote(video("Přednáška"), "Firefox", ratings, day))
    }

    @Test
    fun unknownTitleUsesTodayOnly() {
        val ratings = listOf(r(1, VideoFeedback.UNKNOWN_VIDEO, 2))
        assertEquals("Dnes ti už 1 video nedalo nic.", VideoFeedback.echoNote(video(null), "YouTube", ratings, day))
    }

    @Test
    fun feedNotes() {
        val tiktok = VideoFeedback.feedTitle("TikTok")
        assertEquals("Minule ti feed na TikTok nedal nic.", VideoFeedback.echoNote(feed(), "TikTok", listOf(r(1, tiktok, 2)), day))
        assertEquals("Dnes ti feed už 1× nedal nic.", VideoFeedback.echoNote(feed(), "TikTok", listOf(r(1, VideoFeedback.feedTitle("Instagram"), 1)), day))
        // Videa se do feedu nepočítají a staré odpovědi ze včerejška taky ne.
        assertNull(VideoFeedback.echoNote(feed(), "TikTok", listOf(r(1, "Kočky", 1), HistoryRating(day - 1, VideoFeedback.feedTitle("X"), null, 1)), day))
    }

    @Test
    fun limiterCapsAndSpaces() {
        val l = VideoFeedback.Limiter(maxPerDay = 2, minGapMillis = 1_000)
        assertTrue(l.tryAsk(day, day))
        assertFalse(l.tryAsk(day + 500, day)) // odstup
        assertTrue(l.tryAsk(day + 1_000, day))
        assertFalse(l.tryAsk(day + 5_000, day)) // strop dne
        assertTrue(l.tryAsk(day + 100_000, day + 50_000)) // nový den
    }
}
