package cz.narsil.detection.accessibility

import android.content.Context
import cz.narsil.core.Surface

/**
 * Rozpracovaná relace a zaplacený čas přes smrt procesu (MIUI Narsil vypíná): po restartu služby
 * relace pokračuje, nezačíná od nuly, a odemčený čas platí dál. Ukládá jen čísla, žádný obsah.
 */
interface SessionPersistence {
    fun load(): SavedSession?
    fun save(saved: SavedSession?)
}

/** Stav [ScrollSessionTracker] k uložení. [session] null = jen odemčený čas. */
data class SavedSession(
    val startedAtMillis: Long,
    val packageName: String,
    val surface: Surface,
    val lastEndlessAtMillis: Long,
    val unlockedUntilMillis: Long,
    val hasSession: Boolean = true,
    /** Do kdy návrat na feed po „Zavřít“ zasáhne hned (155, 158); 0 = žádné okno. */
    val relapseUntilMillis: Long = 0,
)

class PrefsSessionPersistence(context: Context) : SessionPersistence {
    private val prefs = context.applicationContext.getSharedPreferences("detection_session", Context.MODE_PRIVATE)

    override fun load(): SavedSession? = runCatching {
        if (!prefs.contains("last") && !prefs.contains("relapse")) return null
        SavedSession(
            startedAtMillis = prefs.getLong("start", 0),
            packageName = prefs.getString("pkg", null).orEmpty(),
            surface = Surface.valueOf(prefs.getString("surface", Surface.FEED.name)!!),
            lastEndlessAtMillis = prefs.getLong("last", 0),
            unlockedUntilMillis = prefs.getLong("unlocked", 0),
            hasSession = prefs.getBoolean("has", true),
            relapseUntilMillis = prefs.getLong("relapse", 0),
        )
    }.getOrNull()

    override fun save(saved: SavedSession?) {
        runCatching {
            prefs.edit().apply {
                if (saved == null) clear() else {
                    putLong("start", saved.startedAtMillis)
                    putString("pkg", saved.packageName)
                    putString("surface", saved.surface.name)
                    putLong("last", saved.lastEndlessAtMillis)
                    putLong("unlocked", saved.unlockedUntilMillis)
                    putBoolean("has", saved.hasSession)
                    putLong("relapse", saved.relapseUntilMillis)
                }
            }.apply()
        }
    }
}
