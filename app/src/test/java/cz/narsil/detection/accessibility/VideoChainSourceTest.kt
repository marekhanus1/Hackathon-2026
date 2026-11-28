package cz.narsil.detection.accessibility

import cz.narsil.core.InterventionOutcome
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.Settings
import cz.narsil.core.Surface
import cz.narsil.core.TriggerReason
import cz.narsil.core.fake.FakeContextRepository
import cz.narsil.core.fake.FakeSettingsRepository
import cz.narsil.detection.youtube.VideoPlayback
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 137: řetězení přes AccessibilityTriggerSource se snímky Firefoxu. */
@OptIn(ExperimentalCoroutinesApi::class)
class VideoChainSourceTest {
    private val ff = "org.mozilla.firefox"

    private fun TestScope.snap(id: String, title: String) = ForegroundSnapshot(
        ff, WatchedApp.FIREFOX, SnapshotKind.WINDOW_CHANGED, testScheduler.currentTime,
        url = "m.youtube.com/watch?v=$id", video = VideoPlayback(id, title = title, readAtMillis = testScheduler.currentTime),
    )

    @Test
    fun chainTriggersWithPlannedSkipped() = runTest {
        val snapshots = MutableSharedFlow<ForegroundSnapshot>(extraBufferCapacity = 8)
        val checked = mutableListOf<String>()
        val source = AccessibilityTriggerSource(
            FakeContextRepository(), FakeSettingsRepository(Settings(videoChainLimit = 2)), backgroundScope, snapshots,
            clock = { testScheduler.currentTime }, tickMillis = 5_000,
            planCheck = { t -> checked += t; t.contains("Rust") },
            planDelayMillis = 2_000,
        )
        val got = mutableListOf<InterventionTrigger>()
        backgroundScope.launch { source.triggers.collect { got += it } }
        runCurrent()
        snapshots.emit(snap("AAAAAAAAAAA", "Kočky kompilace"))
        advanceTimeBy(60_000)
        snapshots.emit(snap("BBBBBBBBBBB", "Přednáška o Rustu"))
        advanceTimeBy(60_000)
        assertEquals(0, got.size) // plánované se nepočítá
        snapshots.emit(snap("CCCCCCCCCCC", "Další kočky"))
        advanceTimeBy(1_000)
        assertEquals(0, got.size) // čeká na titulek a ContentMatcher
        advanceTimeBy(2_000)
        assertEquals(1, got.size)
        assertEquals(TriggerReason.UNPLANNED_VIDEO_CHAIN, got[0].reason)
        assertEquals(Surface.VIDEO, got[0].event.surface)
        assertEquals("Další kočky", got[0].event.videoTitle)
        assertEquals(listOf("Kočky kompilace", "Přednáška o Rustu", "Další kočky"), checked)

        source.onOutcome(InterventionOutcome.CLOSED)
        snapshots.emit(snap("DDDDDDDDDDD", "Pes"))
        advanceTimeBy(5_000)
        assertEquals(1, got.size) // nový řetěz od nuly
    }
}
