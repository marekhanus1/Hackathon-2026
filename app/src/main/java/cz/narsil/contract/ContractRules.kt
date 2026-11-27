package cz.narsil.contract

import cz.narsil.core.ContractRepository
import cz.narsil.core.DailyContract
import cz.narsil.core.PlanRules
import cz.narsil.core.PlanTask
import cz.narsil.core.SettingsRepository
import cz.narsil.core.data.DayKey
import java.time.Clock
import java.time.LocalDate

/** Rozepsaná večerní smlouva z obrazovky (313). Datum doplní [ContractSigner]. */
data class ContractDraft(
    val stepGoal: Int = PlanRules.DEFAULT_STEP_GOAL,
    val plannedContent: List<String> = emptyList(),
    val tasks: List<PlanTask> = emptyList(),
    val videoUri: String? = null,
)

/** Limity plánu dne (SPEC 6, [PlanRules]). Čistý Kotlin. */
object ContractRules {
    /** Očistí smlouvu: ≤ 5 položek obsahu (bez prázdných a duplicit), ≤ 6 úkolů, odměna 5–60 min, cíl ≥ 0. */
    fun sanitize(contract: DailyContract): DailyContract = contract.copy(
        stepGoal = contract.stepGoal.coerceAtLeast(0),
        plannedContent = contract.plannedContent
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(PlanRules.MAX_PLANNED_CONTENT),
        tasks = contract.tasks
            .map { it.copy(title = it.title.trim(), target = it.target.trim()) }
            .filter { it.title.isNotEmpty() }
            .take(PlanRules.MAX_TASKS)
            .map { it.copy(rewardMinutes = it.rewardMinutes.coerceIn(PlanRules.MIN_REWARD_MINUTES, PlanRules.MAX_REWARD_MINUTES)) },
        videoUri = contract.videoUri?.takeIf { it.isNotBlank() },
    )

    fun ContractDraft.toContract(date: LocalDate): DailyContract =
        sanitize(DailyContract(date, stepGoal, plannedContent, videoUri, tasks))
}

/**
 * Podpis smlouvy (212): smlouva uložená mezi 17:00 a 3:59 platí na zítřek, jinak na dnešek;
 * demo přepínač „smlouva platí hned“ (`Settings.demoContractToday`) ji uloží na dnešek (SPEC 1).
 */
class ContractSigner(
    private val repository: ContractRepository,
    private val settings: SettingsRepository? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    /** Pro který den by se smlouva teď uložila (UI: „Smlouva na zítřek“ / „na dnešek“). */
    fun targetDate(): LocalDate =
        DayKey.contractDateFor(clock.millis(), settings?.settings?.value?.demoContractToday == true, clock.zone)

    suspend fun sign(draft: ContractDraft): DailyContract {
        val contract = with(ContractRules) { draft.toContract(targetDate()) }
        runCatching { repository.save(contract) }
        return contract
    }
}
