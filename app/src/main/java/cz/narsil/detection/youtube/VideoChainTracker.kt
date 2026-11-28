package cz.narsil.detection.youtube

/**
 * Řetězení neplánovaných videí (SPEC 3, 137). Čistý Kotlin, čas jen z parametrů.
 *
 * - Nové video ([observe]): ve Firefoxu jiný `v=` ([VideoPlayback.videoKey]). V aplikaci YouTube (bez ID)
 *   příchod na přehrávač odjinud, nebo konec videa a pak znovu přehrávání (autoplay, další video).
 * - Každé nové video se vyhodnotí ([resolve]) až po zjištění, zda je plánované (ContentMatcher).
 *   Plánované se nepočítá a řetěz nepřeruší; neplánované přičte 1.
 * - Zásah na začátku [limit]. neplánovaného videa po sobě (hranice videa, ne uprostřed, viz 134),
 *   jen když je to video pořád na obrazovce. Jinak přijde u dalšího videa.
 * - Pauza > [resetAfterMillis] bez videa řetěz nuluje.
 * - Zavřít / odemknout řetěz nuluje; během zaplaceného času se nezasahuje, videa se ale počítají dál.
 */
class VideoChainTracker(private val resetAfterMillis: Long = RESET_AFTER_MILLIS) {
    /** Jedno video řetězu. Identita objektu = konkrétní zhlédnutí (i bez ID v aplikaci). */
    class Video internal constructor(val key: String?, val startedAtMillis: Long)

    /** Neplánovaných videí po sobě (vyhodnocených). */
    var count: Int = 0
        private set

    /** Začátek řetězu (první započtené video), pro délku v události. */
    var chainStartedAtMillis: Long? = null
        private set

    /** Video právě na obrazovce, null = mimo přehrávač. */
    var current: Video? = null
        private set

    private var lastVideoAt = Long.MIN_VALUE / 2
    private var lastKey: String? = null
    private var lastEnded = false
    private var awaitingOutcome = false
    private var unlockedUntil = 0L

    /**
     * Pozorování (snímek nebo tik). Vrací nové video, které je potřeba vyhodnotit ([resolve]), jinak null.
     *
     * @param onVideo je na obrazovce přehrávač jednoho videa (povrch VIDEO, sledovaná aplikace, doma)
     */
    fun observe(onVideo: Boolean, video: VideoPlayback?, now: Long): Video? {
        if (!onVideo) {
            current = null
            return null
        }
        if (now - lastVideoAt > resetAfterMillis) reset()
        lastVideoAt = now
        val key = video?.videoKey
        val ended = video?.ended == true
        val prev = current
        val isNew = when {
            key != null -> key != lastKey
            prev == null -> true // aplikace: příchod na přehrávač odjinud
            else -> lastEnded && !ended // aplikace: po konci znovu přehrává = další video
        }
        lastEnded = ended
        if (key != null) lastKey = key
        if (!isNew) {
            // Firefox: návrat na stejné video (jiná aplikace, zpět) není nové video.
            if (prev == null) current = Video(key, now)
            return null
        }
        return Video(key, now).also { current = it }
    }

    /**
     * Výsledek ContentMatcheru pro [video]. true = zasáhnout teď (UNPLANNED_VIDEO_CHAIN).
     * [limit] = `Settings.videoChainLimit`.
     */
    fun resolve(video: Video, planned: Boolean, limit: Int, now: Long): Boolean {
        if (planned) return false
        if (count == 0) chainStartedAtMillis = video.startedAtMillis
        count++
        if (count < limit.coerceAtLeast(1) || awaitingOutcome || now < unlockedUntil) return false
        if (current !== video) return false // uživatel už je jinde: zásah u dalšího videa
        awaitingOutcome = true
        return true
    }

    /** Zavřít: nový řetěz od nuly. */
    fun onClosed() {
        reset()
        awaitingOutcome = false
        unlockedUntil = 0
    }

    /** Zaplaceno / odemčeno do [untilMillis]: řetěz od nuly, do té doby bez zásahu. */
    fun onUnlocked(untilMillis: Long) {
        reset()
        awaitingOutcome = false
        unlockedUntil = untilMillis
    }

    /** Okno zavřeno bez volby: další video může zasáhnout znovu. */
    fun onDismissed() {
        awaitingOutcome = false
    }

    private fun reset() {
        count = 0
        chainStartedAtMillis = null
    }

    companion object {
        /** SPEC 3: pauza delší než 10 min řetěz nuluje. */
        const val RESET_AFTER_MILLIS = 10 * 60_000L
    }
}
