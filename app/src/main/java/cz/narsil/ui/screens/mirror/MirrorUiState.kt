package cz.narsil.ui.screens.mirror

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.narsil.core.History
import cz.narsil.core.HistoryRepository
import cz.narsil.core.fake.FakeHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val valueEntries: List<ContentValueEntry> = defaultValueEntries(),
    /** Ze skutečné databáze (140); false = ukázková data do prvního načtení. */
    val isLoaded: Boolean = false,
    /** Dny s nějakým záznamem (0 = zatím žádná historie). */
    val daysWithData: Int = 21,
) {
    /** Nadpis Zrcadla: hodiny, nebo minuty, dokud nejde o celou hodinu. */
    val reclaimedHeadline: String
        get() = when {
            daysWithData == 0 -> "Zrcadlo se naplní po prvních dnech s Narsilem"
            totalReclaimedMinutes >= 60 -> "Za $daysWithData dní sis vzal zpět ${totalReclaimedMinutes / 60} h ${totalReclaimedMinutes % 60} min"
            else -> "Za $daysWithData dní sis vzal zpět $totalReclaimedMinutes min"
        }

    val trendLine: String
        get() = if (daysWithData < 8) {
            "Pokles scrollování ukážu po prvním týdnu · v průměru $averageDailySquats dřepů denně."
        } else {
            "Pasivní scrollování kleslo o $feedDropPercent % · v průměru $averageDailySquats dřepů denně."
        }
}

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

/** Zrcadlo nad skutečnou historií (140). Fake výchozí jen pro náhledy. */
class MirrorViewModel(
    private val history: HistoryRepository = FakeHistoryRepository(),
    private val loadOnStart: Boolean = true,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MirrorUiState())
    val uiState: StateFlow<MirrorUiState> = _uiState.asStateFlow()

    init {
        if (loadOnStart) refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val h = runCatching { history.load(HistoryRepository.DEFAULT_DAYS) }.getOrDefault(History())
            _uiState.update { MirrorMapper.map(h, activeTab = it.activeTab) }
        }
    }

    fun selectTab(tab: MirrorTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }
}
