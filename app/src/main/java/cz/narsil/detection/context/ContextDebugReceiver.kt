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
 * - `--es cmd home|away|auto` nastaví ruční přepínač.
 */
class ContextDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val repo = repo ?: WifiContextRepository(context, DataStoreSettingsRepository(context), scope).also { repo = it }
        scope.launch {
            try {
                when (intent.getStringExtra("cmd")) {
                    "save_home" -> Log.i(TAG, "uložit domov: ${repo.saveCurrentAsHome()}")
                    "home" -> repo.setManualOverride(ComfortContext.HOME)
                    "away" -> repo.setManualOverride(ComfortContext.AWAY)
                    "auto" -> repo.setManualOverride(null)
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
