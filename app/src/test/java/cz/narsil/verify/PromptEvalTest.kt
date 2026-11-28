package cz.narsil.verify

import cz.narsil.core.LlmProvider
import cz.narsil.llm.client.LlmKeys
import cz.narsil.llm.client.MultiProviderLlmClient
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class PromptEvalTest {
    @Test
    fun datasetIsBalanced() {
        assertEquals(20, PromptEvaluation.CASES.size)
        assertEquals(10, PromptEvaluation.CASES.count { it.expectedYes })
        PromptEvaluation.CASES.forEach { assertEquals(true, it.text.length <= 300) }
    }

    /**
     * Skutečné měření (stojí peníze, ~20 krátkých dotazů na poskytovatele). Spuštění:
     * `NARSIL_LIVE_EVAL=1 CLAUDE_API_KEY=… GEMINI_API_KEY=… OPENAI_API_KEY=… ./gradlew testDebugUnitTest --tests '*PromptEvalTest*'`
     * (bezplatná Gemini: přidat `NARSIL_LIVE_PAUSE_MS=4000`, jinak část dotazů skončí 429)
     * Výsledek se zapíše do `app/build/overeni-vysledky.md` (zkopírovat do docs/overeni-testy.md).
     */
    @Test
    fun liveEvaluation() = runBlocking {
        assumeTrue("NARSIL_LIVE_EVAL=1 není nastavené", System.getenv("NARSIL_LIVE_EVAL") == "1")
        val keys = LlmKeys { p -> System.getenv("${p.name}_API_KEY")?.takeIf { it.isNotBlank() } }
        val providers = LlmProvider.entries.filter { keys.keyFor(it) != null }
        assumeTrue("žádný klíč v prostředí", providers.isNotEmpty())
        val client = MultiProviderLlmClient({ providers.first() }, keys)
        val pause = System.getenv("NARSIL_LIVE_PAUSE_MS")?.toLongOrNull() ?: 0
        val reports = providers.map { PromptEvaluation.evaluate(client, it, pause) }
        File("build/overeni-vysledky.md").apply { parentFile?.mkdirs() }.writeText(PromptEvaluation.markdown(reports))
        reports.forEach { println("${it.provider}: ${it.correct}/20") }
    }
}
