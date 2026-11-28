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
 * Vodorovný prvek se pozná i bez posunu (RecyclerView ho často neposílá) podle [ScrollSample.orientation].
 * Indexy a pozice se pamatují pro každý posouvaný prvek zvlášť ([ScrollSample.sourceKey]): hlavní feed
 * a karusel v něm jsou často stejná třída a jinak by si míchaly směr.
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
        val lastScrollY = HashMap<String, Int>()
        var feedUntil = Long.MIN_VALUE
        var pager = false
        var streakStart: Long? = null
    }

    private val states = HashMap<String, State>()

    /** Zpracuje scroll událost aplikace [packageName]. */
    fun onScroll(packageName: String, sample: ScrollSample, now: Long) {
        val cls = sample.className.orEmpty()
        if (NOT_FEED_CLASSES.any { cls.endsWith(it) }) return
        val dx = sample.deltaX
        val dy = sample.deltaY
        if (kotlin.math.abs(dx) > 1 && kotlin.math.abs(dx) > kotlin.math.abs(dy)) return // vodorovně
        if (sample.orientation == ScrollOrientation.HORIZONTAL) return // karusel bez údaje o posunu
        val state = states.getOrPut(packageName) { State() }
        val key = if (sample.sourceKey != 0) "$cls#${sample.sourceKey}" else cls
        val prevTo = state.lastToIndex[key]
        val prevY = state.lastScrollY[key]
        if (sample.toIndex >= 0) state.lastToIndex[key] = sample.toIndex
        if (sample.scrollY >= 0) state.lastScrollY[key] = sample.scrollY
        val forward = when {
            dy > 1 -> true
            dy < -1 -> false
            sample.scrollY >= 0 && prevY != null && sample.scrollY != prevY -> sample.scrollY > prevY
            sample.toIndex >= 0 && prevTo != null && sample.toIndex != prevTo -> sample.toIndex > prevTo
            // Směr neznámý (Compose bez údajů, nebo první událost): ve feedu se posouvá skoro jen dolů.
            sample.scrollY < 0 && sample.toIndex < 0 -> true
            else -> false
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
            if (now > state.feedUntil) state.streakStart = beats.first().second * 1_000
            state.feedUntil = now + holdMillis
            state.pager = isPager
        }
    }

    /**
     * Kdy začalo scrollování, které feed prozradilo (první takt v okně). Relace se počítá od něj,
     * ne až od rozpoznání (jinak by prvních ~15 s chybělo). null = teď žádný feed.
     */
    fun streakStart(packageName: String, now: Long): Long? =
        states[packageName]?.takeIf { now <= it.feedUntil }?.streakStart

    /** Stav pro log: takty v okně, rozpětí, feed do. */
    fun describe(packageName: String): String {
        val s = states[packageName] ?: return "bez scrollu"
        val span = if (s.beats.isEmpty()) 0 else s.beats.last().second - s.beats.first().second
        return "takty=${s.beats.size} rozpětí=${span}s pager=${s.beats.count { it.pager }}"
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
