package cz.narsil.app.nav

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cz.narsil.app.ServiceLocator
import cz.narsil.core.MovementRates
import cz.narsil.gamification.RulesGamificationEngine
import cz.narsil.overlay.SquatOverlayContent
import cz.narsil.overlay.hasCamera
import kotlinx.coroutines.launch

/**
 * Obrazovka dřepů v aplikaci (124): Janova `SquatScreen` se skutečným počítadlem (210/231).
 * Oprávnění ke kameře si vyžádá při prvním otevření. Dokončení zapíše dřepy do pohybu.
 */
@Composable
fun SquatRoute(onBack: () -> Unit) {
    val sl = ServiceLocator
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by sl.settingsRepository.settings.collectAsState()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!hasCamera(context)) permission.launch(Manifest.permission.CAMERA)
    }
    SquatOverlayContent(
        counter = sl.squatCounter,
        onCancel = onBack,
        onDone = { count ->
            scope.launch {
                runCatching { sl.movementRepository.recordSquats(count) }
                (sl.gamificationEngine as? RulesGamificationEngine)
                    ?.let { runCatching { it.onExtraSquats(count - MovementRates.SQUATS_PER_UNLOCK) } }
                onBack()
            }
        },
        demoMode = settings.demoMode,
        onRequestPermission = { permission.launch(Manifest.permission.CAMERA) },
    )
}
