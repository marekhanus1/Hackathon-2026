package cz.narsil.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeContractCardTest {
    @Test
    fun withoutContractItWaits() {
        val s = HomeUiState(contractCompletedToday = false, contractSigned = false)
        assertEquals("Večerní smlouva čeká (2 minuty)", s.contractTitle)
        assertEquals("Uzavřít večerní smlouvu", s.contractButton)
        assertFalse(s.contractDone)
    }

    @Test
    fun signedContractShowsDayAndTaskCount() {
        val today = HomeUiState(contractSigned = true, contractSignedForToday = true, contractTaskCount = 3)
        assertEquals("Smlouva na dnes podepsaná · 3 úkoly", today.contractTitle)
        assertEquals("Upravit smlouvu", today.contractButton)
        assertTrue(today.contractDone)

        val tomorrow = HomeUiState(contractSigned = true, contractSignedForToday = false, contractTaskCount = 1)
        assertEquals("Smlouva na zítra podepsaná · 1 úkol", tomorrow.contractTitle)
        val none = HomeUiState(contractSigned = true, contractTaskCount = 0)
        assertTrue(none.contractTitle.endsWith("0 úkolů"))
    }

    @Test
    fun fulfilledContractWins() {
        val s = HomeUiState(contractCompletedToday = true, contractSigned = true)
        assertEquals("Dnešní smlouva splněná (+100 XP)", s.contractTitle)
    }

    @Test
    fun debtLabelIncludesCarriedDebt() {
        assertEquals(null, HomeUiState(debtMinutes = 0, carriedDebtMinutes = 0).debtLabel)
        assertEquals("Dluh ze včerejška: 10 min", HomeUiState(debtMinutes = 0, carriedDebtMinutes = 10).debtLabel)
        assertEquals("Dluh do zítřka: 10 min", HomeUiState(debtMinutes = 10, carriedDebtMinutes = 0).debtLabel)
        assertEquals(
            "Dluh ze včerejška: 5 min · dnes půjčeno 10 min",
            HomeUiState(debtMinutes = 10, carriedDebtMinutes = 5).debtLabel,
        )
    }
}
