package cz.narsil.movement.currency

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 133: limit dluhu z nastavení se čte při každé půjčce. */
class DebtLimitSettingTest {
    @Test
    fun providerLimitIsReadLive() = runTest {
        var limit = 10
        val book = CurrencyBook(debtLimitProvider = { limit })
        assertTrue(book.borrow(10).accepted)
        val over = book.borrow(10)
        assertFalse(over.accepted)
        assertTrue(over.partnerAlert)
        limit = 30
        assertEquals(30, book.debtLimitMinutes)
        assertTrue(book.borrow(10).accepted)
        assertEquals(30, book.state(0).debtLimitMinutes)
    }

    @Test
    fun zeroLimitMeansNoBorrowing() = runTest {
        assertFalse(CurrencyBook(debtLimitProvider = { 0 }).borrow(10).accepted)
    }
}
