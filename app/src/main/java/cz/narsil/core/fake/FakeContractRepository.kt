package cz.narsil.core.fake

import cz.narsil.core.ContractRepository
import cz.narsil.core.DailyContract
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.core.VerificationMethod
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake v paměti; dnes je předvyplněná demo smlouva ([demoContract]). Úkoly bez id dostanou id při [save]. */
class FakeContractRepository(
    private val clock: () -> LocalDate = { LocalDate.now() },
    withDemoContract: Boolean = true,
) : ContractRepository {
    private val contracts = mutableMapOf<LocalDate, DailyContract>()
    private var nextTaskId = 1L
    private val _today = MutableStateFlow<DailyContract?>(null)
    override val today: StateFlow<DailyContract?> = _today.asStateFlow()

    init {
        if (withDemoContract) store(demoContract(clock()))
    }

    override suspend fun save(contract: DailyContract) = store(contract)

    override suspend fun forDate(date: LocalDate): DailyContract? = contracts[date]

    override suspend fun updateTask(task: PlanTask) {
        val entry = contracts.entries.firstOrNull { (_, c) -> c.tasks.any { it.id == task.id } } ?: return
        contracts[entry.key] = entry.value.copy(tasks = entry.value.tasks.map { if (it.id == task.id) task else it })
        refreshToday()
    }

    private fun store(contract: DailyContract) {
        val tasks = contract.tasks.map { if (it.id == 0L) it.copy(id = nextTaskId++) else it }
        contracts[contract.date] = contract.copy(tasks = tasks)
        refreshToday()
    }

    private fun refreshToday() {
        _today.value = contracts[clock()]
    }

    companion object {
        /** 6 000 kroků, přednáška o Rustu + seriál, 4 úkoly (běh ověřený přes Stravu, ostatní čekají). */
        fun demoContract(date: LocalDate) = DailyContract(
            date = date,
            stepGoal = 6_000,
            plannedContent = listOf("přednáška o Rustu", "jeden díl seriálu"),
            videoUri = FakeEchoVideoRepository.DEMO_URI,
            tasks = listOf(
                PlanTask(type = TaskType.MOVEMENT, title = "Běh", target = "5 km", status = TaskStatus.VERIFIED, attempts = 1),
                PlanTask(type = TaskType.SCHOOL, title = "Lineární algebra", target = "kapitola 3, příklady 1–10"),
                PlanTask(type = TaskType.WORK, title = "Dopsat report", method = VerificationMethod.TEXT),
                PlanTask(type = TaskType.OTHER, title = "Uklidit pokoj"),
            ),
        )
    }
}
