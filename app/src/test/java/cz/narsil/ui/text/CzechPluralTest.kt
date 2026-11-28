package cz.narsil.ui.text

import org.junit.Assert.assertEquals
import org.junit.Test

class CzechPluralTest {
    @Test
    fun formsByCount() {
        assertEquals("0 dní", CzechPlural.days(0))
        assertEquals("1 den", CzechPlural.days(1))
        assertEquals("2 dny", CzechPlural.days(2))
        assertEquals("4 dny", CzechPlural.days(4))
        assertEquals("5 dní", CzechPlural.days(5))
        assertEquals("1 hodinu", CzechPlural.hoursAccusative(1))
        assertEquals("3 hodiny", CzechPlural.hoursAccusative(3))
        assertEquals("12 hodin", CzechPlural.hoursAccusative(12))
        assertEquals("1 výhra", CzechPlural.wins(1))
        assertEquals("7 výher", CzechPlural.wins(7))
        assertEquals("2 dřepy", CzechPlural.squats(2))
        assertEquals("1 úkol", CzechPlural.tasks(1))
        assertEquals("6000 kroků", CzechPlural.steps(6000))
    }
}
