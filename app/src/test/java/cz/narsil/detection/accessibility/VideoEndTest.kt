package cz.narsil.detection.accessibility

import cz.narsil.core.ComfortContext.HOME
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.Settings
import cz.narsil.core.Surface
import cz.narsil.core.fake.FakeSettingsRepository
import cz.narsil.detection.youtube.VideoEndGate
import cz.narsil.detection.youtube.VideoPlayback
import cz.narsil.detection.youtube.YouTubePlayback
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 134: limit vyprší během videa → Ozvěna až na konci videa / u jiného videa / po stropu. */
class VideoEndTest {
    private val ff = "org.mozilla.firefox"
    private val yt = "com.google.android.youtube"
    private val sec = 1_000L
    private val min = 60_000L
    private val threshold = 5 * min

    private fun play(key: String?, pos: Long? = null, dur: Long? = null, ended: Boolean = false, at: Long = 0) =
        VideoPlayback(key, pos, dur, ended, at)

    /** Sledování videa po tiku 5 s v [from, to): zásah nesmí přijít. */
    private fun ScrollSessionTracker.watch(from: Long, to: Long, pkg: String = ff, video: (Long) -> VideoPlayback?) {
        var now = from
        while (now < to) {
            assertNull("zásah uprostřed videa v $now", observe(pkg, Surface.VIDEO, HOME, now, threshold, video = video(now), onVideoSite = true))
            now += 5 * sec
        }
    }

    /** Tracker po 4 min Shorts ve Firefoxu (relace od 0, limit v 5:00). */
    private fun shortsThenVideo(): ScrollSessionTracker {
        val t = ScrollSessionTracker()
        var now = 0L
        while (now < 4 * min) {
            assertNull(t.observe(ff, Surface.SHORTS, HOME, now, threshold, scrolled = true, onVideoSite = true))
            now += 10 * sec
        }
        return t
    }

    @Test
    fun limitDuringVideoWaitsForEndScreen() {
        val t = shortsThenVideo()
        // Video běží: 4:00 → 7:00, limit (5:00) uprostřed, nic.
        val now = 7 * min
        t.watch(4 * min, now) { play("A", at = it) }
        assertTrue(t.isWaitingForVideoEnd)
        assertEquals(Surface.VIDEO, t.session?.surface)
        // End-screen „Přehrát znovu“ → zásah.
        assertNotNull(t.observe(ff, Surface.VIDEO, HOME, now, threshold, video = play("A", ended = true, at = now), onVideoSite = true))
    }

    @Test
    fun switchToAnotherVideoTriggers() {
        val t = shortsThenVideo()
        t.watch(4 * min, 7 * min + 5 * sec) { play("A", at = it) }
        // Kliknutí na doporučené: jiné v=.
        assertNotNull(t.observe(ff, Surface.VIDEO, HOME, 7 * min + 5 * sec, threshold, video = play("B", at = 7 * min), onVideoSite = true))
    }

    @Test
    fun leavingVideoToShortsOrFeedTriggers() {
        val t = shortsThenVideo()
        t.watch(4 * min, 6 * min + 5 * sec) { play("A") }
        assertNotNull(t.observe(ff, Surface.SHORTS, HOME, 6 * min + 5 * sec, threshold, scrolled = true, onVideoSite = true))

        val f = shortsThenVideo()
        f.watch(4 * min, 6 * min + 5 * sec) { play("A") }
        // Návrat na m.youtube.com (feed, v klasifikaci MESSAGES „v nejistotě“).
        assertNotNull(f.observe(ff, Surface.MESSAGES, HOME, 6 * min + 5 * sec, threshold, onVideoSite = true))
    }

