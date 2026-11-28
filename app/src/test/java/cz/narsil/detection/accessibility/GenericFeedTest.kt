package cz.narsil.detection.accessibility

import cz.narsil.core.InterventionTrigger
import cz.narsil.core.Settings
import cz.narsil.core.Surface
import cz.narsil.core.TrackedApp
import cz.narsil.core.fake.FakeContextRepository
import cz.narsil.core.fake.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 149: obecná detekce feedu podle chování, zprávy a hledání podle snímku. Umělé snímky X, Redditu a další aplikace. */
class GenericFeedTest {
    private val x = "com.twitter.android"
    private val reddit = "com.reddit.frontpage"
    private val sec = 1_000L

    private val list = "androidx.recyclerview.widget.RecyclerView"
    private fun down(dy: Int = 900, from: Int = -1, to: Int = -1, cls: String = list) = ScrollSample(cls, deltaY = dy, fromIndex = from, toIndex = to, itemCount = 50)

    /** Scrolluje feed: posun dolů každých [every] ms od [from] do [to]. */
    private fun FeedBehavior.scroll(pkg: String, from: Long, to: Long, every: Long = 3 * sec, sample: (Int) -> ScrollSample = { down() }) {
        var t = from
        var i = 0
        while (t < to) {
            onScroll(pkg, sample(i++), t)
            t += every
        }
    }

    @Test
    fun xHomeTimelineBecomesFeedAfterSustainedScrolling() {
        val f = FeedBehavior()
        f.onScroll(x, down(), 0)
        f.onScroll(x, down(), 3 * sec)
        assertNull(f.surface(x, 3 * sec)) // dva posuny nestačí
        f.scroll(x, 6 * sec, 16 * sec)
        assertEquals(Surface.FEED, f.surface(x, 16 * sec))
        // Čte jeden tweet 50 s: pořád feed, po 60 s bez posunu už ne.
        assertEquals(Surface.FEED, f.surface(x, 60 * sec))
        assertNull(f.surface(x, 90 * sec))
    }

    @Test
    fun oneFlingIsNotAFeed() {
        val f = FeedBehavior()
        // 30 událostí během jedné sekundy (švihnutí) = jeden takt.
        repeat(30) { f.onScroll(x, down(), it * 30L) }
        f.onScroll(x, down(), 5 * sec)
        assertNull(f.surface(x, 5 * sec))
    }

    @Test
    fun horizontalBackwardsAndTextScrollingIgnored() {
        val f = FeedBehavior()
        f.scroll(reddit, 0, 30 * sec) { ScrollSample(list, deltaX = 1000, deltaY = 0) } // karusel
        f.scroll(reddit, 0, 30 * sec) { down(dy = -800) } // zpět
        f.scroll(reddit, 0, 30 * sec) { down(cls = "androidx.core.widget.NestedScrollView") } // článek
        f.scroll(reddit, 0, 30 * sec) { down(cls = "android.webkit.WebView") }
        assertNull(f.surface(reddit, 30 * sec))
    }

    @Test
    fun unknownDeltaUsesItemIndexes() {
        val f = FeedBehavior()
        f.scroll(reddit, 0, 20 * sec) { i -> down(dy = 0, from = i * 3, to = i * 3 + 4) }
        assertEquals(Surface.FEED, f.surface(reddit, 20 * sec))
    }

    @Test
    fun composeListWithoutDeltaOrIndexes() {
        // X (Compose, Nothing 3a): scroll události jen se scrollY, nebo úplně bez údajů.
        val f = FeedBehavior()
        f.scroll(x, 0, 20 * sec) { i -> ScrollSample("android.view.View", scrollY = 1_000 + i * 800) }
        assertEquals(Surface.FEED, f.surface(x, 20 * sec))
        val bare = FeedBehavior()
        bare.scroll(x, 0, 20 * sec) { ScrollSample("android.view.View") }
        assertEquals(Surface.FEED, bare.surface(x, 20 * sec))
        // Posun nahoru podle scrollY se nepočítá.
        val up = FeedBehavior()
        up.scroll(x, 0, 20 * sec) { i -> ScrollSample("android.view.View", scrollY = 100_000 - i * 800) }
        assertNull(up.surface(x, 20 * sec))
    }

