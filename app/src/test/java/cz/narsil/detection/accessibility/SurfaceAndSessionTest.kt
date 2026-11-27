package cz.narsil.detection.accessibility

import cz.narsil.core.ComfortContext.AWAY
import cz.narsil.core.ComfortContext.HOME
import cz.narsil.core.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SurfaceAndSessionTest {
    private val ig = "com.instagram.android"
    private val tt = "com.zhiliaoapp.musically"
    private val min = 60_000L
    private val threshold = 5 * min

    private fun snap(pkg: String, ids: Set<String> = emptySet(), cls: String? = null, url: String? = null) =
        ForegroundSnapshot(pkg, WatchedApp.of(pkg), SnapshotKind.WINDOW_CHANGED, 0, cls, url, ids.map { "$pkg:id/$it" }.toSet())

    @Test
    fun instagramSurfaces() {
        assertEquals(Surface.FEED, SurfaceClassifier.classify(snap(ig, setOf("row_feed_photo_imageview", "action_bar_inbox_button"))))
        assertEquals(Surface.REELS, SurfaceClassifier.classify(snap(ig, setOf("clips_viewer_view_pager"))))
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(ig, setOf("row_thread_composer_edittext", "row_feed_x"))))
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(ig, setOf("something_unknown"))))
    }

    @Test
    fun otherAppsAndWeb() {
        assertEquals(Surface.FEED, SurfaceClassifier.classify(snap(tt, cls = "com.ss.android.ugc.aweme.main.MainActivity")))
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(tt, cls = "com.ss.android.ugc.aweme.im.chat.ChatRoomActivity")))
        assertEquals(Surface.SHORTS, SurfaceClassifier.classify(snap("com.google.android.youtube", setOf("reel_player_page_container"))))
        assertNull(SurfaceClassifier.classify(snap("com.whatsapp", setOf("conversation"))))
        assertEquals(Surface.REELS, SurfaceClassifier.classifyUrl("instagram.com/reels/abc"))
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classifyUrl("https://www.instagram.com/direct/inbox"))
        assertEquals(Surface.VIDEO, SurfaceClassifier.classifyUrl("youtube.com/watch?v=1"))
        assertEquals(Surface.SHORTS, SurfaceClassifier.classifyUrl("m.youtube.com/shorts/x"))
        assertNull(SurfaceClassifier.classifyUrl("wikipedia.org/wiki/Rust"))
    }

    private val step = 30_000L

    /** Pozorování každých 30 s v [from, to); vrací časy zásahů. */
    private fun ScrollSessionTracker.run(pkg: String, surface: Surface?, from: Long, to: Long, ctx: cz.narsil.core.ComfortContext = HOME, scrolled: Boolean = false): List<Long> {
        val hits = mutableListOf<Long>()
        var t = from
        while (t < to) {
            if (observe(pkg, surface, ctx, t, threshold, scrolled) != null) hits += t
            t += step
        }
        return hits
    }

    @Test
    fun triggersAfterThresholdAtHomeOnce() {
        val t = ScrollSessionTracker()
        assertEquals(emptyList<Long>(), t.run(ig, Surface.FEED, 0, 3 * min))
        assertEquals(listOf(5 * min), t.run(tt, Surface.FEED, 3 * min, 10 * min)) // přepnutí nepřeruší, jen jednou
    }

    @Test
    fun messagesAndAwayNeverTrigger() {
        assertEquals(emptyList<Long>(), ScrollSessionTracker().run(ig, Surface.MESSAGES, 0, 20 * min))
        assertEquals(emptyList<Long>(), ScrollSessionTracker().run(ig, Surface.FEED, 0, 20 * min, ctx = AWAY))
    }

    @Test
    fun pauseEndsSession() {
        val t = ScrollSessionTracker()
        t.run(ig, Surface.FEED, 0, 4 * min)
        t.run("launcher", null, 4 * min, 6 * min + step)
        t.run(ig, Surface.FEED, 6 * min + step, 7 * min)
        assertEquals(6 * min + step, t.session?.startedAtMillis)
        val u = ScrollSessionTracker()
        u.run(ig, Surface.FEED, 0, 4 * min)
        u.run("launcher", null, 4 * min, 5 * min) // krátká pauza relaci neukončí
        assertEquals(listOf(5 * min), u.run(ig, Surface.FEED, 5 * min, 6 * min))
    }

    @Test
    fun paidTimeThenImmediateTrigger() {
        val t = ScrollSessionTracker()
        assertEquals(listOf(5 * min), t.run(ig, Surface.FEED, 0, 5 * min + step))
        t.onUnlocked(15 * min)
        assertEquals(listOf(15 * min), t.run(ig, Surface.FEED, 5 * min + step, 16 * min))
    }

    @Test
    fun closedRestartsFromZero() {
        val t = ScrollSessionTracker()
        assertEquals(listOf(5 * min), t.run(ig, Surface.FEED, 0, 5 * min + step))
        t.onClosed()
        assertEquals(listOf(10 * min + step), t.run(ig, Surface.FEED, 5 * min + step, 12 * min))
    }

    @Test
    fun reelFromChatCountsOnlyAfterScroll() {
        val t = ScrollSessionTracker()
        t.observe("com.whatsapp", null, HOME, 0, threshold)
        assertEquals(emptyList<Long>(), t.run(ig, Surface.REELS, step, 10 * min)) // jedno video z chatu
        assertNull(t.observe(ig, Surface.REELS, HOME, 10 * min, threshold, scrolled = true)) // posun: od teď relace
        assertEquals(listOf(15 * min), t.run(ig, Surface.REELS, 10 * min + step, 16 * min))
        assertNotNull(t.session)
    }
}
