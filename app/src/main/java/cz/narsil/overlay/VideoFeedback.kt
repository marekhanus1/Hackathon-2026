package cz.narsil.overlay

import cz.narsil.core.HistoryRating
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.Surface
import cz.narsil.ui.text.CzechPlural

/**
 * „Co ti to video dalo?“ (156): názvy toho, co se hodnotí, strop otázek a věta pro Ozvěnu z minulých odpovědí.
 * Čistý Kotlin kvůli testům.
 */
object VideoFeedback {
    /** Video z aplikace YouTube, jehož název nepřečteme. */
    const val UNKNOWN_VIDEO = "Video na YouTube"

    /** Předpona názvu u zaplaceného času feedu: „Feed · TikTok“. */
    private const val FEED_PREFIX = "Feed · "

    /** Nízká hodnota = „Nic, jen čas“ a „Je mi hůř“. */
    const val LOW_RATING = 2

    private val TRAILING_NUMBER = Regex("""\s*#?\d+\s*$""")

    /** Název pořadu bez pořadového čísla („Když je pondělí #312“ → „Když je pondělí“); i pro mapu hodnoty. */
    fun stem(title: String): String = title.replace(TRAILING_NUMBER, "").trim().ifEmpty { title }

    fun videoTitle(title: String?): String = title?.trim()?.takeIf { it.isNotEmpty() } ?: UNKNOWN_VIDEO

    fun feedTitle(appName: String): String = FEED_PREFIX + appName

    fun isFeed(title: String): Boolean = title.startsWith(FEED_PREFIX)

    /** Text v panelu u zaplaceného času: „10 minut feedu · TikTok“. */
    fun feedCaption(minutes: Int, appName: String): String = "${CzechPlural.count(minutes, "minuta", "minuty", "minut")} feedu · $appName"

    /**
     * Věta pro Ozvěnu z odpovědí (156), null = nic k připomenutí.
     * 1. Stejný pořad (nebo feed stejné aplikace) naposledy nedal nic → „Minule ti „…“ nedalo nic.“
     * 2. Jinak dnešní nízké odpovědi stejného druhu (videa / feed) → „Dnes ti už 2 videa nedala nic.“
     *
     * @param dayStartMillis začátek dnešního dne (4:00)
     */
    fun echoNote(trigger: InterventionTrigger, appName: String, ratings: List<HistoryRating>, dayStartMillis: Long): String? {
        val feed = trigger.event.surface != Surface.VIDEO
        val key = if (feed) feedTitle(appName) else stem(videoTitle(trigger.event.videoTitle))
        // Nečitelný název (aplikace YouTube) není pořad: rovnou dnešní souhrn.
        val same = if (key == UNKNOWN_VIDEO) emptyList() else ratings.filter { stem(it.title) == key }.sortedByDescending { it.atMillis }
        val latest = same.firstOrNull()
        if (latest != null && latest.rating <= LOW_RATING) {
            val low = same.takeWhile { it.rating <= LOW_RATING }.size
            return when {
                feed && low >= 2 -> "Feed na $appName ti už ${low}× za sebou nedal nic."
                feed -> "Minule ti feed na $appName nedal nic."
                low >= 2 -> "Už ${low}× za sebou ti „$key“ nedalo nic."
                else -> "Minule ti „$key“ nedalo nic."
            }
        }
        val lowToday = ratings.count { it.atMillis >= dayStartMillis && it.rating <= LOW_RATING && isFeed(it.title) == feed }
        if (lowToday == 0) return null
        return if (feed) {
            "Dnes ti feed už ${lowToday}× nedal nic."
        } else {
            val verb = CzechPlural.word(lowToday, "nedalo", "nedala", "nedalo")
            "Dnes ti už ${CzechPlural.count(lowToday, "video", "videa", "videí")} $verb nic."
        }
    }

    /**
     * Strop otázek (156): nejvýš [maxPerDay] za den a s odstupem [minGapMillis]. Drží se v paměti
     * (restart služby strop vynuluje – raději se zeptat jednou navíc než nikdy).
     */
    class Limiter(
        private val maxPerDay: Int = MAX_PER_DAY,
        private val minGapMillis: Long = MIN_GAP_MILLIS,
    ) {
        private val asked = mutableListOf<Long>()

        /** Smí se teď zeptat? Při true se otázka započte. */
        fun tryAsk(now: Long, dayStartMillis: Long): Boolean {
            asked.removeAll { it < dayStartMillis }
            if (asked.size >= maxPerDay) return false
            if (asked.isNotEmpty() && now - asked.last() < minGapMillis) return false
            asked += now
            return true
        }
    }

    /** Stejně jako strop XP za hodnocení (SPEC 8). */
    const val MAX_PER_DAY = 10

    /** Krátká videa za sebou nesmí panel ukazovat po každém z nich. */
    const val MIN_GAP_MILLIS = 3 * 60_000L

    /** Panel sám zmizí, když na něj nikdo nesáhne. */
    const val AUTO_DISMISS_MILLIS = 10_000L

    /** Po odpovědi panel chvíli poděkuje. */
    const val THANKS_MILLIS = 1_500L
}
