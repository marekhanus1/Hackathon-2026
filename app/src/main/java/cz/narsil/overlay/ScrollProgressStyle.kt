package cz.narsil.overlay

import kotlin.math.ceil

/**
 * Chování ukazatele limitu (148): kdy pulsovat, kdy ukázat štítek a co v něm. Čistý Kotlin kvůli testům.
 */
object ScrollProgressStyle {
    /** Prahy, při jejichž překročení (směrem nahoru) pruh krátce pulsuje. */
    val PULSE_AT = listOf(50, 80)

    /** Od kolika procent se ukáže štítek se zbývajícím časem (posledních ~20 %). */
    const val LABEL_FROM = 80

    /** Výška pruhu, okraje a štítek v dp. */
    const val BAR_DP = 7f
    const val SIDE_MARGIN_DP = 10f
    const val TOP_GAP_DP = 3f
    const val LABEL_HEIGHT_DP = 24f
    const val LABEL_TEXT_SP = 13f
    const val PULSE_MILLIS = 650L

    /** Překročil posun z [previous] na [current] některý práh pulsu? (null = pruh se právě objevil) */
    fun crossedPulse(previous: Int?, current: Int): Boolean =
        PULSE_AT.any { t -> (previous ?: -1) < t && current >= t }

    fun showLabel(percent: Int): Boolean = percent >= LABEL_FROM

    /** Zbývající čas do Ozvěny podle procenta a prahu relace. */
    fun remainingMillis(percent: Int, thresholdMillis: Long): Long =
        (thresholdMillis * (100 - percent.coerceIn(0, 100)) / 100).coerceAtLeast(0)

    /** „0:15“, „1:05“ – sekundy nahoru, ať se 0:00 ukáže až na konci. */
    fun format(millis: Long): String {
        val total = ceil(millis.coerceAtLeast(0) / 1000.0).toLong()
        return "%d:%02d".format(total / 60, total % 60)
    }

    fun label(millis: Long): String = "Ozvěna za ${format(millis)}"
}
