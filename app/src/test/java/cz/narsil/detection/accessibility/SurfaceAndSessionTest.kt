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
        // ID z Nothing 3a (viditelná stránka po odfiltrování neviditelných uzlů), tabbar je vidět všude.
        val tabbar = setOf("feed_tab", "clips_tab", "direct_tab", "search_tab", "profile_tab")
        val feed = tabbar + setOf("main_feed_action_bar", "row_feed_profile_header", "row_feed_button_like", "clips_media_component", "clips_video_container")
        val reels = tabbar + setOf("clips_viewer_container", "clips_viewer_view_pager", "root_clips_layout", "clips_video_container")
        val inbox = tabbar + setOf("direct_inbox_action_bar")
        assertEquals(Surface.FEED, SurfaceClassifier.classify(snap(ig, feed)))
        assertEquals(Surface.REELS, SurfaceClassifier.classify(snap(ig, reels)))
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(ig, inbox)))
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(ig, setOf("something_unknown"))))
    }

    @Test
    fun instagramSelectedTabDecidesWhenIdsAmbiguous() {
        val ambiguous = setOf("clips_video_container")
        assertEquals(Surface.FEED, SurfaceClassifier.classify(snap(ig, ambiguous).copy(selectedTab = "feed_tab")))
        assertEquals(Surface.REELS, SurfaceClassifier.classify(snap(ig, ambiguous).copy(selectedTab = "clips_tab")))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classify(snap(ig, ambiguous).copy(selectedTab = "search_tab")))
        // Celoobrazovkový reel otevřený z feedu: přehrávač má přednost před záložkou.
        assertEquals(Surface.REELS, SurfaceClassifier.classify(snap(ig, setOf("clips_viewer_container")).copy(selectedTab = "feed_tab")))
        // Zprávy mají přednost vždy.
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(ig, setOf("direct_inbox_action_bar")).copy(selectedTab = "feed_tab")))
    }

    @Test
    fun otherAppsAndWeb() {
        assertEquals(Surface.FEED, SurfaceClassifier.classify(snap(tt, cls = "com.ss.android.ugc.aweme.main.MainActivity")))
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(tt, cls = "com.ss.android.ugc.aweme.im.chat.ChatRoomActivity")))
        assertEquals(Surface.SHORTS, SurfaceClassifier.classify(snap("com.google.android.youtube", setOf("reel_player_page_container"))))
        assertNull(SurfaceClassifier.classify(snap("com.whatsapp", setOf("conversation"))))
        val yt = "com.google.android.youtube"
        val ytShell = setOf("watch_while_layout_coordinator_layout")
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(yt, ytShell + "results"))) // domovská stránka
        assertEquals(Surface.VIDEO, SurfaceClassifier.classify(snap(yt, ytShell + "watch_player")))
        assertEquals(Surface.SHORTS, SurfaceClassifier.classify(snap(yt, ytShell + setOf("reel_recycler", "reel_watch_player"))))
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
