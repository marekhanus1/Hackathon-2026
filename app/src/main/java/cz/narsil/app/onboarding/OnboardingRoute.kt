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
import cz.narsil.app.nav.RecorderPreview
import cz.narsil.app.nav.hasCameraPermission
import cz.narsil.contract.ContractVideoRecorder
import cz.narsil.contract.PrefsOnboardingVideoStore
import cz.narsil.core.PlanRules
import cz.narsil.core.data.DayKey
import cz.narsil.ui.screens.onboarding.OnboardingVideoState
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

    // Video pro Ozvěnu (139): kamera se zapne až po „Natočit video“, hotové video jde do zálohy Ozvěny.
    val videoStore = remember { PrefsOnboardingVideoStore(context) }
    val recorder = remember { ContractVideoRecorder(context) }
    val recording by recorder.state.collectAsState()
    var cameraOpen by rememberSaveable { mutableStateOf(false) }
    var hasVideo by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasVideo = runCatching { videoStore.video() }.getOrNull() != null }
    LaunchedEffect(recording.videoUri) {
        val uri = recording.videoUri ?: return@LaunchedEffect
        runCatching { videoStore.save(uri, DayKey.today()) }
        hasVideo = true
    }

    val wifi by sl.contextRepository.currentWifiSsid.collectAsState()
    val locationFlow = remember { (sl.contextRepository as? WifiContextRepository)?.locationServiceOn ?: kotlinx.coroutines.flow.MutableStateFlow(true) }
    val locationOn by locationFlow.collectAsState()
    val settings by sl.settingsRepository.settings.collectAsState()

    // Kroky MIUI nejdou ověřit: označí se jako hotové po otevření nastavení (uživatel je může odškrtnout).
    var miuiExpanded by rememberSaveable { mutableStateOf(true) }
    var miuiAutoStart by remember { mutableStateOf(OnboardingPrefs.miuiStep(context, "autostart")) }
    var miuiBattery by remember { mutableStateOf(OnboardingPrefs.miuiStep(context, "battery")) }
    var miuiPopup by remember { mutableStateOf(OnboardingPrefs.miuiStep(context, "popup")) }
    var miuiLock by remember { mutableStateOf(OnboardingPrefs.miuiStep(context, "lock")) }

    fun granted(step: PermissionStep) = PermissionChecker.isGranted(context, step) == true
    val stepsGranted = granted(PermissionStep.STEPS)

    val state = remember(refresh, wifi, locationOn, settings.homeSsid, healthGranted, miuiExpanded, miuiAutoStart, miuiBattery, miuiPopup, miuiLock) {
        val base = OnboardingUiState()
        OnboardingUiState(
            detectedWifiSsid = wifi,
            homeWifiSsid = settings.homeSsid,
            locationServiceOff = !locationOn,
            permissions = base.permissions.map { item ->
                item.copy(
                    isGranted = when (item.type) {
                        PermissionType.ACCESSIBILITY -> granted(PermissionStep.ACCESSIBILITY)
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
            miuiLockDone = miuiLock,
        )
    }.copy(
        video = OnboardingVideoState(
            cameraOpen = cameraOpen,
            isRecording = recording.isRecording,
            seconds = recording.elapsedSeconds,
            maxSeconds = PlanRules.MAX_VIDEO_SECONDS,
            hasVideo = hasVideo,
            error = recording.error,
        ),
    )

    val cameraAllowed = refresh >= 0 && hasCameraPermission(context)
    OnboardingScreen(
        state = state,
        onVideoButton = {
            when {
                !cameraAllowed -> runtime.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)).also { cameraOpen = true }
                !cameraOpen -> cameraOpen = true
                recording.isRecording -> recorder.stop()
                else -> recorder.start(DayKey.today(), prefix = "onboarding")
            }
        },
        videoCameraSlot = { RecorderPreview(recorder, cameraAllowed) },
        onConfirmHomeWifi = {
            scope.launch {
                if (!locationOn && granted(PermissionStep.LOCATION)) {
                    // Oprávnění máme, jen je vypnutý přepínač polohy v systému.
                    runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                } else if (!sl.contextRepository.saveCurrentWifiAsHome()) {
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
            OnboardingPrefs.setMiuiStep(context, "autostart", miuiAutoStart)
        },
        onToggleMiuiBattery = {
            if (!miuiBattery) PermissionChecker.open(context, PermissionStep.BATTERY)
            miuiBattery = !miuiBattery
            OnboardingPrefs.setMiuiStep(context, "battery", miuiBattery)
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
            OnboardingPrefs.setMiuiStep(context, "popup", miuiPopup)
        },
        onToggleMiuiLock = {
            // Zamknout v posledních aplikacích nejde otevřít z aplikace: návod je v kartě, tady jen zaškrtnutí.
            miuiLock = !miuiLock
            OnboardingPrefs.setMiuiStep(context, "lock", miuiLock)
        },
        onFinishOnboarding = {
            // Notifikace (hlídání, zpráva Parťákovi) jsou nepovinné: jedna žádost na konci. Mikrofon se žádá
            // až s kamerou při natáčení videa (smlouva, video pro Ozvěnu), ne tady.
            val optional = buildList {
                if (Build.VERSION.SDK_INT >= 33 && !PermissionChecker.has(context, Manifest.permission.POST_NOTIFICATIONS)) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
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
        PermissionType.LOCATION_FOR_WIFI -> launchRuntime(PermissionStep.LOCATION.runtime.toTypedArray())
        PermissionType.CAMERA -> launchRuntime(PermissionStep.CAMERA.runtime.toTypedArray())
        PermissionType.HEALTH_CONNECT -> {
            launchRuntime(PermissionStep.STEPS.runtime.toTypedArray())
            if (healthAvailable) runCatching { launchHealth() }
        }
    }
}