    @Test
    fun leavingFirefoxDoesNotTriggerButNewVideoAfterReturnDoes() {
        val t = shortsThenVideo()
        t.watch(4 * min, 6 * min + 5 * sec) { play("A") }
        // Plocha / jiná aplikace: mimo sledované nezasahovat.
        assertNull(t.observe("com.nothing.launcher", null, HOME, 6 * min + 10 * sec, threshold))
        // Zpět na stejné video: dál se čeká.
        assertNull(t.observe(ff, Surface.VIDEO, HOME, 6 * min + 40 * sec, threshold, video = play("A"), onVideoSite = true))
        assertTrue(t.isWaitingForVideoEnd)
        assertNotNull(t.observe(ff, Surface.VIDEO, HOME, 6 * min + 50 * sec, threshold, video = play("C"), onVideoSite = true))
    }

    @Test
    fun videoFromChatLinkDoesNotTrigger() {
        // SPEC 3: video z odkazu v chatu nic nespustí, ani když limit čekal na konec předchozího videa.
        val t = shortsThenVideo()
        t.watch(4 * min, 6 * min + 5 * sec) { play("A") }
        assertNull(t.observe("com.whatsapp", null, HOME, 6 * min + 10 * sec, threshold))
        t.watch(6 * min + 20 * sec, 8 * min) { play("C") }
    }

    @Test
    fun estimatedEndFromPlayerTime() {
        val t = shortsThenVideo()
        val at = 5 * min
        // Ovládání ukázalo 1:00 / 4:00 → konec v 8:00 + rezerva 20 s.
        val v = play("A", pos = 1 * min, dur = 4 * min, at = at)
        t.watch(4 * min, 8 * min + 20 * sec) { if (it >= at) v else play("A") }
        assertNotNull(t.observe(ff, Surface.VIDEO, HOME, 8 * min + 20 * sec, threshold, video = v, onVideoSite = true))
    }

    @Test
    fun capWhenVideoIsLongOrUnreadable() {
        // Hodinové video: strop 15 min od vypršení limitu.
        val g = VideoEndGate()
        val long = play("A", pos = 0, dur = 60 * min, at = 0)
        assertEquals(false, g.mayIntervene(long, 0))
        assertEquals(false, g.mayIntervene(long, VideoEndGate.MAX_WAIT_MILLIS - 1))
        assertEquals(true, g.mayIntervene(long, VideoEndGate.MAX_WAIT_MILLIS))
        // Detekce selhala (žádný stav přehrávače): strop 10 min.
        val u = VideoEndGate()
        assertEquals(false, u.mayIntervene(null, 0))
        assertEquals(false, u.mayIntervene(play(null), VideoEndGate.UNKNOWN_WAIT_MILLIS - 1))
        assertEquals(true, u.mayIntervene(null, VideoEndGate.UNKNOWN_WAIT_MILLIS))
    }

    @Test
    fun videoAloneNeverStartsSession() {
        // Video z odkazu (bez předchozího scrollování) relaci nezačne, ani po hodině.
        val t = ScrollSessionTracker()
        var now = 0L
        while (now < 60 * min) {
            assertNull(t.observe(ff, Surface.VIDEO, HOME, now, threshold, video = play("A"), onVideoSite = true))
            now += 30 * sec
        }
        assertNull(t.session)
    }

