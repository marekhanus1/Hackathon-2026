package cz.narsil.app.nav

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
import cz.narsil.BuildConfig
import cz.narsil.app.ServiceLocator
import cz.narsil.core.LlmProvider
import cz.narsil.movement.PartnerNotifier
import cz.narsil.ui.screens.settings.SettingsActions
import cz.narsil.app.onboarding.PermissionChecker
import cz.narsil.app.onboarding.PermissionStep
import cz.narsil.core.fake.FakeStravaRepository
import cz.narsil.integrations.strava.HttpStravaRepository
import cz.narsil.ui.screens.settings.StravaCardState
import kotlinx.coroutines.flow.map
import cz.narsil.ui.screens.settings.SettingsPermissionType
import cz.narsil.ui.screens.settings.SettingsScreen
import cz.narsil.ui.screens.settings.SettingsViewModel

/** Nastavení (325 + 125 + 133): Janova obrazovka nad skutečnými repozitáři ze [ServiceLocator]. */
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
                    llmKeys = mapOf(
                        LlmProvider.CLAUDE to BuildConfig.CLAUDE_API_KEY.isNotBlank(),
                        LlmProvider.GEMINI to BuildConfig.GEMINI_API_KEY.isNotBlank(),
                        LlmProvider.OPENAI to BuildConfig.OPENAI_API_KEY.isNotBlank(),
                    ),
                    onProtectionPaused = { PartnerNotifier(context.applicationContext).notifyProtectionPaused() },
                ) as T
        },
    )
    val state by vm.uiState.collectAsState()
    val refreshPermissions = {
        vm.updatePermissionStatuses(
            accessibility = PermissionChecker.isGranted(context, PermissionStep.ACCESSIBILITY) == true,
            overlay = PermissionChecker.isGranted(context, PermissionStep.OVERLAY) == true,
            location = PermissionChecker.isGranted(context, PermissionStep.LOCATION) == true,
            camera = PermissionChecker.isGranted(context, PermissionStep.CAMERA) == true,
            notifications = PermissionChecker.isGranted(context, PermissionStep.NOTIFICATIONS) != false,
        )
    }
    val runtimePermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        (sl.contextRepository as? cz.narsil.detection.context.WifiContextRepository)?.refresh()
        refreshPermissions()
    }
    // Stav oprávnění po každém návratu z nastavení systému.
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { refreshPermissions() }
    }
    fun request(step: PermissionStep) {
        if (step.runtime.isEmpty()) PermissionChecker.open(context, step)
        else runCatching { runtimePermissions.launch(step.runtime.toTypedArray()) }
            .onFailure { PermissionChecker.open(context, step) }
    }
    SettingsScreen(
        state = state,
        actions = SettingsActions(
            onBack = onBack,
            onSaveHomeWifi = vm::saveCurrentWifiAsHome,
            onSelectManualZone = vm::setManualZone,
            onSelectLlmProvider = vm::selectLlmProvider,
            onSelectExercise = vm::setExercise,
            onToleranceMinutes = vm::setToleranceMinutes,
            onVideoChainLimit = vm::setVideoChainLimit,
            onSquatsPerUnlock = vm::setSquatsPerUnlock,
            onPushupsPerUnlock = vm::setPushupsPerUnlock,
            onDebtLimitMinutes = vm::setDebtLimitMinutes,
            onAppTracked = vm::setAppTracked,
            onPartnerAlerts = vm::setPartnerAlerts,
            onShowScrollProgress = vm::setShowScrollProgress,
            onPauseProtection = vm::pauseProtectionUntilMorning,
            onResumeProtection = vm::resumeProtection,
            onResetSection = vm::resetSection,
            onConfirmPending = vm::confirmPendingChange,
            onCancelPending = vm::cancelPendingChange,
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
                    SettingsPermissionType.ACCESSIBILITY -> request(PermissionStep.ACCESSIBILITY)
                    SettingsPermissionType.OVERLAY -> request(PermissionStep.OVERLAY)
                    SettingsPermissionType.LOCATION -> request(PermissionStep.LOCATION)
                    SettingsPermissionType.CAMERA -> request(PermissionStep.CAMERA)
                    SettingsPermissionType.NOTIFICATIONS -> request(PermissionStep.NOTIFICATIONS)
                }
            },
            onOpenOnboarding = { onNavigate(NarsilRoute.Onboarding) },
            onOpenDemoPanel = { onNavigate(NarsilRoute.Demo) },
        ),
    )
}
