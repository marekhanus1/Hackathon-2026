package cz.narsil.detection.accessibility

import cz.narsil.core.ComfortContext
import cz.narsil.core.ComfortContext.AWAY
import cz.narsil.core.ComfortContext.HOME
import cz.narsil.core.Settings
import cz.narsil.core.Surface
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.fake.FakeContextRepository
import cz.narsil.core.fake.FakeSettingsRepository
import cz.narsil.detection.youtube.VideoPlayback
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 135: výpočet progresu ukazatele blížícího se limitu. */
class ScrollProgressTest {
    private val ig = "com.instagram.android"
    private val min = 60_000L
    private val threshold = 5 * min

    private fun ScrollSessionTracker.see(surface: Surface?, now: Long, ctx: ComfortContext = HOME, pkg: String = ig) =
        observe(pkg, surface, ctx, now, threshold, scrolled = true)

    /** Scrollování po 30 s v [from, to) (pauza > 2 min by relaci ukončila). */
    private fun ScrollSessionTracker.scroll(from: Long, to: Long) {
        var now = from
        while (now < to) {
            assertNull(see(Surface.REELS, now))
            now += 30_000
        }
    }

    @Test
    fun growsWithSessionAndCapsAtFull() {
        val t = ScrollSessionTracker()
        assertNull(t.progress(0, threshold, HOME)) // bez relace
        t.see(Surface.REELS, 0)
        assertEquals(0f, t.progress(0, threshold, HOME)!!, 0.001f)
        t.scroll(0, 2 * min + 30_000)
        t.see(Surface.REELS, 2 * min + 30_000)
        assertEquals(0.5f, t.progress(2 * min + 30_000, threshold, HOME)!!, 0.001f)
        t.scroll(3 * min, 4 * min)
        t.see(Surface.FEED, 4 * min)
        assertEquals(0.8f, t.progress(4 * min, threshold, HOME)!!, 0.001f)
        // Limit: zásah, ukazatel se schová (Ozvěna je na obrazovce).
        assertNotNull(t.see(Surface.FEED, 5 * min))
        assertNull(t.progress(5 * min, threshold, HOME))
    }

    @Test
    fun hiddenOutsideWatchedAppPauseAndAway() {
        val t = ScrollSessionTracker()
        t.see(Surface.REELS, 0)
        t.see(Surface.REELS, min)
        // Chat / plocha (pauza): skrýt, relace ale běží dál.
        t.see(null, min + 10_000, pkg = "com.whatsapp")
        assertNull(t.progress(min + 10_000, threshold, HOME))
        t.see(Surface.MESSAGES, min + 20_000)
        assertNull(t.progress(min + 20_000, threshold, HOME))
        // Mimo domov: nic se nepočítá.
        t.see(Surface.REELS, min + 30_000, AWAY)
        assertNull(t.progress(min + 30_000, threshold, AWAY))
    }

    @Test
    fun hiddenWhileUnlockedAndAfterClose() {
        val t = ScrollSessionTracker()
        t.scroll(0, 5 * min)
        assertNotNull(t.see(Surface.REELS, 5 * min))
        t.onUnlocked(15 * min)
        t.see(Surface.REELS, 6 * min)
        assertNull(t.progress(6 * min, threshold, HOME)) // zaplacený čas
        t.onClosed()
        t.see(Surface.REELS, 7 * min)
        assertEquals(0f, t.progress(7 * min, threshold, HOME)!!, 0.001f) // nová relace od nuly
    }

    @Test
    fun hiddenOnVideo() {
        val t = ScrollSessionTracker()
        t.see(Surface.SHORTS, 0)
        t.observe("org.mozilla.firefox", Surface.VIDEO, HOME, 10_000, threshold, video = VideoPlayback("A"), onVideoSite = true)
        assertNull(t.progress(10_000, threshold, HOME)) // video se nescrolluje, pruh by rušil
    }

    @Test
    fun percentRounding() {
        assertEquals(0, AccessibilityTriggerSource.toPercent(0f))
        assertEquals(49, AccessibilityTriggerSource.toPercent(0.499f))
        assertEquals(100, AccessibilityTriggerSource.toPercent(1f))
        assertEquals(100, AccessibilityTriggerSource.toPercent(1.2f))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun triggerSourceEmitsPercentOnlyOnChange() = runTest {
        val snapshots = MutableSharedFlow<ForegroundSnapshot>(extraBufferCapacity = 8)
        val source = AccessibilityTriggerSource(
            contextRepository = FakeContextRepository(),
            settings = FakeSettingsRepository(Settings(demoThresholdSec = 20)),
            scope = backgroundScope,
            snapshots = snapshots,
            clock = { testScheduler.currentTime },
            tickMillis = 5_000,
        )
        val seen = mutableListOf<Int?>()
        backgroundScope.launch { source.progressPercent.collect { seen += it } }
        runCurrent()
        val pkg = "com.zhiliaoapp.musically"
        snapshots.emit(ForegroundSnapshot(pkg, WatchedApp.TIKTOK, SnapshotKind.WINDOW_CHANGED, testScheduler.currentTime, "MainActivity"))
        runCurrent()
        // Opakované scrollování ve stejné milisekundě nic nového nevydá.
        repeat(5) { snapshots.emit(ForegroundSnapshot(pkg, WatchedApp.TIKTOK, SnapshotKind.SCROLLED, testScheduler.currentTime, "MainActivity")) }
        runCurrent()
        advanceTimeBy(10_001)
        runCurrent()
        assertEquals(listOf(null, 0, 25, 50), seen)
        advanceTimeBy(10_000) // 20 s = limit → Ozvěna, pruh pryč
        runCurrent()
        assertEquals(listOf(null, 0, 25, 50, 75, null), seen)
        source.onOutcome(InterventionOutcome.CLOSED)
        runCurrent()
        assertEquals(null, source.progressPercent.value)
    }
}
