package cz.narsil.gamification

import cz.narsil.core.InterventionOutcome
import cz.narsil.core.fake.FakeGamificationEngine
import cz.narsil.gamification.debug.GamificationDebug
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoTuningTest {
    @Test
    fun demoStartsJustBelowLevel() {
        assertEquals(1_480, DemoTuning.DEMO_START_XP)
        assertEquals(3, DemoTuning.DEMO_STATE.level)
        assertEquals(480, DemoTuning.DEMO_STATE.xpInLevel)
        assertTrue(DemoTuning.levelsUp(DemoTuning.DEMO_START_XP, GamificationRules.XP_SQUATS))
        assertFalse(DemoTuning.levelsUp(DemoTuning.DEMO_START_XP, GamificationRules.XP_PAID_STEPS))
    }

    @Test
    fun xpToPrepare() {
        assertEquals(480, DemoTuning.xpToPrepare(0))
        assertEquals(140, DemoTuning.xpToPrepare(1_340)) // fake DEMO_STATE → 1 480
        assertEquals(0, DemoTuning.xpToPrepare(980))
        assertEquals(490, DemoTuning.xpToPrepare(990)) // za hranicí → stejná pozice v další úrovni
    }

    @Test
    fun wholeDemoOnRealEngineEndsWithLevelUp() = runTest {
        val clock = TestClock(LocalDateTime.of(2026, 11, 28, 14, 0))
        val engine = RulesGamificationEngine(clock = clock)
        engine.addXp(777) // libovolná historie
        assertTrue(GamificationDebug.prepareDemo(engine))
        val before = engine.state.value
        assertEquals(480, before.xpInLevel)

        // Scénář: smlouva (0), venku nic, doma Ozvěna → 20 dřepů naživo.
        clock.advanceMinutes(2)
        val squatsXp = engine.onOutcome(InterventionOutcome.SQUATS)
        assertTrue(DemoTuning.levelsUp(before.xp, squatsXp))
        assertEquals(before.level + 1, engine.state.value.level)
        // Long content: plánované zdarma, neplánované za kroky – žádná druhá úroveň.
        engine.onOutcome(InterventionOutcome.PLANNED_FREE)
        engine.onOutcome(InterventionOutcome.PAID_STEPS)
        assertEquals(before.level + 1, engine.state.value.level)
    }

    @Test
    fun prepareDemoOnFake() = runTest {
        val fake = FakeGamificationEngine()
        assertTrue(GamificationDebug.prepareDemo(fake))
        assertEquals(1_480, fake.state.value.xp)
        fake.onOutcome(InterventionOutcome.SQUATS)
        assertEquals(4, fake.state.value.level)
    }
}
