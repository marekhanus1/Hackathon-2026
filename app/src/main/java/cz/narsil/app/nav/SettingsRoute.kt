package cz.narsil.app.nav

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.narsil.app.ServiceLocator
import cz.narsil.app.onboarding.PermissionChecker
import cz.narsil.app.onboarding.PermissionStep
import cz.narsil.core.fake.FakeStravaRepository
import cz.narsil.integrations.strava.HttpStravaRepository
import cz.narsil.ui.screens.settings.StravaCardState
import kotlinx.coroutines.flow.map
import cz.narsil.ui.screens.settings.SettingsPermissionType
import cz.narsil.ui.screens.settings.SettingsScreen
import cz.narsil.ui.screens.settings.SettingsViewModel

/** Nastavení (325 + 125): Janova obrazovka nad skutečnými repozitáři ze [ServiceLocator]. */
@Composable
fun SettingsRoute(onNavigate: (NarsilRoute) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val sl = ServiceLocator
    val strava = sl.stravaRepository
    val vm: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(
                    sl.settingsRepository,
                    sl.contextRepository,
                    strava,
                    stravaStatus = (strava as? HttpStravaRepository)?.status?.map {
                        StravaCardState(configured = it.configured, connected = it.connected, demo = it.demo, message = it.message)
                    },
                ) as T
        },
    )
    val state by vm.uiState.collectAsState()
    val location = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        (sl.contextRepository as? cz.narsil.detection.context.WifiContextRepository)?.refresh()
    }
    // Stav oprávnění po každém návratu z nastavení systému.
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.updatePermissionStatuses(
                accessibility = PermissionChecker.isGranted(context, PermissionStep.ACCESSIBILITY) == true,
                overlay = PermissionChecker.isGranted(context, PermissionStep.OVERLAY) == true,
                location = PermissionChecker.isGranted(context, PermissionStep.LOCATION) == true,
            )
        }
    }
    SettingsScreen(
        state = state,
        onSaveHomeWifi = vm::saveCurrentWifiAsHome,
        onSelectManualZone = vm::setManualZone,
        onSelectLlmProvider = vm::selectLlmProvider,
        // 132: bez klíčů se nic nespouští (hláška + demo), chybějící prohlížeč = hláška, ne pád.
        onConnectStrava = {
            (strava as? HttpStravaRepository)?.connect { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        },
        onStartStravaDemo = {
            (strava as? HttpStravaRepository)?.startDemo() ?: (strava as? FakeStravaRepository)?.setConnected(true)
        },
        onDisconnectStrava = {
            (strava as? HttpStravaRepository)?.disconnect() ?: (strava as? FakeStravaRepository)?.setConnected(false)
        },
        onOpenSystemSettings = { type ->
            when (type) {
                SettingsPermissionType.ACCESSIBILITY -> PermissionChecker.open(context, PermissionStep.ACCESSIBILITY)
                SettingsPermissionType.OVERLAY -> PermissionChecker.open(context, PermissionStep.OVERLAY)
                SettingsPermissionType.LOCATION ->
                    location.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        },
        onOpenOnboarding = { onNavigate(NarsilRoute.Onboarding) },
        onOpenDemoPanel = { onNavigate(NarsilRoute.Demo) },
        onBack = onBack,
    )
}
