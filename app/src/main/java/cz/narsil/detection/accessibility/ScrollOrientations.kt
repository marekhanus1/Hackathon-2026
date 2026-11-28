package cz.narsil.detection.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction

/** Orientace posouvaného uzlu: CollectionInfo seznamu, jinak směrové akce posunu. Čte jen strukturu, ne obsah. */
internal object ScrollOrientations {
    fun of(node: AccessibilityNodeInfo): ScrollOrientation? {
        val info = node.collectionInfo
        val byCollection = info?.let { fromCollection(it.rowCount, it.columnCount) }
        if (byCollection != null) return byCollection
        val ids = node.actionList.orEmpty().map { it.id }
        return fromActions(
            vertical = ids.any { it == AccessibilityAction.ACTION_SCROLL_DOWN.id || it == AccessibilityAction.ACTION_SCROLL_UP.id },
            horizontal = ids.any { it == AccessibilityAction.ACTION_SCROLL_LEFT.id || it == AccessibilityAction.ACTION_SCROLL_RIGHT.id },
        )
    }

    /** Vodorovný LinearLayoutManager hlásí 1 řádek × N sloupců, svislý N řádků × 1 sloupec. Mřížka = neznámá. */
    fun fromCollection(rows: Int, columns: Int): ScrollOrientation? = when {
        rows == 1 && columns > 1 -> ScrollOrientation.HORIZONTAL
        columns == 1 && rows > 1 -> ScrollOrientation.VERTICAL
        else -> null
    }

    fun fromActions(vertical: Boolean, horizontal: Boolean): ScrollOrientation? = when {
        horizontal && !vertical -> ScrollOrientation.HORIZONTAL
        vertical && !horizontal -> ScrollOrientation.VERTICAL
        else -> null
    }
}
