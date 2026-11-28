package cz.narsil.detection.accessibility

import cz.narsil.core.ComfortContext
import cz.narsil.core.Surface
import cz.narsil.detection.youtube.VideoEndGate
import cz.narsil.detection.youtube.VideoPlayback

/**
 * Heuristika relace nekonečného obsahu (SPEC 3). Čistý Kotlin, bez času ze systému.
 *
 * - Relace = souvislý čas na nekonečném povrchu (Instagram ↔ TikTok ji nepřeruší).
 * - Pauza > [pauseMillis] mimo nekonečný povrch relaci ukončí.
 * - Zásah, když relace ≥ práh a kontext je HOME. MESSAGES, AWAY, UNKNOWN nikdy.
 * - Během zaplaceného času ([unlockedUntil]) se nezasahuje; po vypršení hned, pokud relace pokračuje.
 * - Po CLOSED ([onClosed]) nová relace od nuly.
 * - Odkaz z chatu: video otevřené hned po zprávách se nepočítá, dokud uživatel neposune na další.
 * - Video (134): VIDEO relaci nezačne, ale probíhající relaci prodlužuje (ze Shorts/feedu na video).
 *   Když limit vyprší na videu, zásah počká na konec videa / jiné video ([VideoEndGate]); odchod z videa
 *   na nekonečný povrch nebo jinou stránku YouTube ([observe] `onVideoSite`) zasáhne hned.
 */
class ScrollSessionTracker(
    private val pauseMillis: Long = 2 * 60_000,
    private val videoGate: VideoEndGate = VideoEndGate(),
) {
    data class Session(val startedAtMillis: Long, val packageName: String, val surface: Surface)

    var session: Session? = null
        private set
    private var lastEndlessAt = Long.MIN_VALUE
    private var lastSurface: Surface? = null
    private var fromChatPending = false
    private var awaitingOutcome = false
    private var unlockedUntil = 0L

    /**
     * Zpracuje pozorování (snímek nebo tik). Vrací relaci, pro kterou má přijít zásah, jinak null.
     *
     * @param surface povrch, null = mimo sledované (plocha, jiná aplikace, chat)
     * @param scrolled pozorování vzniklo scrollem (posun na další obsah)
     * @param video stav přehrávače na povrchu VIDEO (134), null = nečitelný
     * @param onVideoSite pozorování je z YouTube (aplikace nebo youtube.com ve Firefoxu)
     */
    fun observe(
        packageName: String,
        surface: Surface?,
        context: ComfortContext,
        now: Long,
        thresholdMillis: Long,
        scrolled: Boolean = false,
        video: VideoPlayback? = null,
        onVideoSite: Boolean = false,
    ): Session? {
        val prevSurface = lastSurface
        lastSurface = surface
        if (surface == null || !counts(surface, context, now)) {
            if (surface == Surface.MESSAGES || (surface == null && WatchedApp.isIgnored(packageName))) {
                // Stránky YouTube mimo video (domů, hledání) jsou MESSAGES jen „v nejistotě“, nejde o chat.
                if (!onVideoSite) fromChatPending = true
            }
            if (session != null && now - lastEndlessAt > pauseMillis) endSession()
            // Limit vypršel během videa a uživatel z videa odešel na jinou stránku YouTube: hranice videa.
            if (videoGate.isWaiting && onVideoSite && surface != Surface.VIDEO && context == ComfortContext.HOME) {
                val s = session ?: return null
                if (!awaitingOutcome && now >= unlockedUntil) return fire(s)
            }
            return null
        }
        // Kontext mimo domov: nic nepočítat, relace se neudržuje.
        if (context != ComfortContext.HOME) {
            endSession()
            return null
        }
        if (fromChatPending) {
            if (prevSurface == null || !prevSurface.isEndless || !scrolled) {
                lastEndlessAt = now
                return null
            }
            fromChatPending = false
        }
        val current = session
        if (current == null || now - lastEndlessAt > pauseMillis) {
            session = Session(now, packageName, surface)
            awaitingOutcome = false
            videoGate.reset()
        } else if (current.packageName != packageName || current.surface != surface) {
            session = current.copy(packageName = packageName, surface = surface)
        }
        lastEndlessAt = now
        val s = session ?: return null
        if (awaitingOutcome || now < unlockedUntil) return null
        if (now - s.startedAtMillis < thresholdMillis) return null
        if (surface == Surface.VIDEO && !videoGate.mayIntervene(video, now)) return null
        return fire(s)
    }

    /** Limit vypršel na videu a zásah čeká na jeho konec (134). Pro UI a ladění. */
    val isWaitingForVideoEnd: Boolean get() = videoGate.isWaiting

    /**
     * Počítá se [surface] do relace? Nekonečný povrch vždy; VIDEO jen jako pokračování živé relace doma
     * (samotné video, např. odkaz z chatu, relaci nezačne).
     */
    private fun counts(surface: Surface, context: ComfortContext, now: Long): Boolean =
        surface.isEndless ||
            (surface == Surface.VIDEO && context == ComfortContext.HOME && session != null && now - lastEndlessAt <= pauseMillis)

    private fun fire(s: Session): Session {
        videoGate.reset()
        awaitingOutcome = true
        return s
    }

    private fun endSession() {
        session = null
        videoGate.reset()
    }

    /** Délka relace v [now], 0 bez relace. */
    fun durationMillis(now: Long): Long = session?.let { now - it.startedAtMillis } ?: 0

    /** Zavřít: nová relace od nuly. */
    fun onClosed() {
        endSession()
        awaitingOutcome = false
        unlockedUntil = 0
    }

    /** Zaplaceno / odemčeno do [untilMillis]: do té doby bez zásahu, pak hned znovu. */
    fun onUnlocked(untilMillis: Long) {
        unlockedUntil = untilMillis
        awaitingOutcome = false
        videoGate.reset()
    }

    /** Okno zavřeno bez volby (např. systémem): dovolí další zásah. */
    fun onDismissed() {
        awaitingOutcome = false
        videoGate.reset()
    }
}
