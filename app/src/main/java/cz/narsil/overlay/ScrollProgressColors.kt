package cz.narsil.overlay

/**
 * Barva ukazatele blížícího se limitu (135, 148): výrazná tyrkysová, v půlce jantarová, ke konci oranžová
 * a sytě červená. Krytí od 90 % do plné. Čistý Kotlin (ARGB jako Int) kvůli testům.
 */
object ScrollProgressColors {
    /** Body přechodu: procento → ARGB. */
    private val STOPS = listOf(
        0 to 0xE600BFA5.toInt(),   // tyrkysová, 90 %
        50 to 0xF2FFC400.toInt(),  // jantarová, 95 %
        80 to 0xFFFF6D00.toInt(),  // oranžová
        100 to 0xFFFF1744.toInt(), // červená
    )

    /** Podklad (stopa) a obrys: tmavě modré (Aero), ať je pruh vidět na světlém i tmavém obsahu. */
    const val TRACK = 0x99042A45.toInt()
    const val OUTLINE = 0xD9031F33.toInt()
    const val LABEL_TEXT = 0xFFFFFFFF.toInt()
    /** Lesk (Frutiger Aero): bílý odlesk v horní polovině pruhu a bílý vnitřní okraj. */
    const val GLOSS = 0xB3FFFFFF.toInt()
    const val RIM = 0x8CFFFFFF.toInt()
    /** Štítek jako toast: tmavě modrá průsvitná pilulka s bílým textem. */
    const val LABEL_BG = 0xE0063B5C.toInt()

    fun color(percent: Int): Int {
        val p = percent.coerceIn(0, 100)
        val upper = STOPS.indexOfFirst { it.first >= p }
        if (upper <= 0) return STOPS.first().second
        val (p0, c0) = STOPS[upper - 1]
        val (p1, c1) = STOPS[upper]
        return lerp(c0, c1, (p - p0).toFloat() / (p1 - p0))
    }

    /** Lineární přechod po kanálech A, R, G, B. */
    internal fun lerp(a: Int, b: Int, t: Float): Int {
        var out = 0
        for (shift in intArrayOf(24, 16, 8, 0)) {
            val ca = (a ushr shift) and 0xFF
            val cb = (b ushr shift) and 0xFF
            val c = (ca + (cb - ca) * t + 0.5f).toInt().coerceIn(0, 255)
            out = out or (c shl shift)
        }
        return out
    }

    fun alpha(color: Int): Int = (color ushr 24) and 0xFF
}
