package cz.narsil.detection.accessibility

import cz.narsil.core.ComfortContext.HOME
import cz.narsil.core.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 148: výsledky vyhledávání nejsou nekonečný povrch, video ani začátek relace. */
class SearchSurfaceTest {
    private val yt = "com.google.android.youtube"
    private val ig = "com.instagram.android"
    private val min = 60_000L
    private val threshold = 5 * min

    private fun snap(pkg: String, ids: Set<String>) =
        ForegroundSnapshot(pkg, WatchedApp.of(pkg), SnapshotKind.WINDOW_CHANGED, 0, null, null, ids.map { "$pkg:id/$it" }.toSet())

    /** Umělý strom aplikace YouTube: obal, spodní lišta a lupa jsou vidět všude. */
    private val ytShell = setOf("watch_while_layout_coordinator_layout", "pivot_bar", "menu_item_search")

    @Test
    fun youtubeAppSearchResults() {
        // Výsledky: pole s dotazem + seznam + polička Shorts (dřív → SHORTS, Ozvěna po hledání).
        val results = ytShell + setOf("search_query", "results", "shorts_shelf_container", "player_view")
        assertEquals(Surface.SEARCH, SurfaceClassifier.classify(snap(yt, results)))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classify(snap(yt, ytShell + "search_edit_text")))
        // Lupa na domovské stránce hledání není.
        assertNull(SurfaceClassifier.classify(snap(yt, ytShell + "results"))) // ne zprávy: feed podle scrollu (149)
        // Polička Shorts na domovské stránce: dál Shorts (beze změny chování).
        assertEquals(Surface.SHORTS, SurfaceClassifier.classify(snap(yt, ytShell + setOf("results", "shorts_shelf_container"))))
        // Z výsledků otevřené Shorts / video: přehrávač má přednost před polem hledání pod ním.
        assertEquals(Surface.SHORTS, SurfaceClassifier.classify(snap(yt, results + setOf("reel_player_page_container", "reel_recycler"))))
        assertEquals(Surface.VIDEO, SurfaceClassifier.classify(snap(yt, results + "watch_player")))
    }

    @Test
    fun webSearchUrls() {
        assertEquals(Surface.SEARCH, SurfaceClassifier.classifyUrl("m.youtube.com/results?search_query=kocky"))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classifyUrl("https://www.instagram.com/explore/search/keyword/?q=rust"))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classifyUrl("x.com/search?q=narsil&src=typed_query"))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classifyUrl("x.com/explore"))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classifyUrl("www.reddit.com/search/?q=android"))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classifyUrl("www.reddit.com/r/androiddev/search/?q=compose"))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classifyUrl("tiktok.com/search?q=psi"))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classifyUrl("m.facebook.com/search/top/?q=x"))
        // Feedy dál feed.
        assertEquals(Surface.FEED, SurfaceClassifier.classifyUrl("x.com/home"))
        assertEquals(Surface.FEED, SurfaceClassifier.classifyUrl("www.reddit.com/r/androiddev/"))
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classifyUrl("www.reddit.com/message/inbox"))
        assertEquals(Surface.VIDEO, SurfaceClassifier.classifyUrl("m.youtube.com/watch?v=abc"))
        assertFalse(Surface.SEARCH.isEndless)
    }

    @Test
    fun searchDoesNotStartOrFeedSession() {
        // Scénář z Nothingu: 3 min na výsledcích hledání, pak jedno Shorts.
        val t = ScrollSessionTracker()
        var now = 0L
        while (now < 3 * min) {
            assertNull(t.observe(yt, Surface.SEARCH, HOME, now, threshold, scrolled = true, onVideoSite = true))
            now += 10_000
        }
        assertNull(t.session)
        assertNull(t.observe(yt, Surface.SHORTS, HOME, now, threshold, scrolled = true, onVideoSite = true))
        assertEquals(now, t.session?.startedAtMillis) // relace až od Shorts
        assertNull(t.observe(yt, Surface.SHORTS, HOME, now + 60_000, threshold, onVideoSite = true))
    }

    @Test
    fun searchIsNotChatSoResultCountsImmediately() {
        // Hledání není chat: Reel otevřený z Prozkoumat/hledání se počítá hned (pravidlo „odkaz z chatu“ neplatí).
        val t = ScrollSessionTracker()
        t.observe(ig, Surface.SEARCH, HOME, 0, threshold)
        t.observe(ig, Surface.REELS, HOME, 10_000, threshold)
        assertNotNull(t.session)
    }
}
