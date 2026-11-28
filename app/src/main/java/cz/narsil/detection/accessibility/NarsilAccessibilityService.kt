package cz.narsil.detection.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import cz.narsil.detection.youtube.VideoPlayback
import cz.narsil.detection.youtube.YouTubePlayback
import java.lang.ref.WeakReference

/**
 * Detekce (110): sleduje změny oken a scrollování, zjistí aplikaci v popředí, URL ve Firefoxu
 * a ID prvků obrazovky. Výstup jde do [AccessibilityEvents]; klasifikaci dělá 111.
 *
 * Chaty ([WatchedApp.IGNORED_PACKAGES]) a nesledované aplikace se nečtou – jen se zapíše balíček,
 * aby 111 poznal, že sledovaná aplikace opustila popředí.
 *
 * Ladění: `adb logcat -s NarsilA11y NarsilA11yTree` (strom se vypisuje při změně okna, když [dumpTree]).
 */
class NarsilAccessibilityService : AccessibilityService() {
    private val throttle = SnapshotThrottle()
    private var lastClassName: String? = null
    private var lastPackage: String? = null
    private var lastSelectedTab: String? = null
    /** Poslední URL Firefoxu; platí, dokud je Firefox v popředí (schovaný adresní řádek, 134). */
    private var lastFirefoxUrl: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this) // dřív než attach: posluchači hned čtou instance
        AccessibilityEvents.attach(this)
        Log.i(TAG, "služba připojena (${System.identityHashCode(this)})")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // vlastní okna (Ozvěna) ignorovat
        val kind = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> SnapshotKind.WINDOW_CHANGED
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> SnapshotKind.SCROLLED
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> SnapshotKind.CONTENT_CHANGED
            else -> return
        }
        if (kind == SnapshotKind.WINDOW_CHANGED) lastClassName = event.className?.toString()
        if (pkg != lastPackage) {
            if (kind != SnapshotKind.WINDOW_CHANGED) lastClassName = null
            lastPackage = pkg
        }
        val now = System.currentTimeMillis()
        // 149: neznámá aplikace jako OTHER jen se zapnutou volbou „Další aplikace s nekonečným feedem“.
        val app = WatchedApp.of(pkg) ?: WatchedApp.OTHER.takeIf { trackOtherApps && WatchedApp.isOtherCandidate(pkg, packageName) }
        val scroll = if (kind == SnapshotKind.SCROLLED) scrollSample(event) else null
        if (app != WatchedApp.FIREFOX && kind == SnapshotKind.WINDOW_CHANGED) lastFirefoxUrl = null

        if (app == null) {
            // Chat, plocha, jiná aplikace: jen balíček a jen při změně okna, obsah se nečte.
            // 149: překryvné okno jiné služby přístupnosti (Minimalist launcher), systému nebo klávesnice
            // nad sledovanou aplikací není změna popředí – jinak by přerušilo relaci.
            if (kind == SnapshotKind.WINDOW_CHANGED && !isAppWindow(event)) {
                Log.d(TAG, "okno $pkg není aplikace (typ ${windowType(event)}), popředí se nemění")
                return
            }
            if (kind == SnapshotKind.WINDOW_CHANGED && throttle.shouldSnapshot(pkg, kind, now)) {
                AccessibilityEvents.emit(ForegroundSnapshot(pkg, null, kind, now))
                Log.d(TAG, "popředí: ${if (WatchedApp.isIgnored(pkg)) "chat (ignorováno)" else pkg}")
            }
            return
        }

        if (scroll != null && app.generic) logScroll(pkg, scroll, now)
        // Scroll se hlásí vždy (heuristika 111 počítá aktivitu), strom se čte omezeně.
        val readTree = throttle.shouldSnapshot(pkg, kind, now)
        if (!readTree) {
            if (kind == SnapshotKind.SCROLLED) {
                val prev = AccessibilityEvents.latest.value
                AccessibilityEvents.emit(
                    if (prev != null && prev.packageName == pkg) prev.copy(kind = kind, atMillis = now, scroll = scroll)
                    else ForegroundSnapshot(pkg, app, kind, now, lastClassName, scroll = scroll),
                )
            }
            return
        }
        lastWatchedPackage = pkg
        readAndEmit(pkg, app, kind, now, scroll)
    }

    /** Naposledy sledovaná aplikace v popředí (pro [pollForeground]). */
    @Volatile
    private var lastWatchedPackage: String? = null

    /**
     * Přečte okno sledované aplikace bez události (134): YouTube během přehrávání nehlásí změny,
     * end-screen by se jinak nepoznal. Volá `EchoOverlayStarter` každé ~2,5 s, jen když brána čeká na konec videa.
     */
    fun pollForeground() {
        val pkg = lastWatchedPackage ?: return
        val app = WatchedApp.of(pkg) ?: return
        val now = System.currentTimeMillis()
        val snapshot = readAndEmit(pkg, app, SnapshotKind.CONTENT_CHANGED, now, scroll = null, requireApp = true) ?: return
        Log.d(TAG, "tik okna $pkg: povrch=${SurfaceClassifier.classify(snapshot)} video=${snapshot.video}")
    }

    private fun readAndEmit(
        pkg: String,
        app: WatchedApp,
        kind: SnapshotKind,
        now: Long,
        scroll: ScrollSample?,
        requireApp: Boolean = false,
    ): ForegroundSnapshot? {
        val root = appRoot(pkg)
        // Tik: aplikace už není v popředí (okno nenalezeno) → nic, další událost řekne, kde uživatel je.
        if (requireApp && root?.packageName?.toString() != pkg) return null
        val dumpNow = dumpTree && kind == SnapshotKind.WINDOW_CHANGED
        var result = root?.let { runCatching { WindowReader.read(it, app, dumpToLog = dumpNow, fallbackUrl = lastFirefoxUrl) }.getOrNull() }
        // Přepnutí záložky v aplikaci nevyvolá WINDOW_CHANGED: při změně záložky vypsat strom znovu.
        if (result != null && result.selectedTab != lastSelectedTab) {
            lastSelectedTab = result.selectedTab
            if (dumpTree && !dumpNow) {
                Log.d(TAG, "záložka: ${result.selectedTab}")
                result = root?.let { runCatching { WindowReader.read(it, app, dumpToLog = true, fallbackUrl = lastFirefoxUrl) }.getOrNull() } ?: result
            }
        }
        if (app == WatchedApp.FIREFOX) lastFirefoxUrl = result?.url ?: lastFirefoxUrl
        val snapshot = ForegroundSnapshot(
            packageName = pkg,
            app = app,
            kind = kind,
            atMillis = now,
            className = lastClassName,
            url = result?.url,
            viewIds = result?.viewIds.orEmpty(),
            texts = result?.texts.orEmpty(),
            selectedTab = result?.selectedTab,
            video = result?.let { videoOf(app, it, now) },
            editFields = result?.editFields.orEmpty(),
            scroll = scroll,
        )
        AccessibilityEvents.emit(snapshot)
        snapshot.video?.takeIf { it.ended || it.durationMillis != null || kind == SnapshotKind.WINDOW_CHANGED }?.let { Log.d(TAG, "video: $it") }
        if (kind == SnapshotKind.WINDOW_CHANGED || snapshot.url != null) {
            Log.d(TAG, "popředí: $app ($pkg) okno=${snapshot.className} tab=${snapshot.selectedTab} url=${snapshot.url} ids=${snapshot.viewIds.size} povrch=${SurfaceClassifier.classify(snapshot)} (${SurfaceClassifier.explain(snapshot)})")
        }
        return snapshot
    }

    /** Stav přehrávače YouTube (134): aplikace z textů okna, Firefox jen na stránce jednoho videa. */
    private fun videoOf(app: WatchedApp, r: WindowReader.Result, now: Long): VideoPlayback? = when (app) {
        // Krátké texty celého okna (čas, end-screen); prvních 20 textů bývá jen lišta a název.
        WatchedApp.YOUTUBE -> YouTubePlayback.parse(null, r.playerTexts.ifEmpty { r.texts }, now)
        WatchedApp.FIREFOX -> YouTubePlayback.videoIdFromUrl(r.url)?.let {
            YouTubePlayback.parse(it, r.playerTexts, now).copy(title = YouTubePlayback.titleFromPage(r.pageTitle))
        }
        else -> null
    }

    /** Orientace posouvaných prvků podle [scrollKey] (uzel se čte jen poprvé – je to dotaz do cizí aplikace). */
    private val orientationCache = object : LinkedHashMap<Int, ScrollOrientation?>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ScrollOrientation?>?) = size > 64
    }

    /** Prvek bez dotazu do aplikace: okno + třída + počet položek (feed a karusel v něm se liší). */
    private fun scrollKey(e: AccessibilityEvent): Int =
        java.util.Objects.hash(e.windowId, e.className?.toString(), e.itemCount).let { if (it == 0) 1 else it }

    /** Scroll bez obsahu (149): třída prvku, posun, indexy položek, pozice; prvek a jeho orientace (bez textu). */
    private fun scrollSample(e: AccessibilityEvent): ScrollSample {
        val key = scrollKey(e)
        val orientation = if (orientationCache.containsKey(key)) {
            orientationCache[key]
        } else {
            runCatching { e.source?.let(ScrollOrientations::of) }.getOrNull().also { orientationCache[key] = it }
        }
        return ScrollSample(
            className = e.className?.toString(),
            sourceKey = key,
            orientation = orientation,
            deltaX = e.scrollDeltaX,
            deltaY = e.scrollDeltaY,
            fromIndex = e.fromIndex,
            toIndex = e.toIndex,
            itemCount = e.itemCount,
            scrollY = e.scrollY.takeIf { e.maxScrollY > 0 || it > 0 } ?: -1,
        )
    }

    private var lastScrollLogAt = 0L

    /** Scroll v obecné aplikaci do logu, nejvýš 1× za s (ladění 149). */
    private fun logScroll(pkg: String, s: ScrollSample, now: Long) {
        if (now - lastScrollLogAt < 1_000) return
        lastScrollLogAt = now
        Log.d(TAG, "scroll: $pkg ${s.className?.substringAfterLast('.')} dY=${s.deltaY} dX=${s.deltaX} y=${s.scrollY} " +
            "položky=${s.fromIndex}–${s.toIndex}/${s.itemCount}")
    }

    /**
     * Kořen okna aplikace [pkg] (149). Nad aplikací může viset cizí překryv s fokusem (dialog Minimalist
     * launcheru „Jak dlouho chcete být na X?“), [rootInActiveWindow] by pak vrátil jeho strom.
     * Proto okno TYPE_APPLICATION s balíčkem [pkg] (nejvyšší vrstva); jinak aktivní okno jako dřív.
     */
    private fun appRoot(pkg: String): android.view.accessibility.AccessibilityNodeInfo? {
        val active = rootInActiveWindow
        if (active?.packageName?.toString() == pkg) return active
        val fromWindows = runCatching {
            windows.filter { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }
                .sortedByDescending { it.layer }
                .firstNotNullOfOrNull { w -> w.root?.takeIf { it.packageName?.toString() == pkg } }
        }.getOrNull()
        if (fromWindows != null && active != null) {
            Log.d(TAG, "strom z okna $pkg, aktivní je překryv ${active.packageName}")
        }
        return fromWindows ?: active
    }

    /**
     * Aplikace v popředí podle oken (153): okno TYPE_APPLICATION s fokusem, jinak aktivní, jinak nejvyšší.
     * Překryvy (Ozvěna, Minimalist launcher, systém) se nepočítají. null = nezjištěno.
     */
    fun foregroundPackage(): String? = runCatching {
        val apps = windows.filter { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }
        val w = apps.firstOrNull { it.isFocused } ?: apps.firstOrNull { it.isActive } ?: apps.maxByOrNull { it.layer }
        w?.root?.packageName?.toString()
    }.getOrNull()

    /** Typ okna události (getWindows, flagRetrieveInteractiveWindows), null = nezjištěno. */
    private fun windowType(e: AccessibilityEvent): Int? =
        runCatching { windows.firstOrNull { it.id == e.windowId }?.type }.getOrNull()

    /** Okno aplikace (nebo nezjištěno). Překryv služby přístupnosti, systém, klávesnice → false. */
    private fun isAppWindow(e: AccessibilityEvent): Boolean {
        val type = windowType(e)
        if (type != null) return type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION
        // Okno v seznamu není: když má fokus pořád sledovaná aplikace, je tohle překryv nad ní.
        val active = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
        return active == null || active == e.packageName?.toString() || WatchedApp.of(active) == null
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.i(TAG, "služba odpojena (${System.identityHashCode(this)})")
        AccessibilityEvents.detach(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.i(TAG, "služba zničena (${System.identityHashCode(this)})")
        // Stará instance po přeinstalaci se ničí až po připojení nové: odpojit jen sebe.
        AccessibilityEvents.detach(this)
        if (instanceRef.get() === this) instanceRef = WeakReference(null)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "NarsilA11y"

        /** „Další aplikace s nekonečným feedem“ (149), nastavuje `EchoOverlayStarter` podle `Settings`. */
        @Volatile
        var trackOtherApps: Boolean = false

        /** Vypisovat strom okna do logu (ladění ID prvků pro 111). Vypnout pro demo. */
        @Volatile
        var dumpTree: Boolean = true

        @Volatile
        private var instanceRef = WeakReference<NarsilAccessibilityService>(null)

        /** Běžící služba (null = vypnutá). Pro okno Ozvěny (TYPE_ACCESSIBILITY_OVERLAY) a návrat na plochu (114). */
        val instance: NarsilAccessibilityService? get() = instanceRef.get()

        /** Pošle aplikaci v popředí do pozadí (plocha). false = služba neběží. */
        fun goHome(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false

        /** Je služba zapnutá v nastavení systému? (onboarding 122) */
        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, NarsilAccessibilityService::class.java)
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
        }
    }
}
