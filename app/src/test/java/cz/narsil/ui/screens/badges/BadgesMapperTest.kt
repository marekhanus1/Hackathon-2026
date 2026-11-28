package cz.narsil.ui.screens.badges

import cz.narsil.core.GamificationState
import cz.narsil.core.History
import cz.narsil.core.HistoryDay
import cz.narsil.ui.components.AvatarState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgesMapperTest {
    private val start = LocalDate.of(2026, 11, 1)

    @Test
    fun badgesProgressAndLeaderboard() {
        val days = (0 until 10).map { i ->
            HistoryDay(start.plusDays(i.toLong()), feedMinutes = 20, reclaimedMinutes = 60, closes = 1, squats = 40, borrows = if (i == 1) 1 else 0, hasData = true)
        }
        val s = BadgesMapper.map(GamificationState(streakDays = 2, badges = listOf("FIRST_CLOSE")), History(days), AvatarState.ENERGIZED, stepsToday = 5_000)
        val byId = s.badges.associateBy { it.id }
        assertTrue(byId.getValue("FIRST_CLOSE").isUnlocked)
        assertTrue(byId.getValue("CLOSE_10").isUnlocked) // 10 zavření
        assertTrue(byId.getValue("NO_DEBT_WEEK").isUnlocked) // dny 3–10 bez půjčky = 8
        assertEquals(400, byId.getValue("SQUATS_1000").progress)
        assertFalse(byId.getValue("SQUATS_1000").isUnlocked)
        assertEquals(2, byId.getValue("PLAN_STREAK_3").progress)
        assertTrue(s.badges.first().isUnlocked) // odemčené nahoře

        val me = s.leaderboard.single { it.isCurrentUser }
        assertEquals(7 * 60, me.reclaimedMinutes)
        assertEquals(listOf(1, 2, 3, 4), s.leaderboard.map { it.rank })
        assertEquals(s.leaderboard.sortedByDescending { it.reclaimedMinutes }, s.leaderboard)
        assertEquals(BadgesMapper.FRIENDS.sumOf { it.steps } + 5_000, s.partyCurrentSteps)
        assertEquals(AvatarState.ENERGIZED, s.currentAvatarState)
        assertTrue(s.avatarSummary7Days.startsWith("Posledních 7 dní: 7 výher"))
    }

    @Test
    fun emptyHistoryStartsAtZero() {
        val s = BadgesMapper.map(GamificationState(), History(), AvatarState.BALANCED, 0)
        assertTrue(s.badges.none { it.isUnlocked })
        assertEquals(0, s.leaderboard.single { it.isCurrentUser }.reclaimedMinutes)
    }
}
