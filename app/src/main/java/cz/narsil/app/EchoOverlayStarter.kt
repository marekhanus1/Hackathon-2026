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
import cz.narsil.overlay.ScrollProgressStyle
import cz.narsil.overlay.EchoStateBuilder
import cz.narsil.overlay.VideoFeedback
import cz.narsil.overlay.VideoFeedbackController
import cz.narsil.core.ComfortContext
import cz.narsil.core.HistoryRating
import cz.narsil.core.data.DayKey
import cz.narsil.core.data.VideoRatingEntity
import kotlinx.coroutines.flow.first
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

    /** Otázka „Co ti to video dalo?“ (156) a její podněty (dosledované video, konec zaplaceného času). */
    private var feedback: VideoFeedbackController? = null
    private var feedbackJob: Job? = null
    private var paidTimeJob: Job? = null

    private val _ready = MutableStateFlow(false)

    /** Ozvěna připravená (běží služba přístupnosti). StateFlow, ať ho Compose sleduje. */
    val isReady: StateFlow<Boolean> = _ready.asStateFlow()

    /** Demo panel (123): ukáže Ozvěnu hned, bez čekání na detekci. false = služba neběží. */
    suspend fun showNow(trigger: InterventionTrigger = FakeInterventionTriggerSource.demoTrigger()): Boolean {
        val c = controller ?: return false
        c.show(trigger)
        return true
    }

    /** Panel po konci videa počká, jestli ve stejné chvíli nepřijde Ozvěna. */
    private const val FEEDBACK_DELAY_MILLIS = 2_000L

    /** Po vypršení zaplaceného času dát detekci tik (5 s) na případnou Ozvěnu. */
    private const val PAID_TIME_GRACE_MILLIS = 8_000L

    /** Všechny odpovědi pro větu v Ozvěně (156). Chyba = žádné. */
    private suspend fun loadRatings(): List<HistoryRating> = runCatching {
        NarsilDatabase.get(ServiceLocator.appContext).videoRatingDao().observeAll().first()
            .map { HistoryRating(it.atMillis, it.title, it.intent, it.rating) }
    }.getOrDefault(emptyList())

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
                // 156: i během sledování videa doma – konec videa spouští otázku „Co ti to video dalo?“.
                combine(source.waitingForVideoEnd, source.watchingVideo) { a, b -> a || b }.distinctUntilChanged().collectLatest { waiting ->
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
                feedbackJob?.cancel()
                paidTimeJob?.cancel()
                feedback?.dismiss()
                feedback = null
                _ready.value = false
                if (service == null) return@collect
                val triggers = sl.interventionTriggerSource
                // Ukazatel blížícího se limitu (135): jen se skutečnou detekcí a zapnutým nastavením.
                val bar = ScrollProgressOverlay(service).also { progressBar = it }
                val percent = (triggers as? AccessibilityTriggerSource)?.progressPercent ?: flowOf(null)
                progressJob = sl.appScope.launch {
                    // Štítek „Ozvěna za 0:15“ (148): zbývající čas z procenta a prahu relace (Settings.toleranceMillis).
                    combine(percent, sl.settingsRepository.settings) { p, s -> p?.takeIf { s.showScrollProgress }?.let { it to s.toleranceMillis() } }
                        .distinctUntilChanged()
                        .collect { v -> bar.update(v?.first, v?.let { ScrollProgressStyle.remainingMillis(it.first, it.second) }) }
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
                        // 156: po vypršení zaplaceného času „Co ti daly ty minuty?“ (když už neběží další Ozvěna).
                        if (r.unlockedMinutes > 0) {
                            val app = EchoStateBuilder.appName(r.trigger.event.packageName)
                            paidTimeJob?.cancel()
                            paidTimeJob = sl.appScope.launch {
                                delay(sl.unlockTimer.remainingMillis() + PAID_TIME_GRACE_MILLIS)
                                feedback?.ask(
                                    title = VideoFeedback.feedTitle(app),
                                    caption = VideoFeedback.feedCaption(r.unlockedMinutes, app),
                                    question = VideoFeedbackController.QUESTION_FEED,
                                )
                            }
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
                    // 153: jen nad aplikací, ze které zásah přišel (null = nevíme → ukázat).
                    isInForeground = { t ->
                        val fg = NarsilAccessibilityService.instance?.foregroundPackage()
                        fg == null || fg == t.event.packageName
                    },
                    onSkipped = { (triggers as? AccessibilityTriggerSource)?.onOutcome(InterventionOutcome.PLANNED_FREE) },
                    // Prošlá rozdělaná Ozvěna: okno zmizelo bez volby, detekce smí zasáhnout znovu.
                    onRestoreExpired = { (triggers as? AccessibilityTriggerSource)?.onOutcome(InterventionOutcome.PLANNED_FREE) },
                    onExtraSquats = { n -> (sl.gamificationEngine as? RulesGamificationEngine)?.onExtraSquats(n) },
                    valueNote = { t ->
                        VideoFeedback.echoNote(t, EchoStateBuilder.appName(t.event.packageName), loadRatings(), DayKey.startMillis(DayKey.today()))
                    },
                    onEchoShown = { feedback?.dismiss() },
                ).also { it.start() }
                val echo = controller
                feedback = VideoFeedbackController(
                    window = EchoOverlayWindow(service, bottomPanel = true),
                    scope = sl.appScope,
                    isEchoShowing = { echo?.isShowing == true },
                    isHome = { sl.contextRepository.context.value == ComfortContext.HOME },
                    dayStartMillis = { DayKey.startMillis(DayKey.today()) },
                    onAnswer = { title, value ->
                        NarsilDatabase.get(sl.appContext).videoRatingDao().insert(
                            VideoRatingEntity(atMillis = System.currentTimeMillis(), title = title, intent = value.label, rating = value.rating),
                        )
                        sl.gamificationEngine.onVideoRated(value.rating)
                    },
                )
                (triggers as? AccessibilityTriggerSource)?.let { source ->
                    feedbackJob = sl.appScope.launch {
                        source.videoFinished.collect { f ->
                            // Konec videa může zároveň spustit Ozvěnu (134, 137): ta má přednost, panel počká na ni.
                            delay(FEEDBACK_DELAY_MILLIS)
                            val title = VideoFeedback.videoTitle(f.title)
                            Log.i(TAG, "dosledováno: „$title“ (${f.watchedMillis / 1000} s, konec=${f.ended})")
                            feedback?.ask(title)
                        }
                    }
                }
                _ready.value = true
                Log.i(TAG, "Ozvěna připravena (fakes=${sl.useFakes})")
            }
        }
    }
}
