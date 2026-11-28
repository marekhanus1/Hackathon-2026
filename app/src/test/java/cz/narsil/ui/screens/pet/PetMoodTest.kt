package cz.narsil.ui.screens.pet

import cz.narsil.core.AvatarState
import org.junit.Assert.assertEquals
import org.junit.Test

class PetMoodTest {

    @Test
    fun `maps avatar states during daytime`() {
        assertEquals(PetMood.OSPALA, petMoodFor(AvatarState.EXHAUSTED, hour = 14))
        assertEquals(PetMood.OSPALA, petMoodFor(AvatarState.TIRED, hour = 14))
        assertEquals(PetMood.KLIDNA, petMoodFor(AvatarState.OK, hour = 14))
        assertEquals(PetMood.SPOKOJENA, petMoodFor(AvatarState.FIT, hour = 14))
        assertEquals(PetMood.HRDA, petMoodFor(AvatarState.THRIVING, hour = 14))
    }

    @Test
    fun `night hours always return SPI`() {
        AvatarState.entries.forEach { avatar ->
            assertEquals(PetMood.SPI, petMoodFor(avatar, hour = 23))
            assertEquals(PetMood.SPI, petMoodFor(avatar, hour = 2))
            assertEquals(PetMood.SPI, petMoodFor(avatar, hour = 5))
        }
    }
}
