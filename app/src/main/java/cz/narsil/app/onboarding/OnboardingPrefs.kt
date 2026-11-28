package cz.narsil.app.onboarding

import android.content.Context

/** Byl průvodce dokončen? Při prvním spuštění se aplikace otevře na onboardingu (122). */
object OnboardingPrefs {
    private const val PREFS = "onboarding"
    private const val KEY_DONE = "done"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

    fun setDone(context: Context, done: Boolean = true) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, done).apply()
    }

    /** Kroky MIUI nejdou ověřit: zaškrtnutí si pamatujeme, ať se po návratu do průvodce neztratí. */
    fun miuiStep(context: Context, key: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("miui_$key", false)

    fun setMiuiStep(context: Context, key: String, done: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("miui_$key", done).apply()
    }
}
