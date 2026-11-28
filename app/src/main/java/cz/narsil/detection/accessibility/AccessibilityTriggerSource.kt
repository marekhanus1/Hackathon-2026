package cz.narsil.detection.accessibility

import cz.narsil.core.ComfortContext
import cz.narsil.core.ConsumptionEvent
import cz.narsil.core.ContextRepository
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.InterventionTriggerSource
import cz.narsil.core.SettingsRepository
import cz.narsil.core.Surface
import cz.narsil.detection.youtube.YouTubePlayback
import cz.narsil.core.TriggerReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Skutečný zdroj zásahů (111): snímky 110 → [SurfaceClassifier] → [ScrollSessionTracker] → [triggers].
 * Práh: `Settings.demoThresholdSec` (demo 20 s), jinak [DEFAULT_THRESHOLD_MILLIS] (5 min).
 * Instanci vytvoří ServiceLocator (124) a overlay (114) hlásí výsledek přes [onOutcome].
 */
class AccessibilityTriggerSource(
    private val contextRepository: ContextRepository,
    private val settings: SettingsRepository,
    scope: CoroutineScope,
    snapshots: SharedFlow<ForegroundSnapshot> = AccessibilityEvents.snapshots,
    private val clock: () -> Long = System::currentTimeMillis,
    tickMillis: Long = 5_000,
) : InterventionTriggerSource {
    private val tracker = ScrollSessionTracker()
    private val lock = Mutex()
    private var last: ForegroundSnapshot? = null

    private val _triggers = MutableSharedFlow<InterventionTrigger>(extraBufferCapacity = 8)
    override val triggers: Flow<InterventionTrigger> = _triggers.asSharedFlow()

    private val _current = MutableStateFlow<ConsumptionEvent?>(null)

    /** Probíhající úsek nekonečného obsahu (pro ukládání v 121 a UI); null = žádná relace. */
    val current: StateFlow<ConsumptionEvent?> = _current.asStateFlow()

    private val _progressPercent = MutableStateFlow<Int?>(null)

    /**
     * Ukazatel blížícího se limitu (135): 0–100 % délky relace vůči prahu, null = skrýt.
     * Zaokrouhleno na celá procenta: StateFlow vydá novou hodnotu (a overlay překreslí) jen při změně.
     */
    val progressPercent: StateFlow<Int?> = _progressPercent.asStateFlow()

    init {
        scope.launch { snapshots.collect { handle(it, scrolled = it.kind == SnapshotKind.SCROLLED) } }
        // Tik: video může běžet bez událostí (autoplay), čas relace musí běžet dál.
        scope.launch {
            while (isActive) {
                delay(tickMillis)
                last?.let { handle(it.copy(atMillis = clock()), scrolled = false) }
            }
        }
    }

    private suspend fun handle(snapshot: ForegroundSnapshot, scrolled: Boolean) = lock.withLock {
        last = snapshot
        val surface = SurfaceClassifier.classify(snapshot)
        val now = snapshot.atMillis
        val context = contextRepository.context.value
        val threshold = thresholdMillis()
        val hit = tracker.observe(
            snapshot.packageName, surface, context, now, threshold, scrolled,
            video = snapshot.video,
            onVideoSite = isVideoSite(snapshot),
        )
        _progressPercent.value = tracker.progress(now, threshold, context)?.let(::toPercent)
        val session = tracker.session
        // Relace běží na nekonečném povrchu, nebo pokračuje na videu (134).
        val counting = surface != null && context == ComfortContext.HOME &&
            (surface.isEndless || (surface == Surface.VIDEO && session?.surface == Surface.VIDEO))
        _current.value = session?.takeIf { counting }?.let { eventOf(it, now, snapshot) }
        if (hit != null) {
            // Zásah na hranici videa může přijít z jiné stránky YouTube (feed): událost z relace.
            val event = _current.value ?: eventOf(hit, now, snapshot)
            _triggers.tryEmit(InterventionTrigger(event, TriggerReason.ENDLESS_SCROLL, now))
        }
    }

    private fun eventOf(s: ScrollSessionTracker.Session, now: Long, snapshot: ForegroundSnapshot) = ConsumptionEvent(
        packageName = s.packageName,
        surface = s.surface,
        startedAtMillis = s.startedAtMillis,
        durationMillis = now - s.startedAtMillis,
        url = snapshot.url,
    )

    private fun isVideoSite(s: ForegroundSnapshot): Boolean =
        s.app == WatchedApp.YOUTUBE || (s.app == WatchedApp.FIREFOX && YouTubePlayback.isYouTubeUrl(s.url))

    private fun thresholdMillis(): Long =
        settings.settings.value.demoThresholdSec?.let { it * 1_000L } ?: DEFAULT_THRESHOLD_MILLIS

    /** Výsledek Ozvěny (114). [unlockMinutes] = odemčené minuty u PAID_STEPS, SQUATS, BORROWED. */
    suspend fun onOutcome(outcome: InterventionOutcome, unlockMinutes: Int = 10) = lock.withLock {
        when (outcome) {
            InterventionOutcome.CLOSED -> tracker.onClosed()
            InterventionOutcome.PAID_STEPS, InterventionOutcome.SQUATS, InterventionOutcome.BORROWED ->
                tracker.onUnlocked(clock() + unlockMinutes * 60_000L)
            InterventionOutcome.PLANNED_FREE -> tracker.onDismissed()
        }
        _progressPercent.value = null // do dalšího pozorování (snímek nebo tik) skrytý
    }

    companion object {
        const val DEFAULT_THRESHOLD_MILLIS = 5 * 60_000L

        /** 0..1 → celá procenta (dolů, 100 jen při plném limitu). */
        fun toPercent(fraction: Float): Int = (fraction * 100).toInt().coerceIn(0, 100)
    }
}
