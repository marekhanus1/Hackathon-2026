package cz.narsil.core.fake

import cz.narsil.core.Settings
import cz.narsil.core.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Fake v paměti (náhledy, testy). */
class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    private val _settings = MutableStateFlow(initial)
    override val settings: StateFlow<Settings> = _settings.asStateFlow()

    override suspend fun update(change: (Settings) -> Settings) = _settings.update(change)
}
