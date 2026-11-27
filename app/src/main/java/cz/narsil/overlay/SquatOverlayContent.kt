package cz.narsil.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cz.narsil.core.MovementRates
import cz.narsil.core.SquatCounter
import cz.narsil.ui.screens.squat.SquatPhase
import cz.narsil.ui.screens.squat.SquatScreen
import cz.narsil.ui.screens.squat.SquatUiState

/**
 * Dřepy (114, 124): obrazovka 311 + náhled kamery ([SquatCameraSlot] spustí [SquatCounter] na lifecycle
 * okna / obrazovky). Po dosažení cíle volá [onDone] až po potvrzení „Pokračovat“.
 * Použití: okno Ozvěny i obrazovka dřepů v navigaci.
 */
@Composable
fun SquatOverlayContent(
    counter: SquatCounter,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    demoMode: Boolean = false,
    onRequestPermission: (() -> Unit)? = null,
) {
    val target = MovementRates.SQUATS_PER_UNLOCK
    val count by counter.count.collectAsState()
    LaunchedEffect(count >= target) { if (count >= target) counter.stop() }
    val state = SquatUiState(
        completedSquats = count.coerceAtMost(target),
        targetSquats = target,
        phase = if (count >= target) SquatPhase.COMPLETED else if (count == 0) SquatPhase.READY else SquatPhase.STANDING_UP,
        feedbackMessage = if (count >= target) "Hotovo, odemykám ${MovementRates.MINUTES_PER_UNLOCK} minut."
        else "Zbývá ${target - count} dřepů.",
        earnedXp = 20,
        unlockedMinutes = MovementRates.MINUTES_PER_UNLOCK,
    )
    SquatScreen(
        state = state,
        onCancel = onCancel,
        onFinishContinue = { if (count >= target) onDone() },
        cameraPreviewSlot = { SquatCameraSlot(counter, demoMode, onRequestPermission) },
    )
}
