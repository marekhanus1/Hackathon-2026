package cz.narsil.detection.accessibility

import cz.narsil.core.InterventionOutcome
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.Settings
import cz.narsil.core.Surface
import cz.narsil.core.TriggerReason
import cz.narsil.core.fake.FakeContextRepository
import cz.narsil.core.fake.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccessibilityTriggerSourceTest {
    @Test
    fun demoThresholdTriggersFromTicks() = runTest {
        val snapshots = MutableSharedFlow<ForegroundSnapshot>(extraBufferCapacity = 8)
        val source = AccessibilityTriggerSource(
            contextRepository = FakeContextRepository(),
            settings = FakeSettingsRepository(Settings(demoThresholdSec = 20)),
            scope = backgroundScope,
            snapshots = snapshots,
            clock = { testScheduler.currentTime },
            tickMillis = 5_000,
        )
        val got = mutableListOf<InterventionTrigger>()
        backgroundScope.launch { source.triggers.collect { got += it } }
        runCurrent()
        val pkg = "com.zhiliaoapp.musically"
        snapshots.emit(ForegroundSnapshot(pkg, WatchedApp.TIKTOK, SnapshotKind.WINDOW_CHANGED, testScheduler.currentTime, "MainActivity"))
        runCurrent()
        assertEquals(Surface.FEED, source.current.value?.surface)
        advanceTimeBy(21_000)
        runCurrent()
        assertEquals(1, got.size)
        assertEquals(TriggerReason.ENDLESS_SCROLL, got[0].reason)

        source.onOutcome(InterventionOutcome.CLOSED)
        advanceTimeBy(15_000)
        runCurrent()
        assertEquals(1, got.size) // nová relace od nuly
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(2, got.size)
    }
}
