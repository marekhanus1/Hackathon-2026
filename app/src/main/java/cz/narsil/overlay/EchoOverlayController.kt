package cz.narsil.overlay

import android.util.Log
import cz.narsil.core.ContractRepository
import cz.narsil.core.EchoVideoRepository
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.InterventionTriggerSource
import cz.narsil.core.MovementRepository
import cz.narsil.core.SquatCounter
import cz.narsil.ui.screens.echo.EchoFeeling
import cz.narsil.ui.screens.echo.EchoScreen
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
        val content = EchoStateBuilder.build(trigger, movement.state.value, contracts.today.value, video)
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
                    onStartSquats = { startSquats(trigger, ui.value.selectedFeeling) },
                    onPayWithSteps = { choose(trigger, InterventionOutcome.PAID_STEPS, ui.value.selectedFeeling) },
                    onBorrow = { choose(trigger, InterventionOutcome.BORROWED, ui.value.selectedFeeling) },
                    videoSlot = { EchoVideoPlayer(content.videoUri, content.fallbackText) },
                )
            }
        }
        Log.i(TAG, "Ozvěna ${if (shown) "ukázána" else "neukázána (oprávnění)"}: ${trigger.reason} ${trigger.event.packageName}")
    }

    private fun startSquats(trigger: InterventionTrigger, feeling: EchoFeeling?) {
        val counter = squatCounter ?: return choose(trigger, InterventionOutcome.SQUATS, feeling)
        window.show {
            NarsilTheme {
                SquatOverlayContent(
                    counter = counter,
                    onCancel = {
                        window.dismiss()
                        scope.launch { showEcho(trigger) } // zpět na Ozvěnu
                    },
                    onDone = { choose(trigger, InterventionOutcome.SQUATS, feeling) },
                )
            }
        }
    }

    private fun choose(trigger: InterventionTrigger, outcome: InterventionOutcome, feeling: EchoFeeling?) {
        window.dismiss()
        scope.launch {
            runCatching { choiceHandler.onChoice(trigger, outcome, feeling) }
                .onFailure { Log.e(TAG, "volba $outcome selhala", it) }
            Log.i(TAG, "volba: $outcome, pocit: $feeling")
        }
    }

    private companion object {
        const val TAG = "NarsilOverlay"
    }
}
