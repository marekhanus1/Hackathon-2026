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
        WatchedApp.FIREFOX -> s.url?.let(::classifyUrl)
        WatchedApp.INSTAGRAM -> classifyInstagram(s)
        // TikTok má obfuskovaná ID: hlavní obrazovka je feed, pokud nejde zjevně o zprávy/profil.
        WatchedApp.TIKTOK -> classifyByIds(s, SurfaceRules.TIKTOK, default = Surface.FEED)
        WatchedApp.YOUTUBE -> classifyByIds(s, SurfaceRules.YOUTUBE, default = Surface.MESSAGES)
        // 149: obecně jen zprávy a hledání; feed rozhodne chování scrollu (FeedBehavior v AccessibilityTriggerSource).
        WatchedApp.X, WatchedApp.REDDIT, WatchedApp.FACEBOOK, WatchedApp.THREADS, WatchedApp.PINTEREST,
        WatchedApp.NINEGAG, WatchedApp.TUMBLR, WatchedApp.LINKEDIN, WatchedApp.BLUESKY, WatchedApp.OTHER -> classifyGeneric(s)
    }

    /**
     * Obecná aplikace (149): rozhovor → MESSAGES (nikdy nezasahovat), hledání → SEARCH, jinak null
     * (feed se pozná podle chování, ne z jednoho snímku).
     *
     * Rozhovor: viditelné pole s nápovědou pro zprávu („Napište zprávu“, „Start a message“), nebo ID / okno
     * typické pro konverzaci. Tlačítka, záložky a ikony („chat_tab“ ve spodní liště je vidět všude) se nepočítají.
     */
    fun classifyGeneric(s: ForegroundSnapshot): Surface? {
        val ids = idHaystack(s)
        val edits = s.editFields
        val chat = SurfaceRules.CHAT_IDS.matches(ids) ||
            edits.any { e -> SurfaceRules.CHAT_HINTS.any { e.contains(it) } }
        if (chat) return Surface.MESSAGES
        val search = edits.any { e -> SurfaceRules.SEARCH_HINTS.any { e.contains(it) } } ||
            SurfaceClassifier.Rule(Surface.SEARCH, SurfaceRules.SEARCH_FRAGMENTS, SurfaceRules.SEARCH_EXCLUDE).matches(ids)
        return if (search) Surface.SEARCH else null
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
        return rules.firstOrNull { rule -> rule.matches(haystack) }?.surface ?: default
    }

    /** Které pravidlo rozhodlo (ladění v logu služby, 148). */
    fun explain(s: ForegroundSnapshot): String {
        val haystack = idHaystack(s)
        val rules = when (s.app) {
            WatchedApp.YOUTUBE -> SurfaceRules.YOUTUBE
            WatchedApp.TIKTOK -> SurfaceRules.TIKTOK
            else -> return ""
        }
        val rule = rules.firstOrNull { it.matches(haystack) } ?: return "výchozí"
        return haystack.firstOrNull { h -> rule.fragments.any { h.contains(it) } && rule.exclude.none { h.contains(it) } } ?: rule.surface.name
    }

    /** Web ve Firefoxu podle URL (bez schématu, jak ho ukazuje adresní řádek). */
    fun classifyUrl(raw: String): Surface? {
        val url = raw.lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").removePrefix("m.")
        val host = url.substringBefore('/')
        val path = url.removePrefix(host)
        return when {
            // Výsledky vyhledávání se nepočítají (148).
            isSearchPath(host, path) -> Surface.SEARCH
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
            host.endsWith("reddit.com") -> when {
                path.startsWith("/message") || path.startsWith("/chat") -> Surface.MESSAGES
                else -> Surface.FEED
            }
            else -> null
        }
    }

    /** Stránka výsledků vyhledávání na sociálních sítích (148). [path] včetně dotazu, malými písmeny. */
    private fun isSearchPath(host: String, path: String): Boolean = when {
        host.endsWith("youtube.com") -> path.startsWith("/results") || path.startsWith("/hashtag/")
        host.endsWith("instagram.com") -> path.startsWith("/explore/search") || path.startsWith("/explore/tags/")
        host == "x.com" || host.endsWith("twitter.com") -> path.startsWith("/search") || path.startsWith("/explore")
        host.endsWith("reddit.com") -> path.startsWith("/search") || path.contains("/search/") || path.contains("/search?")
        host.endsWith("tiktok.com") -> path.startsWith("/search") || path.startsWith("/tag/")
        host.endsWith("facebook.com") -> path.startsWith("/search")
        else -> false
    }

    /**
     * @param fragments část ID prvku nebo názvu okna (malými písmeny)
     * @param exclude ID s fragmentem se nepočítá, když obsahuje i tohle (např. tlačítko lupy na feedu)
     */
    data class Rule(val surface: Surface, val fragments: List<String>, val exclude: List<String> = emptyList()) {
        fun matches(haystack: List<String>): Boolean =
            fragments.any { f -> haystack.any { h -> h.contains(f) && exclude.none { h.contains(it) } } }
    }
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
        // Prozkoumat / hledání (148) a profil: nepočítat.
        "search_tab" to Surface.SEARCH,
        "profile_tab" to Surface.MESSAGES,
    )

    val TIKTOK = listOf(
        SurfaceClassifier.Rule(Surface.MESSAGES, listOf("chatroom", "chat_", "im.chat", "inboxactivity", "messageactivity")),
        SurfaceClassifier.Rule(Surface.SEARCH, listOf("searchactivity", "generalsearch", "search.ui")),
    )

    /**
     * Pole hledání a seznam výsledků v aplikacích bez vlastních pravidel (148). Ne lupa na feedu:
     * tlačítka, ikony, položky menu a záložky s „search“ v ID se nepočítají.
     */
    val SEARCH_FRAGMENTS = listOf("search")

    /** Obecná aplikace (149): nápověda pole hledání (malými písmeny). */
    val SEARCH_HINTS = listOf("hledat", "vyhled", "search")

    /** Obecná aplikace (149): nápověda pole pro zprávu. */
    val CHAT_HINTS = listOf("zpráv", "message", "napiš", "napište", "chat", "odpověz soukrom")

    /** Obecná aplikace (149): ID / okno konverzace; bez tlačítek, záložek a ikon. */
    val CHAT_IDS = SurfaceClassifier.Rule(
        Surface.MESSAGES,
        listOf("conversation", "chat", "direct_message", "dm_", "dms_", "message_list", "messages_list", "message_input", "messaging", "inbox_thread"),
        exclude = listOf("tab", "nav", "button", "icon", "badge", "menu", "fab", "count", "indicator", "bottom_bar", "toolbar_action"),
    )
    val SEARCH_EXCLUDE = listOf("button", "icon", "menu", "tab", "voice", "action_search", "_hint")

    /**
     * YouTube (148): nejdřív přehrávač (Shorts, otevřené video), pak vyhledávání, nakonec obecné „shorts“
     * (polička Shorts je i ve výsledcích hledání a na domovské stránce – kvůli ní se dřív hledání počítalo jako Shorts).
     */
    val YOUTUBE = listOf(
        SurfaceClassifier.Rule(Surface.SHORTS, listOf("reel_player", "reel_recycler")),
        // Ne „watch_while“: watch_while_layout_coordinator_layout je viditelné na všech obrazovkách (ověřeno na Nothing 3a).
        SurfaceClassifier.Rule(Surface.VIDEO, listOf("watch_player")),
        SurfaceClassifier.Rule(Surface.SEARCH, SEARCH_FRAGMENTS, SEARCH_EXCLUDE),
        SurfaceClassifier.Rule(Surface.VIDEO, listOf("player_view")),
        SurfaceClassifier.Rule(Surface.SHORTS, listOf("shorts")),
    )
}
