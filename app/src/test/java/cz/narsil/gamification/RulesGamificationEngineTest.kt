package cz.narsil.gamification

import cz.narsil.core.AvatarState
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Posouvatelné hodiny pro testy. */
class TestClock(start: LocalDateTime, private val zoneId: ZoneId = ZoneOffset.UTC) : Clock() {
    var now: Instant = start.atZone(zoneId).toInstant()
    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = TestClock(LocalDateTime.ofInstant(now, zone), zone)
    override fun instant(): Instant = now
    fun advanceMinutes(minutes: Long) {
        now = now.plusSeconds(minutes * 60)
    }
    fun set(time: LocalDateTime) {
        now = time.atZone(zoneId).toInstant()
    }
}

class RulesGamificationEngineTest {
    private val day = LocalDate.of(2026, 11, 28)
    private fun at(hour: Int, minute: Int = 0, date: LocalDate = day) = LocalDateTime.of(date, java.time.LocalTime.of(hour, minute))

    @Test
    fun xpForChoices() = runTest {
        val clock = TestClock(at(10))
        val engine = RulesGamificationEngine(clock = clock)
        assertEquals(50, engine.onOutcome(InterventionOutcome.CLOSED))
        assertEquals(20, engine.onOutcome(InterventionOutcome.SQUATS))
        assertEquals(10, engine.onOutcome(InterventionOutcome.PAID_STEPS))
        assertEquals(0, engine.onOutcome(InterventionOutcome.BORROWED))
        assertEquals(0, engine.onOutcome(InterventionOutcome.PLANNED_FREE))
        assertEquals(30, engine.onTaskVerified(PlanTask(type = TaskType.SCHOOL, title = "Úkol")))
        assertEquals(100, engine.onContractFulfilled())
        assertEquals(5, engine.onVideoRated(4))
        assertEquals(215, engine.state.value.xp)
    }

    @Test
    fun closedIsCappedAndNeedsGap() = runTest {
        val clock = TestClock(at(8))
        val engine = RulesGamificationEngine(clock = clock)
        assertEquals(50, engine.onOutcome(InterventionOutcome.CLOSED))
        clock.advanceMinutes(5)
        assertEquals(0, engine.onOutcome(InterventionOutcome.CLOSED)) // odstup < 10 min
        clock.advanceMinutes(5)
        assertEquals(50, engine.onOutcome(InterventionOutcome.CLOSED))
        repeat(3) {
            clock.advanceMinutes(10)
            assertEquals(50, engine.onOutcome(InterventionOutcome.CLOSED))
        }
        clock.advanceMinutes(30)
        assertEquals(0, engine.onOutcome(InterventionOutcome.CLOSED)) // 6. odměna ve dni ne
        clock.set(at(8, date = day.plusDays(1)))
        assertEquals(50, engine.onOutcome(InterventionOutcome.CLOSED)) // nový den
    }

    @Test
    fun levelsEvery500Xp() = runTest {
        val engine = RulesGamificationEngine(clock = TestClock(at(10)))
        engine.addXp(480)
        assertEquals(1, engine.state.value.level)
        engine.onOutcome(InterventionOutcome.CLOSED)
        assertEquals(530, engine.state.value.xp)
        assertEquals(2, engine.state.value.level)
        assertEquals(30, engine.state.value.xpInLevel)
        assertEquals(500, engine.state.value.xpPerLevel)
    }

    @Test
    fun contractOncePerDayAndStreak() = runTest {
        val clock = TestClock(at(20))
        val engine = RulesGamificationEngine(clock = clock)
        assertEquals(100, engine.onContractFulfilled())
        assertEquals(0, engine.onContractFulfilled())
        assertEquals(1, engine.state.value.streakDays)
        clock.set(at(20, date = day.plusDays(1)))
        engine.onContractFulfilled()
        assertEquals(2, engine.state.value.streakDays)
        // den bez smlouvy a bez žolíku: série po jeho skončení padá na 0
        clock.set(at(20, date = day.plusDays(3)))
        engine.refresh()
        assertEquals(0, engine.state.value.streakDays)
    }

    @Test
    fun jokerSavesStreak() = runTest {
        val clock = TestClock(at(20))
        val engine = RulesGamificationEngine(clock = clock)
        engine.onContractFulfilled()
        assertEquals(0, engine.onExtraSquats(30))
        assertEquals(1, engine.onExtraSquats(20)) // 50 navíc = žolík
        assertEquals(1, engine.state.value.jokers)
        // další den nesplněn → žolík se spotřebuje, série zůstane
        clock.set(at(20, date = day.plusDays(2)))
        engine.onContractFulfilled()
        assertEquals(0, engine.state.value.jokers)
        assertEquals(2, engine.state.value.streakDays)
    }

    @Test
    fun jokersMaxThree() = runTest {
        val engine = RulesGamificationEngine(clock = TestClock(at(12)))
        engine.onExtraSquats(250)
        assertEquals(3, engine.state.value.jokers)
    }

    @Test
    fun todayNeverBreaksStreak() {
        val records = listOf(XpRecord(day, 0, XpSource.CONTRACT, 100))
        val (streak, _) = GamificationCalculator.streakAndJokers(records, day.plusDays(1))
        assertEquals(1, streak)
    }

