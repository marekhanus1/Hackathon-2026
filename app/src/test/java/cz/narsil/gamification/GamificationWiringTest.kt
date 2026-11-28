package cz.narsil.gamification

import cz.narsil.core.InterventionOutcome
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.core.data.BadgeDao
import cz.narsil.core.data.BadgeEntity
import cz.narsil.core.data.LedgerDao
import cz.narsil.core.data.LedgerEntryEntity
import cz.narsil.core.data.LedgerKind
import cz.narsil.core.data.XpDao
import cz.narsil.core.data.XpEventEntity
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.core.fake.FakeMovementRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeXpDao : XpDao {
    val rows = MutableStateFlow(listOf<XpEventEntity>())
    override suspend fun insert(event: XpEventEntity): Long {
        rows.value = rows.value + event.copy(id = rows.value.size + 1L)
        return rows.value.size.toLong()
    }
    override fun observeTotal(): Flow<Int> = rows.map { r -> r.sumOf { it.xp } }
    override suspend fun total(): Int = rows.value.sumOf { it.xp }
    override suspend fun forDate(date: String): List<XpEventEntity> = rows.value.filter { it.date == date }
    override fun observeForDate(date: String): Flow<List<XpEventEntity>> = rows.map { r -> r.filter { it.date == date } }
    override suspend fun countForDate(date: String, source: String): Int = rows.value.count { it.date == date && it.source == source }
}

private class FakeBadgeDao : BadgeDao {
    val rows = MutableStateFlow(listOf<BadgeEntity>())
    override suspend fun insert(badge: BadgeEntity): Long {
        if (rows.value.any { it.id == badge.id }) return -1
        rows.value = rows.value + badge
        return 1
    }
    override suspend fun all(): List<BadgeEntity> = rows.value
    override fun observeAll(): Flow<List<BadgeEntity>> = rows
}

private class FakeLedgerDao : LedgerDao {
    val rows = mutableListOf<LedgerEntryEntity>()
    override suspend fun insert(entry: LedgerEntryEntity): Long {
        rows += entry
        return rows.size.toLong()
    }
    override suspend fun forDate(date: String): List<LedgerEntryEntity> = rows.filter { it.date == date }
    override fun observeForDate(date: String): Flow<List<LedgerEntryEntity>> = MutableStateFlow(rows.filter { it.date == date })
    override suspend fun sumForDate(date: String, kind: String): Int = rows.filter { it.date == date && it.kind == kind }.sumOf { it.minutes }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GamificationWiringTest {
    private val day = LocalDate.of(2026, 11, 28)

    @Test
    fun roomStorePersistsAcrossRestart() = runTest {
        val clock = TestClock(LocalDateTime.of(day, java.time.LocalTime.of(10, 0)))
        val xp = FakeXpDao()
        val badges = FakeBadgeDao()
        val ledger = FakeLedgerDao()
        val store = RoomGamificationStore(xp, badges, ledger, clock)
        val engine = RulesGamificationEngine(store, clock)
        engine.onOutcome(InterventionOutcome.CLOSED)
        engine.onTaskVerified(PlanTask(id = 1, type = TaskType.SCHOOL, title = "Úkol"))
        ledger.insert(LedgerEntryEntity(date = day.toString(), atMillis = 0, kind = LedgerKind.TASK_REWARD, minutes = 20))
        assertEquals(2, xp.rows.value.size)
        assertEquals(emptyList<String>(), badges.rows.value.map { it.id }) // zavření ještě nepotvrzené (155)

        clock.advanceMinutes(10)
        val restarted = RulesGamificationEngine(RoomGamificationStore(xp, badges, ledger, clock), clock)
        restarted.refresh()
        val state = restarted.state.value
        assertEquals(3, xp.rows.value.size) // + CLOSE_CONFIRMED
        assertEquals(listOf(Badges.FIRST_CLOSE), badges.rows.value.map { it.id })
        assertEquals(80, state.xp)
        assertEquals(15 + 20, state.today.reclaimedMinutes) // CLOSED × 15 + odměna úkolu z ledgeru
        assertEquals(listOf(Badges.FIRST_CLOSE), state.badges)
    }

    @Test
    fun fulfilledContractGivesXpOnce() = runTest {
        val clock = TestClock(LocalDateTime.of(day, java.time.LocalTime.of(12, 0)))
        val engine = RulesGamificationEngine(clock = clock)
        val contracts = FakeContractRepository(clock = { day })
        val movement = FakeMovementRepository(stepsToday = 7_000)
        val job = watchContractFulfillment(engine, contracts, movement, backgroundScope)
        runCurrent()
        assertEquals(0, engine.state.value.xp) // demo smlouva má úkoly, které čekají

        val contract = contracts.today.value!!
        contract.tasks.forEach { contracts.updateTask(it.copy(status = TaskStatus.VERIFIED)) }
        runCurrent()
        assertEquals(100, engine.state.value.xp)
        assertEquals(1, engine.state.value.streakDays)

        movement.setSteps(7_500)
        runCurrent()
        assertEquals(100, engine.state.value.xp)
        assertTrue(job.isActive)
    }
}
