package cz.narsil.overlay

import cz.narsil.core.InterventionOutcome
import cz.narsil.core.fake.FakeInterventionTriggerSource
import cz.narsil.ui.screens.echo.EchoFeeling
import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionRecordsTest {
    @Test
    fun mapsResultToEntity() {
        val trigger = FakeInterventionTriggerSource.demoTrigger(1_000L)
        val e = InterventionRecords.entity(EchoResult(trigger, InterventionOutcome.CLOSED, true, 0, 50, EchoFeeling.UNAVA), 5_000L)
        assertEquals(5_000L, e.atMillis)
        assertEquals(trigger.event.packageName, e.packageName)
        assertEquals(trigger.reason.name, e.reason)
        assertEquals("CLOSED", e.outcome)
        assertEquals("unava", e.feeling) // jako mock data
        assertEquals(50, e.xpGained)
    }
}
