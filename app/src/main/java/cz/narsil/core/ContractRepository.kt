package cz.narsil.core

import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

/**
 * Večerní smlouva na den [date] (píše se večer předem).
 *
 * @param stepGoal pohybový cíl v krocích
 * @param plannedContent plánovaný obsah volným textem, např. ["přednáška o Rustu", "jeden díl seriálu"]
 * @param videoUri lokální URI 15s videa (content:// nebo file://), null = bez videa
 */
data class DailyContract(
    val date: LocalDate,
    val stepGoal: Int,
    val plannedContent: List<String> = emptyList(),
    val videoUri: String? = null,
)

/** Smlouvy (212, Room). */
interface ContractRepository {
    /** Smlouva platná dnes; null = včera večer nic nepodepsáno. */
    val today: StateFlow<DailyContract?>

    /** Uloží/přepíše smlouvu pro [DailyContract.date] (obvykle zítřek). */
    suspend fun save(contract: DailyContract)

    /** Smlouva pro libovolný den. */
    suspend fun forDate(date: LocalDate): DailyContract?
}
