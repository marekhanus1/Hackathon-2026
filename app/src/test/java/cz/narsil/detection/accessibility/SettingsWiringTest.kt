package cz.narsil.detection.accessibility

import cz.narsil.core.InterventionTrigger
import cz.narsil.core.Settings
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
import org.junit.Assert.assertNull
import org.junit.Test

/** 136: detekce čte toleranci, sledované aplikace a pauzu ochrany z Nastavení. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsWiringTest {
    private val tiktok = "com.zhiliaoapp.musically"

    private class Run(val source: AccessibilityTriggerSource, val got: MutableList<InterventionTrigger>, val snapshots: MutableSharedFlow<ForegroundSnapshot>)

    private fun TestScope.start(settings: Settings): Run {
        val snapshots = MutableSharedFlow<ForegroundSnapshot>(extraBufferCapacity = 8)
        val source = AccessibilityTriggerSource(
            FakeContextRepository(), FakeSettingsRepository(settings), backgroundScope, snapshots,
            clock = { testScheduler.currentTime }, tickMillis = 5_000,
        )
        val got = mutableListOf<InterventionTrigger>()
        backgroundScope.launch { source.triggers.collect { got += it } }
        runCurrent()
        return Run(source, got, snapshots)
    }

    private suspend fun TestScope.openTikTok(r: Run) {
        r.snapshots.emit(ForegroundSnapshot(tiktok, WatchedApp.TIKTOK, SnapshotKind.WINDOW_CHANGED, testScheduler.currentTime, "MainActivity"))
        runCurrent()
    }

    @Test
    fun toleranceFromSettings() = runTest {
        val r = start(Settings(toleranceMinutes = 3))
        openTikTok(r)
        advanceTimeBy(2 * 60_000L + 55_000)
        runCurrent()
        assertEquals(0, r.got.size)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(1, r.got.size) // 3 min místo výchozích 5
    }

    @Test
    fun demoThresholdStillWins() = runTest {
        val r = start(Settings(toleranceMinutes = 15, demoThresholdSec = 20))
        openTikTok(r)
        advanceTimeBy(21_000)
        runCurrent()
        assertEquals(1, r.got.size)
    }

    @Test
    fun untrackedAppIsIgnored() = runTest {
        val r = start(Settings(demoThresholdSec = 20, trackedApps = setOf(TrackedApp.INSTAGRAM, TrackedApp.YOUTUBE)))
        openTikTok(r)
        assertNull(r.source.progressPercent.value)
        assertNull(r.source.current.value)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(0, r.got.size)
    }

    @Test
    fun pausedProtectionNoTriggerNoProgress() = runTest {
        val r = start(Settings(demoThresholdSec = 20, protectionPausedUntilMillis = 60_000))
        openTikTok(r)
        assertNull(r.source.progressPercent.value)
        advanceTimeBy(50_000)
        runCurrent()
        assertEquals(0, r.got.size)
        // Pauza skončila (60 s): počítá se od nuly, zásah po dalších 20 s.
        advanceTimeBy(15_000)
        runCurrent()
        assertEquals(25, r.source.progressPercent.value) // relace od tiku v 60 s
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(1, r.got.size)
    }
}
