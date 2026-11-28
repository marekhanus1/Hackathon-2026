package cz.narsil.core

import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

/** Typ úkolu v plánu dne (SPEC 6). Určuje výchozí odměnu a způsob ověření. */
enum class TaskType(val defaultRewardMinutes: Int, val defaultMethod: VerificationMethod) {
    MOVEMENT(30, VerificationMethod.PHOTO), // Strava se nepoužije (146)
    SCHOOL(20, VerificationMethod.PHOTO),
    WORK(20, VerificationMethod.PHOTO),
    OTHER(10, VerificationMethod.PHOTO),
}

/** Jak se úkol ověří (SPEC 7). STRAVA má zálohu PHOTO. */
enum class VerificationMethod { STRAVA, PHOTO, TEXT }

/** PENDING → VERIFIED / REJECTED / VERIFY_LATER (SPEC 6). */
enum class TaskStatus { PENDING, VERIFIED, REJECTED, VERIFY_LATER }

/** Pravidla plánu dne (SPEC 6). */
object PlanRules {
    const val DEFAULT_STEP_GOAL = 6_000
    const val MAX_PLANNED_CONTENT = 5
    const val MAX_TASKS = 6
    const val MIN_REWARD_MINUTES = 5
    const val MAX_REWARD_MINUTES = 60

    /** Celkem pokusů o ověření (1 + 2 opakování po REJECTED). */
    const val MAX_ATTEMPTS = 3
    const val MAX_TEXT_PROOF_CHARS = 300
    const val MAX_VIDEO_SECONDS = 15
}

/**
 * Úkol plánu dne.
 *
 * @param target cíl volným textem, např. "kapitola 3, 20 stran"
 * @param rewardMinutes odměna po ověření (5–60 min, výchozí [TaskType.defaultRewardMinutes])
 * @param attempts dosavadní pokusy o ověření
 * @param proofUri poslední důkaz (fotka content:// / file://), jinak null
 * @param proofHash SHA-256 otisk fotky (hex), brání opakovanému použití
 */
data class PlanTask(
    val id: Long = 0,
    val type: TaskType,
    val title: String,
    val target: String = "",
    val rewardMinutes: Int = type.defaultRewardMinutes,
    val method: VerificationMethod = type.defaultMethod,
    val status: TaskStatus = TaskStatus.PENDING,
    val attempts: Int = 0,
    val proofUri: String? = null,
    val proofHash: String? = null,
) {
    /** Jde ještě ověřovat (není VERIFIED a nevyčerpal pokusy). */
    val canRetry: Boolean
        get() = status != TaskStatus.VERIFIED && attempts < PlanRules.MAX_ATTEMPTS
}

/**
 * Večerní smlouva na den [date] (píše se večer předem, SPEC 1 a 6).
 *
 * @param stepGoal pohybový cíl v krocích
 * @param plannedContent plánovaný obsah volným textem (nejvýš 5), např. ["přednáška o Rustu", "jeden díl seriálu"]
 * @param videoUri lokální URI ≤ 15s videa (content:// nebo file://), null = bez videa
 * @param tasks úkoly plánu (nejvýš 6)
 */
data class DailyContract(
    val date: LocalDate,
    val stepGoal: Int,
    val plannedContent: List<String> = emptyList(),
    val videoUri: String? = null,
    val tasks: List<PlanTask> = emptyList(),
) {
    /** Všechny úkoly VERIFIED (prázdný seznam = true). */
    val allTasksVerified: Boolean get() = tasks.all { it.status == TaskStatus.VERIFIED }

    /** Smlouva splněna = krokový cíl splněn a všechny úkoly VERIFIED (SPEC 6). */
    fun isFulfilled(stepsToday: Int): Boolean = stepsToday >= stepGoal && allTasksVerified
}

/** Smlouvy a úkoly plánu (212, Room). */
interface ContractRepository {
    /** Smlouva platná dnes (včetně úkolů); null = nic nepodepsáno. */
    val today: StateFlow<DailyContract?>

    /** Uloží/přepíše smlouvu pro [DailyContract.date] včetně úkolů (obvykle zítřek). */
    suspend fun save(contract: DailyContract)

    /** Smlouva pro libovolný den. */
    suspend fun forDate(date: LocalDate): DailyContract?

    /** Uloží změnu jednoho úkolu (stav, pokusy, důkaz) podle [PlanTask.id]. */
    suspend fun updateTask(task: PlanTask)
}
