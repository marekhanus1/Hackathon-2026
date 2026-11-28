package cz.narsil.detection.youtube

/**
 * Co víme o právě přehrávaném videu (134). Čte se ze snímku přístupnosti, nic se neukládá.
 *
 * @param videoKey identita videa: `v=` z URL ve Firefoxu; v aplikaci YouTube null (název spolehlivě nečteme)
 * @param positionMillis aktuální pozice z ovládání přehrávače („0:15 / 4:32“), null = nečitelné
 * @param durationMillis délka videa z téhož textu
 * @param ended konec videa: „Přehrát znovu“, odpočet autoplay „Další video za 5“, nebo pozice = délka
 * @param readAtMillis kdy se stav přečetl (tik opakuje starý snímek, pozice tedy stárne)
 * @param title název videa (Firefox: titulek stránky bez „ - YouTube“), null = nečitelný; jen pro ContentMatcher (137)
 */
data class VideoPlayback(
    val videoKey: String?,
    val positionMillis: Long? = null,
    val durationMillis: Long? = null,
    val ended: Boolean = false,
    val readAtMillis: Long = 0,
    val title: String? = null,
) {
    /** Zbývající čas videa v okamžiku [readAtMillis], null = délka neznámá. */
    val remainingMillis: Long?
        get() {
            val pos = positionMillis ?: return null
            val dur = durationMillis?.takeIf { it > 0 } ?: return null
            return (dur - pos).coerceAtLeast(0)
        }
}

/**
 * Čtení stavu přehrávače YouTube z krátkých textů přístupnosti (aplikace i m.youtube.com ve Firefoxu).
 * Čistý Kotlin kvůli testům nad umělými snímky.
 */
object YouTubePlayback {
    private val VIDEO_ID = Regex("[A-Za-z0-9_-]{6,20}")

    /** Čas „pozice / délka“, i když jsou části v sousedních uzlech („0:15“ „/“ „4:32“). */
    private val TIME_PAIR = Regex("""(?<![\d:])((?:\d{1,2}:)?\d{1,2}:\d{2})\s*/\s*((?:\d{1,2}:)?\d{1,2}:\d{2})(?![\d:])""")

    /** End-screen. Malými písmeny, česky i anglicky (jazyk telefonu i stránky se může lišit). */
    private val ENDED_MARKERS = listOf(
        "přehrát znovu", "přehrát video znovu", "spustit znovu", "replay", "watch again",
        // Aplikace YouTube na konci videa (Nothing 3a, 11/2026): „Navrhované video · Zavřít · Přehrát“.
        "navrhované video", "suggested video",
    )

    /** Odpočet autoplay: „Další video za 5“, „Up next in 8“. Samotné tlačítko „Další video“ konec neznamená. */
    private val AUTOPLAY_COUNTDOWN = Regex("""(další video|následující video|up next|next video|playing next)\D{0,8}\b(za|in)\s*\d{1,2}\b""")

    /** Tolerance pro „pozice = délka“ (zaokrouhlení na sekundy, poslední snímek před end-screen). */
    private const val END_TOLERANCE_MILLIS = 1_500L

    /**
     * ID videa z URL adresního řádku (bez nebo se schématem). Jen stránka jednoho videa:
     * `youtube.com/watch?v=ID`, `youtu.be/ID`, `youtube.com/live/ID`. Shorts a feed vrací null.
     */
    fun videoIdFromUrl(raw: String?): String? {
        val (host, path) = split(raw) ?: return null
        val id = when {
            host == "youtu.be" -> path.removePrefix("/").substringBefore('?').substringBefore('/')
            !host.endsWith("youtube.com") -> null
            path.startsWith("/watch") ->
                path.substringAfter('?', "").split('&').firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
            path.startsWith("/live/") -> path.removePrefix("/live/").substringBefore('?').substringBefore('/')
            else -> null
        }
        return id?.substringBefore('#')?.takeIf { VIDEO_ID.matches(it) }
    }

    /** Je URL stránka YouTube (feed, hledání, video, Shorts)? Pro „návrat na feed“ ve Firefoxu. */
    fun isYouTubeUrl(raw: String?): Boolean {
        val host = split(raw)?.first ?: return false
        return host.endsWith("youtube.com") || host == "youtu.be"
    }

    /** Host (malými písmeny, bez www./m.) a cesta s dotazem (velikost písmen zachovaná: ID videa na ní záleží). */
    private fun split(raw: String?): Pair<String, String>? {
        val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val noScheme = s.replaceFirst(Regex("^https?://", RegexOption.IGNORE_CASE), "")
        val hostPart = noScheme.substringBefore('/').substringBefore('?')
        return hostPart.lowercase().removePrefix("www.").removePrefix("m.") to noScheme.substring(hostPart.length)
    }

    /** Název videa z titulku stránky („Název - YouTube“). Obecné titulky („YouTube“) → null. */
    fun titleFromPage(raw: String?): String? {
        val t = raw?.trim()?.removeSuffix("- YouTube")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return t.takeUnless { it.equals("YouTube", ignoreCase = true) }
    }

    /** Stav přehrávače z [texts] (krátké texty a contentDescription uzlů). */
    fun parse(videoKey: String?, texts: List<String>, readAtMillis: Long): VideoPlayback {
        val joined = texts.joinToString(" ") { it.trim() }
        val lower = joined.lowercase()
        val time = TIME_PAIR.find(joined)
        val pos = time?.groupValues?.get(1)?.let(::parseClock)
        val dur = time?.groupValues?.get(2)?.let(::parseClock)?.takeIf { it > 0 }
        val atEnd = pos != null && dur != null && pos >= dur - END_TOLERANCE_MILLIS
        val ended = atEnd || ENDED_MARKERS.any { it in lower } || AUTOPLAY_COUNTDOWN.containsMatchIn(lower)
        return VideoPlayback(videoKey, pos, dur, ended, readAtMillis)
    }

    /** „1:02:03“ / „4:32“ → ms. */
    internal fun parseClock(s: String): Long? {
        val parts = s.split(':').map { it.toLongOrNull() ?: return null }
        if (parts.size !in 2..3 || parts.drop(1).any { it >= 60 }) return null
        return parts.fold(0L) { acc, p -> acc * 60 + p } * 1_000
    }
}
