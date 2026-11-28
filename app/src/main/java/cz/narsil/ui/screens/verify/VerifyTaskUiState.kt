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
    val nextTaskTitle: String? = null
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
