package cz.narsil.ui.screens.pet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetLinesTest {

    @Test
    fun `isCrisis detects crisis phrases and ignores normal boredom`() {
        assertTrue(PetLines.isCrisis("Už nechci žít, je to moc"))
        assertTrue(PetLines.isCrisis("Mám silnou úzkost a deprese"))
        assertFalse(PetLines.isCrisis("Nudím se a chce se mi scrollovat"))
    }

    @Test
    fun `reply offers first planned task on boredom`() {
        val ctx = PetContext(plannedTasks = listOf("Úkol z matiky", "Procházka"))
        val response = PetLines.reply("nudím se", ctx)
        assertTrue("Expected task in reply, got: $response", response.contains("Úkol z matiky"))
    }

    @Test
    fun `shorten limits to two sentences and 280 chars`() {
        val threeSentences = "První věta je tady. Druhá věta následuje hned po ní! Třetí věta už by se neměla objevit."
        val shortened = PetLines.shorten("\"$threeSentences\"")
        assertFalse(shortened.contains("Třetí věta"))
        assertTrue(shortened.length <= 280)

        val longText = "Ahoj ".repeat(80) + "konec."
        val shortenedLong = PetLines.shorten(longText)
        assertTrue("Expected <= 280 chars, got ${shortenedLong.length}", shortenedLong.length <= 280)
    }

    @Test
    fun `greeting returns non-empty text for every mood`() {
        val ctx = PetContext(streakDays = 4, reclaimedMinutes = 30, feedMinutes = 10)
        PetMood.entries.forEach { mood ->
            val text = PetLines.greeting(ctx, mood, dayIndex = 1)
            assertTrue("Greeting empty for $mood", text.isNotBlank())
        }
    }

    @Test
    fun `tryLocalReply handles quick replies, greetings and crisis without api, returns null for open prompt`() {
        val ctx = PetContext(plannedTasks = listOf("Přečíst kapitolu"))

        assertNotNull(PetLines.tryLocalReply("Mám chuť scrollovat", ctx))
        assertNotNull(PetLines.tryLocalReply("Ahoj", ctx))
        assertNotNull(PetLines.tryLocalReply("Už nechci žít", ctx))
        assertNull(PetLines.tryLocalReply("Proč mě dneska nebaví učení na fyziku?", ctx))
    }
}
