package cz.narsil.overlay

import android.util.Log
import cz.narsil.core.ContractRepository
import cz.narsil.core.EchoVideoRepository
import cz.narsil.core.Exercise
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.InterventionTriggerSource
import cz.narsil.core.GamificationEngine
import cz.narsil.core.MovementRates
import cz.narsil.core.MovementRepository
import cz.narsil.core.SquatCounter
import cz.narsil.ui.screens.echo.EchoFeeling
import cz.narsil.ui.screens.echo.EchoScreen
import cz.narsil.ui.screens.reward.RewardOverlay
import cz.narsil.ui.theme.NarsilTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/** Volba v Ozvěně: výsledek a nepovinný pocit. Zpracuje 114. */
fun interface EchoChoiceHandler {
    suspend fun onChoice(trigger: InterventionTrigger, outcome: InterventionOutcome, feeling: EchoFeeling?)
}

/**
 * Spojí zdroj zásahů s oknem (113): na každý [InterventionTrigger] vybere video, složí stav
 * a ukáže [EchoScreen] (310) s přehrávačem ve slotu. Volby předá [choiceHandler] (114) a okno zavře.
 * „20 dřepů“ s [squatCounter] přepne okno na dřepy (311) a volbu předá až po dosažení cíle.
 * Během otevřeného okna další zásahy ignoruje.
 */
class EchoOverlayController(
    private val window: EchoOverlayWindow,
    private val triggers: InterventionTriggerSource,
    private val movement: MovementRepository,
    private val contracts: ContractRepository,
    private val videos: EchoVideoRepository,
    private val scope: CoroutineScope,
    private val choiceHandler: EchoChoiceHandler = EchoChoiceHandler { _, _, _ -> },
    private val squatCounter: SquatCounter? = null,
    /** Počítadlo kliků (232); použije se, když [exercise] vrátí PUSHUPS. */
    private val pushupCounter: SquatCounter? = null,
    /** Zvolený cvik (232), čte se při každém zobrazení. */
    private val exercise: () -> Exercise = { Exercise.SQUATS },
    /** Demo režim: ladicí ukazatel přes kameru dřepů. */
    private val demoMode: () -> Boolean = { false },
    /** XP a úroveň před/po volbě → obrazovka odměny (321). null = bez odměny. */
    private val gamification: GamificationEngine? = null,
    /** Dřepy nad 20 (SPEC 8: žolík za každých 50 navíc). */
    private val onExtraSquats: suspend (Int) -> Unit = {},
    /** Uložení přepnutého cviku (dřepy / kliky) přímo z Ozvěny nebo obrazovky cviku. */
    private val onSelectExercise: (Exercise) -> Unit = {},
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch { triggers.triggers.collect { show(it) } }
    }

    fun stop() {
        job?.cancel()
        job = null
        window.dismiss()
    }

    /** Ukáže Ozvěnu pro [trigger] (i ručně z demo panelu). */
    suspend fun show(trigger: InterventionTrigger) {
        if (window.isShowing) return
        showEcho(trigger)
    }

    private suspend fun showEcho(trigger: InterventionTrigger) {
        val video = runCatching { videos.pickEchoVideo() }.getOrNull()
        val content = EchoStateBuilder.build(trigger, movement.state.value, contracts.today.value, video, exercise())
        val ui = MutableStateFlow(content.ui)
        val shown = window.show {
            val state by ui.collectAsState()
            NarsilTheme {
                EchoScreen(
                    state = state,
                    onSelectFeeling = { f ->
                        ui.value = state.copy(selectedFeeling = if (state.selectedFeeling == f) null else f)
                    },
                    onCloseFeed = { choose(trigger, InterventionOutcome.CLOSED, ui.value.selectedFeeling) },
                    onStartSquats = { startSquats(trigger, ui.value.selectedFeeling, ui.value.selectedExercise) },
                    onPayWithSteps = { choose(trigger, InterventionOutcome.PAID_STEPS, ui.value.selectedFeeling) },
                    onBorrow = { choose(trigger, InterventionOutcome.BORROWED, ui.value.selectedFeeling) },
                    onSelectExercise = { newEx ->
                        if (newEx != state.selectedExercise) {
                            onSelectExercise(newEx)
                            ui.value = state.copy(
                                selectedExercise = newEx,
                                exerciseOptionTitle = ExerciseText.echoOption(newEx),
                            )
                        }
                    },
                    videoSlot = { EchoVideoPlayer(content.videoUri, content.fallbackText) },
                )
            }
        }
        Log.i(TAG, "Ozvěna ${if (shown) "ukázána" else "neukázána (oprávnění)"}: ${trigger.reason} ${trigger.event.packageName}")
    }

    private fun counterFor(ex: Exercise): SquatCounter? =
        if (ex == Exercise.PUSHUPS) pushupCounter else squatCounter

    private fun startSquats(
        trigger: InterventionTrigger,
        feeling: EchoFeeling?,
        initialExercise: Exercise = exercise(),
    ) {
        if (counterFor(initialExercise) == null) {
            return choose(trigger, InterventionOutcome.SQUATS, feeling, initialExercise)
        }
        val activeExercise = MutableStateFlow(initialExercise)
        window.show {
            val ex by activeExercise.collectAsState()
            val counter = counterFor(ex) ?: return@show
            NarsilTheme {
                SquatOverlayContent(
                    counter = counter,
                    onCancel = {
                        counter.stop()
                        window.dismiss()
                        scope.launch { showEcho(trigger) } // zpět na Ozvěnu
                    },
                    onDone = { count ->
                        choose(trigger, InterventionOutcome.SQUATS, feeling, ex)
                        // Pohyb a gamifikace počítají v dřepech: kliky se převedou (10 kliků = 20 dřepů).
                        val extra = ExerciseText.squatEquivalent(ex, count) - MovementRates.SQUATS_PER_UNLOCK
                        if (extra > 0) scope.launch { runCatching { onExtraSquats(extra) } }
                    },
                    demoMode = demoMode(),
                    exercise = ex,
                    onSelectExercise = { newEx ->
                        if (newEx != ex && counterFor(newEx) != null) {
                            counter.stop()
                            onSelectExercise(newEx)
                            activeExercise.value = newEx
                        }
                    },
                )
            }
        }
    }

    private fun choose(
        trigger: InterventionTrigger,
        outcome: InterventionOutcome,
        feeling: EchoFeeling?,
        chosenExercise: Exercise = exercise(),
    ) {
        val before = gamification?.state?.value
        // Okno se schová hned (Zavřít → plocha, odemknutí → zpět do aplikace); odměna se ukáže zvlášť.
        window.dismiss()
        scope.launch {
            runCatching { choiceHandler.onChoice(trigger, outcome, feeling) }
                .onFailure { Log.e(TAG, "volba $outcome selhala", it) }
            Log.i(TAG, "volba: $outcome, pocit: $feeling")
            val after = gamification?.state?.value
            val reward = RewardBuilder.build(outcome, before, after, chosenExercise) ?: return@launch
            Log.i(TAG, "odměna: +${reward.gainedXp} XP, úroveň ${reward.previousLevel} → ${reward.newLevel}")
            window.show {
                NarsilTheme {
                    RewardOverlay(state = reward, onContinue = { window.dismiss() })
                }
            }
        }
    }

    private companion object {
        const val TAG = "NarsilOverlay"
    }
}
