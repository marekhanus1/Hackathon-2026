package cz.narsil.app.nav

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.narsil.app.ServiceLocator
import cz.narsil.core.AvatarState
import cz.narsil.core.ComfortContext
import cz.narsil.core.DailyContract
import cz.narsil.core.TaskStatus
import cz.narsil.core.data.DayKey
import cz.narsil.llm.client.LlmRequest
import cz.narsil.llm.client.MultiProviderLlmClient
import cz.narsil.overlay.ExerciseText
import cz.narsil.overlay.RewardBuilder
import cz.narsil.ui.screens.badges.BadgesScreen
import cz.narsil.ui.screens.badges.BadgesUiState
import cz.narsil.ui.screens.contract.ContractScreen
import cz.narsil.ui.screens.contract.ContractViewModel
import cz.narsil.ui.screens.contract.MovementGoalType
import cz.narsil.ui.screens.home.HomeScreen
import cz.narsil.ui.screens.home.HomeUiState
import cz.narsil.ui.screens.home.ZoneStatus
import cz.narsil.ui.screens.mirror.MirrorScreen
import cz.narsil.ui.screens.mirror.MirrorViewModel
import cz.narsil.ui.screens.pet.PetBubble
import cz.narsil.ui.screens.pet.PetChatScreen
import cz.narsil.ui.screens.pet.PetContext
import cz.narsil.ui.screens.pet.PetSpeechPolicy
import cz.narsil.ui.screens.pet.PetViewModel
import cz.narsil.ui.screens.pet.SharedPrefsPetMemoryStore
import cz.narsil.ui.components.AvatarState as UiAvatar
import kotlinx.coroutines.flow.combine

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
    val petVm: PetViewModel = viewModel(
        key = "pet",
        factory = viewModelFactory { initializer { createPetViewModel() } },
    )
    val petState by petVm.uiState.collectAsState()
    val state = HomeUiState(
        level = game.level,
        levelTitle = RewardBuilder.levelTitle(game.level),
        currentXp = game.xp,
        avatarState = game.avatar.toUi(),
        avatarSummary7Days = "Série ${game.streakDays} dní · vyhraných dnů ${game.wonDays}",
        reclaimedMinutesToday = game.today.reclaimedMinutes,
        feedMinutesToday = game.today.feedMinutes,
        wonDaysTotal = game.wonDays,
        streakDays = game.streakDays,
        availableJokers = game.jokers,
        stepsToday = movement.stepsToday,
        stepGoal = contract?.stepGoal ?: 6_000,
        availableStepMinutes = movement.balanceMinutes,
        debtMinutes = movement.debtMinutes,
        contractCompletedToday = contract?.isFulfilled(movement.stepsToday) == true,
        plannedContentSummary = contract?.plannedContent?.joinToString(", ")?.ifBlank { null } ?: "Zatím bez plánu",
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
            TextButton(onClick = { onNavigate(NarsilRoute.Pet) }) { Text("Sova") }
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
            topSlot = {
                val bubbleText = petState.bubbleText
                if (petState.bubbleVisible && bubbleText != null) {
                    PetBubble(
                        mood = petState.mood,
                        text = bubbleText,
                        onOpenChat = { onNavigate(NarsilRoute.Pet) },
                        onDismiss = petVm::muteToday,
                    )
                }
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun ContractRoute(onBack: () -> Unit) {
    val sl = ServiceLocator
    val vm: ContractViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val settings by sl.settingsRepository.settings.collectAsState()
    // Odeslaná smlouva → skutečné uložení (212). Večer na zítřek, demo přepínač „platí hned“ na dnešek.
    LaunchedEffect(state.isSubmitted) {
        if (!state.isSubmitted) return@LaunchedEffect
        val date = DayKey.contractDateFor(System.currentTimeMillis(), settings.demoContractToday)
        val previous = runCatching { sl.contractRepository.forDate(date) }.getOrNull()
        val contract = DailyContract(
            date = date,
            stepGoal = if (state.goalType == MovementGoalType.STEPS) state.goalAmount else previous?.stepGoal ?: 6_000,
            plannedContent = state.plannedItems,
            videoUri = previous?.videoUri,
            tasks = previous?.tasks.orEmpty(),
        )
        runCatching { sl.contractRepository.save(contract) }
    }
    ContractScreen(
        state = state,
        onSelectGoalType = vm::selectGoalType,
        onSetGoalAmount = vm::setGoalAmount,
        onAdjustGoalAmount = vm::adjustGoalAmount,
        onDraftChange = vm::updateDraftItemText,
        onAddPlannedItem = vm::addPlannedItem,
        onRemovePlannedItem = vm::removePlannedItem,
        onToggleRecording = vm::toggleRecording,
        onNextStep = vm::nextStep,
        onPreviousStep = vm::previousStep,
        onSubmitContract = vm::submitContract,
        onBackToHome = onBack,
    )
}

@Composable
fun MirrorRoute() {
    val vm: MirrorViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    MirrorScreen(state = state, onSelectTab = vm::selectTab)
}

@Composable
fun BadgesRoute() {
    val game by ServiceLocator.gamificationEngine.state.collectAsState()
    BadgesScreen(
        state = BadgesUiState(
            currentAvatarState = game.avatar.toUi(),
            avatarSummary7Days = "Série ${game.streakDays} dní · úroveň ${game.level} · ${game.xp} XP",
        ),
    )
}

@Composable
fun PetRoute(onBack: () -> Unit) {
    val vm: PetViewModel = viewModel(
        key = "pet",
        factory = viewModelFactory { initializer { createPetViewModel() } },
    )
    val state by vm.uiState.collectAsState()
    PetChatScreen(
        state = state,
        onInputChange = vm::onInputChange,
        onSend = { vm.send(state.input) },
        onQuickReply = vm::send,
        onMuteToday = vm::muteToday,
        onBack = onBack,
    )
}

private fun createPetViewModel(): PetViewModel {
    val sl = ServiceLocator
    val contextFlow = combine(
        sl.gamificationEngine.state,
        sl.movementRepository.state,
        sl.contractRepository.today,
    ) { game, movement, contract ->
        val pendingTasks = contract?.tasks
            ?.filter { it.status != TaskStatus.VERIFIED }
            ?.map { it.title }
            .orEmpty()
        val planned = (pendingTasks + contract?.plannedContent.orEmpty()).distinct()
        PetContext(
            avatar = game.avatar,
            streakDays = game.streakDays,
            reclaimedMinutes = game.today.reclaimedMinutes,
            feedMinutes = game.today.feedMinutes,
            debtMinutes = movement.debtMinutes,
            stepsToday = movement.stepsToday,
            plannedTasks = planned,
        )
    }
    val llmClient = MultiProviderLlmClient({ sl.settingsRepository.settings.value.llmProvider })
    val policy = PetSpeechPolicy(SharedPrefsPetMemoryStore(sl.appContext))
    return PetViewModel(
        context = contextFlow,
        replier = { prompt ->
            llmClient.complete(
                LlmRequest(prompt = prompt, maxTokens = 150, timeoutMillis = 8_000L),
            ).text
        },
        policy = policy,
    )
}
