package cz.narsil.overlay

import cz.narsil.core.GamificationState
import cz.narsil.core.InterventionOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardBuilderTest {
    @Test
    fun levelUpFromSquats() {
        val r = RewardBuilder.build(InterventionOutcome.SQUATS, GamificationState(xp = 1_480, level = 3), GamificationState(xp = 1_500, level = 4))!!
        assertEquals(20, r.gainedXp)
        assertTrue(r.leveledUp)
        assertEquals("20 dřepů hotovo!", r.reasonTitle)
    }

    @Test
    fun noRewardWithoutXp() {
        val s = GamificationState(xp = 100)
        assertNull(RewardBuilder.build(InterventionOutcome.BORROWED, s, s))
        assertNull(RewardBuilder.build(InterventionOutcome.CLOSED, null, s))
    }

    @Test
    fun closedShowsPendingXp() {
        val s = GamificationState(xp = 1_480, level = 3)
        val r = RewardBuilder.build(InterventionOutcome.CLOSED, s, s, pendingCloseXp = 50)!!
        assertEquals(50, r.gainedXp)
        assertEquals(1_480, r.totalXpAfter) // zatím nepřipsané
        assertEquals(3, r.newLevel)
        assertTrue(r.encouragingSubtitle.contains("10 minut"))
        assertNull(RewardBuilder.build(InterventionOutcome.CLOSED, s, s, pendingCloseXp = 0)) // nad stropem
    }
}
