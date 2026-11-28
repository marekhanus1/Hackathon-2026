package cz.narsil.overlay

import cz.narsil.core.EchoVideo
import cz.narsil.core.MovementState
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.core.fake.FakeInterventionTriggerSource
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoStateBuilderTest {
    private val trigger = FakeInterventionTriggerSource.demoTrigger(now = 10 * 60_000L)

    @Test
    fun buildsFromMovementContractAndVideo() {
        val contract = FakeContractRepository.demoContract(LocalDate.of(2026, 11, 28))
        val c = EchoStateBuilder.build(
            trigger,
            MovementState(stepsToday = 4_200, balanceMinutes = 42, debtMinutes = 10),
            contract,
            EchoVideo("file://v.mp4", LocalDate.of(2026, 11, 27)),
        )
        assertEquals("TikTok", c.ui.sourceAppName)
        assertEquals(4_200, c.ui.availableSteps)
        assertEquals(1_000, c.ui.stepCostForTenMin)
        assertEquals(10, c.ui.currentDebtMinutes)
        assertEquals("file://v.mp4", c.videoUri)
        assertEquals("Dnes jsi chtěl: Běh", c.fallbackText)
    }

    @Test
    fun withoutContractOrVideo() {
        val c = EchoStateBuilder.build(trigger, MovementState(), null, null)
        assertNull(c.videoUri)
        assertNull(c.fallbackText)
        assertEquals("Scrolluješ už 6 min.", c.ui.videoCaption)
    }

    @Test
    fun valueNoteWinsCaption() {
        val contract = FakeContractRepository.demoContract(LocalDate.of(2026, 11, 28))
        val c = EchoStateBuilder.build(trigger, MovementState(), contract, null, valueNote = "Minule ti feed na TikTok nedal nic.")
        assertEquals("Minule ti feed na TikTok nedal nic.", c.ui.videoCaption)
        assertEquals("Dnes jsi chtěl: Běh", c.fallbackText) // text místo videa zůstává z plánu
    }

    /** 159: Půjčit je aktivní, jen když dluh po půjčce (+10) nepřekročí limit; stejně jako EchoOutcomeHandler. */
    @Test
    fun borrowDisabledOverDebtLimit() {
        fun canBorrow(debt: Int, limit: Int) =
            EchoStateBuilder.build(trigger, MovementState(debtMinutes = debt, debtLimitMinutes = limit), null, null).ui.canBorrow
        assertTrue(canBorrow(debt = 0, limit = 30))
        assertTrue(canBorrow(debt = 20, limit = 30)) // přesně na limit
        assertFalse(canBorrow(debt = 30, limit = 30))
        assertFalse(canBorrow(debt = 0, limit = 0)) // limit 0 v Nastavení = půjčky vypnuté
        assertEquals(45, EchoStateBuilder.build(trigger, MovementState(debtLimitMinutes = 45), null, null).ui.debtLimitMinutes)
    }
}
