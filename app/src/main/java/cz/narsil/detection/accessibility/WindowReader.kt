package cz.narsil.detection.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/** Čtení stromu okna: ID prvků, URL z Firefoxu, krátké texty a ladicí výpis. */
internal object WindowReader {
    private const val TAG = "NarsilA11yTree"
    private const val MAX_NODES = 400
    private const val MAX_TEXTS = 20
    private const val MAX_TEXT_LEN = 120

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

    data class Result(val viewIds: Set<String>, val texts: List<String>, val url: String?, val selectedTab: String? = null)

    fun read(root: AccessibilityNodeInfo, app: WatchedApp?, dumpToLog: Boolean): Result {
        val ids = linkedSetOf<String>()
        val texts = mutableListOf<String>()
        var url: String? = null
        var selectedTab: String? = null
        var visited = 0
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
            if (!text.isNullOrEmpty() && texts.size < MAX_TEXTS) texts += text
            if (dumpToLog) {
                Log.d(TAG, "${"  ".repeat(depth.coerceAtMost(20))}${node.className?.toString()?.substringAfterLast('.')} id=$id" +
                    (if (!text.isNullOrEmpty()) " \"$text\"" else "") + (if (node.isScrollable) " [scroll]" else "") + (if (node.isSelected) " [selected]" else ""))
            }
            // Obsah webové stránky nečíst: soukromí a stovky uzlů by vyčerpaly MAX_NODES dřív než adresní řádek.
            if (app == WatchedApp.FIREFOX && node.className?.toString() == WEBVIEW_CLASS) continue
            for (i in node.childCount - 1 downTo 0) {
                node.getChild(i)?.let { stack.addLast(it to depth + 1) }
            }
        }
        return Result(ids, texts, url, selectedTab)
    }
}
