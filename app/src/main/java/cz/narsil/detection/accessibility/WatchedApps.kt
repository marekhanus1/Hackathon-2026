package cz.narsil.detection.accessibility

/**
 * Sledované aplikace. Ostatní (hlavně chaty) se nečtou: jen víme, že je v popředí „něco jiného“.
 *
 * @param generic obecná detekce (149): feed se pozná podle chování scrollu ([FeedBehavior]), ne podle ID.
 *   Z okna se čtou jen ID prvků a nápovědy polí (chat / hledání), žádné texty.
 */
enum class WatchedApp(val packages: Set<String>, val generic: Boolean = false) {
    INSTAGRAM(setOf("com.instagram.android")),
    TIKTOK(setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme")),
    YOUTUBE(setOf("com.google.android.youtube")),
    // Web se sleduje ve Firefoxu, ne v Chrome (docs/decisions.md 2026-11-28).
    FIREFOX(setOf("org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fenix")),

    // 149: známé sítě s nekonečným feedem, obecná detekce. Názvy = core.TrackedApp.
    X(setOf("com.twitter.android"), generic = true),
    REDDIT(setOf("com.reddit.frontpage"), generic = true),
    FACEBOOK(setOf("com.facebook.katana", "com.facebook.lite"), generic = true),
    THREADS(setOf("com.instagram.barcelona"), generic = true),
    PINTEREST(setOf("com.pinterest"), generic = true),
    NINEGAG(setOf("com.ninegag.android.app"), generic = true),
    TUMBLR(setOf("com.tumblr"), generic = true),
    LINKEDIN(setOf("com.linkedin.android"), generic = true),
    BLUESKY(setOf("xyz.blueskyweb.app"), generic = true),

    /** Další aplikace s nekonečným feedem (149): jen když je to zapnuté v Nastavení, viz [isOtherCandidate]. */
    OTHER(emptySet(), generic = true);

    companion object {
        /** Chaty: nikdy nečíst obsah ani ID prvků (rozhodnutí 5 v briefu). Snapchat je hlavně chat, proto tady. */
        val IGNORED_PACKAGES = setOf(
            "com.facebook.orca", // Messenger
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.google.android.apps.messaging",
            "com.discord",
            "com.snapchat.android",
            "com.viber.voip",
            "org.thoughtcrime.securesms", // Signal
        )

        /** Systém, plocha, klávesnice a nástroje: nikdy ne „další aplikace s feedem“. */
        private val OTHER_EXCLUDED_FRAGMENTS = listOf(
            "launcher", "inputmethod", "keyboard", "systemui", "settings", "permissioncontroller", "packageinstaller",
            "dialer", "contacts", "camera", "clock", "calculator", "calendar", "mail", "maps", "documentsui", "files",
        )

        fun of(packageName: String?): WatchedApp? =
            packageName?.let { p -> entries.firstOrNull { p in it.packages } }

        fun isIgnored(packageName: String?): Boolean = packageName in IGNORED_PACKAGES

        /** Smí se [packageName] sledovat jako [OTHER]? Ne známé, ne chaty, ne systém a ne Narsil. */
        fun isOtherCandidate(packageName: String, ownPackage: String = "cz.narsil"): Boolean {
            if (of(packageName) != null || isIgnored(packageName) || packageName.startsWith(ownPackage)) return false
            if (packageName == "android" || packageName.startsWith("com.android.") || packageName.startsWith("com.google.android.gms")) return false
            if (packageName.startsWith("com.nothing.") || packageName == "com.google.android.gm") return false
            return OTHER_EXCLUDED_FRAGMENTS.none { packageName.contains(it) }
        }
    }
}
