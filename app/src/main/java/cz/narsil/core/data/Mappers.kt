package cz.narsil.core.data

import cz.narsil.core.DailyContract
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskType
import cz.narsil.core.VerificationMethod
import java.time.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// Převody entit ↔ doménové modely pro repozitáře (212, 520). Čistý Kotlin, testuje se bez Androidu.

private val stringList = ListSerializer(String.serializer())

fun plannedContentToJson(items: List<String>): String = Json.encodeToString(stringList, items)

fun plannedContentFromJson(json: String): List<String> =
    runCatching { Json.decodeFromString(stringList, json) }.getOrDefault(emptyList())

fun PlanTaskEntity.toDomain(): PlanTask = PlanTask(
    id = id,
    type = enumOr(type, TaskType.OTHER),
    title = title,
    target = target,
    rewardMinutes = rewardMinutes,
    method = enumOr(method, VerificationMethod.PHOTO),
    status = enumOr(status, TaskStatus.PENDING),
    attempts = attempts,
    proofUri = proofUri,
    proofHash = proofHash,
)

/** [verifiedAtMillis] se nastaví, když je úkol VERIFIED a dosud čas neměl. */
fun PlanTask.toEntity(date: LocalDate, verifiedAtMillis: Long? = null): PlanTaskEntity = PlanTaskEntity(
    id = id,
    date = date.toString(),
    type = type.name,
    title = title,
    target = target,
    rewardMinutes = rewardMinutes,
    method = method.name,
    status = status.name,
    attempts = attempts,
    proofUri = proofUri,
    proofHash = proofHash,
    verifiedAtMillis = verifiedAtMillis ?: if (status == TaskStatus.VERIFIED) System.currentTimeMillis() else null,
)

fun ContractEntity.toDomain(tasks: List<PlanTaskEntity>): DailyContract = DailyContract(
    date = LocalDate.parse(date),
    stepGoal = stepGoal,
    plannedContent = plannedContentFromJson(plannedContent),
    videoUri = videoUri,
    tasks = tasks.map { it.toDomain() },
)

fun DailyContract.toEntity(createdAtMillis: Long = System.currentTimeMillis()): ContractEntity = ContractEntity(
    date = date.toString(),
    stepGoal = stepGoal,
    plannedContent = plannedContentToJson(plannedContent),
    videoUri = videoUri,
    createdAtMillis = createdAtMillis,
)

/** Uložený název enumu zpět na hodnotu; neznámý text = [default]. */
inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default
