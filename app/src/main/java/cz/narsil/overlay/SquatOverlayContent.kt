package cz.narsil.overlay

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.narsil.core.MovementRates
import cz.narsil.core.SquatCounter
import cz.narsil.ui.screens.squat.SquatPhase
import cz.narsil.ui.screens.squat.SquatScreen
import cz.narsil.ui.screens.squat.SquatUiState

/**
 * Dřepy v okně Ozvěny (114): obrazovka 311 + náhled kamery; [SquatCounter] (210) se naváže
 * na lifecycle okna. Po dosažení cíle volá [onDone] až po potvrzení „Pokračovat“.
 */
@Composable
fun SquatOverlayContent(
    counter: SquatCounter,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    val target = MovementRates.SQUATS_PER_UNLOCK
    val count by counter.count.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    DisposableEffect(counter) {
        counter.start(lifecycleOwner, previewView.surfaceProvider)
        onDispose { counter.stop() }
    }
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
        cameraPreviewSlot = { AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) },
    )
}
