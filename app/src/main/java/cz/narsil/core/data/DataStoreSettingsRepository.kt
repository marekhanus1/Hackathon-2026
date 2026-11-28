package cz.narsil.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cz.narsil.core.LlmProvider
import cz.narsil.core.Settings
import cz.narsil.core.SettingsRepository
import cz.narsil.core.TrackedApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Nastavení v DataStore (SPEC 11.4). Do prvního načtení vrací výchozí [Settings]. */
class DataStoreSettingsRepository(
    context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : SettingsRepository {
    private val store = context.applicationContext.settingsStore

    override val settings: StateFlow<Settings> = store.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, Settings())

    override suspend fun update(change: (Settings) -> Settings) {
        runCatching {
            store.edit { prefs ->
                val new = change(prefs.toSettings())
                prefs.write(new)
            }
        }
    }

    private fun Preferences.toSettings() = Settings(
        homeSsid = this[HOME_SSID],
        llmProvider = enumOr(this[LLM_PROVIDER], Settings().llmProvider),
        demoMode = this[DEMO_MODE] ?: false,
        demoThresholdSec = this[DEMO_THRESHOLD_SEC],
        demoContractToday = this[DEMO_CONTRACT_TODAY] ?: false,
        demoAlwaysYes = this[DEMO_ALWAYS_YES] ?: false,
        exercise = enumOr(this[EXERCISE], Settings().exercise),
        showScrollProgress = this[SHOW_SCROLL_PROGRESS] ?: true,
        toleranceMinutes = this[TOLERANCE_MINUTES] ?: DEFAULTS.toleranceMinutes,
        videoChainLimit = this[VIDEO_CHAIN_LIMIT] ?: DEFAULTS.videoChainLimit,
        squatsPerUnlock = this[SQUATS_PER_UNLOCK] ?: DEFAULTS.squatsPerUnlock,
        pushupsPerUnlock = this[PUSHUPS_PER_UNLOCK] ?: DEFAULTS.pushupsPerUnlock,
        debtLimitMinutes = this[DEBT_LIMIT_MINUTES] ?: DEFAULTS.debtLimitMinutes,
        trackedApps = trackedAppsFrom(this[TRACKED_APPS], this[TRACKED_APPS_KNOWN]),
        protectionPausedUntilMillis = this[PAUSED_UNTIL],
        partnerAlerts = this[PARTNER_ALERTS] ?: DEFAULTS.partnerAlerts,
        trackOtherFeedApps = this[TRACK_OTHER_FEED_APPS] ?: DEFAULTS.trackOtherFeedApps,
    )

    private fun androidx.datastore.preferences.core.MutablePreferences.write(s: Settings) {
        if (s.homeSsid != null) this[HOME_SSID] = s.homeSsid else remove(HOME_SSID)
        this[LLM_PROVIDER] = s.llmProvider.name
        this[DEMO_MODE] = s.demoMode
        if (s.demoThresholdSec != null) this[DEMO_THRESHOLD_SEC] = s.demoThresholdSec else remove(DEMO_THRESHOLD_SEC)
        this[DEMO_CONTRACT_TODAY] = s.demoContractToday
        this[DEMO_ALWAYS_YES] = s.demoAlwaysYes
        this[EXERCISE] = s.exercise.name
        this[SHOW_SCROLL_PROGRESS] = s.showScrollProgress
        this[TOLERANCE_MINUTES] = s.toleranceMinutes
        this[VIDEO_CHAIN_LIMIT] = s.videoChainLimit
        this[SQUATS_PER_UNLOCK] = s.squatsPerUnlock
        this[PUSHUPS_PER_UNLOCK] = s.pushupsPerUnlock
        this[DEBT_LIMIT_MINUTES] = s.debtLimitMinutes
        this[TRACKED_APPS] = s.trackedApps.map { it.name }.toSet()
        this[TRACKED_APPS_KNOWN] = TrackedApp.entries.map { it.name }.toSet()
        this[TRACK_OTHER_FEED_APPS] = s.trackOtherFeedApps
        if (s.protectionPausedUntilMillis != null) this[PAUSED_UNTIL] = s.protectionPausedUntilMillis else remove(PAUSED_UNTIL)
        this[PARTNER_ALERTS] = s.partnerAlerts
    }

    private companion object {
        val HOME_SSID = stringPreferencesKey("home_ssid")
        val EXERCISE = stringPreferencesKey("exercise")
        val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val DEMO_THRESHOLD_SEC = intPreferencesKey("demo_threshold_sec")
        val DEMO_CONTRACT_TODAY = booleanPreferencesKey("demo_contract_today")
        val DEMO_ALWAYS_YES = booleanPreferencesKey("demo_always_yes")
        val SHOW_SCROLL_PROGRESS = booleanPreferencesKey("show_scroll_progress")
        val TOLERANCE_MINUTES = intPreferencesKey("tolerance_minutes")
        val VIDEO_CHAIN_LIMIT = intPreferencesKey("video_chain_limit")
        val SQUATS_PER_UNLOCK = intPreferencesKey("squats_per_unlock")
        val PUSHUPS_PER_UNLOCK = intPreferencesKey("pushups_per_unlock")
        val DEBT_LIMIT_MINUTES = intPreferencesKey("debt_limit_minutes")
        val TRACKED_APPS = stringSetPreferencesKey("tracked_apps")
        /** Aplikace, které existovaly při uložení [TRACKED_APPS]; novější (149) jsou ve výchozím stavu zapnuté. */
        val TRACKED_APPS_KNOWN = stringSetPreferencesKey("tracked_apps_known")
        val TRACK_OTHER_FEED_APPS = booleanPreferencesKey("track_other_feed_apps")
        val PAUSED_UNTIL = longPreferencesKey("protection_paused_until")
        val PARTNER_ALERTS = booleanPreferencesKey("partner_alerts")
        val DEFAULTS = Settings()
    }
}

/**
 * Uložené sledované aplikace (149). Aplikace přidané do [TrackedApp] po uložení (chybí v [known]) se zapnou,
 * jinak by nové sítě po aktualizaci tiše chyběly. Bez [known] (uloženo před 149) = původní čtyři.
 */
internal fun trackedAppsFrom(saved: Set<String>?, known: Set<String>?): Set<TrackedApp> {
    if (saved == null) return TrackedApp.entries.toSet()
    val knownAtSave = known ?: setOf("INSTAGRAM", "TIKTOK", "YOUTUBE", "FIREFOX")
    return TrackedApp.entries.filter { it.name in saved || it.name !in knownAtSave }.toSet()
}
