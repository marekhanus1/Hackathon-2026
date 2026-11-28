package cz.narsil.app.nav

import cz.narsil.ui.text.CzechPlural
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.narsil.app.ServiceLocator
import cz.narsil.core.AvatarState
import cz.narsil.core.ComfortContext
import cz.narsil.core.DailyContract
import cz.narsil.contract.ContractSigner
import cz.narsil.core.History
import cz.narsil.core.data.DayKey
import cz.narsil.core.data.LedgerKind
import cz.narsil.core.data.NarsilDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cz.narsil.overlay.ExerciseText
import cz.narsil.overlay.RewardBuilder
import cz.narsil.ui.screens.badges.BadgesScreen
import cz.narsil.ui.screens.badges.BadgesViewModel
import cz.narsil.ui.screens.home.HomeScreen
import cz.narsil.ui.screens.home.HomeUiState
import cz.narsil.ui.screens.home.ZoneStatus
import cz.narsil.ui.screens.mirror.MirrorScreen
import cz.narsil.ui.screens.mirror.MirrorViewModel
import cz.narsil.ui.components.AvatarState as UiAvatar

// Janovy obrazovky (312, 313, 322, 323, 324) v navigaci (124). Domov a odznaky ze skutečného
// stavu (pohyb, gamifikace, smlouva, kontext); ostatní zatím přes Janovy ViewModely s ukázkovými daty.

/** Postava z jádra → ilustrace v UI (302). */
internal fun AvatarState.toUi(): UiAvatar = when (this) {
    AvatarState.EXHAUSTED -> UiAvatar.TIRED
    AvatarState.TIRED -> UiAvatar.RESTING
    AvatarState.OK -> UiAvatar.BALANCED
    AvatarState.FIT -> UiAvatar.ENERGIZED
    AvatarState.THRIVING -> UiAvatar.THRIVING
}

@Composable
fun HomeRoute(onNavigate: (NarsilRoute) -> Unit) {
    val sl = ServiceLocator
    val game by sl.gamificationEngine.state.collectAsState()
    val movement by sl.movementRepository.state.collectAsState()
    val contract by sl.contractRepository.today.collectAsState()
    val comfort by sl.contextRepository.context.collectAsState()
    val appSettings by sl.settingsRepository.settings.collectAsState()
    // Smlouva pro den, na který by se teď podepisovala (ráno / demo dnešek, večer zítřek).
    val signer = remember { ContractSigner(sl.contractRepository, sl.settingsRepository) }
    val signed by produceState<DailyContract?>(null, contract, appSettings.demoContractToday) {
        val date = signer.targetDate()
        value = if (contract?.date == date) contract else runCatching { sl.contractRepository.forDate(date) }.getOrNull()
    }
    val state = HomeUiState(
        level = game.level,
        levelTitle = RewardBuilder.levelTitle(game.level),
        currentXp = game.xp,
        avatarState = game.avatar.toUi(),
        avatarSummary7Days = "Série ${CzechPlural.days(game.streakDays)} · vyhrané dny: ${game.wonDays}",
        reclaimedMinutesToday = game.today.reclaimedMinutes,
        feedMinutesToday = game.today.feedMinutes,
        wonDaysTotal = game.wonDays,
        streakDays = game.streakDays,
        availableJokers = game.jokers,
        stepsToday = movement.stepsToday,
        stepGoal = contract?.stepGoal ?: 6_000,
        availableStepMinutes = movement.balanceMinutes,
        debtMinutes = movement.debtMinutes,
        carriedDebtMinutes = movement.carriedDebtMinutes,
        contractCompletedToday = contract?.isFulfilled(movement.stepsToday) == true,
        contractSigned = signed != null,
        contractSignedForToday = signed?.date == DayKey.today(),
        contractTaskCount = signed?.tasks?.size ?: 0,
        plannedContentSummary = (contract ?: signed)?.plannedContent?.joinToString(", ")?.ifBlank { null } ?: "Zatím bez plánu",
        zoneStatus = when (comfort) {
            ComfortContext.HOME -> ZoneStatus.HOME
            ComfortContext.AWAY -> ZoneStatus.AWAY
            ComfortContext.UNKNOWN -> ZoneStatus.UNKNOWN
        },
    )
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Rychlé vstupy: dřepy, ověření úkolu, nastavení (odtud onboarding a demo panel).
        // Vlastní lišta nad Janovou obrazovkou, ať se nepřekrývá se štítkem zóny v jejím záhlaví.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            val exercise = ServiceLocator.settingsRepository.settings.collectAsState().value.exercise
            TextButton(onClick = { onNavigate(NarsilRoute.Squat) }) { Text(ExerciseText.label(exercise)) }
            TextButton(onClick = { onNavigate(NarsilRoute.Verify) }) { Text("Ověřit úkol") }
            IconButton(onClick = { onNavigate(NarsilRoute.Settings) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Nastavení", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
        HomeScreen(
            state = state,
            onOpenContract = { onNavigate(NarsilRoute.Contract) },
            onOpenMirror = { onNavigate(NarsilRoute.Mirror) },
            onOpenBadges = { onNavigate(NarsilRoute.Badges) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun MirrorRoute() {
    val vm: MirrorViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    // Skutečná data z Room (121): posledních 21 dní včetně dneška. Bez dat zůstanou připravená čísla.
    val days by produceState(emptyList<MirrorStats.Day>()) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val db = NarsilDatabase.get(ServiceLocator.appContext)
                val to = DayKey.today()
                val from = to.minusDays(MirrorStats.DAYS - 1L)
                val fromMillis = DayKey.startMillis(from)
                val toMillis = DayKey.startMillis(to.plusDays(1))
                val tasks = (0 until MirrorStats.DAYS).associate { i ->
                    val d = from.plusDays(i.toLong())
                    d to db.ledgerDao().sumForDate(d.toString(), LedgerKind.TASK_REWARD)
                }
                MirrorStats.days(
                    consumption = db.consumptionDao().between(fromMillis, toMillis),
                    interventions = db.interventionDao().between(fromMillis, toMillis),
                    taskMinutes = { d -> tasks[d] ?: 0 },
                    from = from,
                    to = to,
                )
            }.getOrDefault(emptyList())
        }
    }
    // Boss a mapa hodnoty z databáze (143); prázdná historie = připravená čísla.
    val history by produceState(History()) {
        value = withContext(Dispatchers.IO) { runCatching { ServiceLocator.historyRepository.load() }.getOrDefault(History()) }
    }
    MirrorScreen(state = MirrorStats.applyHistory(MirrorStats.apply(state, days), history), onSelectTab = vm::selectTab)
}

/** Odznaky a žebříček (323 + 140): odznaky a „Ty“ ze skutečných dat, kamarádi v MVP ukázkoví. */
@Composable
fun BadgesRoute() {
    val sl = ServiceLocator
    val vm: BadgesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BadgesViewModel(sl.historyRepository, sl.gamificationEngine, sl.movementRepository, avatarOf = { it.toUi() }) as T
        },
    )
    val state by vm.uiState.collectAsState()
    BadgesScreen(state = state)
}
