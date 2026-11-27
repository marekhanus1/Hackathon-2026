package cz.narsil.core.fake

import cz.narsil.core.ContractRepository
import cz.narsil.core.DailyContract
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake v paměti; dnes je předvyplněná demo smlouva (6 000 kroků, přednáška o Rustu). */
class FakeContractRepository(
    private val clock: () -> LocalDate = { LocalDate.now() },
    withDemoContract: Boolean = true,
) : ContractRepository {
    private val contracts = mutableMapOf<LocalDate, DailyContract>()
    private val _today = MutableStateFlow<DailyContract?>(null)
    override val today: StateFlow<DailyContract?> = _today.asStateFlow()

    init {
        if (withDemoContract) {
            val date = clock()
            contracts[date] = DailyContract(
                date = date,
                stepGoal = 6_000,
                plannedContent = listOf("přednáška o Rustu", "jeden díl seriálu"),
                videoUri = FakeEchoVideoRepository.DEMO_URI,
            )
            refreshToday()
        }
    }

    override suspend fun save(contract: DailyContract) {
        contracts[contract.date] = contract
        refreshToday()
    }

    override suspend fun forDate(date: LocalDate): DailyContract? = contracts[date]

    private fun refreshToday() {
        _today.value = contracts[clock()]
    }
}
