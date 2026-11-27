package cz.narsil.llm.matcher

import cz.narsil.core.LlmProvider
import cz.narsil.llm.client.LlmClient
import cz.narsil.llm.client.LlmRequest
import cz.narsil.llm.client.LlmResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmContentMatcherTest {
    private val plan = listOf("přednáška o Rustu", "jeden díl seriálu")

    private class FakeLlm(val answer: String?) : LlmClient {
        val prompts = mutableListOf<String>()
        override suspend fun complete(request: LlmRequest): LlmResponse {
            prompts += request.prompt
            return LlmResponse(LlmProvider.CLAUDE, answer)
        }
    }

    @Test
    fun keywordFallbackHandlesCzech() {
        assertTrue(KeywordMatcher.match("Rust Conference 2026 – ownership talk", plan).planned)
        assertTrue(KeywordMatcher.match("Přednášky z Rustu: async", plan).planned)
        assertFalse(KeywordMatcher.match("10 nejlepších koček", plan).planned)
        assertFalse(KeywordMatcher.match("Nové video každý den", plan).planned)
    }

    @Test
    fun similarTitleMatchesWithoutLlm() = runTest {
        val llm = FakeLlm("{\"planned\": false}")
        val m = LlmContentMatcher(llm).isPlanned("Rust in 100 Seconds", plan)
        assertTrue(m.planned)
        assertTrue(llm.prompts.isEmpty())
    }

    @Test
    fun llmDecidesUnclearTitles() = runTest {
        val llm = FakeLlm("Jasně: {\"planned\": true, \"reason\": \"Ferris je maskot Rustu\"}")
        val m = LlmContentMatcher(llm).isPlanned("Ferris crab explains borrowing", plan)
        assertTrue(m.planned)
        assertEquals("Ferris je maskot Rustu", m.reason)
        val prompt = llm.prompts.single()
        assertTrue(prompt.contains("Ferris crab explains borrowing") && prompt.contains("přednáška o Rustu"))
    }

    @Test
    fun offlineOrBadAnswerUsesFallback() = runTest {
        val offline = LlmContentMatcher(FakeLlm("{\"planned\": true}"), isOnline = { false })
        assertFalse(offline.isPlanned("Ferris crab explains borrowing", plan).planned)
        assertFalse(LlmContentMatcher(FakeLlm(null)).isPlanned("Kočky", plan).planned)
        assertFalse(LlmContentMatcher(FakeLlm("nevím")).isPlanned("Kočky", plan).planned)
        assertFalse(LlmContentMatcher(FakeLlm("x")).isPlanned("Cokoli", emptyList()).planned)
    }

    @Test
    fun parse() {
        assertEquals(false, LlmContentMatcher.parse("{\"planned\":false,\"reason\":\"jiné téma\"}")!!.planned)
        assertNull(LlmContentMatcher.parse("{\"reason\":\"bez planned\"}"))
        assertNull(LlmContentMatcher.parse("ANO"))
    }
}
