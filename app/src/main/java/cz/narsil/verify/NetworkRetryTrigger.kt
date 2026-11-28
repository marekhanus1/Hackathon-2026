package cz.narsil.verify

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * „Znovu při další síti“ (SPEC 7): po připojení k internetu zkusí úkoly ve stavu VERIFY_LATER.
 * [start] při startu aplikace (ServiceLocator, 124), [stop] při ukončení. Nic nehází.
 */
class NetworkRetryTrigger(
    context: Context,
    private val service: TaskVerificationService,
    private val scope: CoroutineScope,
) {
    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun start() {
        if (callback != null) return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch { runCatching { service.retryPending() } }
            }
        }
        runCatching { cm?.registerDefaultNetworkCallback(cb) }.onSuccess { callback = cb }
    }

    fun stop() {
        callback?.let { cb -> runCatching { cm?.unregisterNetworkCallback(cb) } }
        callback = null
    }
}
