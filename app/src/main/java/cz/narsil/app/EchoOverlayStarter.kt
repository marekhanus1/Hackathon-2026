package cz.narsil.app

import android.util.Log
import cz.narsil.gamification.RulesGamificationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.fake.FakeInterventionTriggerSource
import cz.narsil.detection.accessibility.AccessibilityEvents
import cz.narsil.detection.accessibility.AccessibilityTriggerSource
import cz.narsil.overlay.EchoOutcomeHandler
import cz.narsil.overlay.EchoOverlayController
import cz.narsil.overlay.EchoOverlayWindow
import cz.narsil.overlay.HomeNavigator
import cz.narsil.overlay.ScrollProgressOverlay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Integrace G2 (115): když běží služba přístupnosti, spustí [EchoOverlayController] nad zásahy
 * z detekce; okno jde přes službu (TYPE_ACCESSIBILITY_OVERLAY). Volby → [EchoOutcomeHandler] →
 * pohyb, gamifikace, časovač a zpět do detekce.
 */
object EchoOverlayStarter {
    private const val TAG = "NarsilApp"
    private var controller: EchoOverlayController? = null
    private var progressBar: ScrollProgressOverlay? = null
    private var progressJob: Job? = null

    private val _ready = MutableStateFlow(false)

    /** Ozvěna připravená (běží služba přístupnosti). StateFlow, ať ho Compose sleduje. */
    val isReady: StateFlow<Boolean> = _ready.asStateFlow()

    /** Demo panel (123): ukáže Ozvěnu hned, bez čekání na detekci. false = služba neběží. */
    suspend fun showNow(trigger: InterventionTrigger = FakeInterventionTriggerSource.demoTrigger()): Boolean {
        val c = controller ?: return false
        c.show(trigger)
        return true
    }

    fun start() {
        val sl = ServiceLocator
        sl.appScope.launch {
            // Sleduje instanci služby: nová instance (přeinstalace) dostane nové okno i ovladač.
            AccessibilityEvents.service.collect { service ->
                controller?.stop()
                controller = null
                progressJob?.cancel()
                progressBar?.dismiss()
                progressBar = null
                _ready.value = false
                if (service == null) return@collect
                val triggers = sl.interventionTriggerSource
                // Ukazatel blížícího se limitu (135): jen se skutečnou detekcí a zapnutým nastavením.
                val bar = ScrollProgressOverlay(service).also { progressBar = it }
                val percent = (triggers as? AccessibilityTriggerSource)?.progressPercent ?: flowOf(null)
                progressJob = sl.appScope.launch {
                    combine(percent, sl.settingsRepository.settings) { p, s -> p.takeIf { s.showScrollProgress } }
                        .distinctUntilChanged()
                        .collect { bar.update(it) }
                }
                val handler = EchoOutcomeHandler(
                    movement = sl.movementRepository,
                    gamification = sl.gamificationEngine,
                    timer = sl.unlockTimer,
                    goHome = { HomeNavigator.goHome(sl.appContext) },
                    onDetectionOutcome = { outcome, minutes ->
                        (triggers as? AccessibilityTriggerSource)?.onOutcome(outcome, minutes)
                    },
                )
                controller = EchoOverlayController(
                    window = EchoOverlayWindow(service),
                    triggers = triggers,
                    movement = sl.movementRepository,
                    contracts = sl.contractRepository,
                    videos = sl.echoVideoRepository,
                    scope = sl.appScope,
                    choiceHandler = handler,
                    squatCounter = sl.squatCounter,
                    pushupCounter = sl.pushupCounter,
                    exercise = { sl.settingsRepository.settings.value.exercise },
                    demoMode = { sl.settingsRepository.settings.value.demoMode },
                    gamification = sl.gamificationEngine,
                    onExtraSquats = { n -> (sl.gamificationEngine as? RulesGamificationEngine)?.onExtraSquats(n) },
                ).also { it.start() }
                _ready.value = true
                Log.i(TAG, "Ozvěna připravena (fakes=${sl.useFakes})")
            }
        }
    }
}
