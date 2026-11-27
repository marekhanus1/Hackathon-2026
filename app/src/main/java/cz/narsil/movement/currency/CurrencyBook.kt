package cz.narsil.movement.currency

import cz.narsil.core.MovementRates
import cz.narsil.core.MovementState
import cz.narsil.core.data.DayKey
import cz.narsil.core.data.LedgerKind
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Úložiště pohybů minut po dnech. Room: `LedgerDao` (211), testy: [InMemoryLedgerStore]. */
interface LedgerStore {
    suspend fun entries(date: LocalDate): List<MinuteEntry>
    suspend fun add(date: LocalDate, entry: MinuteEntry)
}

class InMemoryLedgerStore : LedgerStore {
    private val days = mutableMapOf<LocalDate, MutableList<MinuteEntry>>()
    override suspend fun entries(date: LocalDate): List<MinuteEntry> = days[date].orEmpty().toList()
    override suspend fun add(date: LocalDate, entry: MinuteEntry) {
        days.getOrPut(date) { mutableListOf() } += entry
    }
}

/** Výsledek půjčky. [partnerAlert] = dluh by přesáhl limit → zpráva Parťákovi, nic se nepůjčilo. */
data class BorrowResult(val accepted: Boolean, val debtMinutes: Int, val partnerAlert: Boolean)

/**
 * Účetní kniha minut (203): platby, půjčky, odměny za úkoly a přenos dluhu přes den (od 4:00).
 * Kroky dodává volající (211 z Health Connect / senzoru). Nic nehází.
 */
class CurrencyBook(
    private val store: LedgerStore = InMemoryLedgerStore(),
    private val clock: Clock = Clock.systemDefaultZone(),
    val debtLimitMinutes: Int = MovementRates.DEFAULT_DEBT_LIMIT_MINUTES,
) {
    private val mutex = Mutex()

    fun today(): LocalDate = DayKey.of(clock.millis(), clock.zone)

    /** Součty za dnešek; při prvním přístupu v novém dni přenese včerejší dluh. */
    suspend fun todayMinutes(): DayMinutes = mutex.withLock { ensureCarry(today()) }

    suspend fun state(stepsToday: Int): MovementState {
        val day = todayMinutes()
        return MovementState(
            stepsToday = stepsToday,
            balanceMinutes = Currency.balance(stepsToday, day),
            debtMinutes = day.borrowed,
            debtLimitMinutes = debtLimitMinutes,
        )
    }

    /** Strhne [minutes] ze zůstatku; false = nestačí, nic se nestrhne. */
    suspend fun pay(stepsToday: Int, minutes: Int = MovementRates.MINUTES_PER_UNLOCK): Boolean = mutex.withLock {
        val date = today()
        val day = ensureCarry(date)
        if (!Currency.canPay(stepsToday, day, minutes)) return@withLock false
        add(date, LedgerKind.PAID, minutes)
    }

    /** Půjčí [minutes]; nad limit nepůjčí a vrátí `partnerAlert = true` (SPEC 5). */
    suspend fun borrow(minutes: Int = MovementRates.MINUTES_PER_UNLOCK): BorrowResult = mutex.withLock {
        val date = today()
        val day = ensureCarry(date)
        if (minutes <= 0) return@withLock BorrowResult(false, day.borrowed, false)
        if (!Currency.canBorrow(day.borrowed, minutes, debtLimitMinutes)) {
            return@withLock BorrowResult(false, day.borrowed, partnerAlert = true)
        }
        val ok = add(date, LedgerKind.BORROW, minutes)
        BorrowResult(ok, if (ok) day.borrowed + minutes else day.borrowed, false)
    }

    /** Odměna za ověřený úkol (RewardService) – zvýší zůstatek. */
    suspend fun addEarnedMinutes(minutes: Int) = mutex.withLock {
        if (minutes <= 0) return@withLock
        val date = today()
        ensureCarry(date)
        add(date, LedgerKind.TASK_REWARD, minutes)
    }

    private suspend fun add(date: LocalDate, kind: String, minutes: Int): Boolean =
        runCatching { store.add(date, MinuteEntry(kind, minutes, clock.millis())) }.isSuccess

    private suspend fun ensureCarry(date: LocalDate): DayMinutes {
        val day = DayMinutes.of(runCatching { store.entries(date) }.getOrDefault(emptyList()))
        if (day.carryDone) return day
        val yesterday = DayMinutes.of(runCatching { store.entries(date.minusDays(1)) }.getOrDefault(emptyList()))
        val carry = Currency.debtToCarry(yesterday)
        // Záznam i s 0 = přenos proběhl (jen jednou za den).
        add(date, LedgerKind.DEBT_CARRY, carry)
        return day.copy(carriedDebt = day.carriedDebt + carry, carryDone = true)
    }
}
