package cz.narsil.ui.screens.pet

/** Výsledek volání Gemini API pro sovu Narsilku. */
sealed interface PetApiResult {
    data class Success(val text: String) : PetApiResult
    data object RateLimited : PetApiResult
    data object Unavailable : PetApiResult
}

/**
 * Chrání bezplatnou kvótu Gemini Free API: hlídá minimální rozestup mezi dotazy,
 * denní strop volání a automatický cooldown po chybě nebo HTTP 429.
 */
class PetRateLimiter(
    private val store: PetMemoryStore,
    private val minIntervalMillis: Long = 6_000L,
    private val maxDailyCalls: Int = 25,
    private val rateLimitCooldownMillis: Long = 60_000L,
    private val errorCooldownMillis: Long = 20_000L,
) {
    fun canCallApi(nowMillis: Long, dayKey: Long): Boolean {
        val cooldownUntil = store.getLong(KEY_COOLDOWN_UNTIL, 0L)
        if (nowMillis < cooldownUntil) return false

        val lastCall = store.getLong(KEY_LAST_CALL, 0L)
        if (lastCall > 0L && nowMillis - lastCall < minIntervalMillis) return false

        return usedToday(dayKey) < maxDailyCalls
    }

    fun recordCall(nowMillis: Long, dayKey: Long) {
        store.putLong(KEY_LAST_CALL, nowMillis)
        val nextCount = usedToday(dayKey) + 1
        store.putLong(KEY_API_DAY, dayKey)
        store.putInt(KEY_API_COUNT, nextCount)
    }

    fun recordError(nowMillis: Long, isRateLimit: Boolean) {
        val cooldown = if (isRateLimit) rateLimitCooldownMillis else errorCooldownMillis
        store.putLong(KEY_COOLDOWN_UNTIL, nowMillis + cooldown)
    }

    fun remainingToday(dayKey: Long): Int =
        (maxDailyCalls - usedToday(dayKey)).coerceAtLeast(0)

    private fun usedToday(dayKey: Long): Int {
        val savedDay = store.getLong(KEY_API_DAY, Long.MIN_VALUE)
        return if (savedDay == dayKey) store.getInt(KEY_API_COUNT, 0) else 0
    }

    private companion object {
        const val KEY_LAST_CALL = "api_last_call"
        const val KEY_API_DAY = "api_day"
        const val KEY_API_COUNT = "api_count"
        const val KEY_COOLDOWN_UNTIL = "api_cooldown_until"
    }
}
