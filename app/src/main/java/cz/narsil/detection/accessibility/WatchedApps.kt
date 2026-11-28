package cz.narsil.detection.accessibility

/** Sledované aplikace. Ostatní (hlavně chaty) se nečtou: jen víme, že je v popředí „něco jiného“. */
enum class WatchedApp(val packages: Set<String>) {
    INSTAGRAM(setOf("com.instagram.android")),
    TIKTOK(setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme")),
    YOUTUBE(setOf("com.google.android.youtube")),
    // Web se sleduje ve Firefoxu, ne v Chrome (docs/decisions.md 2026-11-28).
    FIREFOX(setOf("org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fenix"));

    companion object {
        /** Chaty: nikdy nečíst obsah ani ID prvků (rozhodnutí 5 v briefu). */
        val IGNORED_PACKAGES = setOf(
            "com.facebook.orca", // Messenger
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.google.android.apps.messaging",
            "com.discord",
            "com.snapchat.android",
        )

        fun of(packageName: String?): WatchedApp? =
            packageName?.let { p -> entries.firstOrNull { p in it.packages } }

        fun isIgnored(packageName: String?): Boolean = packageName in IGNORED_PACKAGES
    }
}
