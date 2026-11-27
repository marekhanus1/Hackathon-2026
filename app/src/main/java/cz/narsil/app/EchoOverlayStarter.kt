package cz.narsil.app

import android.util.Log
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
import kotlinx.coroutines.launch

/**
 * Integrace G2 (115): když běží služba přístupnosti, spustí [EchoOverlayController] nad zásahy
 * z detekce; okno jde přes službu (TYPE_ACCESSIBILITY_OVERLAY). Volby → [EchoOutcomeHandler] →
 * pohyb, gamifikace, časovač a zpět do detekce.
 */
object EchoOverlayStarter {
    private const val TAG = "NarsilApp"
    private var controller: EchoOverlayController? = null

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
                _ready.value = false
                if (service == null) return@collect
                val triggers = sl.interventionTriggerSource
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
                ).also { it.start() }
                _ready.value = true
                Log.i(TAG, "Ozvěna připravena (fakes=${sl.useFakes})")
            }
        }
    }
}
