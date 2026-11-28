package cz.narsil.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PermissionType {
    ACCESSIBILITY,
    OVERLAY,
    LOCATION_FOR_WIFI,
    CAMERA,
    HEALTH_CONNECT
}

data class PermissionItem(
    val type: PermissionType,
    val title: String,
    val oneSentenceReason: String,
    val isGranted: Boolean
)

data class OnboardingUiState(
    /** Aktuální Wi-Fi; null = žádná, nebo chybí poloha (Android název bez ní neprozradí). */
    val detectedWifiSsid: String? = "Domov_5G",
    val homeWifiSsid: String? = null,
    val permissions: List<PermissionItem> = defaultPermissions(),
    val isMiuiDevice: Boolean = true,
    val miuiStepsExpanded: Boolean = true,
    val miuiAutoStartDone: Boolean = false,
    val miuiBatteryDone: Boolean = false,
    val miuiBackgroundPopupDone: Boolean = false
) {
    val allRequiredGranted: Boolean
        get() = homeWifiSsid != null && permissions.all { it.isGranted }

    /** Telefon je teď připojený k uložené domácí Wi-Fi. */
    val isOnHomeWifi: Boolean
        get() = homeWifiSsid != null && detectedWifiSsid == homeWifiSsid

    /**
     * Text tlačítka domácí Wi-Fi (138). Bez Wi-Fi nebo bez polohy tlačítko požádá o polohu,
     * proto nesmí slibovat „Aktualizovat na žádná Wi-Fi“.
     */
    val homeWifiButtonLabel: String
        get() = when {
            detectedWifiSsid == null -> "Najít moji Wi-Fi (povolit polohu)"
            homeWifiSsid == null -> "Tohle je můj domov: $detectedWifiSsid"
            isOnHomeWifi -> "Domov uložen: $homeWifiSsid"
            else -> "Uložit jako domov: $detectedWifiSsid"
        }

    /** Řádek nad tlačítkem. */
    val homeWifiStatusText: String
        get() = when {
            homeWifiSsid != null && detectedWifiSsid != null && !isOnHomeWifi ->
                "Domov: $homeWifiSsid · teď jsi na: $detectedWifiSsid"
            homeWifiSsid != null -> "Domov: $homeWifiSsid"
            detectedWifiSsid == null -> "Nevidím žádnou Wi-Fi. Připoj se k domácí síti, nebo povol polohu."
            else -> "Teď jsi připojený k: $detectedWifiSsid"
        }
}

private fun defaultPermissions(): List<PermissionItem> = listOf(
    PermissionItem(
        type = PermissionType.ACCESSIBILITY,
        title = "Usnadnění (Accessibility)",
        oneSentenceReason = "Pozná nekonečný feed od zpráv, ale zprávy a chaty nikdy nečte.",
        isGranted = true
    ),
    PermissionItem(
        type = PermissionType.OVERLAY,
        title = "Zobrazování přes jiné aplikace",
        oneSentenceReason = "Umožní ukázat tvoje video Ozvěny přímo přes feed v okamžiku pokušení.",
        isGranted = false
    ),
    PermissionItem(
        type = PermissionType.LOCATION_FOR_WIFI,
        title = "Poloha (jen název Wi-Fi)",
        oneSentenceReason = "Android vyžaduje toto oprávnění jen proto, aby poznal domácí Wi-Fi.",
        isGranted = true
    ),
    PermissionItem(
        type = PermissionType.CAMERA,
        title = "Kamera",
        oneSentenceReason = "Slouží pouze k večernímu videu a počítání dřepů přímo v telefonu.",
        isGranted = false
    ),
    PermissionItem(
        type = PermissionType.HEALTH_CONNECT,
        title = "Health Connect / Kroky",
        oneSentenceReason = "Načte dnešní kroky jako měnu pro odemčení odpočinku.",
        isGranted = false
    )
)

class OnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun confirmHomeWifi() {
        _uiState.update { it.copy(homeWifiSsid = it.detectedWifiSsid) }
    }

    fun togglePermissionMock(type: PermissionType) {
        _uiState.update { current ->
            current.copy(
                permissions = current.permissions.map {
                    if (it.type == type) it.copy(isGranted = !it.isGranted) else it
                }
            )
        }
    }

    fun toggleMiuiExpanded() {
        _uiState.update { it.copy(miuiStepsExpanded = !it.miuiStepsExpanded) }
    }

    fun toggleMiuiAutoStart() {
        _uiState.update { it.copy(miuiAutoStartDone = !it.miuiAutoStartDone) }
    }

    fun toggleMiuiBattery() {
        _uiState.update { it.copy(miuiBatteryDone = !it.miuiBatteryDone) }
    }

    fun toggleMiuiBackgroundPopup() {
        _uiState.update { it.copy(miuiBackgroundPopupDone = !it.miuiBackgroundPopupDone) }
    }
}
