package cz.narsil.ui.screens.settings

import cz.narsil.core.ComfortContext
import cz.narsil.core.LlmProvider

enum class ManualZoneChoice(val label: String) {
    AUTO("Automaticky"),
    HOME("Doma"),
    AWAY("Venku")
}

enum class SettingsPermissionType {
    ACCESSIBILITY,
    OVERLAY,
    LOCATION
}

data class SettingsPermissionItem(
    val type: SettingsPermissionType,
    val title: String,
    val reason: String,
    val isGranted: Boolean
)

/**
 * Karta Stravy (132).
 *
 * @param configured aplikace má klíče Stravy; false = „Strava není nastavená“ + nabídka dema
 * @param demo připojeno jen ukázkově (mock aktivita), v UI vždy označené „Demo“
 * @param message hláška o posledním neúspěchu (zrušeno, chybí prohlížeč…), null = nic
 */
data class StravaCardState(
    val configured: Boolean = true,
    val connected: Boolean = false,
    val demo: Boolean = false,
    val message: String? = null,
)

data class SettingsUiState(
    val currentWifiSsid: String? = "Domov_5G",
    val homeWifiSsid: String? = "Domov_5G",
    val comfortContext: ComfortContext = ComfortContext.HOME,
    val manualZone: ManualZoneChoice = ManualZoneChoice.AUTO,
    val llmProvider: LlmProvider = LlmProvider.CLAUDE,
    val strava: StravaCardState = StravaCardState(),
    val permissions: List<SettingsPermissionItem> = defaultSettingsPermissions(),
    val homeSaveFeedback: String? = null,
    val appVersion: String = "Narsil 1.0 · Hackathon 2026"
)

internal fun defaultSettingsPermissions(
    accessibilityGranted: Boolean = true,
    overlayGranted: Boolean = true,
    locationGranted: Boolean = true
): List<SettingsPermissionItem> = listOf(
    SettingsPermissionItem(
        type = SettingsPermissionType.ACCESSIBILITY,
        title = "Služba přístupnosti",
        reason = "Rozpozná nekonečný feed od zpráv.",
        isGranted = accessibilityGranted
    ),
    SettingsPermissionItem(
        type = SettingsPermissionType.OVERLAY,
        title = "Překrytí přes aplikace",
        reason = "Ukáže Ozvěnu přímo přes scrollovaný feed.",
        isGranted = overlayGranted
    ),
    SettingsPermissionItem(
        type = SettingsPermissionType.LOCATION,
        title = "Poloha (jen název Wi-Fi)",
        reason = "Systém ji vyžaduje pro zjištění domácí Wi-Fi.",
        isGranted = locationGranted
    )
)
