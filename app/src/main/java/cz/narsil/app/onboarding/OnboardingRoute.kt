package cz.narsil.app.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import cz.narsil.app.ServiceLocator
import cz.narsil.detection.context.WifiContextRepository
import cz.narsil.movement.steps.HealthConnectStepsSource
import cz.narsil.ui.screens.onboarding.OnboardingScreen
import cz.narsil.ui.screens.onboarding.OnboardingUiState
import cz.narsil.ui.screens.onboarding.PermissionItem
import cz.narsil.ui.screens.onboarding.PermissionType
import kotlinx.coroutines.launch

/**
 * Průvodce oprávněními a domácí Wi-Fi (122): Janova obrazovka (320) nad [PermissionChecker].
 * Stav se přepočítá po každém návratu ze systémových nastavení a dialogů.
 */
@Composable
fun OnboardingRoute(onDone: () -> Unit) {
    val context = LocalContext.current
    val sl = ServiceLocator
    val scope = rememberCoroutineScope()

    var refresh by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { refresh++ } }

    val health = remember { HealthConnectStepsSource(context) }
    val healthAvailable = remember { HealthConnectStepsSource.isAvailable(context) }
    var healthGranted by remember { mutableStateOf(false) }
    LaunchedEffect(refresh) { if (healthAvailable) healthGranted = health.hasPermission() }

    val runtime = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
        (sl.contextRepository as? WifiContextRepository)?.refresh()
    }
    val healthLauncher = rememberLauncherForActivityResult(HealthConnectStepsSource.permissionContract()) { refresh++ }

    val wifi by sl.contextRepository.currentWifiSsid.collectAsState()
    val settings by sl.settingsRepository.settings.collectAsState()

    // Kroky MIUI nejdou ověřit: označí se jako hotové po otevření nastavení (uživatel je může odškrtnout).
    var miuiExpanded by rememberSaveable { mutableStateOf(true) }
    var miuiAutoStart by rememberSaveable { mutableStateOf(false) }
    var miuiBattery by rememberSaveable { mutableStateOf(false) }
    var miuiPopup by rememberSaveable { mutableStateOf(false) }

    fun granted(step: PermissionStep) = PermissionChecker.isGranted(context, step) == true
    val stepsGranted = granted(PermissionStep.STEPS)

    val state = remember(refresh, wifi, settings.homeSsid, healthGranted, miuiExpanded, miuiAutoStart, miuiBattery, miuiPopup) {
        val base = OnboardingUiState()
        OnboardingUiState(
            detectedWifiSsid = wifi ?: "žádná Wi-Fi (nebo chybí poloha)",
            homeWifiSsid = settings.homeSsid,
            permissions = base.permissions.map { item ->
                item.copy(
                    isGranted = when (item.type) {
                        PermissionType.ACCESSIBILITY -> granted(PermissionStep.ACCESSIBILITY)
                        PermissionType.OVERLAY -> granted(PermissionStep.OVERLAY)
                        PermissionType.LOCATION_FOR_WIFI -> granted(PermissionStep.LOCATION)
                        PermissionType.CAMERA -> granted(PermissionStep.CAMERA)
                        // Kroky: Health Connect, nebo záloha přes senzor (ACTIVITY_RECOGNITION).
                        PermissionType.HEALTH_CONNECT -> healthGranted || stepsGranted
                    },
                )
            },
            isMiuiDevice = PermissionChecker.isMiui,
            miuiStepsExpanded = miuiExpanded,
            miuiAutoStartDone = miuiAutoStart,
            miuiBatteryDone = miuiBattery || granted(PermissionStep.BATTERY),
            miuiBackgroundPopupDone = miuiPopup,
        )
    }

    OnboardingScreen(
        state = state,
        onConfirmHomeWifi = {
            scope.launch {
                if (!sl.contextRepository.saveCurrentWifiAsHome()) {
                    // Bez polohy Android název Wi-Fi neprozradí: rovnou o ni požádat.
                    runtime.launch(PermissionStep.LOCATION.runtime.toTypedArray())
                }
            }
        },
        onRequestPermission = { type -> request(type, context, runtime::launch, healthAvailable) { healthLauncher.launch(HealthConnectStepsSource.PERMISSIONS) } },
        onToggleMiuiExpanded = { miuiExpanded = !miuiExpanded },
        onToggleMiuiAutoStart = {
            if (!miuiAutoStart) PermissionChecker.open(context, PermissionStep.MIUI_AUTOSTART)
            miuiAutoStart = !miuiAutoStart
        },
        onToggleMiuiBattery = {
            if (!miuiBattery) PermissionChecker.open(context, PermissionStep.BATTERY)
            miuiBattery = !miuiBattery
        },
        onToggleMiuiBackgroundPopup = {
            // MIUI: „Zobrazit vyskakovací okna na pozadí“ je v oprávněních aplikace.
            if (!miuiPopup) runCatching {
                context.startActivity(
                    Intent("miui.intent.action.APP_PERM_EDITOR")
                        .putExtra("extra_pkgname", context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.onFailure { runCatching { context.startActivity(PermissionChecker.appDetails(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
            miuiPopup = !miuiPopup
        },
        onFinishOnboarding = {
            // Notifikace (zpráva Parťákovi) a mikrofon (video smlouvy) jsou nepovinné: jedna žádost na konci.
            val optional = buildList {
                if (Build.VERSION.SDK_INT >= 33 && !PermissionChecker.has(context, Manifest.permission.POST_NOTIFICATIONS)) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (!PermissionChecker.has(context, Manifest.permission.RECORD_AUDIO)) add(Manifest.permission.RECORD_AUDIO)
            }
            if (optional.isNotEmpty()) runtime.launch(optional.toTypedArray())
            OnboardingPrefs.setDone(context)
            onDone()
        },
    )
}

private fun request(
    type: PermissionType,
    context: android.content.Context,
    launchRuntime: (Array<String>) -> Unit,
    healthAvailable: Boolean,
    launchHealth: () -> Unit,
) {
    when (type) {
        PermissionType.ACCESSIBILITY -> PermissionChecker.open(context, PermissionStep.ACCESSIBILITY)
        PermissionType.OVERLAY -> PermissionChecker.open(context, PermissionStep.OVERLAY)
        PermissionType.LOCATION_FOR_WIFI -> launchRuntime(PermissionStep.LOCATION.runtime.toTypedArray())
        PermissionType.CAMERA -> launchRuntime(PermissionStep.CAMERA.runtime.toTypedArray())
        PermissionType.HEALTH_CONNECT -> {
            launchRuntime(PermissionStep.STEPS.runtime.toTypedArray())
            if (healthAvailable) runCatching { launchHealth() }
        }
    }
}