    @Test
    fun avatarFromLastSevenDays() {
        assertEquals(AvatarState.EXHAUSTED, GamificationRules.avatarFor(1))
        assertEquals(AvatarState.TIRED, GamificationRules.avatarFor(2))
        assertEquals(AvatarState.OK, GamificationRules.avatarFor(4))
        assertEquals(AvatarState.FIT, GamificationRules.avatarFor(6))
        assertEquals(AvatarState.THRIVING, GamificationRules.avatarFor(7))

        val start = day.minusDays(20)
        val noise = XpRecord(start, 0, XpSource.PLANNED_FREE, 0) // dlouhá historie
        val fulfilled = (0L until 5L).map { XpRecord(day.minusDays(it), 0, XpSource.CONTRACT, 100) }
        assertEquals(AvatarState.FIT, GamificationCalculator.avatar(fulfilled + noise, day))
        assertEquals(AvatarState.EXHAUSTED, GamificationCalculator.avatar(listOf(noise), day))
        // nový uživatel nezačíná vyčerpaný
        assertEquals(AvatarState.OK, GamificationCalculator.avatar(emptyList(), day))
    }

    @Test
    fun avatarOverride() = runTest {
        val engine = RulesGamificationEngine(clock = TestClock(at(12)))
        engine.setAvatar(AvatarState.THRIVING)
        assertEquals(AvatarState.THRIVING, engine.state.value.avatar)
        engine.setAvatar(null)
        assertEquals(AvatarState.OK, engine.state.value.avatar)
    }

    @Test
    fun dailyScoreAndWonDay() = runTest {
        val clock = TestClock(at(10))
        val engine = RulesGamificationEngine(clock = clock)
        engine.onOutcome(InterventionOutcome.CLOSED)
        engine.onOutcome(InterventionOutcome.PAID_STEPS)
        val score = engine.state.value.today
        assertEquals(15, score.reclaimedMinutes)
        assertEquals(10, score.feedMinutes)
        assertTrue(score.isWon)
        assertEquals(1, engine.state.value.wonDays)
        engine.onTaskVerified(PlanTask(type = TaskType.MOVEMENT, title = "Běh"))
        assertEquals(45, engine.state.value.today.reclaimedMinutes)
    }

    @Test
    fun feedMinutesFromDetection() = runTest {
        val engine = RulesGamificationEngine(clock = TestClock(at(10)), feedMinutesFor = { 40 })
        engine.onOutcome(InterventionOutcome.CLOSED)
        assertEquals(40, engine.state.value.today.feedMinutes)
        assertFalse(engine.state.value.today.isWon)
        assertEquals(0, engine.state.value.wonDays)
    }

    @Test
    fun bossWhenCriticalHourOnlyClosed() = runTest {
        val clock = TestClock(at(21, 15))
        val engine = RulesGamificationEngine(clock = clock)
        engine.onOutcome(InterventionOutcome.CLOSED)
        val before = engine.state.value.xp
        clock.set(at(22, 5))
        engine.refresh()
        assertEquals(before + 150, engine.state.value.xp)
        engine.refresh()
        assertEquals(before + 150, engine.state.value.xp) // jen jednou
    }

    @Test
    fun noBossWhenPaidInCriticalHour() = runTest {
        val clock = TestClock(at(21, 30))
        val engine = RulesGamificationEngine(clock = clock)
        engine.onOutcome(InterventionOutcome.PAID_STEPS)
        clock.set(at(23))
        engine.refresh()
        assertEquals(10, engine.state.value.xp)
    }

    @Test
    fun videoRatingCapped() = runTest {
        val engine = RulesGamificationEngine(clock = TestClock(at(10)))
        repeat(10) { assertEquals(5, engine.onVideoRated(5)) }
        assertEquals(0, engine.onVideoRated(5))
    }

    @Test
    fun badgesUnlockOnce() = runTest {
        val clock = TestClock(at(8))
        val store = InMemoryGamificationStore()
        val engine = RulesGamificationEngine(store, clock)
        engine.onOutcome(InterventionOutcome.CLOSED)
        assertEquals(listOf(Badges.FIRST_CLOSE), engine.state.value.badges)
        repeat(9) {
            clock.advanceMinutes(1)
            engine.onOutcome(InterventionOutcome.CLOSED)
        }
        assertTrue(Badges.CLOSE_10 in engine.state.value.badges)
        assertEquals(2, store.load().badges.size)
    }

    @Test
    fun squatsBadgeAndNoDebtWeek() {
        val squats = (1..50).map { XpRecord(day, it.toLong(), XpSource.SQUATS, 20) }
        assertTrue(Badges.SQUATS_1000 in GamificationCalculator.earnedBadges(squats, day))
        val week = listOf(XpRecord(day.minusDays(6), 0, XpSource.CLOSED, 50))
        assertTrue(Badges.NO_DEBT_WEEK in GamificationCalculator.earnedBadges(week, day))
        val borrowed = week + XpRecord(day.minusDays(2), 0, XpSource.BORROWED, 0)
        assertFalse(Badges.NO_DEBT_WEEK in GamificationCalculator.earnedBadges(borrowed, day))
    }

    @Test
    fun stateSurvivesRestart() = runTest {
        val clock = TestClock(at(10))
        val store = InMemoryGamificationStore()
        RulesGamificationEngine(store, clock).apply {
            onOutcome(InterventionOutcome.CLOSED)
            onContractFulfilled()
        }
        val restarted = RulesGamificationEngine(store, clock)
        restarted.refresh()
        assertEquals(150, restarted.state.value.xp)
        assertEquals(1, restarted.state.value.streakDays)
        assertEquals(listOf(Badges.FIRST_CLOSE), restarted.state.value.badges)
    }

    @Test
    fun dayStartsAtFour() = runTest {
        val clock = TestClock(at(3, 30, date = day.plusDays(1))) // 3:30 ještě patří do včerejška
        val engine = RulesGamificationEngine(clock = clock)
        engine.onContractFulfilled()
        clock.set(at(4, 30, date = day.plusDays(1)))
        assertEquals(100, engine.onContractFulfilled())
    }
}
