package cz.narsil.detection.context

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import cz.narsil.core.ComfortContext
import cz.narsil.core.ContextRepository
import cz.narsil.core.SettingsRepository
import cz.narsil.core.data.DayKey
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Komfortní zóna přes domácí Wi-Fi (112). Domov = `Settings.homeSsid` (uloží [saveCurrentAsHome]).
 * Ruční přepínač platí do konce dne (4:00, [DayKey]) a přežije restart (SharedPreferences).
 * Od Androidu 10 je název sítě jen s oprávněním k poloze a zapnutou polohou, jinak UNKNOWN.
 */
class WifiContextRepository(
    context: Context,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val today: () -> LocalDate = { DayKey.today() },
) : ContextRepository {
    private val app = context.applicationContext
    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    private val wifiManager = app.getSystemService(WifiManager::class.java)
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _wifi = MutableStateFlow<WifiReading>(WifiReading.NotOnWifi)
    private val _override = MutableStateFlow(loadOverride())
    private val _context = MutableStateFlow(ComfortContext.UNKNOWN)

    /** Aktuální Wi-Fi (pro onboarding: „tohle je můj domov“). */
    val wifi: StateFlow<WifiReading> = _wifi.asStateFlow()
    override val context: StateFlow<ComfortContext> = _context.asStateFlow()
    override val manualOverride: StateFlow<ComfortContext?> = _override.asStateFlow()

    init {
        registerCallback()
        scope.launch {
            combine(_override, _wifi, settings.settings.map { it.homeSsid }.distinctUntilChanged()) { m, w, h ->
                ContextResolver.resolve(m, w, h)
            }.distinctUntilChanged().collect {
                Log.i(TAG, "kontext: $it (wifi=${_wifi.value}, ruční=${_override.value})")
                _context.value = it
            }
        }
        // Ruční přepínač vyprší ve 4:00.
        scope.launch {
            while (isActive) {
                delay(60_000)
                val loaded = loadOverride()
                if (loaded != _override.value) _override.value = loaded
            }
        }
    }

    override suspend fun setManualOverride(context: ComfortContext?) {
        prefs.edit().apply {
            if (context == null) remove(KEY_OVERRIDE).remove(KEY_OVERRIDE_DAY)
            else putString(KEY_OVERRIDE, context.name).putString(KEY_OVERRIDE_DAY, today().toString())
        }.apply()
        _override.value = context
    }

    /** Uloží aktuální Wi-Fi jako domov. false = nejsme na Wi-Fi nebo neznáme název (oprávnění). */
    suspend fun saveCurrentAsHome(): Boolean {
        refresh()
        val ssid = (_wifi.value as? WifiReading.Connected)?.ssid ?: return false
        settings.update { it.copy(homeSsid = ssid) }
        return true
    }

    /** Znovu přečte Wi-Fi (např. po udělení oprávnění k poloze). */
    fun refresh() {
        val caps = connectivity.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
        _wifi.value = readingFrom(caps)
    }

    private fun registerCallback() {
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _wifi.value = readingFrom(caps)
                }
                override fun onLost(network: Network) = refresh()
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _wifi.value = readingFrom(caps)
                }
                override fun onLost(network: Network) = refresh()
            }
        }
        runCatching { connectivity.registerNetworkCallback(request, callback) }
            .onFailure { Log.w(TAG, "registerNetworkCallback selhal", it) }
        refresh()
    }

    @Suppress("DEPRECATION")
    private fun readingFrom(caps: NetworkCapabilities?): WifiReading {
        if (caps == null || !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            // Aktivní síť může být mobilní, i když je Wi-Fi připojená (bez internetu) – rozhoduje WifiManager.
            if (wifiManager?.connectionInfo?.networkId?.let { it != -1 } != true) return WifiReading.NotOnWifi
        }
        if (!hasLocationPermission()) return WifiReading.Unknown
        val info: WifiInfo? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) caps?.transportInfo as? WifiInfo ?: wifiManager?.connectionInfo
            else wifiManager?.connectionInfo
        val ssid = ContextResolver.normalize(info?.ssid)
        return if (ssid != null) WifiReading.Connected(ssid) else WifiReading.Unknown
    }

    private fun hasLocationPermission(): Boolean =
        app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun loadOverride(): ComfortContext? {
        val day = prefs.getString(KEY_OVERRIDE_DAY, null) ?: return null
        if (day != today().toString()) return null
        return ComfortContext.entries.firstOrNull { it.name == prefs.getString(KEY_OVERRIDE, null) }
    }

    private companion object {
        const val TAG = "NarsilContext"
        const val PREFS = "context"
        const val KEY_OVERRIDE = "manual_override"
        const val KEY_OVERRIDE_DAY = "manual_override_day"
    }
}
