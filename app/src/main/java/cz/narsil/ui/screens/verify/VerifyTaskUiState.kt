package cz.narsil.ui.screens.verify

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Task categories and default minute rewards per docs/SPEC.md section 6.
 */
enum class TaskType(
    val label: String,
    val defaultRewardMinutes: Int
) {
    MOVEMENT("Pohyb", 30),
    SCHOOL("Škola", 20),
    WORK("Práce", 20),
    OTHER("Ostatní", 10)
}

/** Způsob ověření (541): fotka výsledku, fotka prostředí, nebo vlastní popis. */
enum class ProofMode(val label: String, val hint: String) {
    PHOTO("Fotka úkolu", "Vyfoť výsledek – sešit s příklady, hotový výrobek, obrazovku s odevzdáním."),
    ROOM("Fotka prostředí", "Vyfoť místo, kde jsi úkol dělal – uklizený pokoj, rozloženou podložku, připravený stůl."),
    TEXT("Popis", "Popiš ve 2–3 větách, co přesně jsi udělal a jak to dopadlo. AI ti dá krátkou zpětnou vazbu.")
}

val ProofMode.isPhoto: Boolean get() = this != ProofMode.TEXT

/** Stav úkolu v seznamech (domov, výběr, záložky ověření) – 334. */
enum class TaskItemStatus { PENDING, VERIFIED, REJECTED, VERIFY_LATER, EXHAUSTED }

/**
 * Úkol dnešní smlouvy pro seznamy (334). [EXHAUSTED] = zamítnutý bez zbylých pokusů.
 * [canVerify] = jde na něj odpovídat (tlačítko „Ověřit“).
 */
data class TaskItem(
    val id: Long,
    val title: String,
    val type: TaskType,
    val rewardMinutes: Int = type.defaultRewardMinutes,
    val status: TaskItemStatus = TaskItemStatus.PENDING,
    val attemptsUsed: Int = 0,
    val maxAttempts: Int = 3,
    val goal: String = "",
) {
    val attemptsLeft: Int get() = (maxAttempts - attemptsUsed).coerceAtLeast(0)
    val canVerify: Boolean get() = status == TaskItemStatus.PENDING || status == TaskItemStatus.REJECTED
    val isDone: Boolean get() = status == TaskItemStatus.VERIFIED

    /** Zkratka kategorie do čtverečku („ŠK“). */
    val short: String
        get() = when (type) {
            TaskType.MOVEMENT -> "PO"
            TaskType.SCHOOL -> "ŠK"
            TaskType.WORK -> "PR"
            TaskType.OTHER -> "OS"
        }

    /** Krátký název do záložky (první slovo, nejvýš 12 znaků). */
    val tabLabel: String
        get() = title.trim().split(' ').firstOrNull().orEmpty().let { if (it.length > 12) it.take(11) + "…" else it }.ifBlank { short }

    /** Druhý řádek v seznamu domova: kategorie + stav. */
    val meta: String
        get() = "${type.label} · " + when (status) {
            TaskItemStatus.VERIFIED -> "ověřeno"
            TaskItemStatus.PENDING -> "fotka nebo popis · +$rewardMinutes min"
            TaskItemStatus.REJECTED -> "zamítnuto, ${attemptsLeftText(attemptsLeft)}"
            TaskItemStatus.EXHAUSTED -> "zamítnuto, pokusy vyčerpány"
            TaskItemStatus.VERIFY_LATER -> "ověří se po připojení"
        }

    /** Řádek pokusů na ověření („Pokus 1 ze 3“, „Minule zamítnuto · pokus 2 ze 3“). */
    val attemptsLine: String
        get() = when {
            status == TaskItemStatus.EXHAUSTED -> "Vyčerpány všechny $maxAttempts pokusy"
            status == TaskItemStatus.REJECTED -> "Minule zamítnuto · pokus ${attemptsUsed + 1} ze $maxAttempts"
            else -> "Pokus ${(attemptsUsed + 1).coerceAtMost(maxAttempts)} ze $maxAttempts"
        }

    val isWarning: Boolean get() = status == TaskItemStatus.REJECTED || status == TaskItemStatus.EXHAUSTED
}

