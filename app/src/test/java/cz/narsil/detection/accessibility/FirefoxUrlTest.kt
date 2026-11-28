package cz.narsil.detection.accessibility

import cz.narsil.core.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirefoxUrlTest {
    @Test
    fun readsUrlFromFirefoxAddressBarDescription() {
        // Skutečný content-desc ADDRESSBAR_URL_BOX z Firefoxu 160 na Nothing 3a.
        val desc = " m.youtube.com/watch?v=dQw4w9WgXcQ. Zadejte hledání nebo adresu"
        val url = WindowReader.extractUrl(desc)
        assertEquals("m.youtube.com/watch?v=dQw4w9WgXcQ", url)
        assertEquals(Surface.VIDEO, SurfaceClassifier.classifyUrl(url!!))
    }

    @Test
    fun ignoresEmptyAddressBar() {
        assertNull(WindowReader.extractUrl("Zadejte hledání nebo adresu"))
        assertNull(WindowReader.extractUrl("  "))
        assertNull(WindowReader.extractUrl(null))
    }

    @Test
    fun keepsSchemeAndShortsPath() {
        assertEquals("https://www.youtube.com/shorts/abc", WindowReader.extractUrl("https://www.youtube.com/shorts/abc"))
        assertEquals(Surface.SHORTS, SurfaceClassifier.classifyUrl("youtube.com/shorts/abc"))
    }
}
