package cz.narsil.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cz.narsil.core.ComfortContext
import cz.narsil.detection.context.WifiContextRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ladění kontextu (112) nad instancemi z [ServiceLocator], log `adb logcat -s NarsilContext`:
 * - `adb shell am broadcast -a cz.narsil.DEBUG_CONTEXT -p cz.narsil` vypíše Wi-Fi a kontext,
 * - `--es cmd save_home` uloží aktuální Wi-Fi jako domov,
 * - `--es cmd home|away|auto` nastaví ruční přepínač (platí hned i pro detekci),
 * - `--es cmd demo --ei sec 20` nastaví demo práh zásahu (0 = výchozích 5 min).
 */
class ContextDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val sl = ServiceLocator
        val repo = sl.contextRepository
        val wifiRepo = repo as? WifiContextRepository
        sl.appScope.launch {
            try {
                when (intent.getStringExtra("cmd")) {
                    "save_home" -> Log.i(TAG, "uložit domov: ${repo.saveCurrentWifiAsHome()}")
                    "home" -> repo.setManualOverride(ComfortContext.HOME)
                    "away" -> repo.setManualOverride(ComfortContext.AWAY)
                    "auto" -> repo.setManualOverride(null)
                    "demo" -> {
                        val sec = intent.getIntExtra("sec", 20).takeIf { it > 0 }
                        sl.settingsRepository.update { it.copy(demoMode = sec != null, demoThresholdSec = sec) }
                        Log.i(TAG, "demo práh: ${sec ?: "výchozí"} s")
                    }
                }
                wifiRepo?.refresh()
                delay(500)
                Log.i(
                    TAG,
                    "wifi=${wifiRepo?.wifi?.value} ruční=${repo.manualOverride.value} kontext=${repo.context.value} " +
                        "domov=${sl.settingsRepository.settings.value.homeSsid} fakes=${sl.useFakes}",
                )
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "NarsilContext"
    }
}
