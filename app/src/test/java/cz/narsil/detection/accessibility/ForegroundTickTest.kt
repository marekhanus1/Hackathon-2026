package cz.narsil.detection.accessibility

import cz.narsil.core.InterventionTrigger
import cz.narsil.core.Settings
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 153: odchod na plochu bez události – tik nesmí dál „vidět“ YouTube a pustit Ozvěnu nad launcherem. */
@OptIn(ExperimentalCoroutinesApi::class)
class ForegroundTickTest {
    private val yt = "com.google.android.youtube"
    private val launcher = "com.qqlabs.minimalistlauncher"

    @Test
    fun leavingToLauncherWithoutEventStopsGateAndProgress() = runTest {
        var fg: String? = yt
        val snapshots = MutableSharedFlow<ForegroundSnapshot>(extraBufferCapacity = 8)
        val source = AccessibilityTriggerSource(
            FakeContextRepository(), FakeSettingsRepository(Settings(demoThresholdSec = 20)), backgroundScope, snapshots,
            clock = { testScheduler.currentTime }, tickMillis = 5_000, foreground = { fg },
        )
        val got = mutableListOf<InterventionTrigger>()
        backgroundScope.launch { source.triggers.collect { got += it } }
        runCurrent()
        fun snap(id: String, video: VideoPlayback? = null) = ForegroundSnapshot(
            yt, WatchedApp.YOUTUBE, SnapshotKind.CONTENT_CHANGED, testScheduler.currentTime, viewIds = setOf("$yt:id/$id"), video = video,
        )
        snapshots.emit(snap("reel_player_underlay"))
        runCurrent()
        advanceTimeBy(10_000)
        snapshots.emit(snap("watch_player", VideoPlayback(null)))
        runCurrent()
        advanceTimeBy(15_000) // limit vyprší na videu → brána čeká
        runCurrent()
        assertTrue(source.waitingForVideoEnd.value)
        assertEquals(0, got.size)

        // Home: launcher bez události pro detekci (překryv / vlastní služba launcheru).
        fg = launcher
        advanceTimeBy(15 * 60_000L) // i za pojistkou 10 min
        runCurrent()
        assertEquals(0, got.size) // nikdy nad launcherem
        assertFalse(source.waitingForVideoEnd.value)
        assertNull(source.progressPercent.value)
        assertNull(source.current.value)
    }

    @Test
    fun unknownForegroundKeepsOldBehaviour() = runTest {
        val snapshots = MutableSharedFlow<ForegroundSnapshot>(extraBufferCapacity = 8)
        val source = AccessibilityTriggerSource(
            FakeContextRepository(), FakeSettingsRepository(Settings(demoThresholdSec = 20)), backgroundScope, snapshots,
            clock = { testScheduler.currentTime }, tickMillis = 5_000, foreground = { null },
        )
        val got = mutableListOf<InterventionTrigger>()
        backgroundScope.launch { source.triggers.collect { got += it } }
        runCurrent()
        snapshots.emit(ForegroundSnapshot("com.zhiliaoapp.musically", WatchedApp.TIKTOK, SnapshotKind.WINDOW_CHANGED, testScheduler.currentTime, "MainActivity"))
        runCurrent()
        advanceTimeBy(21_000)
        runCurrent()
        assertEquals(1, got.size)
    }
}