    @Test
    fun sessionCountsFromFirstScroll() {
        val f = FeedBehavior()
        f.scroll(x, 10 * sec, 26 * sec)
        assertEquals(10 * sec, f.streakStart(x, 26 * sec))
        // Tracker: relace od prvního scrollu, ne od rozpoznání (Ozvěna po 20 s scrollování, ne po 35 s).
        val t = ScrollSessionTracker()
        t.observe(x, Surface.FEED, cz.narsil.core.ComfortContext.HOME, 26 * sec, 20 * sec, scrolled = true, startedAtHint = f.streakStart(x, 26 * sec))
        assertEquals(10 * sec, t.session?.startedAtMillis)
        assertNull(f.streakStart(x, 200 * sec))
    }

    @Test
    fun verticalPagerOfShortVideosIsReels() {
        val f = FeedBehavior()
        f.scroll(x, 0, 13 * sec, every = 6 * sec) { i -> ScrollSample("androidx.viewpager2.widget.ViewPager2", deltaY = 2000, fromIndex = i, toIndex = i, itemCount = 100) }
        assertEquals(Surface.REELS, f.surface(x, 13 * sec))
    }

    private fun snap(pkg: String, ids: Set<String> = emptySet(), edits: List<String> = emptyList(), cls: String? = null) =
        ForegroundSnapshot(pkg, WatchedApp.of(pkg) ?: WatchedApp.OTHER, SnapshotKind.WINDOW_CHANGED, 0, cls, null, ids.map { "$pkg:id/$it" }.toSet(), editFields = edits)

