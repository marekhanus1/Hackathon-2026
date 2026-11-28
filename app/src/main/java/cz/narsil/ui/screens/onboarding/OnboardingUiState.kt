package cz.narsil.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PermissionType {
    ACCESSIBILITY,
    LOCATION_FOR_WIFI,
    CAMERA,
    HEALTH_CONNECT
}

data class PermissionItem(
    val type: PermissionType,
    val title: String,
    val oneSentenceReason: String,
    val isGranted: Boolean,
    /** Nepovinné kroky průvodce nedrží; aplikace se na ně zeptá až při prvním použití. */
    val required: Boolean = true,
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
    val miuiBackgroundPopupDone: Boolean = false,
    /** MIUI: Narsil zamčený v posledních aplikacích (jinak ho „Vyčistit vše“ zabije i se službou). */
    val miuiLockDone: Boolean = false,
    /** Video pro Ozvěnu z onboardingu (SPEC 9, záloha než vznikne video ze smlouvy) – 139. */
    val video: OnboardingVideoState = OnboardingVideoState(),
) {
    val allRequiredGranted: Boolean
        get() = homeWifiSsid != null && permissions.filter { it.required }.all { it.isGranted }

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

/**
 * Karta „Video pro Ozvěnu“ (139).
 *
 * @param cameraOpen náhled kamery je rozbalený (kamera se zapne až na požádání)
 * @param hasVideo video je uložené (přehraje ho Ozvěna, dokud nevznikne video ze smlouvy)
 */
data class OnboardingVideoState(
    val cameraOpen: Boolean = false,
    val isRecording: Boolean = false,
    val seconds: Int = 0,
    val maxSeconds: Int = 15,
    val hasVideo: Boolean = false,
    val error: String? = null,
) {
    val buttonLabel: String
        get() = when {
            isRecording -> "Ukončit nahrávání ($seconds/$maxSeconds s)"
            !cameraOpen && hasVideo -> "Natočit znovu"
            !cameraOpen -> "Natočit video"
            hasVideo -> "Natočit znovu"
            else -> "Začít nahrávat"
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
        type = PermissionType.LOCATION_FOR_WIFI,
        title = "Poloha (jen název Wi-Fi)",
        oneSentenceReason = "Android vyžaduje toto oprávnění jen proto, aby poznal domácí Wi-Fi.",
        isGranted = true
    ),
    PermissionItem(
        type = PermissionType.CAMERA,
        title = "Kamera (nepovinné)",
        oneSentenceReason = "Na dřepy a večerní video. Zeptáme se i sami, až ji poprvé použiješ.",
        isGranted = false,
        required = false,
    ),
    PermissionItem(
        type = PermissionType.HEALTH_CONNECT,
        title = "Kroky (nepovinné)",
        oneSentenceReason = "Načte dnešní kroky jako měnu pro odemčení odpočinku.",
        isGranted = false,
        required = false,
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

    fun toggleMiuiLock() {
        _uiState.update { it.copy(miuiLockDone = !it.miuiLockDone) }
    }

    fun toggleMiuiBackgroundPopup() {
        _uiState.update { it.copy(miuiBackgroundPopupDone = !it.miuiBackgroundPopupDone) }
    }
}
