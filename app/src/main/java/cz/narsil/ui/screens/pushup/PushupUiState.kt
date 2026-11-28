package cz.narsil.ui.screens.pushup

enum class PushupPlacementStatus(val label: String, val hint: String) {
    READY("Telefon leží rovně na zemi", "Sestup hrudníkem až nad kameru / displej."),
    NOT_FLAT("Polož telefon rovně na zem", "Displejem nahoru pod hrudník."),
    FACE_DOWN("Otoč telefon displejem nahoru", "Přední kamera musí mířit vzhůru."),
    MOVING("Nehýbej telefonem v ruce", "Počítání se zapne, až se telefon ustálí.")
}

data class PushupUiState(
    val completedPushups: Int = 5,
    val targetPushups: Int = 15,
    val depth: Double = 0.0,
    val placementStatus: PushupPlacementStatus = PushupPlacementStatus.READY,
    val proximityNear: Boolean = false,
    val personVisible: Boolean = true,
    val feedbackMessage: String = "Polož telefon pod hrudník a sjeď dolů téměř k displeji.",
    val earnedXp: Int = 20,
    val unlockedMinutes: Int = 10
) {
    val isCompleted: Boolean
        get() = completedPushups >= targetPushups
}
