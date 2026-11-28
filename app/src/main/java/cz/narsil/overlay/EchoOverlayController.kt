package cz.narsil.overlay

import android.util.Log
import cz.narsil.core.ContractRepository
import cz.narsil.core.EchoVideoRepository
import cz.narsil.core.Exercise
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.InterventionTriggerSource
import cz.narsil.core.GamificationEngine
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
    /** Vrací XP za volbu; u CLOSED XP čekající na potvrzení (155), 0 = bez XP. */
    suspend fun onChoice(trigger: InterventionTrigger, outcome: InterventionOutcome, feeling: EchoFeeling?): Int
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
    private val choiceHandler: EchoChoiceHandler = EchoChoiceHandler { _, _, _ -> 0 },
    private val squatCounter: SquatCounter? = null,
    /** Počítadlo kliků (232); použije se, když [exercise] vrátí PUSHUPS. */
    private val pushupCounter: SquatCounter? = null,
    /** Zvolený cvik (232), čte se při každém zobrazení. */
    private val exercise: () -> Exercise = { Exercise.SQUATS },
    /** Opakování na odemčení pro cvik (136): `Settings.repsPerUnlock(exercise)`. */
    private val repsPerUnlock: (Exercise) -> Int = ExerciseText::target,
    /** Demo režim: ladicí ukazatel přes kameru dřepů. */
    private val demoMode: () -> Boolean = { false },
    /** XP a úroveň před/po volbě → obrazovka odměny (321). null = bez odměny. */
    private val gamification: GamificationEngine? = null,
    /** Dřepy nad 20 (SPEC 8: žolík za každých 50 navíc). */
    private val onExtraSquats: suspend (Int) -> Unit = {},
    /** Rozdělaná Ozvěna mimo instanci služby (141); sdílí ji ovladače po sobě jdoucích instancí. */
    private val session: EchoSessionStore = EchoSessionStore(),
    /** Rozdělaná Ozvěna byla moc stará na obnovu: detekce smí zasáhnout znovu. */
    private val onRestoreExpired: suspend (InterventionTrigger) -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
    /** Je aplikace zásahu pořád v popředí? (153) Ozvěna z detekce se nikdy neukáže nad jinou aplikací. */
    private val isInForeground: (InterventionTrigger) -> Boolean = { true },
    /** Zásah zahozen, protože uživatel už je jinde: detekce ho zopakuje po návratu do feedu. */
    private val onSkipped: suspend (InterventionTrigger) -> Unit = {},
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            restore()
            triggers.triggers.collect { t ->
                if (window.isShowing) return@collect
                if (!isInForeground(t)) {
                    Log.i(TAG, "Ozvěna neukázána: ${t.event.packageName} už není v popředí, odloženo do návratu")
                    runCatching { onSkipped(t) }
                    return@collect
                }
                show(t)
            }
        }
    }

    /** Obnoví Ozvěnu rozdělanou ve staré instanci služby (141). */
    private suspend fun restore() {
        when (val r = session.takeForRestore(clock())) {
            null -> Unit
            is EchoSessionStore.Restore.Expired -> {
                Log.i(TAG, "rozdělaná Ozvěna prošlá, neobnovuji")
                runCatching { onRestoreExpired(r.pending.trigger) }
            }
            is EchoSessionStore.Restore.Show -> {
                val p = r.pending
                Log.i(TAG, "obnovuji rozdělanou Ozvěnu (${p.phase}, pocit ${p.feeling})")
                when (p.phase) {
                    EchoSessionStore.Phase.ECHO -> showEcho(p.trigger, p.feeling)
                    EchoSessionStore.Phase.EXERCISE -> startSquats(p.trigger, p.feeling)
                }
            }
        }
    }

    /** Okno pryč se starou instancí služby; rozdělaná volba zůstává v [session] pro obnovu. */
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

    private suspend fun showEcho(trigger: InterventionTrigger, feeling: EchoFeeling? = session.pending?.takeIf { it.trigger == trigger }?.feeling) {
        session.begin(trigger, clock())
        session.setPhase(EchoSessionStore.Phase.ECHO)
        session.setFeeling(feeling)
        val video = runCatching { videos.pickEchoVideo() }.getOrNull()
        val content = exercise().let { ex ->
            EchoStateBuilder.build(trigger, movement.state.value, contracts.today.value, video, ex, repsPerUnlock(ex))
        }
        val ui = MutableStateFlow(content.ui.copy(selectedFeeling = feeling))
        val shown = window.show {
            val state by ui.collectAsState()
            NarsilTheme {
                EchoScreen(
                    state = state,
                    onSelectFeeling = { f ->
                        ui.value = state.copy(selectedFeeling = if (state.selectedFeeling == f) null else f)
                        session.setFeeling(ui.value.selectedFeeling)
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
        val ex = exercise()
        val reps = repsPerUnlock(ex)
        val counter = (if (ex == Exercise.PUSHUPS) pushupCounter else squatCounter)
            ?: return choose(trigger, InterventionOutcome.SQUATS, feeling)
        session.begin(trigger, clock())
        session.setFeeling(feeling)
        session.setPhase(EchoSessionStore.Phase.EXERCISE)
        window.show {
            NarsilTheme {
                SquatOverlayContent(
                    counter = counter,
                    onCancel = {
                        window.dismiss()
                        scope.launch { showEcho(trigger) } // zpět na Ozvěnu
                    },
                    onDone = { count ->
                        choose(trigger, InterventionOutcome.SQUATS, feeling)
                        // Pohyb a gamifikace počítají v dřepech: kliky se převedou (10 kliků = 20 dřepů).
                        val extra = ExerciseText.squatEquivalent(ex, count - reps)
                        if (extra > 0) scope.launch { runCatching { onExtraSquats(extra) } }
                    },
                    demoMode = demoMode(),
                    exercise = ex,
                    target = reps,
                )
            }
        }
    }

    private fun choose(trigger: InterventionTrigger, outcome: InterventionOutcome, feeling: EchoFeeling?) {
        session.clear()
        val before = gamification?.state?.value
        // Okno se schová hned (Zavřít → plocha, odemknutí → zpět do aplikace); odměna se ukáže zvlášť.
        window.dismiss()
        scope.launch {
            val xp = runCatching { choiceHandler.onChoice(trigger, outcome, feeling) }
                .onFailure { Log.e(TAG, "volba $outcome selhala", it) }
                .getOrDefault(0)
            Log.i(TAG, "volba: $outcome, pocit: $feeling")
            val after = gamification?.state?.value
            val ex = exercise()
            val reward = RewardBuilder.build(
                outcome, before, after, ex, repsPerUnlock(ex),
                pendingCloseXp = if (outcome == InterventionOutcome.CLOSED) xp else 0,
            ) ?: return@launch
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
