package cz.narsil.app.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class DemoEventTest {
    @Test
    fun `jednoduche udalosti`() {
        assertEquals("""{"e":"echo"}""", DemoEvent.EchoShown.toJson())
        assertEquals("""{"e":"squats","n":12,"target":20}""", DemoEvent.Squats(12).toJson())
        assertEquals("""{"e":"task","title":"Běh","status":"VERIFIED"}""", DemoEvent.Task("Běh", "VERIFIED").toJson())
        assertEquals("""{"e":"context","where":"home"}""", DemoEvent.Context("home").toJson())
        assertEquals("""{"e":"xp","delta":50,"xp":1000}""", DemoEvent.Xp(50, 1000).toJson())
    }

    @Test
    fun `nazev videa se escapuje`() {
        assertEquals("""{"e":"rated","title":"Rust \"v\" embedded\n"}""", DemoEvent.Rated("Rust \"v\" embedded\n").toJson())
        assertEquals("""{"e":"rated","title":null}""", DemoEvent.Rated(null).toJson())
    }

    @Test
    fun stav() {
        assertEquals(
            """{"e":"state","where":"away","level":4,"xp":1990,"echo":false,"min":0}""",
            DemoEvent.State("away", 4, 1990, false, 0).toJson(),
        )
        assertEquals("""{"e":"ack","cmd":"home","ok":true}""", DemoEvent.Ack("home", true).toJson())
    }
}
