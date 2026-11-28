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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cz.narsil.core.SquatCounter
import cz.narsil.movement.pushup.CameraPushupCounter
import cz.narsil.movement.pushup.PushupCameraPreview
import cz.narsil.movement.squat.PoseSquatCounter
import cz.narsil.movement.squat.SquatCameraPreview
import cz.narsil.movement.squat.SquatTuningOverlay
import cz.narsil.ui.components.AeroBlueOrb
import cz.narsil.ui.components.NeuIcons
import cz.narsil.ui.components.NeuPrimaryButton
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.OnSky

/**
 * Náhled kamery pro dřepy (124): s oprávněním CAMERA [SquatCameraPreview] (spustí a zastaví [counter]),
 * v demo režimu navrch [SquatTuningOverlay]. Bez oprávnění text a tlačítko; nic nespadne.
 *
 * @param onRequestPermission v aktivitě systémový dialog; null (okno Ozvěny, bez aktivity) = systémový dialog
 *   přes průhlednou [cz.narsil.app.CameraPermissionActivity] (kamera až při prvním použití), záložně nastavení aplikace
 */
@Composable
fun SquatCameraSlot(
    counter: SquatCounter,
    demoMode: Boolean,
    onRequestPermission: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    // Fake počítadlo kameru nepotřebuje.
    val needsCamera = counter is PoseSquatCounter || counter is CameraPushupCounter
    var granted by remember { mutableStateOf(!needsCamera || hasCamera(context)) }
    // Po návratu z dialogu / nastavení oprávnění znovu zkontrolovat.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, needsCamera) {
        if (!needsCamera) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { granted = hasCamera(context) }
    }
    // Okno Ozvěny zůstává „resumed“ i během dialogu – oprávnění kontrolovat průběžně, dokud nepřijde.
    LaunchedEffect(granted, needsCamera) {
        while (needsCamera && !granted) {
            kotlinx.coroutines.delay(700)
            granted = hasCamera(context)
        }
    }
    if (!granted) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Aero: modrá skleněná koule s kamerou, bílý text se stínem a zelené lesklé tlačítko.
            AeroBlueOrb(NeuIcons.Camera, size = 72.dp)
            OnSky {
                NeuText(
                    if (counter is CameraPushupCounter) "Kliky počítá přední kamera. Obraz zůstává jen v telefonu."
                    else "Dřepy počítá kamera. Obraz zůstává jen v telefonu.",
                    size = 16.sp,
                    weight = FontWeight.Bold,
                    align = TextAlign.Center,
                    lineHeight = 21.sp,
                )
            }
            NeuPrimaryButton("Povolit kameru", onClick = {
                when {
                    onRequestPermission != null -> onRequestPermission()
                    !cz.narsil.app.CameraPermissionActivity.start(context) -> openAppSettings(context)
                }
            })
        }
        return
    }
    Box(Modifier.fillMaxSize()) {
        if (counter is CameraPushupCounter) PushupCameraPreview(counter, Modifier.fillMaxSize())
        else SquatCameraPreview(counter, Modifier.fillMaxSize())
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
