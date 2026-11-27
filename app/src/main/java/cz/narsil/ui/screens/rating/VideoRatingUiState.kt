package cz.narsil.ui.screens.rating

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class WatchIntention(val label: String) {
    LEARNING("Učení"),
    REST("Odpočinek"),
    INSPIRATION("Inspirace"),
    AIMLESS("Jen tak")
}

data class VideoRatingUiState(
    val videoTitle: String = "Rust Async – kompletní průvodce pro začátečníky",
    val channelName: String = "TechTalk CZ",
    val selectedIntention: WatchIntention = WatchIntention.LEARNING,
    val selectedRating: Int? = 4,
    val earnedXp: Int = 5
) {
    val canSubmit: Boolean
        get() = selectedRating != null
}

class VideoRatingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VideoRatingUiState())
    val uiState: StateFlow<VideoRatingUiState> = _uiState.asStateFlow()

    fun selectIntention(intention: WatchIntention) {
        _uiState.update { it.copy(selectedIntention = intention) }
    }

    fun selectRating(rating: Int) {
        _uiState.update { it.copy(selectedRating = rating.coerceIn(1, 5)) }
    }
}
