package cz.narsil.llm.matcher

import cz.narsil.core.ContentMatch
import cz.narsil.core.ContentMatcher
import cz.narsil.llm.client.LlmClient
import cz.narsil.llm.client.LlmRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * [ContentMatcher] přes LLM (512, dřív 221): je název videa v plánovaném obsahu? Do LLM jde
 * **jen název videa a plán**. Timeout 3 s; chyba, timeout, nejasná odpověď nebo bez sítě → záloha
 * [KeywordMatcher]. Nikdy nehází. Prázdný plán = nic není plánované (bez volání LLM).
 */
class LlmContentMatcher(
    private val llm: LlmClient,
    private val isOnline: () -> Boolean = { true },
    private val timeoutMillis: Long = TIMEOUT_MILLIS,
) : ContentMatcher {

    override suspend fun isPlanned(title: String, plan: List<String>): ContentMatch {
        val items = plan.map { it.trim() }.filter { it.isNotEmpty() }
        if (items.isEmpty() || title.isBlank()) return ContentMatch(false, "prázdný plán nebo název")
        // Jasná shoda klíčovým slovem nepotřebuje LLM (rychlejší a funguje i offline).
        val keyword = KeywordMatcher.match(title, items)
        if (keyword.planned || !isOnline()) return keyword
        val answer = runCatching {
            llm.complete(LlmRequest(prompt(title, items), maxTokens = 80, timeoutMillis = timeoutMillis)).text
        }.getOrNull()
        return parse(answer) ?: keyword
    }

    companion object {
        const val TIMEOUT_MILLIS = 3_000L
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun prompt(title: String, plan: List<String>): String = buildString {
            append("Uživatel si večer naplánoval tento obsah:\n")
            plan.forEach { append("- ").append(it).append('\n') }
            append("Teď pouští video s názvem: \"").append(title.trim()).append("\".\n")
            append("Patří toto video k naplánovanému obsahu (stejné téma, pořad nebo díl)?\n")
            append("Odpověz jen JSON bez dalšího textu: {\"planned\": true nebo false, \"reason\": \"krátké zdůvodnění česky\"}")
        }

        /** Vytáhne první JSON objekt z odpovědi; null = nejasná odpověď (použije se záloha). */
        fun parse(answer: String?): ContentMatch? {
            if (answer.isNullOrBlank()) return null
            val start = answer.indexOf('{')
            val end = answer.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            return runCatching {
                val obj = json.parseToJsonElement(answer.substring(start, end + 1)) as JsonObject
                val planned = (obj["planned"] as? JsonPrimitive)?.booleanOrNull ?: return null
                val reason = (obj["reason"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                ContentMatch(planned, reason.take(200))
            }.getOrNull()
        }
    }
}
