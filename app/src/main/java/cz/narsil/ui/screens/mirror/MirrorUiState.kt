package cz.narsil.ui.screens.mirror

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MirrorTab(val label: String) {
    MIRROR("Zrcadlo (21 dní)"),
    VALUE_MAP("Mapa hodnoty")
}

data class DailyTrendPoint(
    val dayLabel: String,
    val reclaimedMinutes: Int,
    val feedMinutes: Int
)

data class ContentValueEntry(
    val channelOrFormat: String,
    val averageRating: Float,
    /** Kolik ohodnocených videí do skupiny patří (minuty sledování se neukládají). */
    val videoCount: Int,
    val isLowValueTrap: Boolean
)

data class MirrorUiState(
    val activeTab: MirrorTab = MirrorTab.MIRROR,
    val totalReclaimedMinutes: Int = 780,
    val feedDropPercent: Int = 58,
    val averageDailySquats: Int = 24,
    val criticalBossHour: String = "22:00 – 23:00",
    val bossSurvivedDays: Int = 5,
    val weeklyTrend: List<DailyTrendPoint> = defaultWeeklyTrend(),
    val valueEntries: List<ContentValueEntry> = defaultValueEntries()
)

private fun defaultWeeklyTrend(): List<DailyTrendPoint> = listOf(
    DailyTrendPoint("Týden 1", reclaimedMinutes = 160, feedMinutes = 340),
    DailyTrendPoint("Týden 2", reclaimedMinutes = 275, feedMinutes = 220),
    DailyTrendPoint("Týden 3", reclaimedMinutes = 345, feedMinutes = 142)
)

private fun defaultValueEntries(): List<ContentValueEntry> = listOf(
    ContentValueEntry("Přednášky a programování", averageRating = 4.7f, videoCount = 12, isLowValueTrap = false),
    ContentValueEntry("Dokumenty a rozhovory", averageRating = 4.3f, videoCount = 8, isLowValueTrap = false),
    ContentValueEntry("Krátké klipy a kompilace", averageRating = 1.6f, videoCount = 15, isLowValueTrap = true),
    ContentValueEntry("Nekonečné herní streamy v noci", averageRating = 2.0f, videoCount = 6, isLowValueTrap = true)
)

class MirrorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MirrorUiState())
    val uiState: StateFlow<MirrorUiState> = _uiState.asStateFlow()

    fun selectTab(tab: MirrorTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }
}
