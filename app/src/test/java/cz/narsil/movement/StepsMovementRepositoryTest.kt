package cz.narsil.movement

import cz.narsil.movement.currency.CurrencyBook
import cz.narsil.movement.steps.CombinedStepsSource
import cz.narsil.movement.steps.StepCounterMath
import cz.narsil.movement.steps.StepsSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepsMovementRepositoryTest {
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = LocalDateTime.of(2026, 11, 28, 12, 0).toInstant(ZoneOffset.UTC)
    }

    private fun repo(alerts: MutableList<Int> = mutableListOf(), steps: StepsSource) =
        StepsMovementRepository(steps, CurrencyBook(clock = clock), clock) { alerts += it }

    @Test
    fun stepsBecomeBalance() = runTest {
        var steps = 2_350
        val repo = repo { steps }
        repo.refresh()
        assertEquals(2_350, repo.state.value.stepsToday)
        assertEquals(23, repo.state.value.balanceMinutes)
        steps = 4_000
        repo.refresh()
        assertEquals(40, repo.state.value.balanceMinutes)
    }

    @Test
    fun payBorrowRewardSquats() = runTest {
        val alerts = mutableListOf<Int>()
        val repo = repo(alerts) { 1_000 }
        repo.refresh()
        assertTrue(repo.pay(10))
        assertFalse(repo.pay(10))
        assertEquals(0, repo.state.value.balanceMinutes)
        repo.addEarnedMinutes(20)
        assertEquals(20, repo.state.value.balanceMinutes)
        repeat(3) { repo.borrow(10) }
        assertEquals(30, repo.state.value.debtMinutes)
        assertTrue(alerts.isEmpty())
        repo.borrow(10) // nad limit → Parťák
        assertEquals(30, repo.state.value.debtMinutes)
        assertEquals(listOf(40), alerts)
        assertEquals(10, repo.recordSquats(20))
        assertEquals(20, repo.squatsTotal)
    }

    @Test
    fun missingStepsKeepLastValue() = runTest {
        var steps: Int? = 3_000
        val repo = repo { steps }
        repo.refresh()
        steps = null
        repo.refresh()
        assertEquals(3_000, repo.state.value.stepsToday)
    }

    @Test
    fun debugAddSteps() = runTest {
        val repo = repo { 0 }
        repo.addSteps(1_000)
        assertEquals(1_000, repo.state.value.stepsToday)
        assertEquals(10, repo.state.value.balanceMinutes)
    }

    @Test
    fun combinedTakesHighestAvailable() = runTest {
        assertEquals(5_000, CombinedStepsSource({ null }, { 5_000 }, { 4_000 }).stepsToday())
        assertNull(CombinedStepsSource({ null }, { throw IllegalStateException() }).stepsToday())
    }

    @Test
    fun stepCounterAcrossDayAndReboot() {
        var s = StepCounterMath.update(StepCounterMath.Saved(null, 0, 0, 0), "2026-11-28", 10_000)
        assertEquals(0L, StepCounterMath.steps(s))
        s = StepCounterMath.update(s, "2026-11-28", 12_500)
        assertEquals(2_500L, StepCounterMath.steps(s))
        s = StepCounterMath.update(s, "2026-11-28", 300) // restart telefonu
        assertEquals(2_800L, StepCounterMath.steps(s))
        s = StepCounterMath.update(s, "2026-11-29", 900) // nový den
        assertEquals(0L, StepCounterMath.steps(s))
    }
}
