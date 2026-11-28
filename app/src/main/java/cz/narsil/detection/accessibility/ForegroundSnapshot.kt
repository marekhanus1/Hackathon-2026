package cz.narsil.detection.accessibility

/** Druh události, ze které snímek vznikl. */
enum class SnapshotKind { WINDOW_CHANGED, SCROLLED, CONTENT_CHANGED }

/**
 * Co je právě v popředí (výstup 110, vstup klasifikace 111).
 *
 * @param packageName aplikace v popředí; u ignorovaných chatů a nesledovaných aplikací skutečný balíček,
 *   ale [viewIds] a [url] jsou vždy prázdné
 * @param app sledovaná aplikace, null = nesledovaná / chat / plocha
 * @param className třída okna z TYPE_WINDOW_STATE_CHANGED (např. aktivita), jinak poslední známá
 * @param url adresa z adresního řádku Firefoxu, jinak null
 * @param viewIds ID prvků (viewIdResourceName) viditelného okna – podle nich 111 pozná feed/Reels/Shorts/zprávy
 * @param texts krátké texty a contentDescription vybraných prvků (název videa YouTube pro 120); u chatů prázdné
 * @param selectedTab ID (bez balíčku) vybrané záložky ve spodní liště, např. "feed_tab", "clips_tab"; null = nezjištěno
 */
data class ForegroundSnapshot(
    val packageName: String,
    val app: WatchedApp?,
    val kind: SnapshotKind,
    val atMillis: Long,
    val className: String? = null,
    val url: String? = null,
    val viewIds: Set<String> = emptySet(),
    val texts: List<String> = emptyList(),
    val selectedTab: String? = null,
) {
    val isWatched: Boolean get() = app != null
}
