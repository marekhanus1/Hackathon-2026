package cz.narsil.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.core.fake.FakeEchoVideoRepository
import cz.narsil.core.fake.FakeGamificationEngine
import cz.narsil.core.fake.FakeInterventionTriggerSource
import cz.narsil.core.fake.FakeMovementRepository
import cz.narsil.core.fake.FakeSquatCounter
import cz.narsil.detection.accessibility.NarsilAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ladění 113/114 bez detekce: `adb shell am broadcast -a cz.narsil.DEBUG_ECHO -p cz.narsil`
 * ukáže Ozvěnu s fake daty (TikTok, 6 min). Volby se zpracují ([EchoOutcomeHandler]), dřepy přibývají
 * samy (fake, 1 za 0,6 s). Výsledek v logu `adb logcat -s NarsilEcho NarsilOverlay`.
 * Okno: přes běžící službu přístupnosti, jinak „zobrazení přes ostatní aplikace“. Jen debug; napojení dělá 124.
 */
class OverlayDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        lastWindow?.dismiss()
        val window = EchoOverlayWindow(NarsilAccessibilityService.instance ?: app).also { lastWindow = it }
        val controller = EchoOverlayController(
            window = window,
            triggers = FakeInterventionTriggerSource(),
            movement = movement,
            contracts = FakeContractRepository(),
            videos = FakeEchoVideoRepository(),
            scope = scope,
            choiceHandler = EchoOutcomeHandler(movement, gamification, timer, goHome = { HomeNavigator.goHome(app) }),
            squatCounter = FakeSquatCounter(scope, intervalMillis = 600),
            gamification = gamification,
        )
        scope.launch {
            try {
                controller.show(FakeInterventionTriggerSource.demoTrigger())
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val movement = FakeMovementRepository()
        val gamification = FakeGamificationEngine()
        val timer = UnlockTimer()
        var lastWindow: EchoOverlayWindow? = null
    }
}
