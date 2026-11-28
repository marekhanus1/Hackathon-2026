package cz.narsil.ui.screens.echo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Common emotional triggers shown in the Ozvěna overlay.
 */
enum class EchoFeeling(val label: String) {
    NUDA("Nuda"),
    UNAVA("Únava"),
    UKOL("Těžký úkol"),
    SAMOTA("Samota"),
    JINE("Jiné")
}

data class EchoUiState(
    val sourceAppName: String = "TikTok",
    val videoCaption: String = "„Večer jsi říkal, že chceš mít ráno čistou hlavu.“",
    val selectedFeeling: EchoFeeling? = null,
    val availableSteps: Int = 2400,
    val stepCostForTenMin: Int = 1000,
    val currentDebtMinutes: Int = 0,
    val isVideoPlaying: Boolean = true,
    /** Titulek volby pohybu (232): „Udělat 20 dřepů hned“ nebo „Udělat 10 kliků hned“. */
    val exerciseOptionTitle: String = "Udělat 20 dřepů hned"
)

class EchoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EchoUiState())
    val uiState: StateFlow<EchoUiState> = _uiState.asStateFlow()

    fun selectFeeling(feeling: EchoFeeling) {
        _uiState.update { current ->
            current.copy(
                selectedFeeling = if (current.selectedFeeling == feeling) null else feeling
            )
        }
    }
}
