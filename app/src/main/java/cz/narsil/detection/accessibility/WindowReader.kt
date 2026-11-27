package cz.narsil.detection.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/** Čtení stromu okna: ID prvků, URL z Chrome, krátké texty a ladicí výpis. */
internal object WindowReader {
    private const val TAG = "NarsilA11yTree"
    private const val MAX_NODES = 400
    private const val MAX_TEXTS = 20
    private const val MAX_TEXT_LEN = 120

    private val CHROME_URL_IDS = listOf("url_bar", "search_box_text", "location_bar_edit_text")

    data class Result(val viewIds: Set<String>, val texts: List<String>, val url: String?)

    fun read(root: AccessibilityNodeInfo, app: WatchedApp?, dumpToLog: Boolean): Result {
        val ids = linkedSetOf<String>()
        val texts = mutableListOf<String>()
        var url: String? = null
        var visited = 0
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = stack.removeLast()
            visited++
            val id = node.viewIdResourceName
            if (id != null) ids += id
            val text = (node.text ?: node.contentDescription)?.toString()?.trim()?.take(MAX_TEXT_LEN)
            if (app == WatchedApp.CHROME && url == null && id != null && CHROME_URL_IDS.any { id.endsWith(":id/$it") }) {
                url = node.text?.toString()?.takeIf { it.isNotBlank() }
            }
            if (!text.isNullOrEmpty() && texts.size < MAX_TEXTS) texts += text
            if (dumpToLog) {
                Log.d(TAG, "${"  ".repeat(depth.coerceAtMost(20))}${node.className?.toString()?.substringAfterLast('.')} id=$id" +
                    (if (!text.isNullOrEmpty()) " \"$text\"" else "") + (if (node.isScrollable) " [scroll]" else ""))
            }
            for (i in node.childCount - 1 downTo 0) {
                node.getChild(i)?.let { stack.addLast(it to depth + 1) }
            }
        }
        return Result(ids, texts, url)
    }
}
