package cz.narsil.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cz.narsil.core.MovementRates
import cz.narsil.core.SquatCounter
import cz.narsil.ui.screens.squat.SquatPhase
import cz.narsil.ui.screens.squat.SquatScreen
import cz.narsil.ui.screens.squat.SquatUiState

/**
 * Dřepy (114, 124): obrazovka 311 + náhled kamery ([SquatCameraSlot] spustí [SquatCounter] na lifecycle
 * okna / obrazovky). Po 20 počítá dál (dřepy navíc → žolík, SPEC 8); [onDone] s celkovým počtem
 * až po potvrzení „Pokračovat“.
 * Použití: okno Ozvěny i obrazovka dřepů v navigaci.
 */
@Composable
fun SquatOverlayContent(
    counter: SquatCounter,
    onCancel: () -> Unit,
    onDone: (count: Int) -> Unit,
    demoMode: Boolean = false,
    onRequestPermission: (() -> Unit)? = null,
) {
    val target = MovementRates.SQUATS_PER_UNLOCK
    val count by counter.count.collectAsState()
    val state = SquatUiState(
        completedSquats = count.coerceAtMost(target),
        targetSquats = target,
        phase = if (count >= target) SquatPhase.COMPLETED else if (count == 0) SquatPhase.READY else SquatPhase.STANDING_UP,
        feedbackMessage = when {
            count > target -> "Hotovo! +${count - target} navíc · každých 50 navíc je žolík."
            count == target -> "Hotovo, odemykám ${MovementRates.MINUTES_PER_UNLOCK} minut. Můžeš pokračovat pro žolíka."
            else -> "Zbývá ${target - count} dřepů."
        },
        earnedXp = 20,
        unlockedMinutes = MovementRates.MINUTES_PER_UNLOCK,
    )
    SquatScreen(
        state = state,
        onCancel = onCancel,
        onFinishContinue = {
            if (count >= target) {
                counter.stop()
                onDone(count)
            }
        },
        cameraPreviewSlot = { SquatCameraSlot(counter, demoMode, onRequestPermission) },
    )
}
