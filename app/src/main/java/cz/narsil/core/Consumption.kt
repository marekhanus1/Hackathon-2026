package cz.narsil.core

/** Typ obrazovky, na které uživatel je. MESSAGES se nikdy nepočítá ani nespouští zásah. */
enum class Surface {
    FEED, REELS, SHORTS, MESSAGES, VIDEO;

    /** Nekonečný obsah (feed, Reels, Shorts) – kandidát na zásah. VIDEO řeší YouTube logika (120). */
    val isEndless: Boolean get() = this == FEED || this == REELS || this == SHORTS
}

/**
 * Jeden úsek spotřeby obsahu z detekce (111, 120). Ukládá se do Room (121).
 *
 * @param packageName aplikace v popředí, např. "com.zhiliaoapp.musically"; u webu prohlížeč
 * @param surface typ obrazovky
 * @param startedAtMillis začátek úseku (epoch ms)
 * @param durationMillis délka úseku dosud
 * @param videoTitle název videa (YouTube), jinak null
 * @param url URL v prohlížeči, jinak null
 */
data class ConsumptionEvent(
    val packageName: String,
    val surface: Surface,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val videoTitle: String? = null,
    val url: String? = null,
)
