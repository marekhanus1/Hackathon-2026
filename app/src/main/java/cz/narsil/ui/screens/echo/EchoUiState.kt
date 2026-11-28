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

/** Prosba o minuty od Parťáka v Ozvěně (163): napsáno → simulovaná odpověď → odemčeno. */
enum class FriendRequest { IDLE, ASKING, GRANTED }

data class EchoUiState(
    val sourceAppName: String = "TikTok",
    val videoCaption: String = "„Večer jsi říkal, že chceš mít ráno čistou hlavu.“",
    val selectedFeeling: EchoFeeling? = null,
    val availableSteps: Int = 2400,
    val stepCostForTenMin: Int = 1000,
    val currentDebtMinutes: Int = 0,
    val isVideoPlaying: Boolean = true,
    /** Titulek volby pohybu (232): „Udělat 20 dřepů hned“ nebo „Udělat 10 kliků hned“. */
    val exerciseOptionTitle: String = "Udělat 20 dřepů hned",
    /** Parťák ještě dnes dá 10 min: nepřekročí denní limit (SPEC 4, 5, 163). false = volba neaktivní (159). */
    val canBorrow: Boolean = true,
    /** Kolik minut Parťák dá za den (Nastavení), pro vysvětlení u neaktivní volby. */
    val debtLimitMinutes: Int = 30,
    /** Kde je prosba Parťákovi (163). */
    val friendRequest: FriendRequest = FriendRequest.IDLE,
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
