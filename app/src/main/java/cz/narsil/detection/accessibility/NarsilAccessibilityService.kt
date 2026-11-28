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
            if (kind == SnapshotKind.WINDOW_CHANGED && throttle.shouldSnapshot(pkg, kind, now)) {
                AccessibilityEvents.emit(ForegroundSnapshot(pkg, null, kind, now))
                Log.d(TAG, "popředí: ${if (WatchedApp.isIgnored(pkg)) "chat (ignorováno)" else pkg}")
            }
            return
        }

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
        val root = rootInActiveWindow
        val dumpNow = dumpTree && kind == SnapshotKind.WINDOW_CHANGED
        var result = root?.let { runCatching { WindowReader.read(it, app, dumpToLog = dumpNow, fallbackUrl = lastFirefoxUrl) }.getOrNull() }
        // Přepnutí záložky v aplikaci nevyvolá WINDOW_CHANGED: při změně záložky vypsat strom znovu.
        if (result != null && result.selectedTab != lastSelectedTab) {
            lastSelectedTab = result.selectedTab
            if (dumpTree && !dumpNow) {
                Log.d(TAG, "záložka: ${result.selectedTab}")
                result = runCatching { WindowReader.read(root, app, dumpToLog = true, fallbackUrl = lastFirefoxUrl) }.getOrNull() ?: result
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
    }

    /** Stav přehrávače YouTube (134): aplikace z textů okna, Firefox jen na stránce jednoho videa. */
    private fun videoOf(app: WatchedApp, r: WindowReader.Result, now: Long): VideoPlayback? = when (app) {
        WatchedApp.YOUTUBE -> YouTubePlayback.parse(null, r.texts, now)
        WatchedApp.FIREFOX -> YouTubePlayback.videoIdFromUrl(r.url)?.let {
            YouTubePlayback.parse(it, r.playerTexts, now).copy(title = YouTubePlayback.titleFromPage(r.pageTitle))
        }
        else -> null
    }

    /** Scroll bez obsahu (149): třída prvku, posun, indexy položek. */
    private fun scrollSample(e: AccessibilityEvent) = ScrollSample(
        className = e.className?.toString(),
        deltaX = e.scrollDeltaX,
        deltaY = e.scrollDeltaY,
        fromIndex = e.fromIndex,
        toIndex = e.toIndex,
        itemCount = e.itemCount,
    )

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
