package cz.narsil.overlay

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cz.narsil.core.SquatCounter
import cz.narsil.movement.squat.PoseSquatCounter
import cz.narsil.movement.squat.SquatCameraPreview
import cz.narsil.movement.squat.SquatTuningOverlay

/**
 * Náhled kamery pro dřepy (124): s oprávněním CAMERA [SquatCameraPreview] (spustí a zastaví [counter]),
 * v demo režimu navrch [SquatTuningOverlay]. Bez oprávnění text a tlačítko; nic nespadne.
 *
 * @param onRequestPermission v aktivitě systémový dialog; null (okno Ozvěny, bez aktivity) = otevře
 *   nastavení aplikace
 */
@Composable
fun SquatCameraSlot(
    counter: SquatCounter,
    demoMode: Boolean,
    onRequestPermission: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    // Fake počítadlo kameru nepotřebuje.
    val needsCamera = counter is PoseSquatCounter
    var granted by remember { mutableStateOf(!needsCamera || hasCamera(context)) }
    // Po návratu z dialogu / nastavení oprávnění znovu zkontrolovat.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, needsCamera) {
        if (!needsCamera) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { granted = hasCamera(context) }
    }
    if (!granted) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Dřepy počítá kamera. Obraz zůstává jen v telefonu.",
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = { onRequestPermission?.invoke() ?: openAppSettings(context) }) {
                Text(if (onRequestPermission != null) "Povolit kameru" else "Povolit kameru v nastavení")
            }
        }
        return
    }
    Box(Modifier.fillMaxSize()) {
        SquatCameraPreview(counter, Modifier.fillMaxSize())
        if (demoMode && counter is PoseSquatCounter) {
            // Nahoře je Janova bublina s instrukcí, dole počítadlo: ukazatel mezi ně, nad počítadlo.
            SquatTuningOverlay(
                counter,
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.92f)
                    .padding(bottom = 136.dp),
            )
        }
    }
}

fun hasCamera(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
