package cz.narsil.ui.screens.home

import cz.narsil.ui.text.CzechPlural
import androidx.lifecycle.ViewModel
import cz.narsil.ui.components.AvatarState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ZoneStatus(val label: String) {
    HOME("Doma · ochrana aktivní"),
    AWAY("Venku · Narsil mlčí"),
    UNKNOWN("Zóna nezjištěna")
}

data class HomeTaskUiState(
    val id: Long,
    val title: String,
    val rewardMinutes: Int,
    val verified: Boolean = false,
)

data class HomeUiState(
    val level: Int = 4,
    val levelTitle: String = "Kovář klidu",
    val currentXp: Int = 1860,
    val avatarState: AvatarState = AvatarState.ENERGIZED,
    val avatarSummary7Days: String = "Posledních 7 dní: 5 výher · 420 dřepů",
    val reclaimedMinutesToday: Int = 45,
    val feedMinutesToday: Int = 14,
    val wonDaysTotal: Int = 11,
    val streakDays: Int = 6,
    val availableJokers: Int = 2,
    val stepsToday: Int = 4820,
    val stepGoal: Int = 6000,
    val availableStepMinutes: Int = 40,
    val debtMinutes: Int = 0,
    /** Dluh ze včerejška, který dnes ještě nepokryly kroky ani odměny. */
    val carriedDebtMinutes: Int = 0,
    val contractCompletedToday: Boolean = false,
    /** Smlouva pro den, na který by se teď podepisovala (ráno dnešek, večer zítřek), už existuje. */
    val contractSigned: Boolean = false,
    /** Podepsaná smlouva platí dnes (jinak zítra). */
    val contractSignedForToday: Boolean = true,
    val contractTaskCount: Int = 0,
    val tasks: List<HomeTaskUiState> = listOf(
        HomeTaskUiState(id = 1L, title = "Dokončit kapitolu 3 z fyziky", rewardMinutes = 30),
        HomeTaskUiState(id = 2L, title = "Jít na 20minutovou procházku", rewardMinutes = 20),
    ),
    val plannedContentSummary: String = "Přednáška o Rustu, 1 díl seriálu",
    val zoneStatus: ZoneStatus = ZoneStatus.HOME
) {
    /** Nadpis karty smlouvy: splněná / podepsaná (dnes, zítra) / čeká. */
    val contractTitle: String
        get() = when {
            contractCompletedToday -> "Dnešní smlouva splněná (+100 XP)"
            contractSigned -> "Smlouva na ${if (contractSignedForToday) "dnes" else "zítra"} podepsaná · " +
                CzechPlural.tasks(contractTaskCount)
            else -> "Večerní smlouva čeká (2 minuty)"
        }

    val contractButton: String
        get() = when {
            contractCompletedToday -> "Upravit dnešní plán"
            contractSigned -> "Upravit smlouvu"
            else -> "Uzavřít večerní smlouvu"
        }

    val contractDone: Boolean get() = contractCompletedToday || contractSigned

    /** Řádek dluhu na kartě pohybu; null = bez dluhu. */
    val debtLabel: String?
        get() = when {
            carriedDebtMinutes > 0 && debtMinutes > 0 ->
                "Dluh ze včerejška: $carriedDebtMinutes min · dnes půjčeno $debtMinutes min"
            carriedDebtMinutes > 0 -> "Dluh ze včerejška: $carriedDebtMinutes min"
            debtMinutes > 0 -> "Dluh do zítřka: $debtMinutes min"
            else -> null
        }
}


class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}
