package cz.narsil.detection.accessibility

import cz.narsil.core.ComfortContext
import cz.narsil.core.Surface

/**
 * Heuristika relace nekonečného obsahu (SPEC 3). Čistý Kotlin, bez času ze systému.
 *
 * - Relace = souvislý čas na nekonečném povrchu (Instagram ↔ TikTok ji nepřeruší).
 * - Pauza > [pauseMillis] mimo nekonečný povrch relaci ukončí.
 * - Zásah, když relace ≥ práh a kontext je HOME. MESSAGES, AWAY, UNKNOWN nikdy.
 * - Během zaplaceného času ([unlockedUntil]) se nezasahuje; po vypršení hned, pokud relace pokračuje.
 * - Po CLOSED ([onClosed]) nová relace od nuly.
 * - Odkaz z chatu: video otevřené hned po zprávách se nepočítá, dokud uživatel neposune na další.
 */
class ScrollSessionTracker(
    private val pauseMillis: Long = 2 * 60_000,
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
     */
    fun observe(
        packageName: String,
        surface: Surface?,
        context: ComfortContext,
        now: Long,
        thresholdMillis: Long,
        scrolled: Boolean = false,
    ): Session? {
        val prevSurface = lastSurface
        lastSurface = surface
        if (surface == null || !surface.isEndless) {
            if (surface == Surface.MESSAGES || (surface == null && WatchedApp.isIgnored(packageName))) {
                fromChatPending = true
            }
            if (session != null && now - lastEndlessAt > pauseMillis) session = null
            return null
        }
        // Kontext mimo domov: nic nepočítat, relace se neudržuje.
        if (context != ComfortContext.HOME) {
            session = null
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
        } else if (current.packageName != packageName || current.surface != surface) {
            session = current.copy(packageName = packageName, surface = surface)
        }
        lastEndlessAt = now
        val s = session ?: return null
        if (awaitingOutcome || now < unlockedUntil) return null
        if (now - s.startedAtMillis < thresholdMillis) return null
        awaitingOutcome = true
        return s
    }

    /** Délka relace v [now], 0 bez relace. */
    fun durationMillis(now: Long): Long = session?.let { now - it.startedAtMillis } ?: 0

    /** Zavřít: nová relace od nuly. */
    fun onClosed() {
        session = null
        awaitingOutcome = false
        unlockedUntil = 0
    }

    /** Zaplaceno / odemčeno do [untilMillis]: do té doby bez zásahu, pak hned znovu. */
    fun onUnlocked(untilMillis: Long) {
        unlockedUntil = untilMillis
        awaitingOutcome = false
    }

    /** Okno zavřeno bez volby (např. systémem): dovolí další zásah. */
    fun onDismissed() {
        awaitingOutcome = false
    }
}
