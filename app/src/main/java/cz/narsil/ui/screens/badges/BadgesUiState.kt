package cz.narsil.ui.screens.badges

import androidx.lifecycle.ViewModel
import cz.narsil.ui.components.AvatarState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BadgeItem(
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val isUnlocked: Boolean
)

data class PartyMemberEntry(
    val rank: Int,
    val name: String,
    val reclaimedMinutes: Int,
    val isCurrentUser: Boolean
)

data class BadgesUiState(
    val currentAvatarState: AvatarState = AvatarState.ENERGIZED,
    val avatarSummary7Days: String = "Posledních 7 dní: 5 výher · 420 dřepů",
    val badges: List<BadgeItem> = defaultBadges(),
    val partyGoalSteps: Int = 100_000,
    val partyCurrentSteps: Int = 74_200,
    val leaderboard: List<PartyMemberEntry> = defaultLeaderboard()
)

private fun defaultBadges(): List<BadgeItem> = listOf(
    BadgeItem(
        id = "closed_10x",
        title = "Odešel jsem sám 10×",
        description = "Zavřít feed při Ozvěně bez placení pohybem.",
        progress = 10,
        target = 10,
        isUnlocked = true
    ),
    BadgeItem(
        id = "week_no_debt",
        title = "Týden bez dluhu",
        description = "Sedm dní po sobě bez přeneseného pohybového dluhu.",
        progress = 7,
        target = 7,
        isUnlocked = true
    ),
    BadgeItem(
        id = "1000_squats",
        title = "1 000 dřepů",
        description = "Celkem tisíc dřepů spočítaných kamerou.",
        progress = 640,
        target = 1000,
        isUnlocked = false
    ),
    BadgeItem(
        id = "echo_keeper",
        title = "Strážce Ozvěny",
        description = "Pět večerních smluv v řadě s vlastním videem.",
        progress = 4,
        target = 5,
        isUnlocked = false
    ),
    BadgeItem(
        id = "reforged",
        title = "Znovu ukovaný",
        description = "Vrátit se do rytmu po náročném dni s pomocí žolíka.",
        progress = 1,
        target = 1,
        isUnlocked = true
    )
)

private fun defaultLeaderboard(): List<PartyMemberEntry> = listOf(
    PartyMemberEntry(rank = 1, name = "Marek", reclaimedMinutes = 310, isCurrentUser = false),
    PartyMemberEntry(rank = 2, name = "Jan (ty)", reclaimedMinutes = 285, isCurrentUser = true),
    PartyMemberEntry(rank = 3, name = "Šimon", reclaimedMinutes = 240, isCurrentUser = false),
    PartyMemberEntry(rank = 4, name = "Štěpán", reclaimedMinutes = 195, isCurrentUser = false)
)

class BadgesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BadgesUiState())
    val uiState: StateFlow<BadgesUiState> = _uiState.asStateFlow()
}
