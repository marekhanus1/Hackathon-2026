package cz.narsil.detection.youtube

import android.content.Context

/**
 * Řetěz neplánovaných videí přes smrt procesu (160), obdoba `SessionPersistence` pro relaci feedu.
 * Ukládá jen čísla a ID videa z URL, žádný název ani obsah.
 */
interface ChainPersistence {
    fun load(): ChainState?
    fun save(state: ChainState?)
}

/** Stav [VideoChainTracker] k uložení. */
data class ChainState(
    val count: Int,
    val chainStartedAtMillis: Long?,
    val lastVideoAtMillis: Long,
    /** `v=` posledního videa ve Firefoxu; null = aplikace YouTube (bez ID). */
    val lastKey: String?,
    val unlockedUntilMillis: Long,
)

class PrefsChainPersistence(context: Context) : ChainPersistence {
    private val prefs = context.applicationContext.getSharedPreferences("detection_chain", Context.MODE_PRIVATE)

    override fun load(): ChainState? = runCatching {
        if (!prefs.contains("last")) return null
        ChainState(
            count = prefs.getInt("count", 0),
            chainStartedAtMillis = prefs.getLong("start", 0).takeIf { prefs.contains("start") },
            lastVideoAtMillis = prefs.getLong("last", 0),
            lastKey = prefs.getString("key", null),
            unlockedUntilMillis = prefs.getLong("unlocked", 0),
        )
    }.getOrNull()

    override fun save(state: ChainState?) {
        runCatching {
            prefs.edit().apply {
                clear()
                if (state != null) {
                    putInt("count", state.count)
                    state.chainStartedAtMillis?.let { putLong("start", it) }
                    putLong("last", state.lastVideoAtMillis)
                    state.lastKey?.let { putString("key", it) }
                    putLong("unlocked", state.unlockedUntilMillis)
                }
            }.apply()
        }
    }
}
