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

    /** Poslední hodnota z callbacku s FLAG_INCLUDE_LOCATION_INFO (jediný zdroj nezačerněného SSID od Androidu 12). */
    @Volatile
    private var callbackWifi: WifiReading? = null

    /** Wi-Fi sítě, které callback hlásí jako připojené (onLost je odebere). */
    private val wifiNetworks = java.util.concurrent.ConcurrentHashMap.newKeySet<Network>()
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
        val ssid = (_wifi.value as? WifiReading.Connected)?.ssid
            ?: (callbackWifi as? WifiReading.Connected)?.ssid
            ?: return false
        settings.update { it.copy(homeSsid = ssid) }
        return true
    }

    /**
     * Znovu přečte Wi-Fi (např. po udělení oprávnění k poloze). getNetworkCapabilities() bez
     * FLAG_INCLUDE_LOCATION_INFO vrací od Androidu 12 začerněné SSID, proto „Unknown“ nepřepíše
     * známou síť z callbacku.
     */
    fun refresh() {
        val known = callbackWifi
        // Dokud callback drží připojenou Wi-Fi, je jeho stav pravdivý: activeNetwork může být mobilní
        // (Wi-Fi bez internetu) a WifiManager na Androidu 16 vrací networkId -1 → falešné NotOnWifi.
        if (wifiNetworks.isNotEmpty() && known != null) {
            val reading = wifiNetworks.firstNotNullOfOrNull { connectivity.getNetworkCapabilities(it) }?.let(::readingFrom)
            _wifi.value = if (reading is WifiReading.Connected) reading else known
            return
        }
        val caps = connectivity.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
        _wifi.value = readingFrom(caps)
    }

    private fun onCallback(network: Network, caps: NetworkCapabilities) {
        wifiNetworks += network
        val reading = readingFrom(caps)
        // Začerněné SSID nepřepíše už známou síť.
        if (reading == WifiReading.Unknown && callbackWifi is WifiReading.Connected) return
        callbackWifi = reading
        _wifi.value = reading
    }

    private fun onWifiLost(network: Network) {
        wifiNetworks -= network
        if (wifiNetworks.isEmpty()) {
            callbackWifi = null
            _wifi.value = WifiReading.NotOnWifi
        }
    }

    private fun registerCallback() {
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = onCallback(network, caps)
                override fun onLost(network: Network) = onWifiLost(network)
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = onCallback(network, caps)
                override fun onLost(network: Network) = onWifiLost(network)
            }
        }
        runCatching { connectivity.registerNetworkCallback(request, callback) }
            .onFailure { Log.w(TAG, "registerNetworkCallback selhal", it) }
        refresh()
    }

    @Suppress("DEPRECATION")
    private fun readingFrom(caps: NetworkCapabilities?): WifiReading {
        val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true || wifiNetworks.isNotEmpty()
        if (!onWifi) {
            // Aktivní síť může být mobilní, i když je Wi-Fi připojená (bez internetu) – rozhoduje WifiManager.
            if (wifiManager?.connectionInfo?.networkId?.let { it != -1 } != true) return WifiReading.NotOnWifi
        }
        if (!hasLocationPermission()) return WifiReading.Unknown
        // transportInfo bývá bez FLAG_INCLUDE_LOCATION_INFO začerněné ("<unknown ssid>") → zkusit WifiManager.
        val fromCaps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (caps?.transportInfo as? WifiInfo)?.ssid else null
        val ssid = ContextResolver.normalize(fromCaps) ?: ContextResolver.normalize(wifiManager?.connectionInfo?.ssid)
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
