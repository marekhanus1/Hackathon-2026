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
import cz.narsil.ui.screens.echo.EchoUiState
import cz.narsil.ui.screens.echo.FriendRequest
import cz.narsil.ui.screens.reward.RewardOverlay
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.NarsilTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
    /** Zásah zahozen (uživatel už je jinde, nebo okno nejde ukázat, 159): detekce ho zopakuje. */
    private val onSkipped: suspend (InterventionTrigger) -> Unit = {},
    /**
     * Systémový dialog kamery (157): spustí `CameraPermissionActivity`, false = nešlo. Okno Ozvěny leží nad
     * všemi aplikacemi, proto se na dobu dialogu schová. null = slot dřepů si poradí sám (nastavení aplikace).
     */
    private val requestCamera: (() -> Boolean)? = null,
    /** Odpověď z dialogu kamery (true = povoleno). */
    private val cameraResults: Flow<Boolean> = emptyFlow(),
    /** Věta z minulých odpovědí „Co ti to video dalo?“ (156), null = žádná. Nesmí házet. */
    private val valueNote: suspend (InterventionTrigger) -> String? = { null },
    /** Ozvěna se právě ukazuje (156: zavřít panel otázky pod ní). */
    private val onEchoShown: () -> Unit = {},
) {
    /** Je teď vidět Ozvěna, cvik nebo odměna? */
    val isShowing: Boolean get() = window.isShowing

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
        val note = runCatching { valueNote(trigger) }.getOrNull()
        val content = exercise().let { ex ->
            EchoStateBuilder.build(trigger, movement.state.value, contracts.today.value, video, ex, repsPerUnlock(ex), note)
        }
        runCatching { onEchoShown() }
        val ui = MutableStateFlow(content.ui.copy(selectedFeeling = feeling))
        val shown = window.show {
            val state by ui.collectAsState()
            NarsilTheme { AeroBackdrop {
                EchoScreen(
                    state = state,
                    onSelectFeeling = { f ->
                        ui.value = state.copy(selectedFeeling = if (state.selectedFeeling == f) null else f)
                        session.setFeeling(ui.value.selectedFeeling)
                    },
                    onCloseFeed = { choose(trigger, InterventionOutcome.CLOSED, ui.value.selectedFeeling) },
                    onStartSquats = { startSquats(trigger, ui.value.selectedFeeling) },
                    onPayWithSteps = { choose(trigger, InterventionOutcome.PAID_STEPS, ui.value.selectedFeeling) },
                    onBorrow = { askFriend(trigger, ui) },
                    videoSlot = { EchoVideoPlayer(content.videoUri, content.fallbackText) },
                )
            } }
        }
        Log.i(TAG, "Ozvěna ${if (shown) "ukázána" else "neukázána (oprávnění)"}: ${trigger.reason} ${trigger.event.packageName}")
        if (!shown) {
            // 159: bez okna by detekce čekala na volbu, která nepřijde, a v relaci už nezasáhla.
            // Zásah se zahodí jako u aplikace mimo popředí; detekce ho zopakuje (po udělení oprávnění se ukáže).
            session.clear()
            runCatching { onSkipped(trigger) }
        }
    }

    /**
     * 163: prosba Parťákovi místo půjčky do zítřka. Bez serveru je odpověď simulovaná:
     * „Píšu Parťákovi…“ → „Jasně, máš 10 min“ → odemknutí (notifikaci s odpovědí pošle pohyb).
     */
    private fun askFriend(trigger: InterventionTrigger, ui: MutableStateFlow<EchoUiState>) {
        if (ui.value.friendRequest != FriendRequest.IDLE) return
        ui.value = ui.value.copy(friendRequest = FriendRequest.ASKING)
        scope.launch {
            delay(FRIEND_REPLY_MILLIS)
            if (!window.isShowing) return@launch
            ui.value = ui.value.copy(friendRequest = FriendRequest.GRANTED)
            delay(FRIEND_GRANTED_MILLIS)
            if (!window.isShowing) return@launch
            choose(trigger, InterventionOutcome.BORROWED, ui.value.selectedFeeling)
        }
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
            NarsilTheme { AeroBackdrop {
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
                    onRequestPermission = requestCamera?.let { { askCamera(trigger, feeling) } },
                    exercise = ex,
                    target = reps,
                )
            } }
        }
    }

    /** Schová okno dřepů, ukáže systémový dialog kamery a po odpovědi (nejvýš [CAMERA_WAIT_MILLIS]) okno vrátí. */
    private fun askCamera(trigger: InterventionTrigger, feeling: EchoFeeling?) {
        window.dismiss()
        scope.launch {
            val started = runCatching { requestCamera?.invoke() == true }.getOrDefault(false)
            val granted = if (started) withTimeoutOrNull(CAMERA_WAIT_MILLIS) { cameraResults.first() } else null
            Log.i(TAG, "kamera: dialog=${started}, povoleno=$granted")
            startSquats(trigger, feeling)
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
                NarsilTheme { AeroBackdrop {
                    RewardOverlay(state = reward, onContinue = { window.dismiss() })
                } }
            }
        }
    }

    private companion object {
        const val TAG = "NarsilOverlay"
        const val CAMERA_WAIT_MILLIS = 60_000L
        const val FRIEND_REPLY_MILLIS = 1_800L
        const val FRIEND_GRANTED_MILLIS = 1_200L
    }
}