internal fun attemptsLeftText(n: Int): String = when (n) {
    1 -> "zbývá 1 pokus"
    in 2..4 -> "zbývají $n pokusy"
    else -> "zbývá $n pokusů"
}

enum class VerificationStatus {
    PENDING,
    VERIFYING,
    VERIFIED,
    REJECTED,
    VERIFY_LATER
}

data class VerifyTaskUiState(
    val taskTitle: String = "Dokončit laboratorní úlohu z databází",
    val taskGoal: String = "Odevzdaný SQL skript nebo zápisky z cvičení",
    val taskType: TaskType = TaskType.SCHOOL,
    val rewardMinutes: Int = TaskType.SCHOOL.defaultRewardMinutes,
    val proofMode: ProofMode = ProofMode.PHOTO,
    val photoCaptured: Boolean = false,
    val textProof: String = "",
    val maxTextLength: Int = 300,
    val minTextLength: Int = 60,
    val status: VerificationStatus = VerificationStatus.PENDING,
    val attemptsUsed: Int = 0,
    val maxAttempts: Int = 3,
    val statusMessage: String = "",
    /** XP za ověřený úkol (ukáže karta „Ověřeno“), 0 = neukazovat. */
    val rewardXp: Int = 0,
    /** Další neověřený úkol smlouvy (tlačítko „Další úkol“), null = žádný. */
    val nextTaskTitle: String? = null,
    /** Úkoly dnešní smlouvy pro záložky nahoře (334); prázdné = bez záložek. */
    val tasks: List<TaskItem> = emptyList(),
    /** Úkol, na který se právě odpovídá. */
    val selectedTaskId: Long? = null,
) {
    val remainingAttempts: Int
        get() = (maxAttempts - attemptsUsed).coerceAtLeast(0)

    val canSubmit: Boolean
        get() = status != VerificationStatus.VERIFYING &&
            status != VerificationStatus.VERIFIED &&
            remainingAttempts > 0 &&
            when (proofMode) {
                ProofMode.PHOTO, ProofMode.ROOM -> photoCaptured
                ProofMode.TEXT -> textProof.trim().length >= minTextLength
            }

    /** Kolik znaků popisu ještě chybí do minima (0 = dost). */
    val missingTextChars: Int
        get() = (minTextLength - textProof.trim().length).coerceAtLeast(0)
}

class VerifyTaskViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VerifyTaskUiState())
    val uiState: StateFlow<VerifyTaskUiState> = _uiState.asStateFlow()

    fun selectProofMode(mode: ProofMode) {
        _uiState.update { it.copy(proofMode = mode) }
    }

    fun updateTextProof(text: String) {
        _uiState.update { current ->
            current.copy(textProof = text.take(current.maxTextLength))
        }
    }

    fun capturePhoto() {
        _uiState.update { it.copy(photoCaptured = true) }
    }

    fun retakePhoto() {
        _uiState.update { it.copy(photoCaptured = false) }
    }

    fun submitProof() {
        _uiState.update { current ->
            if (!current.canSubmit) return@update current
            val nextAttempts = current.attemptsUsed + 1
            val looksValid = when (current.proofMode) {
                ProofMode.PHOTO, ProofMode.ROOM -> current.photoCaptured
                ProofMode.TEXT -> current.textProof.trim().length >= current.minTextLength
            }
            if (looksValid) {
                current.copy(
                    status = VerificationStatus.VERIFIED,
                    attemptsUsed = nextAttempts,
                    statusMessage = "Skvěle! Úkol ověřen a připsáno +${current.rewardMinutes} min."
                )
            } else {
                current.copy(
                    status = VerificationStatus.REJECTED,
                    attemptsUsed = nextAttempts,
                    statusMessage = "Zatím to nevypadá jako jasný důkaz. Zkus to prosím doplnit."
                )
            }
        }
    }

    fun retry() {
        _uiState.update { current ->
            if (current.remainingAttempts <= 0) return@update current
            current.copy(
                status = VerificationStatus.PENDING,
                photoCaptured = false,
                statusMessage = ""
            )
        }
    }
}
