package cz.narsil.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Aktivita ze Stravy.
 *
 * @param type typ podle Strava API, např. "Run", "Ride", "Walk", "Workout"
 * @param startMillis začátek (epoch ms)
 * @param movingTimeSec pohybový čas v sekundách
 */
data class StravaActivity(
    val id: Long,
    val type: String,
    val startMillis: Long,
    val movingTimeSec: Int,
    val distanceMeters: Float,
)

/** Strava (511, Člen 5). Pohybový úkol je splněný aktivitou ≥ 15 min dnes od 4:00 (SPEC 7). */
interface StravaRepository {
    val isConnected: StateFlow<Boolean>

    /** Aktivity od začátku dne (4:00). Chyba nebo nepřipojeno = prázdný seznam. */
    suspend fun todayActivities(): List<StravaActivity>

    companion object {
        val MOVEMENT_TYPES = setOf("Run", "Ride", "Walk", "Workout")
        const val MIN_MOVING_TIME_SEC = 15 * 60
    }
}
