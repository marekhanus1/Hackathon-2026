package cz.narsil.llm.client

import cz.narsil.core.LlmProvider
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** HTTP požadavek nezávislý na OkHttp (testy bez sítě). */
data class HttpCall(val url: String, val headers: Map<String, String>, val body: String)

/**
 * Formáty API tří poskytovatelů: sestavení požadavku a vytažení textu z odpovědi. Čistý Kotlin.
 *
 * - Claude: Messages API, `claude-haiku-5-5`. Haiku 5.5 nepřijímá `temperature` (400) – determinismus
 *   zajišťuje vypnuté thinking a effort `low`; odpověď je jedno slovo.
 * - Gemini: `generateContent`, `temperature 0`, `gemini-3.5-flash-lite`. Je to thinking model – přemýšlení
 *   je stažené na `thinkingLevel: minimal` (`thinkingBudget: 0` API odmítá 400). Části odpovědi s
 *   `thoughtSignature` nesou i text, části s `thought: true` (shrnutí přemýšlení) se přeskočí.
 * - OpenAI: Chat Completions, `temperature 0`.
 */
object LlmProtocols {
    const val CLAUDE_URL = "https://api.anthropic.com/v1/messages"
    const val CLAUDE_API_VERSION = "2026-10-21"
    const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"
    fun geminiUrl(model: String) = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    private val json = Json { ignoreUnknownKeys = true }

    fun build(provider: LlmProvider, request: LlmRequest, apiKey: String): HttpCall = when (provider) {
        LlmProvider.CLAUDE -> claude(request, apiKey)
        LlmProvider.GEMINI -> gemini(request, apiKey)
        LlmProvider.OPENAI -> openai(request, apiKey)
    }

    fun parse(provider: LlmProvider, body: String): String? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        when (provider) {
            LlmProvider.CLAUDE -> parseClaude(root)
            LlmProvider.GEMINI -> parseGemini(root)
            LlmProvider.OPENAI -> parseOpenAi(root)
        }
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun claude(r: LlmRequest, key: String): HttpCall {
        val body = buildJsonObject {
            put("model", LlmDefaults.CLAUDE_MODEL)
            put("max_tokens", r.maxTokens)
            putJsonObject("thinking") { put("type", "disabled") }
            putJsonObject("output_config") { put("effort", "low") }
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        r.imageJpeg?.let { img ->
                            addJsonObject {
                                put("type", "image")
                                putJsonObject("source") {
                                    put("type", "base64")
                                    put("media_type", "image/jpeg")
                                    put("data", b64(img))
                                }
                            }
                        }
                        addJsonObject {
                            put("type", "text")
                            put("text", r.prompt)
                        }
                    }
                }
            }
        }
        return HttpCall(
            CLAUDE_URL,
            mapOf("x-api-key" to key, "anthropic-version" to CLAUDE_API_VERSION, "content-type" to "application/json"),
            body.toString(),
        )
    }

    /** Text z odpovědi Claude; odmítnutí (`stop_reason: refusal`) = null. */
    private fun parseClaude(root: JsonObject): String? {
        if (root.str("stop_reason") == "refusal") return null
        return root["content"]?.jsonArray
            ?.mapNotNull { it as? JsonObject }
            ?.filter { it.str("type") == "text" }
            ?.joinToString("") { it.str("text").orEmpty() }
    }

    private fun gemini(r: LlmRequest, key: String): HttpCall {
        val body = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject { put("text", r.prompt) }
                        r.imageJpeg?.let { img ->
                            addJsonObject {
                                putJsonObject("inline_data") {
                                    put("mime_type", "image/jpeg")
                                    put("data", b64(img))
                                }
                            }
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0)
                put("maxOutputTokens", r.maxTokens)
                putJsonObject("thinkingConfig") { put("thinkingLevel", "minimal") }
            }
        }
        return HttpCall(
            geminiUrl(LlmDefaults.GEMINI_MODEL),
            mapOf("x-goog-api-key" to key, "content-type" to "application/json"),
            body.toString(),
        )
    }

    private fun parseGemini(root: JsonObject): String? =
        root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?.mapNotNull { part -> (part as? JsonObject)?.takeUnless { it.str("thought") == "true" }?.str("text") }
            ?.joinToString("")

    private fun openai(r: LlmRequest, key: String): HttpCall {
        val body = buildJsonObject {
            put("model", LlmDefaults.OPENAI_MODEL)
            put("temperature", 0)
            put("max_tokens", r.maxTokens)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    if (r.imageJpeg == null) {
                        put("content", r.prompt)
                    } else {
                        putJsonArray("content") {
                            addJsonObject {
                                put("type", "text")
                                put("text", r.prompt)
                            }
                            addJsonObject {
                                put("type", "image_url")
                                putJsonObject("image_url") { put("url", "data:image/jpeg;base64," + b64(r.imageJpeg)) }
                            }
                        }
                    }
                }
            }
        }
        return HttpCall(
            OPENAI_URL,
            mapOf("Authorization" to "Bearer $key", "content-type" to "application/json"),
            body.toString(),
        )
    }

    private fun parseOpenAi(root: JsonObject): String? {
        val message = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject ?: return null
        if (message["refusal"]?.let { it is JsonPrimitive && it.contentOrNull != null } == true) return null
        return when (val c = message["content"]) {
            is JsonPrimitive -> c.contentOrNull
            is JsonArray -> c.mapNotNull { (it as? JsonObject)?.str("text") }.joinToString("")
            else -> null
        }
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
