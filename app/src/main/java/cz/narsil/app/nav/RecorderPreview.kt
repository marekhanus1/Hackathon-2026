package cz.narsil.app.nav

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import cz.narsil.contract.ContractVideoRecorder

/** Má aplikace kameru? (oprávnění) */
fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/**
 * Náhled přední kamery pro [ContractVideoRecorder] (139): naváže kameru při zobrazení a uvolní ji
 * při odchodu. Bez oprávnění ukáže text (o oprávnění žádá volající).
 */
@Composable
fun RecorderPreview(recorder: ContractVideoRecorder, cameraAllowed: Boolean) {
    if (!cameraAllowed) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Povol kameru, ať můžeš natočit video.\nZůstane jen v telefonu.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(recorder, lifecycleOwner) {
        recorder.bind(lifecycleOwner, previewView.surfaceProvider)
        onDispose { recorder.unbind() }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
