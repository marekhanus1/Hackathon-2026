package cz.narsil.overlay

/**
 * Barva ukazatele blížícího se limitu (135): ze začátku nenápadná tyrkysová, ke konci jantarová,
 * oranžová a sytě červená. S procenty roste i krytí. Čistý Kotlin (ARGB jako Int) kvůli testům.
 */
object ScrollProgressColors {
    /** Body přechodu: procento → ARGB. */
    private val STOPS = listOf(
        0 to 0x5926A69A,          // tyrkysová, 35 %
        60 to 0xA6FFB300.toInt(), // jantarová, 65 %
        85 to 0xD9FF6D00.toInt(), // oranžová, 85 %
        100 to 0xFFE53935.toInt(), // červená, plná
    )

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
