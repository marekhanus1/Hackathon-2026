package cz.narsil.detection.context

import cz.narsil.core.ComfortContext.AWAY
import cz.narsil.core.ComfortContext.HOME
import cz.narsil.core.ComfortContext.UNKNOWN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextResolverTest {
    @Test
    fun homeWifiIsHomeOtherIsAway() {
        assertEquals(HOME, ContextResolver.resolve(null, WifiReading.Connected("\"Doma 5G\""), "Doma 5G"))
        assertEquals(AWAY, ContextResolver.resolve(null, WifiReading.Connected("eduroam"), "Doma 5G"))
        assertEquals(AWAY, ContextResolver.resolve(null, WifiReading.NotOnWifi, "Doma 5G"))
    }

    @Test
    fun unknownOnlyWithoutSsid() {
        assertEquals(UNKNOWN, ContextResolver.resolve(null, WifiReading.Unknown, "Doma 5G"))
        assertEquals(UNKNOWN, ContextResolver.resolve(null, WifiReading.Unknown, null))
        // Bez uloženého domova: jiná nebo žádná Wi-Fi = AWAY (SPEC 2).
        assertEquals(AWAY, ContextResolver.resolve(null, WifiReading.Connected("Doma 5G"), null))
        assertEquals(AWAY, ContextResolver.resolve(null, WifiReading.NotOnWifi, null))
    }

    @Test
    fun manualOverrideWins() {
        assertEquals(HOME, ContextResolver.resolve(HOME, WifiReading.NotOnWifi, null))
        assertEquals(AWAY, ContextResolver.resolve(AWAY, WifiReading.Connected("Doma 5G"), "Doma 5G"))
    }

    @Test
    fun normalizeSsid() {
        assertEquals("Doma", ContextResolver.normalize("\"Doma\""))
        assertNull(ContextResolver.normalize("<unknown ssid>"))
        assertNull(ContextResolver.normalize("  "))
    }
}
