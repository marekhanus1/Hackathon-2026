package cz.narsil.llm.client

import cz.narsil.core.LlmProvider
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** Odpověď HTTP: [code] a tělo (i u chyby – kvůli diagnostice). */
data class HttpResult(val code: Int, val body: String?) {
    val isSuccess: Boolean get() = code in 200..299
}

/** Odeslání [HttpCall]; null = síťová chyba nebo timeout. Testy dosadí falešný transport. */
fun interface LlmTransport {
    suspend fun post(call: HttpCall, timeoutMillis: Long): HttpResult?
}

/** API klíče (jen z `local.properties` přes BuildConfig, nikdy v gitu). Prázdný = poskytovatel nepoužitelný. */
fun interface LlmKeys {
    fun keyFor(provider: LlmProvider): String?
}

/**
 * Klíče z `cz.narsil.BuildConfig`: `CLAUDE_API_KEY` už je v kostře (101), `GEMINI_API_KEY` a
 * `OPENAI_API_KEY` přidá Marek do `app/build.gradle.kts` – do té doby se čtou reflexí a chybí = null.
 */
object BuildConfigLlmKeys : LlmKeys {
    override fun keyFor(provider: LlmProvider): String? {
        val field = when (provider) {
            LlmProvider.CLAUDE -> "CLAUDE_API_KEY"
            LlmProvider.GEMINI -> "GEMINI_API_KEY"
            LlmProvider.OPENAI -> "OPENAI_API_KEY"
        }
        return runCatching { Class.forName("cz.narsil.BuildConfig").getField(field).get(null) as? String }
            .getOrNull()?.takeIf { it.isNotBlank() }
    }
}

/** OkHttp transport s pevným celkovým timeoutem (callTimeout) a zrušením při zrušení korutiny. */
class OkHttpTransport(base: OkHttpClient = OkHttpClient()) : LlmTransport {
    private val client = base

    override suspend fun post(call: HttpCall, timeoutMillis: Long): HttpResult? = withContext(Dispatchers.IO) {
        val http = client.newBuilder().callTimeout(timeoutMillis, TimeUnit.MILLISECONDS).build()
        val request = Request.Builder()
            .url(call.url)
            .apply { call.headers.forEach { (k, v) -> header(k, v) } }
            .post(call.body.toRequestBody(JSON))
            .build()
        suspendCancellableCoroutine { cont ->
            val okCall = http.newCall(request)
            cont.invokeOnCancellation { okCall.cancel() }
            okCall.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching { response.use { r -> HttpResult(r.code, r.body?.string()) } }.getOrNull()
                    if (cont.isActive) cont.resume(result)
                }
            })
        }
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}

/**
 * [LlmClient] pro tři poskytovatele (510). Poskytovatele bere z [provider] (typicky
 * `SettingsRepository.settings.value.llmProvider`). Nikdy nehází; chybějící klíč, chyba sítě,
 * HTTP chyba, timeout nebo odmítnutí = `text == null`.
 */
class MultiProviderLlmClient(
    private val provider: () -> LlmProvider,
    private val keys: LlmKeys = BuildConfigLlmKeys,
    private val transport: LlmTransport = OkHttpTransport(),
) : LlmClient {

    override suspend fun complete(request: LlmRequest): LlmResponse = completeWith(provider(), request)

    /** Dotaz na konkrétního poskytovatele (porovnání tří poskytovatelů v 530). */
    suspend fun completeWith(p: LlmProvider, request: LlmRequest): LlmResponse {
        val key = runCatchingCancellable { keys.keyFor(p) }.getOrNull()
            ?: return LlmResponse(p, null, "chybí API klíč pro $p (local.properties)")
        return runCatchingCancellable {
            val call = LlmProtocols.build(p, request, key)
            val http = withTimeoutOrNull(request.timeoutMillis) { transport.post(call, request.timeoutMillis) }
                ?: return LlmResponse(p, null, "chyba sítě nebo timeout")
            if (!http.isSuccess) {
                return LlmResponse(p, null, "HTTP ${http.code}: ${http.body.orEmpty().take(200)}")
            }
            val text = http.body?.let { LlmProtocols.parse(p, it) }
                ?: return LlmResponse(p, null, "prázdná nebo odmítnutá odpověď")
            LlmResponse(p, text)
        }.getOrElse { LlmResponse(p, null, it.message ?: "chyba") }
    }
}
