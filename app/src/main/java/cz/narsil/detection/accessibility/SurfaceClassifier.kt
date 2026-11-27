package cz.narsil.detection.accessibility

import cz.narsil.core.Surface

/**
 * Povrch z jednoho snímku 110 (111). null = nesledovaná aplikace, plocha nebo web mimo sociální sítě.
 * V nejistotě [Surface.MESSAGES] (raději zásah propásnout než otravovat, SPEC 3).
 *
 * Fragmenty ID prvků jsou odhad z veřejně známých názvů; ověřit na telefonu
 * (`adb logcat -s NarsilA11yTree`) a doplnit tady do [SurfaceRules].
 */
object SurfaceClassifier {

    fun classify(s: ForegroundSnapshot): Surface? = when (s.app) {
        null -> null
        WatchedApp.CHROME -> s.url?.let(::classifyUrl)
        WatchedApp.INSTAGRAM -> classifyInstagram(s)
        // TikTok má obfuskovaná ID: hlavní obrazovka je feed, pokud nejde zjevně o zprávy/profil.
        WatchedApp.TIKTOK -> classifyByIds(s, SurfaceRules.TIKTOK, default = Surface.FEED)
        WatchedApp.YOUTUBE -> classifyByIds(s, SurfaceRules.YOUTUBE, default = Surface.MESSAGES)
    }

    /**
     * Instagram (ověřeno na Nothing 3a, 11/2026): viditelná ID zpráv → MESSAGES, celoobrazovkový přehrávač
     * Reels → REELS, pak vybraná záložka, pak ID feedu. Inline reels ve feedu mají clips_video_*, proto
     * REELS jen podle clips_viewer_*.
     */
    private fun classifyInstagram(s: ForegroundSnapshot): Surface {
        val ids = idHaystack(s)
        fun has(rule: List<String>) = rule.any { f -> ids.any { it.contains(f) } }
        return when {
            has(SurfaceRules.INSTAGRAM_MESSAGES) -> Surface.MESSAGES
            has(SurfaceRules.INSTAGRAM_REELS) -> Surface.REELS
            s.selectedTab != null -> SurfaceRules.INSTAGRAM_TABS[s.selectedTab] ?: Surface.MESSAGES
            has(SurfaceRules.INSTAGRAM_FEED) -> Surface.FEED
            else -> Surface.MESSAGES
        }
    }

    private fun idHaystack(s: ForegroundSnapshot): List<String> = buildList {
        s.className?.let { add(it.lowercase()) }
        s.viewIds.mapTo(this) { it.substringAfter(":id/").lowercase() }
    }

    private fun classifyByIds(s: ForegroundSnapshot, rules: List<Rule>, default: Surface): Surface {
        val haystack = idHaystack(s)
        // Pravidla jsou seřazená podle priority: zprávy první.
        return rules.firstOrNull { rule -> rule.fragments.any { f -> haystack.any { it.contains(f) } } }?.surface ?: default
    }

    /** Web v Chrome podle URL (bez schématu, jak ho ukazuje adresní řádek). */
    fun classifyUrl(raw: String): Surface? {
        val url = raw.lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").removePrefix("m.")
        val host = url.substringBefore('/')
        val path = url.removePrefix(host)
        return when {
            host.endsWith("instagram.com") -> when {
                path.startsWith("/direct") -> Surface.MESSAGES
                path.startsWith("/reels") || path.startsWith("/reel/") -> Surface.REELS
                else -> Surface.FEED
            }
            host.endsWith("tiktok.com") -> if (path.startsWith("/messages")) Surface.MESSAGES else Surface.FEED
            host.endsWith("youtube.com") || host == "youtu.be" -> when {
                path.startsWith("/shorts") -> Surface.SHORTS
                path.startsWith("/watch") || host == "youtu.be" -> Surface.VIDEO
                else -> Surface.MESSAGES
            }
            host.endsWith("facebook.com") -> when {
                path.startsWith("/messages") -> Surface.MESSAGES
                path.startsWith("/reel") -> Surface.REELS
                else -> Surface.FEED
            }
            host == "x.com" || host.endsWith("twitter.com") -> if (path.startsWith("/messages")) Surface.MESSAGES else Surface.FEED
            else -> null
        }
    }

    data class Rule(val surface: Surface, val fragments: List<String>)
}

/** Fragmenty názvu aktivity a ID prvků (malými písmeny, bez balíčku), seřazené podle priority. */
object SurfaceRules {
    // Instagram: ID ze stromu bez neviditelných stránek ViewPageru (WindowReader).
    val INSTAGRAM_MESSAGES = listOf("direct_inbox_action_bar", "direct_thread", "row_thread", "thread_title", "message_list")
    val INSTAGRAM_REELS = listOf("clips_viewer_container", "clips_viewer_view_pager", "root_clips_layout")
    val INSTAGRAM_FEED = listOf("main_feed_action_bar", "row_feed")
    val INSTAGRAM_TABS = mapOf(
        "feed_tab" to Surface.FEED,
        "clips_tab" to Surface.REELS,
        "direct_tab" to Surface.MESSAGES,
        // Prozkoumat a profil: v nejistotě nezasahovat.
        "search_tab" to Surface.MESSAGES,
        "profile_tab" to Surface.MESSAGES,
    )

    val TIKTOK = listOf(
        SurfaceClassifier.Rule(Surface.MESSAGES, listOf("chatroom", "chat_", "im.chat", "inboxactivity", "messageactivity")),
    )
    val YOUTUBE = listOf(
        SurfaceClassifier.Rule(Surface.SHORTS, listOf("reel_player", "reel_recycler", "shorts")),
        SurfaceClassifier.Rule(Surface.VIDEO, listOf("watch_player", "player_view", "watch_while", "watch_list")),
    )
}
