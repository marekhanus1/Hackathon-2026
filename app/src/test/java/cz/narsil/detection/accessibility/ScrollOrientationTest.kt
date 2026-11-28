package cz.narsil.detection.accessibility

import cz.narsil.core.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Karusely a vnořené seznamy v obecné detekci feedu (149): reprodukce chyb z telefonu a jejich oprava. */
class ScrollOrientationTest {
    private val rv = "androidx.recyclerview.widget.RecyclerView"
    private val sec = 1_000L
    private val fb = "com.facebook.katana"
    private val reddit = "com.reddit.frontpage"

    @Test
    fun horizontalCarouselWithoutDeltaIsNotFeed() {
        // Listování fotkami v jednom příspěvku: RecyclerView bez scrollDelta, toIndex roste, orientace z uzlu.
        val f = FeedBehavior()
        var t = 0L
        repeat(8) { i ->
            f.onScroll(fb, ScrollSample(rv, sourceKey = 7, orientation = ScrollOrientation.HORIZONTAL, fromIndex = i, toIndex = i + 1, itemCount = 10), t)
            t += 4 * sec
        }
        assertNull(f.surface(fb, t))
    }

    @Test
    fun scrollingUpWithCarouselInsideIsNotFeed() {
        // Hlavní seznam zpět nahoru (indexy klesají) a karusel uvnitř se posouvá: dřív si míchaly indexy (stejná třída).
        val f = FeedBehavior()
        var t = 0L
        var main = 42
        repeat(8) { i ->
            f.onScroll(reddit, ScrollSample(rv, sourceKey = 1, fromIndex = i, toIndex = i + 1, itemCount = 5, orientation = ScrollOrientation.HORIZONTAL), t)
            t += 1_500
            main -= 1
            f.onScroll(reddit, ScrollSample(rv, sourceKey = 2, fromIndex = main - 2, toIndex = main, itemCount = 200), t)
            t += 1_500
        }
        assertNull(f.surface(reddit, t))
    }

    @Test
    fun indexesArePerElementNotPerClass() {
        // Dva svislé seznamy stejné třídy (feed a otevřené komentáře): feed dolů se pozná i s komentáři nahoře.
        val f = FeedBehavior()
        var t = 0L
        repeat(8) { i ->
            f.onScroll(reddit, ScrollSample(rv, sourceKey = 2, fromIndex = 3 * i, toIndex = 3 * i + 3, itemCount = 200), t)
            t += 1_500
            f.onScroll(reddit, ScrollSample(rv, sourceKey = 3, fromIndex = 0, toIndex = 2, itemCount = 30), t)
            t += 1_500
        }
        assertEquals(Surface.FEED, f.surface(reddit, t))
    }

    @Test
    fun verticalFeedStillDetected() {
        val f = FeedBehavior()
        var t = 0L
        repeat(8) { i ->
            f.onScroll(fb, ScrollSample(rv, sourceKey = 9, orientation = ScrollOrientation.VERTICAL, fromIndex = 2 * i, toIndex = 2 * i + 3, itemCount = 100), t)
            t += 3 * sec
        }
        assertEquals(Surface.FEED, f.surface(fb, t))
    }

    @Test
    fun orientationFromCollectionAndActions() {
        assertEquals(ScrollOrientation.HORIZONTAL, ScrollOrientations.fromCollection(rows = 1, columns = 12))
        assertEquals(ScrollOrientation.VERTICAL, ScrollOrientations.fromCollection(rows = 80, columns = 1))
        assertNull(ScrollOrientations.fromCollection(rows = 20, columns = 3)) // mřížka
        assertNull(ScrollOrientations.fromCollection(rows = -1, columns = -1))
        assertEquals(ScrollOrientation.HORIZONTAL, ScrollOrientations.fromActions(vertical = false, horizontal = true))
        assertEquals(ScrollOrientation.VERTICAL, ScrollOrientations.fromActions(vertical = true, horizontal = false))
        assertNull(ScrollOrientations.fromActions(vertical = true, horizontal = true))
    }
}
