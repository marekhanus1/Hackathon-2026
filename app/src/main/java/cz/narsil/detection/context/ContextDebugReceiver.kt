package cz.narsil.detection.context

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cz.narsil.core.ComfortContext
import cz.narsil.core.data.DataStoreSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ladění 112 (než to napojí 124), log `adb logcat -s NarsilContext`:
 * - `adb shell am broadcast -a cz.narsil.DEBUG_CONTEXT -p cz.narsil` vypíše Wi-Fi a kontext,
 * - `--es cmd save_home` uloží aktuální Wi-Fi jako domov,
 * - `--es cmd home|away|auto` nastaví ruční přepínač,
 * - `--es cmd demo --ei sec 20` nastaví demo práh zásahu (0 = výchozích 5 min).
 */
class ContextDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val settings = DataStoreSettingsRepository(context, scope)
        val repo = repo ?: WifiContextRepository(context, settings, scope).also { repo = it }
        scope.launch {
            try {
                when (intent.getStringExtra("cmd")) {
                    "save_home" -> Log.i(TAG, "uložit domov: ${repo.saveCurrentAsHome()}")
                    "home" -> repo.setManualOverride(ComfortContext.HOME)
                    "away" -> repo.setManualOverride(ComfortContext.AWAY)
                    "auto" -> repo.setManualOverride(null)
                    "demo" -> {
                        val sec = intent.getIntExtra("sec", 20).takeIf { it > 0 }
                        settings.update { it.copy(demoMode = sec != null, demoThresholdSec = sec) }
                        Log.i(TAG, "demo práh: ${sec ?: "výchozí"} s")
                    }
                }
                repo.refresh()
                delay(500)
                Log.i(TAG, "wifi=${repo.wifi.value} ruční=${repo.manualOverride.value} kontext=${repo.context.value}")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "NarsilContext"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var repo: WifiContextRepository? = null
    }
}
