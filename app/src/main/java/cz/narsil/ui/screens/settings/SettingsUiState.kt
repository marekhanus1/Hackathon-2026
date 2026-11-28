package cz.narsil.ui.screens.settings

import cz.narsil.core.ComfortContext
import cz.narsil.core.LlmProvider
import cz.narsil.core.Settings
import cz.narsil.core.TrackedApp

enum class ManualZoneChoice(val label: String) {
    AUTO("Automaticky"),
    HOME("Doma"),
    AWAY("Venku")
}

enum class SettingsPermissionType {
    ACCESSIBILITY,
    OVERLAY,
    LOCATION,
    CAMERA,
    NOTIFICATIONS
}

/**
 * Uvolnění čekající na potvrzení (133). Potvrdit jde až po odpočtu ([secondsLeft] = 0).
 *
 * @param reasons co se změnou uvolní, česky (z [SettingsGuard.loosenings])
 */
data class PendingChange(
    val title: String,
    val reasons: List<String>,
    val secondsLeft: Int,
) {
    val canConfirm: Boolean get() = secondsLeft <= 0
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
    val appVersion: String = "Narsil 1.0 · Hackathon 2026",
    /** Celé nastavení (133): parametry chování v sekcích. */
    val settings: Settings = Settings(),
    /** Ochrana pozastavená; text „do 4:00“ apod., null = běží. */
    val protectionPausedText: String? = null,
    /** Má poskytovatel AI klíč v `local.properties`? Bez klíče ověření čeká (VERIFY_LATER). */
    val llmKeys: Map<LlmProvider, Boolean> = LlmProvider.entries.associateWith { true },
    /** Uvolnění čekající na potvrzení s odpočtem; null = žádný dialog. */
    val pendingChange: PendingChange? = null,
    /** Nainstalované sítě s obecnou detekcí (149); null = nezjištěno → ukázat všechny. */
    val installedApps: Set<TrackedApp>? = null,
) {
    val isProtectionPaused: Boolean get() = protectionPausedText != null
}

internal fun defaultSettingsPermissions(
    accessibilityGranted: Boolean = true,
    overlayGranted: Boolean = true,
    locationGranted: Boolean = true,
    cameraGranted: Boolean = true,
    notificationsGranted: Boolean = true
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
    ),
    SettingsPermissionItem(
        type = SettingsPermissionType.CAMERA,
        title = "Kamera",
        reason = "Počítá dřepy a kliky. Obraz zůstává v telefonu.",
        isGranted = cameraGranted
    ),
    SettingsPermissionItem(
        type = SettingsPermissionType.NOTIFICATIONS,
        title = "Notifikace",
        reason = "Ukázková zpráva Parťákovi.",
        isGranted = notificationsGranted
    )
)
