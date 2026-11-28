package cz.narsil.detection.accessibility

import cz.narsil.core.Surface

/**
 * Obecná detekce nekonečného feedu podle chování (149), bez ID konkrétní aplikace. Čistý Kotlin.
 *
 * Feed = opakované posouvání **dopředu** ve **svislém seznamu nebo pageru**, rozložené v čase:
 * - „takt“ = sekunda, ve které přišel aspoň jeden posun dopředu (jedno rychlé švihnutí = jeden takt),
 * - seznam (X, Reddit, Facebook…): aspoň [minBeats] taktů za [windowMillis] a mezi prvním a posledním ≥ [minSpanMillis],
 * - pager s jednou položkou na obrazovku (krátká videa): aspoň [pagerMinBeats] taktů a rozpětí ≥ [pagerMinSpanMillis],
 * - po splnění platí [holdMillis] od posledního posunu dopředu (dívá se na položku), pak zase nic.
 * Nepočítá se: vodorovný posun (karusel, záložky), posun zpět, dlouhý text (ScrollView, WebView).
 */
class FeedBehavior(
    private val windowMillis: Long = 90_000,
    private val minBeats: Int = 4,
    private val minSpanMillis: Long = 15_000,
    private val pagerMinBeats: Int = 3,
    private val pagerMinSpanMillis: Long = 10_000,
    private val holdMillis: Long = 60_000,
) {
    private class Beat(val second: Long, val pager: Boolean)

    private class State {
        val beats = ArrayDeque<Beat>()
        val lastToIndex = HashMap<String, Int>()
        var feedUntil = Long.MIN_VALUE
        var pager = false
    }

    private val states = HashMap<String, State>()

    /** Zpracuje scroll událost aplikace [packageName]. */
    fun onScroll(packageName: String, sample: ScrollSample, now: Long) {
        val cls = sample.className.orEmpty()
        if (NOT_FEED_CLASSES.any { cls.endsWith(it) }) return
        val dx = sample.deltaX
        val dy = sample.deltaY
        if (kotlin.math.abs(dx) > 1 && kotlin.math.abs(dx) > kotlin.math.abs(dy)) return // vodorovně
        val state = states.getOrPut(packageName) { State() }
        val prevTo = state.lastToIndex[cls]
        if (sample.toIndex >= 0) state.lastToIndex[cls] = sample.toIndex
        val forward = when {
            dy > 1 -> true
            dy < -1 -> false
            else -> prevTo != null && sample.toIndex > prevTo // posun neznámý: podle indexů položek
        }
        if (!forward) return
        val pager = cls.contains("ViewPager") ||
            (sample.fromIndex >= 0 && sample.fromIndex == sample.toIndex && sample.itemCount > 1)
        val second = now / 1_000
        if (state.beats.lastOrNull()?.second != second) state.beats.addLast(Beat(second, pager))
        while (state.beats.isNotEmpty() && state.beats.first().second * 1_000 < now - windowMillis) state.beats.removeFirst()

        val beats = state.beats
        val span = (beats.last().second - beats.first().second) * 1_000
        val pagerBeats = beats.count { it.pager }
        val isPager = pagerBeats * 2 > beats.size
        val qualifies = if (isPager) pagerBeats >= pagerMinBeats && span >= pagerMinSpanMillis
        else beats.size >= minBeats && span >= minSpanMillis
        if (qualifies) {
            state.feedUntil = now + holdMillis
            state.pager = isPager
        }
    }

    /** Nekonečný feed v [now]: FEED (seznam), REELS (pager krátkých videí), null = nic / už ne. */
    fun surface(packageName: String, now: Long): Surface? {
        val state = states[packageName] ?: return null
        if (now > state.feedUntil) return null
        return if (state.pager) Surface.REELS else Surface.FEED
    }

    private companion object {
        /** Posouvání textu a stránek, ne feedu. */
        val NOT_FEED_CLASSES = listOf("ScrollView", "WebView", "EditText", "TextView")
    }
}
