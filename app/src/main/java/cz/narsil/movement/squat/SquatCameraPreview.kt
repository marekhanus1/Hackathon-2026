package cz.narsil.movement.squat

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.narsil.core.SquatCounter

/**
 * Náhled kamery pro obrazovku dřepů (311, Jan): vložit do `SquatScreen(cameraPreviewSlot = { … })`.
 * Při zobrazení spustí [counter] (vynuluje počet) a při odchodu ho zastaví. Počet čte UI z `counter.count`.
 * Funguje s [PoseSquatCounter] i s fake (fake jen počítá, náhled zůstane prázdný).
 */
@Composable
fun SquatCameraPreview(counter: SquatCounter, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(counter, lifecycleOwner) {
        counter.start(lifecycleOwner, previewView.surfaceProvider)
        onDispose { counter.stop() }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
