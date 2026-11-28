package cz.narsil.llm.client

import cz.narsil.core.LlmProvider
import kotlin.coroutines.cancellation.CancellationException

/**
 * Jako `runCatching`, ale zrušení korutiny ([CancellationException]) nepolyká – zavřená obrazovka
 * nebo zrušený scope opravdu skončí. Používá se ve všech „nikdy nehází“ místech 5xx.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

/**
 * Jeden krátký dotaz na LLM (ověření úkolu, párování obsahu).
 *
 * @param prompt text dotazu
 * @param imageJpeg volitelná fotka (JPEG, už zmenšená – 501 ji zmenší na 512 px)
 * @param maxTokens strop výstupu (ověření potřebuje jediné slovo)
 * @param timeoutMillis celkový čas na odpověď (SPEC 7: 5 s, párování 3 s)
 */
data class LlmRequest(
    val prompt: String,
    val imageJpeg: ByteArray? = null,
    val maxTokens: Int = LlmDefaults.MAX_TOKENS,
    val timeoutMillis: Long = LlmDefaults.TIMEOUT_MILLIS,
) {
    override fun equals(other: Any?): Boolean =
        other is LlmRequest && prompt == other.prompt && maxTokens == other.maxTokens &&
            timeoutMillis == other.timeoutMillis && imageJpeg.contentEqualsNullable(other.imageJpeg)

    override fun hashCode(): Int = prompt.hashCode() * 31 + (imageJpeg?.contentHashCode() ?: 0)

    private fun ByteArray?.contentEqualsNullable(o: ByteArray?) = if (this == null) o == null else o != null && contentEquals(o)
}

/** Výsledek dotazu. [text] = null při chybě, timeoutu, chybějícím klíči nebo odmítnutí. */
data class LlmResponse(
    val provider: LlmProvider,
    val text: String?,
    val error: String? = null,
) {
    val isOk: Boolean get() = text != null
}

/** Klient pro Claude, Gemini a OpenAI (510). Nikdy nehází – chyba = [LlmResponse.text] null. */
interface LlmClient {
    suspend fun complete(request: LlmRequest): LlmResponse
}

/** Výchozí parametry (SPEC 7): teplota 0, krátký výstup, timeout 5 s, nejmenší rychlý model. */
object LlmDefaults {
    const val TIMEOUT_MILLIS = 5_000L
    const val MAX_TOKENS = 16

    /**
     * Nejmenší rychlé modely. Claude ověřen podle dokumentace Anthropic (Haiku 5.5);
     * Gemini a OpenAI ověřit s klíčem (530) – kdyby model neexistoval, API vrátí 404 a ověření dá LATER.
     */
    const val CLAUDE_MODEL = "claude-haiku-5-5"
    const val GEMINI_MODEL = "gemini-2.5-flash-lite"
    const val OPENAI_MODEL = "gpt-4.1-nano"
}
