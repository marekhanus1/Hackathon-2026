package cz.narsil.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cz.narsil.core.fake.FakeContractRepository
import cz.narsil.core.fake.FakeEchoVideoRepository
import cz.narsil.core.fake.FakeInterventionTriggerSource
import cz.narsil.core.fake.FakeMovementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ladění 113 bez detekce: `adb shell am broadcast -a cz.narsil.DEBUG_ECHO -p cz.narsil`
 * ukáže Ozvěnu s fake daty (TikTok, 6 min). Potřebuje povolené „zobrazení přes ostatní aplikace“.
 * Jen pro debug; skutečné napojení (detekce → overlay) dělá 124.
 */
class OverlayDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val controller = EchoOverlayController(
            window = window ?: EchoOverlayWindow(context.applicationContext).also { window = it },
            triggers = FakeInterventionTriggerSource(),
            movement = FakeMovementRepository(),
            contracts = FakeContractRepository(),
            videos = FakeEchoVideoRepository(),
            scope = scope,
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
        var window: EchoOverlayWindow? = null
    }
}
