package cz.narsil.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.narsil.core.ComfortContext
import cz.narsil.core.ContextRepository
import cz.narsil.core.Exercise
import cz.narsil.core.LlmProvider
import cz.narsil.core.Settings
import cz.narsil.core.SettingsRepository
import cz.narsil.core.StravaRepository
import cz.narsil.core.TrackedApp
import cz.narsil.core.data.DayKey
import cz.narsil.core.fake.FakeContextRepository
import cz.narsil.core.fake.FakeSettingsRepository
import cz.narsil.core.fake.FakeStravaRepository
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
 *
 * 133: všechny parametry chování v sekcích. Zpřísnění se uloží hned; uvolnění (viz [SettingsGuard])
 * čeká v [SettingsUiState.pendingChange] na potvrzení po odpočtu [SettingsGuard.CONFIRM_DELAY_SECONDS].
 *
 * @param llmKeys má poskytovatel AI klíč (z BuildConfig, dodá route)
 * @param onProtectionPaused po potvrzeném pozastavení ochrany (zpráva Parťákovi, když je zapnutá)
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository = FakeSettingsRepository(),
    private val contextRepository: ContextRepository = FakeContextRepository(),
    private val stravaRepository: StravaRepository = FakeStravaRepository(),
    /** Podrobný stav Stravy (nenastaveno, demo, hláška); null = jen připojeno/nepřipojeno z [stravaRepository]. */
    stravaStatus: Flow<StravaCardState>? = null,
    private val llmKeys: Map<LlmProvider, Boolean> = LlmProvider.entries.associateWith { true },
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val onProtectionPaused: () -> Unit = {},
) : ViewModel() {

    private val stravaCard: Flow<StravaCardState> =
        stravaStatus ?: stravaRepository.isConnected.map { StravaCardState(connected = it) }

    private val permissionsState = MutableStateFlow(defaultSettingsPermissions())
    private val feedbackState = MutableStateFlow<String?>(null)
    private val pendingState = MutableStateFlow<PendingChange?>(null)
    private var pendingAction: (suspend () -> Unit)? = null
    private var countdown: Job? = null

    private val contextCombined = combine(
        contextRepository.context,
        contextRepository.manualOverride,
        contextRepository.currentWifiSsid
    ) { context, override, ssid ->
        Triple(context, override, ssid)
    }

    private val localState = combine(permissionsState, feedbackState, pendingState) { permissions, feedback, pending ->
        Triple(permissions, feedback, pending)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        contextCombined,
        stravaCard,
        localState,
    ) { settings, (context, override, ssid), strava, (permissions, feedback, pending) ->
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
            homeSaveFeedback = feedback,
            settings = settings,
            protectionPausedText = if (settings.isProtectionActive(clock())) null else "do 4:00 ráno",
            llmKeys = llmKeys,
            pendingChange = pending,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(settings = settingsRepository.settings.value, llmKeys = llmKeys)
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

    /** „Venku“ vypne Ozvěnu do 4:00, proto jde přes potvrzení; „Doma“ a „Automaticky“ hned. */
    fun setManualZone(choice: ManualZoneChoice) {
        val target = when (choice) {
            ManualZoneChoice.AUTO -> null
            ManualZoneChoice.HOME -> ComfortContext.HOME
            ManualZoneChoice.AWAY -> ComfortContext.AWAY
        }
        val action: suspend () -> Unit = { contextRepository.setManualOverride(target) }
        if (choice == ManualZoneChoice.AWAY && contextRepository.manualOverride.value != ComfortContext.AWAY) {
            ask("Přepnout na „Venku“?", listOf("Ozvěna do 4:00 nezasáhne, ani když budeš doma."), action)
        } else {
            viewModelScope.launch { action() }
        }
    }

    fun selectLlmProvider(provider: LlmProvider) = change("Změnit ověření úkolů") { it.copy(llmProvider = provider) }

    fun setExercise(exercise: Exercise) = change("Změnit cvik") { it.copy(exercise = exercise) }

    fun setToleranceMinutes(minutes: Int) = change("Prodloužit toleranci?") { it.copy(toleranceMinutes = minutes) }

    fun setVideoChainLimit(count: Int) = change("Povolit víc videí po sobě?") { it.copy(videoChainLimit = count) }

    fun setSquatsPerUnlock(count: Int) = change("Snížit počet dřepů?") { it.copy(squatsPerUnlock = count) }

    fun setPushupsPerUnlock(count: Int) = change("Snížit počet kliků?") { it.copy(pushupsPerUnlock = count) }

    fun setDebtLimitMinutes(minutes: Int) = change("Zvýšit limit dluhu?") { it.copy(debtLimitMinutes = minutes) }

    fun setAppTracked(app: TrackedApp, tracked: Boolean) = change("Přestat sledovat aplikaci?") {
        it.copy(trackedApps = if (tracked) it.trackedApps + app else it.trackedApps - app)
    }

    /** Další aplikace s nekonečným feedem (149): zapnutí je zpřísnění, vypnutí uvolnění s potvrzením. */
    fun setTrackOtherFeedApps(enabled: Boolean) = change("Přestat hlídat další aplikace?") { it.copy(trackOtherFeedApps = enabled) }

    /** Ukazatel limitu při scrollování (135): jen informace, vypnutí nic neuvolňuje. */
    fun setShowScrollProgress(show: Boolean) = change("Ukazatel limitu") { it.copy(showScrollProgress = show) }

    fun setPartnerAlerts(enabled: Boolean) = change("Vypnout zprávy Parťákovi?") { it.copy(partnerAlerts = enabled) }

    /** Pozastaví ochranu do nejbližších 4:00 (konec dne Narsilu). Vždy přes potvrzení. */
    fun pauseProtectionUntilMorning() {
        val until = DayKey.startMillis(DayKey.of(clock(), zone).plusDays(1), zone)
        change("Pozastavit ochranu?", afterApply = {
            if (settingsRepository.settings.value.partnerAlerts) onProtectionPaused()
        }) { it.copy(protectionPausedUntilMillis = until) }
    }

    fun resumeProtection() = change("Obnovit ochranu") { it.copy(protectionPausedUntilMillis = null) }

    fun resetSection(section: SettingsSection) =
        change("Obnovit výchozí: ${section.title}?") { SettingsGuard.resetSection(it, section) }

    /** Potvrzení čekajícího uvolnění; před koncem odpočtu nic neudělá. */
    fun confirmPendingChange() {
        val pending = pendingState.value ?: return
        if (!pending.canConfirm) return
        val action = pendingAction
        clearPending()
        if (action != null) viewModelScope.launch { action() }
    }

    fun cancelPendingChange() = clearPending()

    fun updatePermissionStatuses(
        accessibility: Boolean,
        overlay: Boolean,
        location: Boolean,
        camera: Boolean = true,
        notifications: Boolean = true,
    ) {
        permissionsState.value = defaultSettingsPermissions(
            accessibilityGranted = accessibility,
            overlayGranted = overlay,
            locationGranted = location,
            cameraGranted = camera,
            notificationsGranted = notifications,
        )
    }

    /** Uloží změnu hned, nebo (když něco uvolňuje) až po potvrzení s odpočtem. */
    private fun change(
        title: String,
        afterApply: suspend () -> Unit = {},
        transform: (Settings) -> Settings,
    ) {
        val current = settingsRepository.settings.value
        val proposed = transform(current)
        if (proposed == current) return
        val reasons = SettingsGuard.loosenings(current, proposed)
        // Ukládá se transformace, ne hotový objekt: mezitím mohla jiná část aplikace změnit jiné pole.
        val action: suspend () -> Unit = {
            settingsRepository.update(transform)
            afterApply()
        }
        if (reasons.isEmpty()) viewModelScope.launch { action() } else ask(title, reasons, action)
    }

    private fun ask(title: String, reasons: List<String>, action: suspend () -> Unit) {
        countdown?.cancel()
        pendingAction = action
        pendingState.value = PendingChange(title, reasons, SettingsGuard.CONFIRM_DELAY_SECONDS)
        countdown = viewModelScope.launch {
            while ((pendingState.value?.secondsLeft ?: 0) > 0) {
                delay(1_000)
                pendingState.value = pendingState.value?.let { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
        }
    }

    private fun clearPending() {
        countdown?.cancel()
        countdown = null
        pendingAction = null
        pendingState.value = null
    }
}