    @Test
    fun conversationsAndSearchFromSnapshot() {
        // X: spodní lišta (vč. zpráv) a tlačítko nového příspěvku jsou vidět všude – feed z toho nepoznáme, chat ne.
        val xShell = setOf("tabs", "dms_tab", "composer_write", "channels_tab_icon")
        assertNull(SurfaceClassifier.classify(snap(x, xShell + "list")))
        // X: konverzace – pole „Start a message“.
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(x, xShell + "list", listOf("dm_compose_edit_text|start a message"))))
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(x, setOf("conversation_view"))))
        // Reddit: chat, česky.
        assertEquals(Surface.MESSAGES, SurfaceClassifier.classify(snap(reddit, setOf("chat_tab"), listOf("|napište zprávu"))))
        assertNull(SurfaceClassifier.classify(snap(reddit, setOf("chat_tab", "bottom_nav"))))
        // Hledání.
        assertEquals(Surface.SEARCH, SurfaceClassifier.classify(snap(x, xShell, listOf("query|search x"))))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classify(snap(reddit, setOf("search_results_list"))))
        assertEquals(Surface.SEARCH, SurfaceClassifier.classify(snap("com.example.news", emptySet(), listOf("|hledat"))))
        // Komentář pod příspěvkem není chat ani feed: zůstane na chování.
        assertNull(SurfaceClassifier.classify(snap(reddit, emptySet(), listOf("|add a comment"))))
    }

    @Test
    fun otherAppCandidates() {
        assertTrue(WatchedApp.isOtherCandidate("com.example.news"))
        assertFalse(WatchedApp.isOtherCandidate(x)) // známá síť má vlastní přepínač
        assertFalse(WatchedApp.isOtherCandidate("com.whatsapp"))
        assertFalse(WatchedApp.isOtherCandidate("com.nothing.launcher"))
        assertFalse(WatchedApp.isOtherCandidate("com.google.android.inputmethod.latin"))
        assertFalse(WatchedApp.isOtherCandidate("com.android.settings"))
        assertFalse(WatchedApp.isOtherCandidate("cz.narsil.debug", ownPackage = "cz.narsil.debug"))
        assertEquals(WatchedApp.X, WatchedApp.of(x))
        // Názvy sedí s Nastavením.
        WatchedApp.entries.filter { it != WatchedApp.OTHER }.forEach { w -> assertTrue(w.name, TrackedApp.entries.any { it.name == w.name }) }
    }

    private class Run(val got: MutableList<InterventionTrigger>, val snapshots: MutableSharedFlow<ForegroundSnapshot>, val source: AccessibilityTriggerSource)

    private fun TestScope.start(settings: Settings): Run {
        val snapshots = MutableSharedFlow<ForegroundSnapshot>(extraBufferCapacity = 64)
        val source = AccessibilityTriggerSource(
            FakeContextRepository(), FakeSettingsRepository(settings), backgroundScope, snapshots,
            clock = { testScheduler.currentTime }, tickMillis = 5_000,
        )
        val got = mutableListOf<InterventionTrigger>()
        backgroundScope.launch { source.triggers.collect { got += it } }
        runCurrent()
        return Run(got, snapshots, source)
    }

    /** Scrollování v aplikaci [pkg] jako [app] po 3 s po dobu [millis]. */
    private suspend fun TestScope.scrollApp(r: Run, pkg: String, app: WatchedApp, millis: Long, edits: List<String> = emptyList()) {
        val end = testScheduler.currentTime + millis
        while (testScheduler.currentTime < end) {
            r.snapshots.emit(ForegroundSnapshot(pkg, app, SnapshotKind.SCROLLED, testScheduler.currentTime, editFields = edits, scroll = down()))
            runCurrent()
            advanceTimeBy(3_000)
        }
        runCurrent()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun xTimelineTriggersEcho() = runTest {
        val r = start(Settings(demoThresholdSec = 20))
        scrollApp(r, x, WatchedApp.X, 27_000)
        assertEquals(1, r.got.size) // relace od prvního scrollu: 20 s práh za ~21 s scrollování
        assertEquals(x, r.got[0].event.packageName)
        assertEquals(Surface.FEED, r.got[0].event.surface)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun youtubeHomeFeedByScrolling() = runTest {
        // Domovská stránka aplikace YouTube (karty videí, žádný přehrávač ani hledání) = feed podle scrollu.
        val r = start(Settings(demoThresholdSec = 20))
        val yt = "com.google.android.youtube"
        val end = testScheduler.currentTime + 27_000
        while (testScheduler.currentTime < end) {
            r.snapshots.emit(
                ForegroundSnapshot(yt, WatchedApp.YOUTUBE, SnapshotKind.SCROLLED, testScheduler.currentTime,
                    viewIds = setOf("$yt:id/results", "$yt:id/pivot_bar"), scroll = down()),
            )
            runCurrent()
            advanceTimeBy(3_000)
        }
        runCurrent()
        assertEquals(1, r.got.size)
        assertEquals(Surface.FEED, r.got[0].event.surface)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun xDirectMessagesNeverTrigger() = runTest {
        val r = start(Settings(demoThresholdSec = 20))
        scrollApp(r, x, WatchedApp.X, 120_000, edits = listOf("dm_compose_edit_text|start a message"))
        assertEquals(0, r.got.size)
        assertNull(r.source.progressPercent.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun switchesInSettings() = runTest {
        // X vypnutý v Nastavení.
        val off = start(Settings(demoThresholdSec = 20, trackedApps = TrackedApp.entries.toSet() - TrackedApp.X))
        scrollApp(off, x, WatchedApp.X, 60_000)
        assertEquals(0, off.got.size)
        // Další aplikace: jen se zapnutou volbou.
        val other = start(Settings(demoThresholdSec = 20))
        scrollApp(other, "com.example.news", WatchedApp.OTHER, 60_000)
        assertEquals(0, other.got.size)
        val otherOn = start(Settings(demoThresholdSec = 20, trackOtherFeedApps = true))
        scrollApp(otherOn, "com.example.news", WatchedApp.OTHER, 60_000)
        assertEquals(1, otherOn.got.size)
    }
}