    @Test
    fun youtubeAppSameRules() {
        // Aplikace YouTube: Shorts → video (bez ID), konec podle end-screen.
        val t = ScrollSessionTracker()
        var now = 0L
        while (now < 4 * min) {
            t.observe(yt, Surface.SHORTS, HOME, now, threshold, scrolled = true, onVideoSite = true)
            now += 10 * sec
        }
        // Domovská stránka (MESSAGES) cestou na video relaci neblokuje jako chat.
        assertNull(t.observe(yt, Surface.MESSAGES, HOME, now, threshold, onVideoSite = true))
        t.watch(now + 5 * sec, 7 * min, yt) { play(null) }
        val ended = YouTubePlayback.parse(null, listOf("Přehrát znovu"), 7 * min)
        assertNotNull(t.observe(yt, Surface.VIDEO, HOME, 7 * min, threshold, video = ended, onVideoSite = true))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun triggerSourceWithFirefoxSnapshots() = runTest {
        val snapshots = MutableSharedFlow<ForegroundSnapshot>(extraBufferCapacity = 8)
        val source = AccessibilityTriggerSource(
            contextRepository = cz.narsil.core.fake.FakeContextRepository(),
            settings = FakeSettingsRepository(Settings(demoThresholdSec = 20)),
            scope = backgroundScope,
            snapshots = snapshots,
            clock = { testScheduler.currentTime },
            tickMillis = 5_000,
        )
        val got = mutableListOf<InterventionTrigger>()
        backgroundScope.launch { source.triggers.collect { got += it } }
        runCurrent()
        fun snap(url: String, video: VideoPlayback? = null) = ForegroundSnapshot(
            ff, WatchedApp.FIREFOX, SnapshotKind.CONTENT_CHANGED, testScheduler.currentTime, url = url, video = video,
        )
        snapshots.emit(snap("m.youtube.com/shorts/abcdefghijk"))
        runCurrent()
        advanceTimeBy(10_000)
        snapshots.emit(snap("m.youtube.com/watch?v=AAAAAAAAAAA", YouTubePlayback.parse("AAAAAAAAAAA", emptyList(), testScheduler.currentTime)))
        runCurrent()
        assertEquals(Surface.VIDEO, source.current.value?.surface)
        advanceTimeBy(60_000) // limit 20 s dávno pryč, video běží (tiky)
        runCurrent()
        assertEquals(0, got.size)
        assertTrue(source.waitingForVideoEnd.value) // EchoOverlayStarter teď čte okno sám
        snapshots.emit(snap("m.youtube.com/watch?v=BBBBBBBBBBB", YouTubePlayback.parse("BBBBBBBBBBB", emptyList(), testScheduler.currentTime)))
        runCurrent()
        assertEquals(1, got.size)
        assertEquals(Surface.VIDEO, got[0].event.surface)
        assertEquals(false, source.waitingForVideoEnd.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun staleEndScreenAtVideoOpenDoesNotTrigger() = runTest {
        // 152: Shorts → video přes intent se zastaralým end-screenem; Ozvěna až na skutečném konci.
        val snapshots = MutableSharedFlow<ForegroundSnapshot>(extraBufferCapacity = 8)
        val source = AccessibilityTriggerSource(
            contextRepository = cz.narsil.core.fake.FakeContextRepository(),
            settings = FakeSettingsRepository(Settings(demoThresholdSec = 20)),
            scope = backgroundScope,
            snapshots = snapshots,
            clock = { testScheduler.currentTime },
            tickMillis = 5_000,
        )
        val got = mutableListOf<InterventionTrigger>()
        backgroundScope.launch { source.triggers.collect { got += it } }
        runCurrent()
        fun snap(ids: String, video: VideoPlayback? = null) = ForegroundSnapshot(
            yt, WatchedApp.YOUTUBE, SnapshotKind.CONTENT_CHANGED, testScheduler.currentTime, viewIds = setOf("$yt:id/$ids"), video = video,
        )
        snapshots.emit(snap("reel_player_underlay"))
        runCurrent()
        advanceTimeBy(25_000) // limit vyprší na Shorts → Ozvěna
        runCurrent()
        got.clear()
        // Okno zavřené bez volby, relace běží dál: další zásah smí přijít, ale ne uprostřed videa.
        source.onOutcome(cz.narsil.core.InterventionOutcome.PLANNED_FREE)
        snapshots.emit(snap("watch_player", VideoPlayback(null, ended = true)))
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(0, got.size) // zastaralý end-screen
        assertTrue(source.waitingForVideoEnd.value)
        snapshots.emit(snap("watch_player", VideoPlayback(null)))
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(0, got.size)
        snapshots.emit(snap("watch_player", VideoPlayback(null, ended = true)))
        runCurrent()
        assertEquals(1, got.size)
    }
}
