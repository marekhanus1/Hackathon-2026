package cz.narsil.detection.accessibility

import cz.narsil.core.ComfortContext
import cz.narsil.core.ConsumptionEvent
import cz.narsil.core.ContextRepository
import cz.narsil.core.InterventionOutcome
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.InterventionTriggerSource
import cz.narsil.core.SettingsRepository
import cz.narsil.core.Surface
import cz.narsil.detection.youtube.VideoChainTracker
import cz.narsil.detection.youtube.YouTubePlayback
import kotlinx.coroutines.withTimeoutOrNull
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
 * Práh: `Settings.toleranceMillis()` (demo práh 20 s má přednost, jinak tolerance z Nastavení, výchozí 5 min).
 * Aplikace vypnutá v Nastavení (`isTracked`) a pozastavená ochrana (`isProtectionActive`) se berou jako
 * „mimo sledované“: nic se nepočítá, žádný zásah ani ukazatel (136).
 * Řetězení videí (137): [VideoChainTracker], limit `Settings.videoChainLimit`, plánované video podle [planCheck].
 * Instanci vytvoří ServiceLocator (124) a overlay (114) hlásí výsledek přes [onOutcome].
 */
class AccessibilityTriggerSource(
    private val contextRepository: ContextRepository,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
    snapshots: SharedFlow<ForegroundSnapshot> = AccessibilityEvents.snapshots,
    private val clock: () -> Long = System::currentTimeMillis,
    tickMillis: Long = 5_000,
    /** Je video s tímto názvem v dnešním plánu? (ContentMatcher, 137) null = vše neplánované. Nesmí házet. */
    private val planCheck: (suspend (title: String) -> Boolean)? = null,
    /** Čekání na titulek nového videu před dotazem (Firefox ho mění se zpožděním po změně URL). */
    private val planDelayMillis: Long = 2_000,
    /** Ladicí výpis (149): změny obecné detekce feedu. ServiceLocator → Log.d. */
    private val onDebug: (String) -> Unit = {},
) : InterventionTriggerSource {
    private val tracker = ScrollSessionTracker()
    private val chain = VideoChainTracker()
    private val feedBehavior = FeedBehavior()
    private val lastGeneric = HashMap<String, Surface?>()
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
        val now = snapshot.atMillis
        val prefs = settings.settings.value
        val active = snapshot.app?.let { prefs.isTracked(it.name) } == true && prefs.isProtectionActive(now)
        val generic = snapshot.app?.generic == true
        // 149: obecné aplikace – feed podle chování scrollu; zprávy a hledání rozhodne snímek.
        if (active && generic && scrolled) snapshot.scroll?.let { feedBehavior.onScroll(snapshot.packageName, it, now) }
        val surface = when {
            !active -> null
            generic -> SurfaceClassifier.classify(snapshot) ?: feedBehavior.surface(snapshot.packageName, now)
            else -> SurfaceClassifier.classify(snapshot)
        }
        if (active && generic && lastGeneric.put(snapshot.packageName, surface) != surface) {
            onDebug("obecná detekce ${snapshot.packageName}: $surface (${feedBehavior.describe(snapshot.packageName)})")
        }
        val context = contextRepository.context.value
        val threshold = prefs.toleranceMillis()
        val hit = tracker.observe(
            snapshot.packageName, surface, context, now, threshold, scrolled,
            video = snapshot.video,
            onVideoSite = active && isVideoSite(snapshot),
            startedAtHint = if (generic) feedBehavior.streakStart(snapshot.packageName, now) else null,
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
        // Řetězení (137): přehrávač jednoho videa doma ve sledované aplikaci (surface je null mimo sledované).
        val newVideo = chain.observe(surface == Surface.VIDEO && context == ComfortContext.HOME, snapshot.video, now)
        if (newVideo != null) scope.launch { evaluate(newVideo, snapshot) }
    }

    /** Je nové video plánované? Pak ho započítá a případně zasáhne (na začátku videa = hranice). */
    private suspend fun evaluate(video: VideoChainTracker.Video, seen: ForegroundSnapshot) {
        var title = seen.video?.title
        val planned = planCheck?.let { check ->
            delay(planDelayMillis)
            last?.video?.takeIf { it.videoKey == video.key }?.title?.let { title = it }
            title?.let { t -> withTimeoutOrNull(PLAN_TIMEOUT_MILLIS) { runCatching { check(t) }.getOrDefault(false) } }
        } ?: false
        lock.withLock {
            val now = clock()
            if (!chain.resolve(video, planned, settings.settings.value.videoChainLimit, now)) return@withLock
            val start = chain.chainStartedAtMillis ?: video.startedAtMillis
            val snap = last ?: seen
            val event = ConsumptionEvent(
                packageName = snap.packageName,
                surface = Surface.VIDEO,
                startedAtMillis = start,
                durationMillis = now - start,
                videoTitle = title,
                url = snap.url,
            )
            _triggers.tryEmit(InterventionTrigger(event, TriggerReason.UNPLANNED_VIDEO_CHAIN, now))
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

    /** Výsledek Ozvěny (114). [unlockMinutes] = odemčené minuty u PAID_STEPS, SQUATS, BORROWED. */
    suspend fun onOutcome(outcome: InterventionOutcome, unlockMinutes: Int = 10) = lock.withLock {
        when (outcome) {
            InterventionOutcome.CLOSED -> {
                tracker.onClosed()
                chain.onClosed()
            }
            InterventionOutcome.PAID_STEPS, InterventionOutcome.SQUATS, InterventionOutcome.BORROWED -> {
                val until = clock() + unlockMinutes * 60_000L
                tracker.onUnlocked(until)
                chain.onUnlocked(until)
            }
            InterventionOutcome.PLANNED_FREE -> {
                tracker.onDismissed()
                chain.onDismissed()
            }
        }
        _progressPercent.value = null // do dalšího pozorování (snímek nebo tik) skrytý
    }

    companion object {
        const val DEFAULT_THRESHOLD_MILLIS = 5 * 60_000L

        /** ContentMatcher má vlastní timeout 3 s; tohle je pojistka. */
        private const val PLAN_TIMEOUT_MILLIS = 5_000L

        /** 0..1 → celá procenta (dolů, 100 jen při plném limitu). */
        fun toPercent(fraction: Float): Int = (fraction * 100).toInt().coerceIn(0, 100)
    }
}
