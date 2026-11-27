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

enum class ProofMode(val label: String) {
    PHOTO("Vyfotit důkaz"),
    TEXT("Krátký text")
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
    val status: VerificationStatus = VerificationStatus.PENDING,
    val attemptsUsed: Int = 0,
    val maxAttempts: Int = 3,
    val statusMessage: String = ""
) {
    val remainingAttempts: Int
        get() = (maxAttempts - attemptsUsed).coerceAtLeast(0)

    val canSubmit: Boolean
        get() = status != VerificationStatus.VERIFYING &&
            status != VerificationStatus.VERIFIED &&
            remainingAttempts > 0 &&
            when (proofMode) {
                ProofMode.PHOTO -> photoCaptured
                ProofMode.TEXT -> textProof.isNotBlank()
            }
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
                ProofMode.PHOTO -> current.photoCaptured
                ProofMode.TEXT -> current.textProof.trim().length >= 8
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
