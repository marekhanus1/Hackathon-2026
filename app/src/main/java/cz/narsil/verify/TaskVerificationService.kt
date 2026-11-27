package cz.narsil.verify

import cz.narsil.core.ContractRepository
import cz.narsil.core.PlanRules
import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.TaskStatus
import cz.narsil.core.TaskVerifier
import cz.narsil.core.VerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Ověřovač s otiskem fotky ([LlmTaskVerifier], demo obal); obyčejný [TaskVerifier] (fake) se obalí přes [of]. */
fun interface DetailedVerifier {
    suspend fun verifyDetailed(task: PlanTask, proof: Proof): VerificationOutcome

    companion object {
        fun of(verifier: TaskVerifier): DetailedVerifier = when (verifier) {
            is DetailedVerifier -> verifier
            else -> DetailedVerifier { t, p -> VerificationOutcome(verifier.verify(t, p)) }
        }
    }
}

/**
 * Uložení důkazu do `PlanTask.proofUri`, aby šlo „ověřit později“ i po restartu:
 * fotka = její URI, text = `text:` + text, Strava = `strava:`.
 */
object ProofCodec {
    private const val TEXT = "text:"
    private const val STRAVA = "strava:"

    fun encode(proof: Proof): String = when (proof) {
        is Proof.Photo -> proof.uri
        is Proof.Text -> TEXT + VerifyRules.clampText(proof.text)
        Proof.Strava -> STRAVA
    }

    fun decode(stored: String?): Proof? = when {
        stored.isNullOrBlank() -> null
        stored == STRAVA -> Proof.Strava
        stored.startsWith(TEXT) -> Proof.Text(stored.removePrefix(TEXT))
        else -> Proof.Photo(stored)
    }
}

/** Co UI ukáže u úkolu během a po ověření. */
data class VerificationUiEvent(val taskId: Long, val status: TaskStatus, val reason: String, val running: Boolean = false)

/**
 * Ověření úkolů plánu v aplikaci (520, SPEC 6 a 7). Volá ho obrazovka ověření (Jan 324):
 * `submit(task, proof)` → stav úkolu (`ContractRepository.updateTask`) a při VERIFIED [onVerified]
 * (odměna, 521). Nikdy nehází.
 *
 * - YES → VERIFIED, NO → REJECTED (pokus +1; nejvýš 3 pokusy celkem), LATER → VERIFY_LATER (pokus se nepočítá).
 * - VERIFY_LATER se zkusí znovu při další síti ([retryPending], [NetworkRetryTrigger]), nejvýš
 *   [maxAutoRetries]× automaticky; pak zůstane a UI ukáže „ověřit znovu“ ([retryNow]).
 */
class TaskVerificationService(
    private val contracts: ContractRepository,
    verifier: TaskVerifier,
    private val onVerified: suspend (PlanTask) -> Unit = {},
    private val maxAutoRetries: Int = 3,
) {
    private val verifier = DetailedVerifier.of(verifier)
    private val mutex = Mutex()
    private val autoRetries = mutableMapOf<Long, Int>()

    private val _events = MutableStateFlow<Map<Long, VerificationUiEvent>>(emptyMap())

    /** Poslední stav ověření podle id úkolu (pro UI: „ověřuji…“, důvod zamítnutí). */
    val events: StateFlow<Map<Long, VerificationUiEvent>> = _events.asStateFlow()

    /** Jde úkol ověřit? (není VERIFIED a nevyčerpal pokusy). */
    fun canSubmit(task: PlanTask): Boolean = task.canRetry

    /** Ověří úkol důkazem a uloží výsledek. Vrací aktualizovaný úkol. */
    suspend fun submit(task: PlanTask, proof: Proof): PlanTask = mutex.withLock { submitLocked(task, proof) }

    /** Tlačítko „ověřit znovu“: vynuluje automatické pokusy a zkusí hned s uloženým důkazem. */
    suspend fun retryNow(task: PlanTask): PlanTask = mutex.withLock {
        autoRetries.remove(task.id)
        val proof = ProofCodec.decode(task.proofUri) ?: return@withLock task
        submitLocked(task, proof)
    }

    /** Zkusí znovu dnešní úkoly ve stavu VERIFY_LATER (volá se při obnovení sítě). */
    suspend fun retryPending(): List<PlanTask> = mutex.withLock {
        val pending = contracts.today.value?.tasks.orEmpty().filter { it.status == TaskStatus.VERIFY_LATER }
        pending.mapNotNull { task ->
            val tries = autoRetries[task.id] ?: 0
            val proof = ProofCodec.decode(task.proofUri)
            if (tries >= maxAutoRetries || proof == null) return@mapNotNull null
            autoRetries[task.id] = tries + 1
            submitLocked(task, proof)
        }
    }

    private suspend fun submitLocked(task: PlanTask, proof: Proof): PlanTask {
        if (!task.canRetry) return task
        emit(task.id, task.status, "Ověřuji…", running = true)
        val outcome = runCatching { verifier.verifyDetailed(task, proof) }
            .getOrElse { VerificationOutcome(VerificationResult.LATER, reason = "chyba ověření") }
        val updated = when (outcome.result) {
            VerificationResult.YES -> task.copy(status = TaskStatus.VERIFIED, attempts = task.attempts + 1)
            VerificationResult.NO -> task.copy(status = TaskStatus.REJECTED, attempts = task.attempts + 1)
            VerificationResult.LATER -> task.copy(status = TaskStatus.VERIFY_LATER)
        }.copy(
            proofUri = ProofCodec.encode(proof),
            proofHash = outcome.proofHash ?: task.proofHash.takeIf { proof !is Proof.Photo },
        )
        runCatching { contracts.updateTask(updated) }
        if (updated.status == TaskStatus.VERIFIED) {
            autoRetries.remove(task.id)
            runCatching { onVerified(updated) }
        }
        emit(updated.id, updated.status, reasonFor(updated, outcome))
        return updated
    }

    private fun reasonFor(task: PlanTask, outcome: VerificationOutcome): String = when (task.status) {
        TaskStatus.VERIFIED -> "Ověřeno"
        TaskStatus.REJECTED -> if (task.attempts >= PlanRules.MAX_ATTEMPTS) {
            "Zamítnuto, pokusy došly"
        } else {
            "Zamítnuto – zkus jiný důkaz (zbývá ${PlanRules.MAX_ATTEMPTS - task.attempts})"
        }
        TaskStatus.VERIFY_LATER -> "Ověří se později (${outcome.reason})"
        TaskStatus.PENDING -> ""
    }

    private fun emit(id: Long, status: TaskStatus, reason: String, running: Boolean = false) =
        _events.update { it + (id to VerificationUiEvent(id, status, reason, running)) }
}
