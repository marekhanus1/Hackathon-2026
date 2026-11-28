package cz.narsil.app

import android.util.Log
import cz.narsil.gamification.RulesGamificationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.fake.FakeInterventionTriggerSource
import cz.narsil.detection.accessibility.AccessibilityEvents
import cz.narsil.detection.accessibility.AccessibilityTriggerSource
import cz.narsil.detection.accessibility.NarsilAccessibilityService
import cz.narsil.overlay.EchoOutcomeHandler
import cz.narsil.overlay.EchoOverlayController
import cz.narsil.overlay.EchoOverlayWindow
import cz.narsil.core.data.ConsumptionRecorder
import cz.narsil.core.data.NarsilDatabase
import cz.narsil.overlay.EchoSessionStore
import cz.narsil.core.InterventionOutcome
import cz.narsil.overlay.ExerciseText
import cz.narsil.overlay.InterventionRecords
import cz.narsil.overlay.HomeNavigator
import cz.narsil.overlay.ScrollProgressOverlay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Integrace G2 (115): když běží služba přístupnosti, spustí [EchoOverlayController] nad zásahy
 * z detekce; okno jde přes službu (TYPE_ACCESSIBILITY_OVERLAY). Volby → [EchoOutcomeHandler] →
 * pohyb, gamifikace, časovač a zpět do detekce.
 */
object EchoOverlayStarter {
    private const val TAG = "NarsilApp"
    private const val VIDEO_POLL_MILLIS = 2_500L
    private var controller: EchoOverlayController? = null

    /** Rozdělaná Ozvěna přežije výměnu instance služby (141): nový ovladač ji obnoví. */
    private val echoSession = EchoSessionStore()
    private var progressBar: ScrollProgressOverlay? = null
    private var progressJob: Job? = null

    private val _ready = MutableStateFlow(false)

    /** Ozvěna připravená (běží služba přístupnosti). StateFlow, ať ho Compose sleduje. */
    val isReady: StateFlow<Boolean> = _ready.asStateFlow()

    /** Demo panel (123): ukáže Ozvěnu hned, bez čekání na detekci. false = služba neběží. */
    suspend fun showNow(trigger: InterventionTrigger = FakeInterventionTriggerSource.demoTrigger()): Boolean {
        val c = controller ?: return false
        c.show(trigger)
        return true
    }

    fun start() {
        val sl = ServiceLocator
        // Úseky spotřeby z detekce → Room (121): souboj s algoritmem a Zrcadlo ze skutečných minut.
        (sl.interventionTriggerSource as? AccessibilityTriggerSource)?.let { source ->
            ConsumptionRecorder(NarsilDatabase.get(sl.appContext).consumptionDao()).start(source.current, sl.appScope)
        }
        // 149: „Další aplikace s nekonečným feedem“ – služba čte neznámé aplikace jen se zapnutou volbou.
        sl.appScope.launch {
            sl.settingsRepository.settings.collect { NarsilAccessibilityService.trackOtherApps = it.trackOtherFeedApps }
        }
        // 134: během čekání na konec videa číst okno samo (YouTube při přehrávání nehlásí události).
        (sl.interventionTriggerSource as? AccessibilityTriggerSource)?.let { source ->
            sl.appScope.launch {
                source.waitingForVideoEnd.collectLatest { waiting ->
                    while (waiting) {
                        delay(VIDEO_POLL_MILLIS)
                        runCatching { NarsilAccessibilityService.instance?.pollForeground() }
                    }
                }
            }
        }
        sl.appScope.launch {
            // Sleduje instanci služby: nová instance (přeinstalace) dostane nové okno i ovladač.
            AccessibilityEvents.service.collect { service ->
                controller?.stop()
                controller = null
                progressJob?.cancel()
                progressBar?.dismiss()
                progressBar = null
                _ready.value = false
                if (service == null) return@collect
                val triggers = sl.interventionTriggerSource
                // Ukazatel blížícího se limitu (135): jen se skutečnou detekcí a zapnutým nastavením.
                val bar = ScrollProgressOverlay(service).also { progressBar = it }
                val percent = (triggers as? AccessibilityTriggerSource)?.progressPercent ?: flowOf(null)
                progressJob = sl.appScope.launch {
                    combine(percent, sl.settingsRepository.settings) { p, s -> p.takeIf { s.showScrollProgress } }
                        .distinctUntilChanged()
                        .collect { bar.update(it) }
                }
                val handler = EchoOutcomeHandler(
                    movement = sl.movementRepository,
                    gamification = sl.gamificationEngine,
                    timer = sl.unlockTimer,
                    goHome = { HomeNavigator.goHome(sl.appContext) },
                    onDetectionOutcome = { outcome, minutes ->
                        (triggers as? AccessibilityTriggerSource)?.onOutcome(outcome, minutes)
                    },
                    // Zásahy do Room (121): Zrcadlo, strop a odstup u Zavřít (SPEC 4). Zamítnutá volba se neukládá.
                    onResult = { r ->
                        if (r.accepted) {
                            NarsilDatabase.get(sl.appContext).interventionDao().insert(InterventionRecords.entity(r, System.currentTimeMillis()))
                        }
                    },
                    squatsForUnlock = {
                        val st = sl.settingsRepository.settings.value
                        ExerciseText.squatEquivalent(st.exercise, st.repsPerUnlock())
                    },
                )
                controller = EchoOverlayController(
                    window = EchoOverlayWindow(service),
                    triggers = triggers,
                    movement = sl.movementRepository,
                    contracts = sl.contractRepository,
                    videos = sl.echoVideoRepository,
                    scope = sl.appScope,
                    choiceHandler = handler,
                    squatCounter = sl.squatCounter,
                    pushupCounter = sl.pushupCounter,
                    exercise = { sl.settingsRepository.settings.value.exercise },
                    repsPerUnlock = { ex -> sl.settingsRepository.settings.value.repsPerUnlock(ex) },
                    demoMode = { sl.settingsRepository.settings.value.demoMode },
                    gamification = sl.gamificationEngine,
                    session = echoSession,
                    // Prošlá rozdělaná Ozvěna: okno zmizelo bez volby, detekce smí zasáhnout znovu.
                    onRestoreExpired = { (triggers as? AccessibilityTriggerSource)?.onOutcome(InterventionOutcome.PLANNED_FREE) },
                    onExtraSquats = { n -> (sl.gamificationEngine as? RulesGamificationEngine)?.onExtraSquats(n) },
                ).also { it.start() }
                _ready.value = true
                Log.i(TAG, "Ozvěna připravena (fakes=${sl.useFakes})")
            }
        }
    }
}
