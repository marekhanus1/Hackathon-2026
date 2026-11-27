package cz.narsil.movement.currency

import cz.narsil.core.data.LedgerKind
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class MutableClock(start: LocalDateTime) : Clock() {
    var now: Instant = start.toInstant(ZoneOffset.UTC)
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = now
    fun set(time: LocalDateTime) {
        now = time.toInstant(ZoneOffset.UTC)
    }
}

class CurrencyTest {
    private val morning = LocalDateTime.of(2026, 11, 28, 10, 0)

    @Test
    fun balanceFormula() {
        assertEquals(42, Currency.balance(4_299, DayMinutes()))
        assertEquals(42 + 20 - 10 - 5, Currency.balance(4_200, DayMinutes(taskRewards = 20, paid = 10, carriedDebt = 5)))
        assertEquals(0, Currency.balance(500, DayMinutes(carriedDebt = 30))) // nikdy pod 0
    }

    @Test
    fun squatsUnlockWholeBatches() {
        assertEquals(10, Currency.squatMinutes(20))
        assertEquals(10, Currency.squatMinutes(39))
        assertEquals(20, Currency.squatMinutes(40))
        assertEquals(0, Currency.squatMinutes(5))
    }

    @Test
    fun payNeverBelowZero() = runTest {
        val book = CurrencyBook(clock = MutableClock(morning))
        assertTrue(book.pay(stepsToday = 1_500, minutes = 10))
        assertEquals(5, book.state(1_500).balanceMinutes)
        assertFalse(book.pay(stepsToday = 1_500, minutes = 10))
        assertEquals(5, book.state(1_500).balanceMinutes)
        assertTrue(book.pay(stepsToday = 2_500, minutes = 10)) // ušel další kroky
        assertEquals(5, book.state(2_500).balanceMinutes)
    }

    @Test
    fun borrowUpToLimitThenPartnerAlert() = runTest {
        val book = CurrencyBook(clock = MutableClock(morning))
        repeat(3) { assertTrue(book.borrow(10).accepted) }
        assertEquals(30, book.state(0).debtMinutes)
        val over = book.borrow(10)
        assertFalse(over.accepted)
        assertTrue(over.partnerAlert)
        assertEquals(30, book.state(0).debtMinutes)
        assertFalse(book.state(0).isOverDebtLimit)
    }

    @Test
    fun taskRewardRaisesBalance() = runTest {
        val book = CurrencyBook(clock = MutableClock(morning))
        book.addEarnedMinutes(30)
        assertEquals(30, book.state(0).balanceMinutes)
        assertTrue(book.pay(0, 10))
        assertEquals(20, book.state(0).balanceMinutes)
    }

    @Test
    fun debtCarriesToNextDayOnce() = runTest {
        val clock = MutableClock(morning)
        val store = InMemoryLedgerStore()
        val book = CurrencyBook(store, clock)
        book.borrow(10)
        book.borrow(10)
        assertEquals(20, book.state(3_000).debtMinutes)
        assertEquals(30, book.state(3_000).balanceMinutes) // dnes dluh zůstatek nesnižuje

        clock.set(morning.plusDays(1))
        val next = book.state(3_000)
        assertEquals(0, next.debtMinutes) // dluh se vynuloval…
        assertEquals(10, next.balanceMinutes) // …a odečetl od zůstatku: 30 − 20
        book.state(3_000)
        assertEquals(1, store.entries(book.today()).count { it.kind == LedgerKind.DEBT_CARRY }) // přenos jen jednou

        clock.set(morning.plusDays(2))
        assertEquals(30, book.state(3_000).balanceMinutes) // předevčírem dluh už nepřechází
    }

    @Test
    fun dayStartsAtFour() = runTest {
        val clock = MutableClock(LocalDateTime.of(2026, 11, 28, 23, 0))
        val book = CurrencyBook(clock = clock)
        book.borrow(10)
        clock.set(LocalDateTime.of(2026, 11, 29, 3, 30)) // pořád stejný den
        assertEquals(10, book.state(0).debtMinutes)
        clock.set(LocalDateTime.of(2026, 11, 29, 4, 30)) // nový den
        assertEquals(0, book.state(0).debtMinutes)
    }
}
