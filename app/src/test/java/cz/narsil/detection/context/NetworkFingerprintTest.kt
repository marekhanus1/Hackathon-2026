package cz.narsil.detection.context

import cz.narsil.core.ComfortContext.HOME
import cz.narsil.core.ComfortContext.UNKNOWN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkFingerprintTest {
    private fun fp(gw: String = "192.168.0.1", prefix: String = "192.168.0.0/24", dns: List<String> = listOf("192.168.0.1")) =
        NetworkFingerprint.of(listOf(prefix), listOf(gw), dns, "/192.168.0.1", null)

    @Test
    fun stejna_sit_stejny_otisk_bez_ohledu_na_poradi() {
        val a = NetworkFingerprint.of(listOf("192.168.0.0/24"), listOf("192.168.0.1"), listOf("8.8.8.8", "1.1.1.1"), "192.168.0.1", "Home")
        val b = NetworkFingerprint.of(listOf(" 192.168.0.0/24"), listOf("/192.168.0.1"), listOf("1.1.1.1", "8.8.8.8"), "/192.168.0.1", "home ")
        assertEquals(a, b)
    }

    @Test
    fun jina_brana_nebo_podsit_jiny_otisk() {
        assertNotEquals(fp(), fp(gw = "192.168.1.1", prefix = "192.168.1.0/24"))
        assertNotEquals(fp(), fp(dns = listOf("10.0.0.53")))
    }

    @Test
    fun bez_brany_nebo_podsite_neni_otisk() {
        assertNull(NetworkFingerprint.of(emptyList(), listOf("192.168.0.1"), emptyList(), null, null))
        assertNull(NetworkFingerprint.of(listOf("192.168.0.0/24"), listOf("0.0.0.0"), emptyList(), null, null))
    }

    @Test
    fun vypnuta_poloha_domov_podle_otisku() {
        assertEquals(HOME, ContextResolver.resolve(null, WifiReading.Unknown, "Doma 5G", fingerprintMatchesHome = true))
        assertEquals(UNKNOWN, ContextResolver.resolve(null, WifiReading.Unknown, "Doma 5G", fingerprintMatchesHome = false))
        // Bez uloženého domova otisk nic neznamená.
        assertEquals(UNKNOWN, ContextResolver.resolve(null, WifiReading.Unknown, null, fingerprintMatchesHome = true))
    }
}
