package cz.narsil.verify

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * „Znovu při další síti“ (SPEC 7): jakmile má síť ověřený internet (VALIDATED), zkusí úkoly ve stavu
 * VERIFY_LATER. `onAvailable` nestačí – přijde dřív, než Android síť ověří, a pokus by skončil zase LATER.
 * [start] při startu aplikace (ServiceLocator, 124), [stop] při ukončení. Nic nehází.
 */
class NetworkRetryTrigger(
    context: Context,
    private val service: TaskVerificationService,
    private val scope: CoroutineScope,
) {
    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private var callback: ConnectivityManager.NetworkCallback? = null
    private val validated = mutableSetOf<Network>()

    fun start() {
        if (callback != null) return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val ok = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                // Jen při přechodu na ověřený internet (callback chodí opakovaně při každé změně signálu).
                val becameValidated = synchronized(validated) { if (ok) validated.add(network) else { validated.remove(network); false } }
                if (becameValidated) scope.launch { runCatching { service.retryPending() } }
            }

            override fun onLost(network: Network) {
                synchronized(validated) { validated.remove(network) }
            }
        }
        runCatching { cm?.registerDefaultNetworkCallback(cb) }.onSuccess { callback = cb }
    }

    fun stop() {
        callback?.let { cb -> runCatching { cm?.unregisterNetworkCallback(cb) } }
        callback = null
    }
}
