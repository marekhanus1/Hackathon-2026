package cz.narsil.detection.youtube

/**
 * Ozvěna nesmí vyskočit uprostřed videa (134, docs/decisions.md). Když limit vyprší na povrchu VIDEO,
 * brána drží zásah, dokud nenastane hranice:
 *
 * - konec videa ([VideoPlayback.ended]: „Přehrát znovu“, odpočet autoplay, pozice = délka),
 * - jiné video (jiný [VideoPlayback.videoKey], tj. jiné `v=` ve Firefoxu),
 * - odhadnutý konec: pozice a délka z ovládání přehrávače + [graceMillis] (ovládání je vidět jen občas),
 * - pojistka: [maxWaitMillis] od vypršení limitu; když délku videa nikdy nepřečteme, [unknownWaitMillis].
 *
 * Odchod z videa (Shorts, feed, jiná stránka YouTube) řeší [cz.narsil.detection.accessibility.ScrollSessionTracker].
 * Čistý Kotlin, čas jen z parametrů.
 */
class VideoEndGate(
    private val maxWaitMillis: Long = MAX_WAIT_MILLIS,
    private val unknownWaitMillis: Long = UNKNOWN_WAIT_MILLIS,
    private val graceMillis: Long = GRACE_MILLIS,
) {
    private class Pending(val sinceMillis: Long, var videoKey: String?, var estimatedEndMillis: Long? = null)

    private var pending: Pending? = null

    /** Limit vypršel během videa a čeká se na hranici. */
    val isWaiting: Boolean get() = pending != null

    /**
     * Limit už vypršel a jsme na videu: smí se zasáhnout teď? Při true se brána vynuluje.
     * První volání začne čekání (a rovnou pustí, pokud video právě skončilo).
     */
    fun mayIntervene(video: VideoPlayback?, now: Long): Boolean {
        val p = pending ?: Pending(now, video?.videoKey).also { pending = it }
        if (video != null) {
            if (video.ended) return release()
            val key = video.videoKey
            if (key != null) {
                if (p.videoKey == null) p.videoKey = key else if (key != p.videoKey) return release()
            }
            video.remainingMillis?.let { p.estimatedEndMillis = video.readAtMillis + it + graceMillis }
        }
        val deadline = p.estimatedEndMillis?.coerceAtMost(p.sinceMillis + maxWaitMillis) ?: (p.sinceMillis + unknownWaitMillis)
        return if (now >= deadline) release() else false
    }

    fun reset() {
        pending = null
    }

    private fun release(): Boolean {
        pending = null
        return true
    }

    companion object {
        /** Strop čekání, když známe délku videa (dlouhé video, přetáčení, pauza). */
        const val MAX_WAIT_MILLIS = 15 * 60_000L

        /** Strop, když délku videa nepřečteme vůbec (detekce selhala). */
        const val UNKNOWN_WAIT_MILLIS = 10 * 60_000L

        /** Rezerva po odhadnutém konci: end-screen, odpočet autoplay, načítání. */
        const val GRACE_MILLIS = 20_000L
    }
}
