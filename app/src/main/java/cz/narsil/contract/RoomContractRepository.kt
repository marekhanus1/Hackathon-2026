package cz.narsil.contract

import cz.narsil.core.ContractRepository
import cz.narsil.core.DailyContract
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskStatus
import cz.narsil.core.data.ContractDao
import cz.narsil.core.data.DayKey
import cz.narsil.core.data.toDomain
import cz.narsil.core.data.toEntity
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Smlouvy a úkoly plánu v Room (212, `ContractDao`). [today] sleduje smlouvu platnou dnes
 * a ve 4:00 sám přejde na nový den. Nic nehází: chyba databáze = null / nic se nezmění.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomContractRepository(
    private val dao: ContractDao,
    scope: CoroutineScope,
    private val clock: Clock = Clock.systemDefaultZone(),
    dayCheckMillis: Long = 60_000,
) : ContractRepository {

    private val days: Flow<LocalDate> = flow {
        while (true) {
            emit(DayKey.of(clock.millis(), clock.zone))
            delay(dayCheckMillis)
        }
    }.distinctUntilChanged()

    override val today: StateFlow<DailyContract?> = days
        .flatMapLatest { day ->
            val key = day.toString()
            combine(dao.observeContract(key), dao.observeTasks(key)) { contract, tasks -> contract?.toDomain(tasks) }
        }
        .catch { emit(null) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun save(contract: DailyContract) {
        val clean = ContractRules.sanitize(contract)
        val now = clock.millis()
        runCatching {
            dao.replaceContract(
                clean.toEntity(createdAtMillis = now),
                clean.tasks.map { it.toEntity(clean.date, verifiedAtMillis = if (it.status == TaskStatus.VERIFIED) now else null) },
            )
        }.onFailure {
            // Kolize id (např. úkoly zkopírované z jiného dne) → vložit jako nové.
            runCatching {
                dao.replaceContract(
                    clean.toEntity(createdAtMillis = now),
                    clean.tasks.map { it.copy(id = 0).toEntity(clean.date) },
                )
            }
        }
    }

    override suspend fun forDate(date: LocalDate): DailyContract? = runCatching {
        dao.contract(date.toString())?.toDomain(dao.tasksFor(date.toString()))
    }.getOrNull()

    /** Uloží stav, pokusy a důkaz úkolu; den úkolu a čas prvního ověření zůstanou. */
    override suspend fun updateTask(task: PlanTask) {
        runCatching {
            val existing = dao.task(task.id) ?: return
            val verifiedAt = existing.verifiedAtMillis
                ?: if (task.status == TaskStatus.VERIFIED) clock.millis() else null
            dao.updateTask(task.toEntity(LocalDate.parse(existing.date), verifiedAt).copy(id = existing.id))
        }
    }
}
