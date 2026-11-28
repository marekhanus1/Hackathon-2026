package cz.narsil.llm.client

import cz.narsil.core.LlmProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmClientTest {
    private val img = byteArrayOf(1, 2, 3)

    private fun recorder(reply: String?): Pair<MutableList<HttpCall>, LlmTransport> {
        val calls = mutableListOf<HttpCall>()
        return calls to LlmTransport { call, _ -> calls += call; HttpResult(200, reply) }
    }

    private val allKeys = LlmKeys { "klic-${it.name}" }

    @Test
    fun claudeRequestShape() {
        val call = LlmProtocols.build(LlmProvider.CLAUDE, LlmRequest("Úkol?", img, maxTokens = 5), "k")
        assertEquals("https://api.anthropic.com/v1/messages", call.url)
        assertEquals("k", call.headers["x-api-key"])
        assertEquals("2026-10-21", call.headers["anthropic-version"])
        val body = Json.parseToJsonElement(call.body).jsonObject
        assertEquals("claude-haiku-5-5", body["model"]!!.jsonPrimitive.content)
        assertEquals("5", body["max_tokens"]!!.jsonPrimitive.content)
        assertFalse("Haiku 5.5 odmítá temperature", "temperature" in body)
        assertEquals("disabled", body["thinking"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        val content = body["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonArray
        assertEquals("image", content[0].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("AQID", content[0].jsonObject["source"]!!.jsonObject["data"]!!.jsonPrimitive.content)
        assertEquals("Úkol?", content[1].jsonObject["text"]!!.jsonPrimitive.content)
    }

    @Test
    fun geminiAndOpenAiRequestShape() {
        val g = LlmProtocols.build(LlmProvider.GEMINI, LlmRequest("Q", img), "gk")
        assertTrue(g.url.endsWith(":generateContent"))
        assertEquals("gk", g.headers["x-goog-api-key"])
        assertTrue(g.body.contains("\"temperature\":0"))
        assertTrue(g.body.contains("inline_data"))
        val o = LlmProtocols.build(LlmProvider.OPENAI, LlmRequest("Q"), "ok")
        assertEquals("Bearer ok", o.headers["Authorization"])
        assertTrue(o.body.contains("\"content\":\"Q\""))
        val oi = LlmProtocols.build(LlmProvider.OPENAI, LlmRequest("Q", img), "ok")
        assertTrue(oi.body.contains("data:image/jpeg;base64,AQID"))
    }

    @Test
    fun geminiUsesCurrentModelWithMinimalThinking() {
        val g = LlmProtocols.build(LlmProvider.GEMINI, LlmRequest("Q"), "gk")
        assertTrue(g.url.contains("/models/gemini-3.5-flash-lite:"))
        val config = Json.parseToJsonElement(g.body).jsonObject["generationConfig"]!!.jsonObject
        assertEquals("minimal", config["thinkingConfig"]!!.jsonObject["thinkingLevel"]!!.jsonPrimitive.content)
        assertEquals(LlmDefaults.MAX_TOKENS.toString(), config["maxOutputTokens"]!!.jsonPrimitive.content)
    }

    @Test
    fun geminiParsesThinkingModelResponse() {
        // Skutečná odpověď gemini-3.5-flash-lite (28. 11. 2026, zkrácený podpis).
        val real = """{"candidates":[{"content":{"parts":[{"text":"ANO","thoughtSignature":"EpYBCpMBAVjVB3tp"}],"role":"model"},
            "finishReason":"STOP","index":0}],"usageMetadata":{"promptTokenCount":22,"candidatesTokenCount":1}}"""
        assertEquals("ANO", LlmProtocols.parse(LlmProvider.GEMINI, real))
        // Shrnutí přemýšlení (thought: true) a části bez textu se přeskočí.
        val mixed = """{"candidates":[{"content":{"parts":[{"text":"Uvažuji o fotce…","thought":true},
            {"thoughtSignature":"abc"},{"text":"NE"}]}}]}"""
        assertEquals("NE", LlmProtocols.parse(LlmProvider.GEMINI, mixed))
        // Přemýšlení snědlo celý limit: žádný text → null (ověření dá LATER).
        val onlyThoughts = """{"candidates":[{"content":{"parts":[{"thoughtSignature":"abc"}]},"finishReason":"MAX_TOKENS"}]}"""
        assertNull(LlmProtocols.parse(LlmProvider.GEMINI, onlyThoughts))
    }

    @Test
    fun parsesAllProviders() {
        assertEquals("ANO", LlmProtocols.parse(LlmProvider.CLAUDE, """{"content":[{"type":"text","text":" ANO\n"}],"stop_reason":"end_turn"}"""))
        assertNull(LlmProtocols.parse(LlmProvider.CLAUDE, """{"content":[],"stop_reason":"refusal"}"""))
        assertEquals("NE", LlmProtocols.parse(LlmProvider.GEMINI, """{"candidates":[{"content":{"parts":[{"text":"NE"}]}}]}"""))
        assertEquals("ANO", LlmProtocols.parse(LlmProvider.OPENAI, """{"choices":[{"message":{"role":"assistant","content":"ANO"}}]}"""))
        assertNull(LlmProtocols.parse(LlmProvider.OPENAI, """{"choices":[{"message":{"content":null,"refusal":"nelze"}}]}"""))
        assertNull(LlmProtocols.parse(LlmProvider.CLAUDE, "není json"))
    }

    @Test
    fun clientUsesSelectedProvider() = runTest {
        var provider = LlmProvider.CLAUDE
        val (calls, transport) = recorder("""{"content":[{"type":"text","text":"ANO"}]}""")
        val client = MultiProviderLlmClient({ provider }, allKeys, transport)
        assertEquals("ANO", client.complete(LlmRequest("Q")).text)
        assertEquals("klic-CLAUDE", calls.last().headers["x-api-key"])
        provider = LlmProvider.OPENAI
        client.complete(LlmRequest("Q"))
        assertEquals("Bearer klic-OPENAI", calls.last().headers["Authorization"])
    }

    @Test
    fun missingKeyErrorAndTimeoutNeverThrow() = runTest {
        val noKey = MultiProviderLlmClient({ LlmProvider.GEMINI }, { null }, LlmTransport { _, _ -> error("nemá se volat") })
        assertFalse(noKey.complete(LlmRequest("Q")).isOk)

        val httpError = MultiProviderLlmClient({ LlmProvider.CLAUDE }, allKeys, LlmTransport { _, _ -> null })
        assertNull(httpError.complete(LlmRequest("Q")).text)

        val throwing = MultiProviderLlmClient({ LlmProvider.CLAUDE }, allKeys, LlmTransport { _, _ -> throw IllegalStateException("x") })
        assertNull(throwing.complete(LlmRequest("Q")).text)

        val status = MultiProviderLlmClient({ LlmProvider.GEMINI }, allKeys, LlmTransport { _, _ -> HttpResult(404, "{\"error\":\"model not found\"}") })
        assertEquals("HTTP 404: {\"error\":\"model not found\"}", status.complete(LlmRequest("Q")).error)

        val slow = MultiProviderLlmClient({ LlmProvider.CLAUDE }, allKeys, LlmTransport { _, _ -> delay(10_000); HttpResult(200, "{}") })
        assertNull(slow.complete(LlmRequest("Q", timeoutMillis = 5_000)).text)
    }

    @Test
    fun sameAnswerFromAllThreeProviders() = runTest {
        val replies = mapOf(
            LlmProvider.CLAUDE to """{"content":[{"type":"text","text":"ANO"}]}""",
            LlmProvider.GEMINI to """{"candidates":[{"content":{"parts":[{"text":"ANO"}]}}]}""",
            LlmProvider.OPENAI to """{"choices":[{"message":{"content":"ANO"}}]}""",
        )
        val transport = LlmTransport { call, _ ->
            HttpResult(
                200,
                when {
                    call.url.contains("anthropic") -> replies[LlmProvider.CLAUDE]
                    call.url.contains("googleapis") -> replies[LlmProvider.GEMINI]
                    else -> replies[LlmProvider.OPENAI]
                },
            )
        }
        val client = MultiProviderLlmClient({ LlmProvider.CLAUDE }, allKeys, transport)
        val answers = LlmProvider.entries.map { client.completeWith(it, LlmRequest("Q", img)).text }
        assertEquals(listOf("ANO", "ANO", "ANO"), answers)
    }

    @Test
    fun cancellationIsNotSwallowed() = runTest {
        val client = MultiProviderLlmClient({ LlmProvider.CLAUDE }, allKeys, LlmTransport { _, _ -> throw kotlinx.coroutines.CancellationException("zavřeno") })
        var cancelled = false
        try {
            client.complete(LlmRequest("Q"))
        } catch (e: kotlinx.coroutines.CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }
}
