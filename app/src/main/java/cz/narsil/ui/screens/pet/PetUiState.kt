package cz.narsil.ui.screens.pet

/** Jedna zpráva v chatu se sovou Narsilkou. */
data class PetMessage(
    val fromPet: Boolean,
    val text: String,
)

/** Rychlé volby odpovědí v chatu. */
val PET_QUICK_REPLIES = listOf(
    "Nudím se",
    "Mám chuť scrollovat",
    "Jak se mi daří?",
    "Jsem unavený",
)

/** Stav obrazovky chatu a bubliny na domovské obrazovce. */
data class PetUiState(
    val mood: PetMood = PetMood.KLIDNA,
    val petName: String = "Narsilka",
    val bubbleText: String? = null,
    val bubbleVisible: Boolean = false,
    val messages: List<PetMessage> = emptyList(),
    val isThinking: Boolean = false,
    val input: String = "",
)
