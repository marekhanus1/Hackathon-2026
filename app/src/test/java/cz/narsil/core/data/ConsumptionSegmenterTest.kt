package cz.narsil.core.data

import cz.narsil.core.ConsumptionEvent
import cz.narsil.core.Surface
import cz.narsil.core.data.ConsumptionSegmenter.Action
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsumptionSegmenterTest {
    private val ig = "com.instagram.android"
    private val tt = "com.zhiliaoapp.musically"
    private val sec = 1_000L

    private fun ev(pkg: String, surface: Surface, start: Long, now: Long) = ConsumptionEvent(pkg, surface, start, now - start)

    @Test
    fun opensThenExtendsEveryThirtySecondsAndClosesOnPause() {
        val s = ConsumptionSegmenter()
        val open = s.onEvent(ev(ig, Surface.REELS, 0, 0)).single() as Action.Open
        assertEquals("REELS", open.entity.surface)
        assertEquals("HOME", open.entity.context)
        assertEquals(0L, open.entity.durationMillis)
        assertTrue(s.onEvent(ev(ig, Surface.REELS, 0, 5 * sec)).isEmpty())
        assertTrue(s.onEvent(ev(ig, Surface.REELS, 0, 25 * sec)).isEmpty())
        assertEquals(listOf(Action.Extend(30 * sec)), s.onEvent(ev(ig, Surface.REELS, 0, 30 * sec)))
        assertTrue(s.onEvent(ev(ig, Surface.REELS, 0, 40 * sec)).isEmpty())
        // Pauza: dopíše se poslední známý konec (40 s), čas pauzy ne.
        assertEquals(listOf(Action.Extend(40 * sec)), s.onEvent(null))
        assertTrue(s.onEvent(null).isEmpty())
    }

    @Test
    fun surfaceOrAppSwitchSplitsSegmentsWithoutGaps() {
        val s = ConsumptionSegmenter()
        s.onEvent(ev(ig, Surface.FEED, 0, 0))
        s.onEvent(ev(ig, Surface.FEED, 0, 20 * sec))
        val a = s.onEvent(ev(tt, Surface.FEED, 0, 25 * sec)) // relace pokračuje v TikToku
        assertEquals(Action.Extend(20 * sec), a[0])
        val o = a[1] as Action.Open
        assertEquals(tt, o.entity.packageName)
        assertEquals(20 * sec, o.entity.startedAtMillis)
        assertEquals(5 * sec, o.entity.durationMillis)
    }

    @Test
    fun resumeAfterShortPauseDoesNotCountPause() {
        val s = ConsumptionSegmenter()
        s.onEvent(ev(ig, Surface.REELS, 0, 0))
        s.onEvent(ev(ig, Surface.REELS, 0, 60 * sec))
        s.onEvent(null) // chat 90 s, relace žije dál (pauza < 2 min)
        val o = s.onEvent(ev(ig, Surface.REELS, 0, 150 * sec)).single() as Action.Open
        assertEquals(150 * sec, o.entity.startedAtMillis) // od návratu, pauza 60–150 s se nepočítá
        assertEquals(0L, o.entity.durationMillis)
    }

    @Test
    fun recorderWritesInsertThenUpdates() = runTest {
        val dao = FakeConsumptionDao()
        val r = ConsumptionRecorder(dao)
        val s = ConsumptionSegmenter()
        r.apply(s.onEvent(ev(ig, Surface.REELS, 0, 0)))
        r.apply(s.onEvent(ev(ig, Surface.REELS, 0, 45 * sec)))
        r.apply(s.onEvent(null))
        assertEquals(1, dao.rows.size)
        assertEquals(45 * sec, dao.rows.values.single().durationMillis)
    }

    private class FakeConsumptionDao : ConsumptionDao {
        val rows = linkedMapOf<Long, ConsumptionEventEntity>()
        override suspend fun insert(event: ConsumptionEventEntity): Long {
            val id = rows.size + 1L
            rows[id] = event.copy(id = id)
            return id
        }
        override suspend fun updateDuration(id: Long, durationMillis: Long) {
            rows[id] = rows.getValue(id).copy(durationMillis = durationMillis)
        }
        override suspend fun between(fromMillis: Long, toMillis: Long) = rows.values.toList()
        override fun observeSince(fromMillis: Long) = throw UnsupportedOperationException()
        override fun observeFeedMinutesSince(fromMillis: Long) = throw UnsupportedOperationException()
    }
}
