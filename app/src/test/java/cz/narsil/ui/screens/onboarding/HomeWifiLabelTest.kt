package cz.narsil.ui.screens.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 138: tlačítko domácí Wi-Fi nesmí ukazovat „Aktualizovat na žádná Wi-Fi“. */
class HomeWifiLabelTest {
    @Test
    fun labels() {
        assertEquals("Najít moji Wi-Fi (povolit polohu)", OnboardingUiState(detectedWifiSsid = null).homeWifiButtonLabel)
        assertEquals("Najít moji Wi-Fi (povolit polohu)", OnboardingUiState(detectedWifiSsid = null, homeWifiSsid = "Doma").homeWifiButtonLabel)
        assertEquals("Tohle je můj domov: Doma", OnboardingUiState(detectedWifiSsid = "Doma").homeWifiButtonLabel)
        val home = OnboardingUiState(detectedWifiSsid = "Doma", homeWifiSsid = "Doma")
        assertTrue(home.isOnHomeWifi)
        assertEquals("Domov uložen: Doma", home.homeWifiButtonLabel)
        val other = OnboardingUiState(detectedWifiSsid = "Kavárna", homeWifiSsid = "Doma")
        assertFalse(other.isOnHomeWifi)
        assertEquals("Uložit jako domov: Kavárna", other.homeWifiButtonLabel)
        assertEquals("Domov: Doma · teď jsi na: Kavárna", other.homeWifiStatusText)
    }
}
