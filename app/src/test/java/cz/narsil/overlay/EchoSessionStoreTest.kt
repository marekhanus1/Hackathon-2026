package cz.narsil.overlay

import cz.narsil.core.fake.FakeInterventionTriggerSource
import cz.narsil.overlay.EchoSessionStore.Phase
import cz.narsil.overlay.EchoSessionStore.Restore
import cz.narsil.ui.screens.echo.EchoFeeling
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoSessionStoreTest {
    private val trigger = FakeInterventionTriggerSource.demoTrigger(1_000L)
    private val min = 60_000L

    @Test
    fun keepsFeelingAndPhaseForRestore() {
        val s = EchoSessionStore()
        assertNull(s.takeForRestore(0))
        s.begin(trigger, 0)
        s.setFeeling(EchoFeeling.NUDA)
        s.setPhase(Phase.EXERCISE)
        // Služba se odpojila a připojila: nový ovladač dostane stejný rozdělaný stav.
        val r = s.takeForRestore(30_000) as Restore.Show
        assertEquals(trigger, r.pending.trigger)
        assertEquals(EchoFeeling.NUDA, r.pending.feeling)
        assertEquals(Phase.EXERCISE, r.pending.phase)
        // Obnova je opakovatelná (služba může vypadnout víckrát), dokud nepadne volba.
        assertTrue(s.takeForRestore(40_000) is Restore.Show)
    }

    @Test
    fun beginWithSameTriggerKeepsOriginalTime() {
        val s = EchoSessionStore()
        s.begin(trigger, 0)
        s.setFeeling(EchoFeeling.SAMOTA)
        s.begin(trigger, 9 * min) // obnovené okno volá begin znovu
        assertEquals(0L, s.pending?.shownAtMillis)
        assertEquals(EchoFeeling.SAMOTA, s.pending?.feeling)
        s.begin(FakeInterventionTriggerSource.demoTrigger(2_000L), 9 * min) // nový zásah
        assertEquals(9 * min, s.pending?.shownAtMillis)
        assertNull(s.pending?.feeling)
    }

    @Test
    fun choiceClearsAndOldPendingExpires() {
        val s = EchoSessionStore()
        s.begin(trigger, 0)
        s.clear()
        assertNull(s.takeForRestore(1_000))

        s.begin(trigger, 0)
        val r = s.takeForRestore(EchoSessionStore.MAX_AGE_MILLIS + 1)
        assertTrue(r is Restore.Expired)
        assertNull(s.pending)
        assertNull(s.takeForRestore(EchoSessionStore.MAX_AGE_MILLIS + 2))
    }
}
