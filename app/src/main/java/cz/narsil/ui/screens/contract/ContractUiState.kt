package cz.narsil.ui.screens.contract

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ContractStep(val index: Int, val title: String) {
    MOVEMENT_GOAL(1, "Pohybový cíl na zítřek"),
    PLANNED_CONTENT(2, "Plánovaný obsah zdarma"),
    ECHO_VIDEO(3, "Tvoje 15s Ozvěna")
}

enum class MovementGoalType(
    val label: String,
    val unit: String,
    val defaultAmount: Int,
    val stepDelta: Int,
    val presets: List<Int>
) {
    STEPS("Kroky", "kroků", 6000, 1000, listOf(4000, 6000, 8000)),
    ACTIVITY_MINUTES("Minuty aktivity", "min", 30, 10, listOf(20, 30, 45)),
    SQUATS("Dřepy", "dřepů", 30, 10, listOf(20, 30, 50))
}

data class ContractUiState(
    val currentStep: ContractStep = ContractStep.MOVEMENT_GOAL,
    val goalType: MovementGoalType = MovementGoalType.STEPS,
    val goalAmount: Int = 6000,
    val plannedItems: List<String> = listOf("Přednáška o Rustu"),
    val draftItemText: String = "",
    val isRecordingVideo: Boolean = false,
    val recordedSeconds: Int = 0,
    val maxVideoSeconds: Int = 15,
    val hasRecordedVideo: Boolean = false,
    val isSubmitted: Boolean = false
)

class ContractViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ContractUiState())
    val uiState: StateFlow<ContractUiState> = _uiState.asStateFlow()

    fun selectGoalType(type: MovementGoalType) {
        _uiState.update { it.copy(goalType = type, goalAmount = type.defaultAmount) }
    }

    fun setGoalAmount(amount: Int) {
        _uiState.update { it.copy(goalAmount = amount.coerceAtLeast(1)) }
    }

    fun adjustGoalAmount(delta: Int) {
        _uiState.update { current ->
            current.copy(goalAmount = (current.goalAmount + delta).coerceAtLeast(current.goalType.stepDelta))
        }
    }

    fun updateDraftItemText(text: String) {
        _uiState.update { it.copy(draftItemText = text) }
    }

    fun addPlannedItem(item: String = _uiState.value.draftItemText) {
        val clean = item.trim()
        if (clean.isEmpty()) return
        _uiState.update { current ->
            if (current.plannedItems.contains(clean)) {
                current.copy(draftItemText = "")
            } else {
                current.copy(
                    plannedItems = current.plannedItems + clean,
                    draftItemText = ""
                )
            }
        }
    }

    fun removePlannedItem(item: String) {
        _uiState.update { current ->
            current.copy(plannedItems = current.plannedItems - item)
        }
    }

    fun toggleRecording() {
        _uiState.update { current ->
            if (current.isRecordingVideo) {
                current.copy(
                    isRecordingVideo = false,
                    recordedSeconds = current.recordedSeconds.coerceAtLeast(12),
                    hasRecordedVideo = true
                )
            } else {
                current.copy(
                    isRecordingVideo = true,
                    recordedSeconds = 8
                )
            }
        }
    }

    fun nextStep() {
        _uiState.update { current ->
            val next = when (current.currentStep) {
                ContractStep.MOVEMENT_GOAL -> ContractStep.PLANNED_CONTENT
                ContractStep.PLANNED_CONTENT -> ContractStep.ECHO_VIDEO
                ContractStep.ECHO_VIDEO -> ContractStep.ECHO_VIDEO
            }
            current.copy(currentStep = next)
        }
    }

    fun previousStep() {
        _uiState.update { current ->
            val prev = when (current.currentStep) {
                ContractStep.MOVEMENT_GOAL -> ContractStep.MOVEMENT_GOAL
                ContractStep.PLANNED_CONTENT -> ContractStep.MOVEMENT_GOAL
                ContractStep.ECHO_VIDEO -> ContractStep.PLANNED_CONTENT
            }
            current.copy(currentStep = prev)
        }
    }

    fun submitContract() {
        _uiState.update { it.copy(isSubmitted = true, hasRecordedVideo = true) }
    }
}
