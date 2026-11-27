package cz.narsil.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Nastavení aplikace (DataStore, ne Room).
 *
 * @param homeSsid Wi-Fi uložená jako domov (112)
 * @param demoThresholdSec práh zásahu v demo režimu (20 s), null = výchozí 5 min
 * @param demoContractToday smlouva platí hned (pro dnešek)
 * @param demoAlwaysYes ověření úkolů vždy ANO (jen demo panel)
 */
data class Settings(
    val homeSsid: String? = null,
    val llmProvider: LlmProvider = LlmProvider.CLAUDE,
    val demoMode: Boolean = false,
    val demoThresholdSec: Int? = null,
    val demoContractToday: Boolean = false,
    val demoAlwaysYes: Boolean = false,
)

/** Nastavení (105). Implementace `cz.narsil.core.data.DataStoreSettingsRepository`. */
interface SettingsRepository {
    val settings: StateFlow<Settings>

    /** Atomická změna, např. `update { it.copy(demoMode = true) }`. */
    suspend fun update(change: (Settings) -> Settings)
}
