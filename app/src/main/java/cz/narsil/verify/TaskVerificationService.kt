package cz.narsil.verify

import cz.narsil.core.ContractRepository
import cz.narsil.core.PhotoKind
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
import cz.narsil.llm.client.runCatchingCancellable

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
 * fotka úkolu = její URI, fotka prostředí = `room:` + URI (541), text = `text:` + text, Strava = `strava:`.
 */
object ProofCodec {
    private const val TEXT = "text:"
    private const val STRAVA = "strava:"
    private const val ROOM = "room:"

    fun encode(proof: Proof): String = when (proof) {
        is Proof.Photo -> if (proof.kind == PhotoKind.ROOM) ROOM + proof.uri else proof.uri
        is Proof.Text -> TEXT + VerifyRules.clampText(proof.text)
        Proof.Strava -> STRAVA
    }

    fun decode(stored: String?): Proof? = when {
        stored.isNullOrBlank() -> null
        stored == STRAVA -> Proof.Strava
        stored.startsWith(TEXT) -> Proof.Text(stored.removePrefix(TEXT))
        stored.startsWith(ROOM) -> Proof.Photo(stored.removePrefix(ROOM), PhotoKind.ROOM)
        else -> Proof.Photo(stored)
    }
}

/**
 * Poslední důvod a zpětná vazba ověření podle id úkolu, ať je UI ukáže i po restartu aplikace
 * (bez změny schématu Room – v aplikaci SharedPreferences, [SharedPrefsReasonStore]).
 */
interface VerificationReasonStore {
    fun load(taskId: Long): Pair<String, String>?
    fun save(taskId: Long, reason: String, feedback: String)
}

/** Důvody ověření v SharedPreferences (`verify_reasons`). */
class SharedPrefsReasonStore(context: android.content.Context) : VerificationReasonStore {
    private val prefs = context.applicationContext.getSharedPreferences("verify_reasons", android.content.Context.MODE_PRIVATE)

    override fun load(taskId: Long): Pair<String, String>? {
        val reason = prefs.getString("r_$taskId", null) ?: return null
        return reason to prefs.getString("f_$taskId", "").orEmpty()
    }

    override fun save(taskId: Long, reason: String, feedback: String) {
        prefs.edit().putString("r_$taskId", reason).putString("f_$taskId", feedback).apply()
    }
}

/**
 * Co UI ukáže u úkolu během a po ověření. [reason] = hotová česká věta stavu,
 * [feedback] = samotná zpětná vazba LLM / místní kontroly (541), prázdná když není.
 */
data class VerificationUiEvent(
    val taskId: Long,
    val status: TaskStatus,
    val reason: String,
    val running: Boolean = false,
    val feedback: String = "",
)

/**
 * Ověření úkolů plánu v aplikaci (520, SPEC 6 a 7). Volá ho obrazovka ověření (Jan 324):
 * `submit(task, proof)` → stav úkolu (`ContractRepository.updateTask`) a při VERIFIED [onVerified]
 * (odměna, 521). Nikdy nehází.
 *
 * - YES → VERIFIED, NO → REJECTED (pokus +1; nejvýš 3 pokusy celkem), LATER → VERIFY_LATER (pokus se nepočítá).
 * - VERIFY_LATER se zkusí znovu při další síti ([retryPending], [NetworkRetryTrigger]), nejvýš
 *   [maxAutoRetries]× automaticky za běh aplikace; pak zůstane a UI ukáže „ověřit znovu“ ([retryNow]).
 *   Čekající úkoly bere z [pendingTasks] – v aplikaci všechny dny (`ContractDao.tasksToVerifyLater`),
 *   aby se úkol odeslaný před 4:00 doověřil i po přelomu dne.
 */
class TaskVerificationService(
    private val contracts: ContractRepository,
    verifier: TaskVerifier,
    private val onVerified: suspend (PlanTask) -> Unit = {},
    private val maxAutoRetries: Int = 3,
    private val pendingTasks: suspend () -> List<PlanTask> = {
        contracts.today.value?.tasks.orEmpty().filter { it.status == TaskStatus.VERIFY_LATER }
    },
    private val reasons: VerificationReasonStore? = null,
) {
    private val verifier = DetailedVerifier.of(verifier)
    private val mutex = Mutex()
    private val autoRetries = mutableMapOf<Long, Int>()

    private val _events = MutableStateFlow<Map<Long, VerificationUiEvent>>(emptyMap())

    /** Poslední stav ověření podle id úkolu (pro UI: „ověřuji…“, důvod zamítnutí). */
    val events: StateFlow<Map<Long, VerificationUiEvent>> = _events.asStateFlow()

    /**
     * Poslední stav ověření úkolu: z běhu aplikace, jinak uložený důvod (po restartu).
     * Uložený důvod platí jen pro stav, ve kterém úkol je (VERIFIED / REJECTED / VERIFY_LATER).
     */
    fun eventFor(task: PlanTask): VerificationUiEvent? {
        _events.value[task.id]?.let { return it }
        if (task.status == TaskStatus.PENDING) return null
        val (reason, feedback) = runCatching { reasons?.load(task.id) }.getOrNull() ?: return null
        return VerificationUiEvent(task.id, task.status, reason, running = false, feedback = feedback)
    }

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
        val pending = runCatchingCancellable { pendingTasks() }.getOrDefault(emptyList())
            .filter { it.status == TaskStatus.VERIFY_LATER }
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
        val outcome = runCatchingCancellable { verifier.verifyDetailed(task, proof) }
            .getOrElse { VerificationOutcome(VerificationResult.LATER, reason = "chyba ověření") }
        val updated = when (outcome.result) {
            VerificationResult.YES -> task.copy(status = TaskStatus.VERIFIED, attempts = task.attempts + 1)
            VerificationResult.NO -> task.copy(status = TaskStatus.REJECTED, attempts = task.attempts + 1)
            VerificationResult.LATER -> task.copy(status = TaskStatus.VERIFY_LATER)
        }.copy(
            proofUri = ProofCodec.encode(proof),
            proofHash = outcome.proofHash ?: task.proofHash.takeIf { proof !is Proof.Photo },
        )
        runCatchingCancellable { contracts.updateTask(updated) }
        if (updated.status == TaskStatus.VERIFIED) {
            autoRetries.remove(task.id)
            runCatchingCancellable { onVerified(updated) }
        }
        val reason = reasonFor(updated, outcome)
        emit(updated.id, updated.status, reason, feedback = outcome.feedback)
        runCatching { reasons?.save(updated.id, reason, outcome.feedback) }
        return updated
    }

    private fun reasonFor(task: PlanTask, outcome: VerificationOutcome): String {
        val feedback = outcome.feedback.trim().trimEnd('.')
        return when (task.status) {
            TaskStatus.VERIFIED -> if (feedback.isEmpty()) "Ověřeno" else "Ověřeno – $feedback."
            TaskStatus.REJECTED -> {
                // Zbývající pokusy ukazuje karta zamítnutí („Zbývající pokusy: N z 3“), tady se neopakují.
                val why = feedback.ifEmpty { "zkus jiný důkaz" }
                "Zamítnuto – $why."
            }
            TaskStatus.VERIFY_LATER -> outcome.reason.ifBlank { "Ověří se později, až bude síť." }
            TaskStatus.PENDING -> ""
        }
    }

    private fun emit(id: Long, status: TaskStatus, reason: String, running: Boolean = false, feedback: String = "") =
        _events.update { it + (id to VerificationUiEvent(id, status, reason, running, feedback)) }
}
