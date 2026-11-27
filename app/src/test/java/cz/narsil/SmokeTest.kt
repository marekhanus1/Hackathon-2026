package cz.narsil

import org.junit.Assert.assertEquals
import org.junit.Test

/** Ověřuje, že testDebugUnitTest běží. */
class SmokeTest {
    @Test
    fun buildRuns() = assertEquals(4, 2 + 2)
}
