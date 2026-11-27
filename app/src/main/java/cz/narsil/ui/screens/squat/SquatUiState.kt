package cz.narsil.ui.screens.squat

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SquatPhase(val instruction: String) {
    READY("Postav se před kameru a začni první dřep"),
    GOING_DOWN("Jdeš dolů – drž rovná záda"),
    STANDING_UP("Skvěle! Nahoru a další opakování"),
    COMPLETED("Hotovo! Odemykám čas a připisuji odměnu")
}

data class SquatUiState(
    val completedSquats: Int = 7,
    val targetSquats: Int = 20,
    val phase: SquatPhase = SquatPhase.STANDING_UP,
    val feedbackMessage: String = "Super tempo, zbývá ještě 13 dřepů.",
    val earnedXp: Int = 20,
    val unlockedMinutes: Int = 10
) {
    val isCompleted: Boolean
        get() = completedSquats >= targetSquats || phase == SquatPhase.COMPLETED
}

class SquatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SquatUiState())
    val uiState: StateFlow<SquatUiState> = _uiState.asStateFlow()

    fun onSquatDetected() {
        _uiState.update { current ->
            val next = (current.completedSquats + 1).coerceAtMost(current.targetSquats)
            val remaining = current.targetSquats - next
            if (remaining <= 0) {
                current.copy(
                    completedSquats = next,
                    phase = SquatPhase.COMPLETED,
                    feedbackMessage = "Skvělá práce! Přesunul ses z hlavy do těla."
                )
            } else {
                current.copy(
                    completedSquats = next,
                    phase = SquatPhase.STANDING_UP,
                    feedbackMessage = "Výborně! Zbývá $remaining dřepů."
                )
            }
        }
    }

    fun reset() {
        _uiState.value = SquatUiState(
            completedSquats = 0,
            phase = SquatPhase.READY,
            feedbackMessage = "Postav se tak, aby tě kamera viděla od ramen po kotníky."
        )
    }
}
