package cz.narsil.core.fake

import cz.narsil.core.StravaActivity
import cz.narsil.core.StravaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake: připojeno, dnes ranní běh 5,2 km za 28 min. Nepřipojený vrací prázdný seznam. */
class FakeStravaRepository(
    connected: Boolean = true,
    var activities: List<StravaActivity> = listOf(demoRun()),
) : StravaRepository {
    private val _connected = MutableStateFlow(connected)
    override val isConnected: StateFlow<Boolean> = _connected.asStateFlow()

    override suspend fun todayActivities(): List<StravaActivity> = if (_connected.value) activities else emptyList()

    fun setConnected(value: Boolean) {
        _connected.value = value
    }

    companion object {
        fun demoRun(now: Long = System.currentTimeMillis()) = StravaActivity(
            id = 1L,
            type = "Run",
            startMillis = now - 60 * 60_000,
            movingTimeSec = 28 * 60,
            distanceMeters = 5_200f,
        )
    }
}
