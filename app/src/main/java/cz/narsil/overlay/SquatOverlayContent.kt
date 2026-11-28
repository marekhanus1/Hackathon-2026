package cz.narsil.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cz.narsil.core.Exercise
import cz.narsil.core.MovementRates
import cz.narsil.core.SquatCounter
import cz.narsil.movement.pushup.CameraPushupCounter
import cz.narsil.movement.pushup.PhonePlacement
import cz.narsil.ui.screens.pushup.PushupPlacementStatus
import cz.narsil.ui.screens.pushup.PushupScreen
import cz.narsil.ui.screens.pushup.PushupUiState
import cz.narsil.ui.screens.squat.SquatPhase
import cz.narsil.ui.screens.squat.SquatScreen
import cz.narsil.ui.screens.squat.SquatUiState

/**
 * Dřepy (114, 124): obrazovka 311 + náhled kamery ([SquatCameraSlot] spustí [SquatCounter] na lifecycle
 * okna / obrazovky). Po 20 počítá dál (dřepy navíc → žolík, SPEC 8); [onDone] s celkovým počtem
 * až po potvrzení „Pokračovat“.
 * Použití: okno Ozvěny i obrazovka dřepů v navigaci.
 * [exercise] = PUSHUPS (232): Janova obrazovka kliků 232 se stejným počítadlem (cíl 10).
 */
@Composable
fun SquatOverlayContent(
    counter: SquatCounter,
    onCancel: () -> Unit,
    onDone: (count: Int) -> Unit,
    demoMode: Boolean = false,
    onRequestPermission: (() -> Unit)? = null,
    exercise: Exercise = Exercise.SQUATS,
    onSelectExercise: (Exercise) -> Unit = {},
) {
    if (exercise == Exercise.PUSHUPS) {
        PushupContent(counter, onCancel, onDone, demoMode, onRequestPermission, onSelectExercise)
        return
    }
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
        onSelectExercise = onSelectExercise,
        cameraPreviewSlot = { SquatCameraSlot(counter, demoMode, onRequestPermission) },
    )
}

/** Kliky na podlaze (232): stav z [CameraPushupCounter] (položení telefonu, přiblížení, člověk v záběru). */
@Composable
private fun PushupContent(
    counter: SquatCounter,
    onCancel: () -> Unit,
    onDone: (count: Int) -> Unit,
    demoMode: Boolean,
    onRequestPermission: (() -> Unit)?,
    onSelectExercise: (Exercise) -> Unit,
) {
    val target = MovementRates.PUSHUPS_PER_UNLOCK
    val count by counter.count.collectAsState()
    val camera = counter as? CameraPushupCounter
    val placement = camera?.placement?.collectAsState()?.value ?: PhonePlacement.READY_ON_FLOOR
    val near = camera?.proximityNear?.collectAsState()?.value ?: false
    val person = camera?.personVisible?.collectAsState()?.value ?: true
    val depth = camera?.depth?.collectAsState()?.value ?: 0.0
    val status = when (placement) {
        PhonePlacement.READY_ON_FLOOR -> PushupPlacementStatus.READY
        PhonePlacement.NOT_FLAT -> PushupPlacementStatus.NOT_FLAT
        PhonePlacement.FACE_DOWN -> PushupPlacementStatus.FACE_DOWN
        PhonePlacement.MOVING -> PushupPlacementStatus.MOVING
    }
    val state = PushupUiState(
        completedPushups = count.coerceAtMost(target),
        targetPushups = target,
        depth = depth,
        placementStatus = status,
        proximityNear = near,
        personVisible = person,
        feedbackMessage = when {
            count > target -> "Hotovo! +${count - target} navíc."
            count == target -> "Hotovo, odemykám ${MovementRates.MINUTES_PER_UNLOCK} minut. Můžeš pokračovat."
            status != PushupPlacementStatus.READY -> status.hint
            else -> "Zbývá ${target - count} kliků."
        },
        earnedXp = 20,
        unlockedMinutes = MovementRates.MINUTES_PER_UNLOCK,
    )
    PushupScreen(
        state = state,
        onCancel = onCancel,
        onFinishContinue = {
            if (count >= target) {
                counter.stop()
                onDone(count)
            }
        },
        onSelectExercise = onSelectExercise,
        cameraPreviewSlot = { SquatCameraSlot(counter, demoMode, onRequestPermission) },
    )
}
