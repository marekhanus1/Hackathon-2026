package cz.narsil.ui.screens.reward

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RewardUiState(
    val gainedXp: Int = 50,
    val totalXpAfter: Int = 2010,
    val previousLevel: Int = 4,
    val newLevel: Int = 5,
    val newLevelTitle: String = "Strážce pozornosti",
    val reasonTitle: String = "Odešel jsi od feedu sám!",
    val encouragingSubtitle: String = "Tvůj klid sílí. Znovu ukovaná vůle drží směr."
) {
    val leveledUp: Boolean
        get() = newLevel > previousLevel
}

class RewardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RewardUiState())
    val uiState: StateFlow<RewardUiState> = _uiState.asStateFlow()
}
