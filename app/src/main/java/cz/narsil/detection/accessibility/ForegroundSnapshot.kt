package cz.narsil.detection.accessibility

import cz.narsil.detection.youtube.VideoPlayback

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
 * @param video stav přehrávače YouTube (134): aplikace YouTube, nebo video na youtube.com ve Firefoxu; jinak null
 * @param editFields viditelná editovatelná pole jako „id|nápověda“ malými písmeny (149: pole pro zprávu / hledání);
 *   text, který uživatel píše, se nečte
 * @param scroll údaje ze scroll události (149, obecná detekce feedu); null u jiných událostí
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
    val video: VideoPlayback? = null,
    val editFields: List<String> = emptyList(),
    val scroll: ScrollSample? = null,
) {
    val isWatched: Boolean get() = app != null
}

/**
 * Jedna scroll událost bez obsahu (149): třída posouvaného prvku, posun a indexy položek.
 * [deltaX]/[deltaY] v px (0 = neznámé), indexy −1 = neznámé. Jetpack Compose (X) často neposílá posun ani indexy,
 * jen [scrollY], nebo vůbec nic – pak se posun bere jako „dopředu“ (ve feedu se posouvá skoro jen dolů).
 */
data class ScrollSample(
    val className: String?,
    /**
     * Který prvek se posunul (okno + třída + počet položek), 0 = neznámý. Hlavní seznam a karusel v něm
     * bývají stejná třída (RecyclerView) – bez toho by se jim míchaly indexy a směr posunu.
     */
    val sourceKey: Int = 0,
    /** Orientace posouvaného prvku podle CollectionInfo / akcí uzlu, null = neznámá. */
    val orientation: ScrollOrientation? = null,
    val deltaX: Int = 0,
    val deltaY: Int = 0,
    val fromIndex: Int = -1,
    val toIndex: Int = -1,
    val itemCount: Int = -1,
    /** Svislá pozice posouvaného prvku (Compose ji hlásí místo posunu), −1 = neznámá. */
    val scrollY: Int = -1,
)

/** Směr, kterým se prvek posouvá (z uzlu události, ne z posunu – ten často chybí). */
enum class ScrollOrientation { VERTICAL, HORIZONTAL }
