package cz.narsil.detection.accessibility

import cz.narsil.core.ComfortContext
import cz.narsil.core.ConsumptionEvent
import cz.narsil.core.ContextRepository
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.InterventionTriggerSource
import cz.narsil.core.SettingsRepository
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
        val hit = tracker.observe(snapshot.packageName, surface, context, now, thresholdMillis(), scrolled)
        val session = tracker.session
        _current.value = session?.takeIf { surface?.isEndless == true && context == ComfortContext.HOME }?.let {
            ConsumptionEvent(
                packageName = it.packageName,
                surface = it.surface,
                startedAtMillis = it.startedAtMillis,
                durationMillis = now - it.startedAtMillis,
                url = snapshot.url,
            )
        }
        if (hit != null) {
            val event = _current.value ?: return@withLock
            _triggers.tryEmit(InterventionTrigger(event, TriggerReason.ENDLESS_SCROLL, now))
        }
    }

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
    }

    companion object {
        const val DEFAULT_THRESHOLD_MILLIS = 5 * 60_000L
    }
}
