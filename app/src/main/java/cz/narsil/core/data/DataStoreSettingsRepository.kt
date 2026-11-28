package cz.narsil.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cz.narsil.core.LlmProvider
import cz.narsil.core.Settings
import cz.narsil.core.SettingsRepository
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
    )

    private fun androidx.datastore.preferences.core.MutablePreferences.write(s: Settings) {
        if (s.homeSsid != null) this[HOME_SSID] = s.homeSsid else remove(HOME_SSID)
        this[LLM_PROVIDER] = s.llmProvider.name
        this[DEMO_MODE] = s.demoMode
        if (s.demoThresholdSec != null) this[DEMO_THRESHOLD_SEC] = s.demoThresholdSec else remove(DEMO_THRESHOLD_SEC)
        this[DEMO_CONTRACT_TODAY] = s.demoContractToday
        this[DEMO_ALWAYS_YES] = s.demoAlwaysYes
    }

    private companion object {
        val HOME_SSID = stringPreferencesKey("home_ssid")
        val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val DEMO_THRESHOLD_SEC = intPreferencesKey("demo_threshold_sec")
        val DEMO_CONTRACT_TODAY = booleanPreferencesKey("demo_contract_today")
        val DEMO_ALWAYS_YES = booleanPreferencesKey("demo_always_yes")
    }
}
