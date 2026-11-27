package cz.narsil.app

import android.util.Log
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.fake.FakeInterventionTriggerSource
import cz.narsil.detection.accessibility.AccessibilityEvents
import cz.narsil.detection.accessibility.AccessibilityTriggerSource
import cz.narsil.detection.accessibility.NarsilAccessibilityService
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

    /** Ozvěna připravená (běží služba přístupnosti). */
    val isReady: Boolean get() = controller != null

    /** Demo panel (123): ukáže Ozvěnu hned, bez čekání na detekci. false = služba neběží. */
    suspend fun showNow(trigger: InterventionTrigger = FakeInterventionTriggerSource.demoTrigger()): Boolean {
        val c = controller ?: return false
        c.show(trigger)
        return true
    }

    fun start() {
        val sl = ServiceLocator
        sl.appScope.launch {
            AccessibilityEvents.serviceConnected.collect { connected ->
                controller?.stop()
                controller = null
                val service = NarsilAccessibilityService.instance
                if (!connected || service == null) return@collect
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
                Log.i(TAG, "Ozvěna připravena (fakes=${sl.useFakes})")
            }
        }
    }
}
