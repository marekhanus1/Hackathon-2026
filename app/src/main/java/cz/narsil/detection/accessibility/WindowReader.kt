package cz.narsil.detection.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import cz.narsil.detection.youtube.YouTubePlayback

/** Čtení stromu okna: ID prvků, URL z Firefoxu, krátké texty a ladicí výpis. */
internal object WindowReader {
    private const val TAG = "NarsilA11yTree"
    private const val MAX_NODES = 400
    private const val MAX_TEXTS = 20
    private const val MAX_TEXT_LEN = 120
    private const val MAX_EDIT_FIELDS = 5

    /** Přehrávač YouTube ve Firefoxu (134): kolik uzlů stránky projít a jaké texty brát (krátké = ovládání). */
    private const val MAX_WEB_NODES = 300
    private const val MAX_PLAYER_TEXTS = 60
    private const val MAX_PLAYER_TEXT_LEN = 40

    /**
     * Adresní řádek Firefoxu. Firefox 160 (Nothing 3a, 11/2026): Compose prvek s ID `ADDRESSBAR_URL_BOX`
     * (testTag bez prefixu balíčku), URL je v content-desc: " m.youtube.com/watch?v=…. Zadejte hledání nebo adresu".
     * Starší verze: `mozac_browser_toolbar_url_view` s URL v textu.
     */
    private val FIREFOX_URL_IDS = listOf("ADDRESSBAR_URL_BOX", "mozac_browser_toolbar_url_view", "mozac_browser_toolbar_edit_url_view")
    private const val WEBVIEW_CLASS = "android.webkit.WebView"

    /** URL z textu nebo popisu adresního řádku: první slovo bez koncové tečky, jen pokud vypadá jako adresa. */
    internal fun extractUrl(raw: String?): String? {
        val token = raw?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.trimEnd('.') ?: return null
        val host = token.removePrefix("https://").removePrefix("http://").substringBefore('/')
        return token.takeIf { host.contains('.') && host.none { it.isWhitespace() } }
    }

    /**
     * @param playerTexts krátké texty ze stránky videa YouTube ve Firefoxu (jen pro [YouTubePlayback.parse],
     *   neukládají se ani nelogují); jinde prázdné
     */
    data class Result(
        val viewIds: Set<String>,
        val texts: List<String>,
        val url: String?,
        val selectedTab: String? = null,
        val playerTexts: List<String> = emptyList(),
        /** Text uzlu WebView ve Firefoxu = titulek stránky (137). */
        val pageTitle: String? = null,
        /** Viditelná editovatelná pole „id|nápověda“ malými písmeny (149); text pole se nečte. */
        val editFields: List<String> = emptyList(),
    )

    /**
     * @param fallbackUrl poslední URL Firefoxu: adresní řádek se při scrollu a na celé obrazovce schová,
     *   stránka se ale nezměnila (134). Použije se, když se URL v tomto stromu nenajde.
     */
    fun read(root: AccessibilityNodeInfo, app: WatchedApp?, dumpToLog: Boolean, fallbackUrl: String? = null): Result {
        val ids = linkedSetOf<String>()
        val texts = mutableListOf<String>()
        var url: String? = null
        var selectedTab: String? = null
        var visited = 0
        val webViews = mutableListOf<AccessibilityNodeInfo>()
        var pageTitle: String? = null
        val editFields = mutableListOf<String>()
        // Obecné aplikace (149): jen ID a nápovědy polí, žádné texty ani výpis stromu (soukromí).
        val generic = app?.generic == true
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = stack.removeLast()
            visited++
            // Instagram drží ve stromu všechny záložky (ViewPager); stránky mimo obrazovku i s podstromem přeskočit.
            if (depth > 0 && !node.isVisibleToUser) continue
            val id = node.viewIdResourceName
            if (id != null && node.isSelected && id.endsWith("_tab") && selectedTab == null) {
                selectedTab = id.substringAfter(":id/")
            }
            if (id != null) ids += id
            val text = (node.text ?: node.contentDescription)?.toString()?.trim()?.take(MAX_TEXT_LEN)
            if (app == WatchedApp.FIREFOX && url == null && id != null && FIREFOX_URL_IDS.any { id == it || id.endsWith(":id/$it") }) {
                url = extractUrl(node.text?.toString()) ?: extractUrl(node.contentDescription?.toString())
            }
            if (node.isEditable && editFields.size < MAX_EDIT_FIELDS) {
                editFields += "${id?.substringAfter(":id/").orEmpty()}|${node.hintText?.toString().orEmpty()}".lowercase().take(MAX_TEXT_LEN)
            }
            if (generic) {
                // Výpis jen tříd a ID, bez textů (ladění obecné detekce, 149).
                if (dumpToLog) {
                    Log.d(TAG, "${"  ".repeat(depth.coerceAtMost(20))}${node.className?.toString()?.substringAfterLast('.')} id=$id" +
                        (if (node.isScrollable) " [scroll]" else "") + (if (node.isEditable) " [edit]" else ""))
                }
                for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it to depth + 1) }
                continue
            }
            if (!text.isNullOrEmpty() && texts.size < MAX_TEXTS) texts += text
            if (dumpToLog) {
                Log.d(TAG, "${"  ".repeat(depth.coerceAtMost(20))}${node.className?.toString()?.substringAfterLast('.')} id=$id" +
                    (if (!text.isNullOrEmpty()) " \"$text\"" else "") + (if (node.isScrollable) " [scroll]" else "") + (if (node.isSelected) " [selected]" else ""))
            }
            // Obsah webové stránky nečíst: soukromí a stovky uzlů by vyčerpaly MAX_NODES dřív než adresní řádek.
            if (app == WatchedApp.FIREFOX && node.className?.toString() == WEBVIEW_CLASS) {
                if (pageTitle == null) pageTitle = node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                webViews += node
                continue
            }
            for (i in node.childCount - 1 downTo 0) {
                node.getChild(i)?.let { stack.addLast(it to depth + 1) }
            }
        }
        if (app == WatchedApp.FIREFOX && url == null) url = fallbackUrl
        // Výjimka (134): na stránce jednoho videa YouTube přečíst ovládání přehrávače (čas, „Přehrát znovu“).
        val playerTexts = if (YouTubePlayback.videoIdFromUrl(url) != null) readPlayerTexts(webViews) else emptyList()
        return Result(ids, texts, url, selectedTab, playerTexts, pageTitle, editFields)
    }

    /** Krátké texty viditelných uzlů stránky (ovládání přehrávače je nahoře, stačí omezený průchod). */
    private fun readPlayerTexts(webViews: List<AccessibilityNodeInfo>): List<String> {
        val out = mutableListOf<String>()
        var visited = 0
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        webViews.asReversed().forEach { w -> for (i in w.childCount - 1 downTo 0) w.getChild(i)?.let(stack::addLast) }
        while (stack.isNotEmpty() && visited < MAX_WEB_NODES && out.size < MAX_PLAYER_TEXTS) {
            val node = stack.removeLast()
            visited++
            if (!node.isVisibleToUser) continue
            val text = (node.text ?: node.contentDescription)?.toString()?.trim()
            if (!text.isNullOrEmpty() && text.length <= MAX_PLAYER_TEXT_LEN) out += text
            for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let(stack::addLast)
        }
        return out
    }
}
