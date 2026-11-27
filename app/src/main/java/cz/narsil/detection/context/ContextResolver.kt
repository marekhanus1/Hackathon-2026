package cz.narsil.detection.context

import cz.narsil.core.ComfortContext

/** Co víme o Wi-Fi. */
sealed interface WifiReading {
    /** Připojeno k síti [ssid] (bez uvozovek). */
    data class Connected(val ssid: String) : WifiReading

    /** Žádná Wi-Fi (mobilní data, offline). */
    data object NotOnWifi : WifiReading

    /** Na Wi-Fi, ale název nejde zjistit (chybí oprávnění k poloze nebo je vypnutá poloha). */
    data object Unknown : WifiReading
}

/**
 * Komfortní zóna (SPEC 2), čistý Kotlin:
 * ruční přepínač → HOME/AWAY; jinak domácí Wi-Fi → HOME, jiná / žádná → AWAY
 * (i když domov ještě není uložený); neznámý název sítě (oprávnění, poloha) → UNKNOWN.
 * AWAY i UNKNOWN znamenají nezasahovat.
 */
object ContextResolver {
    fun resolve(manual: ComfortContext?, wifi: WifiReading, homeSsid: String?): ComfortContext {
        if (manual != null) return manual
        val home = normalize(homeSsid)
        return when (wifi) {
            is WifiReading.Connected -> if (home != null && normalize(wifi.ssid) == home) ComfortContext.HOME else ComfortContext.AWAY
            WifiReading.NotOnWifi -> ComfortContext.AWAY
            WifiReading.Unknown -> ComfortContext.UNKNOWN
        }
    }

    /** Android vrací SSID v uvozovkách a bez oprávnění jako "<unknown ssid>". */
    fun normalize(ssid: String?): String? {
        val s = ssid?.trim()?.removeSurrounding("\"")?.trim()
        return s?.takeIf { it.isNotEmpty() && it != UNKNOWN_SSID && it != "0x" }
    }

    const val UNKNOWN_SSID = "<unknown ssid>"
}
