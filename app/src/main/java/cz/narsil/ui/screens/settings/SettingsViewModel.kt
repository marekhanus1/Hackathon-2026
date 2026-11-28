package cz.narsil.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.narsil.core.ComfortContext
import cz.narsil.core.ContextRepository
import cz.narsil.core.LlmProvider
import cz.narsil.core.SettingsRepository
import cz.narsil.core.StravaRepository
import cz.narsil.core.fake.FakeContextRepository
import cz.narsil.core.fake.FakeSettingsRepository
import cz.narsil.core.fake.FakeStravaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for [SettingsScreen]. Uses only core repository interfaces (fake defaults for previews).
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository = FakeSettingsRepository(),
    private val contextRepository: ContextRepository = FakeContextRepository(),
    private val stravaRepository: StravaRepository = FakeStravaRepository(),
    /** Podrobný stav Stravy (nenastaveno, demo, hláška); null = jen připojeno/nepřipojeno z [stravaRepository]. */
    stravaStatus: Flow<StravaCardState>? = null,
) : ViewModel() {

    private val stravaCard: Flow<StravaCardState> =
        stravaStatus ?: stravaRepository.isConnected.map { StravaCardState(connected = it) }

    private val permissionsState = MutableStateFlow(defaultSettingsPermissions())
    private val feedbackState = MutableStateFlow<String?>(null)

    private val contextCombined = combine(
        contextRepository.context,
        contextRepository.manualOverride,
        contextRepository.currentWifiSsid
    ) { context, override, ssid ->
        Triple(context, override, ssid)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        contextCombined,
        stravaCard,
        permissionsState,
        feedbackState
    ) { settings, (context, override, ssid), strava, permissions, feedback ->
        val manualChoice = when (override) {
            ComfortContext.HOME -> ManualZoneChoice.HOME
            ComfortContext.AWAY -> ManualZoneChoice.AWAY
            ComfortContext.UNKNOWN, null -> ManualZoneChoice.AUTO
        }
        SettingsUiState(
            currentWifiSsid = ssid,
            homeWifiSsid = settings.homeSsid,
            comfortContext = context,
            manualZone = manualChoice,
            llmProvider = settings.llmProvider,
            strava = strava,
            permissions = permissions,
            homeSaveFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun saveCurrentWifiAsHome() {
        viewModelScope.launch {
            val saved = contextRepository.saveCurrentWifiAsHome()
            feedbackState.value = if (saved) {
                "Domácí Wi-Fi uložena."
            } else {
                "Připoj se nejdřív k domácí Wi-Fi a povol polohu."
            }
        }
    }

    fun setManualZone(choice: ManualZoneChoice) {
        viewModelScope.launch {
            val target = when (choice) {
                ManualZoneChoice.AUTO -> null
                ManualZoneChoice.HOME -> ComfortContext.HOME
                ManualZoneChoice.AWAY -> ComfortContext.AWAY
            }
            contextRepository.setManualOverride(target)
        }
    }

    fun selectLlmProvider(provider: LlmProvider) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(llmProvider = provider) }
        }
    }

    fun updatePermissionStatuses(
        accessibility: Boolean,
        overlay: Boolean,
        location: Boolean
    ) {
        permissionsState.value = defaultSettingsPermissions(
            accessibilityGranted = accessibility,
            overlayGranted = overlay,
            locationGranted = location
        )
    }
}
