package cz.narsil.detection.accessibility

/**
 * Omezí, jak často se čte strom okna: změna okna/aplikace hned, scroll a změna obsahu
 * nejvýš jednou za [minIntervalMillis] pro stejnou aplikaci. Čistý Kotlin kvůli testům.
 */
class SnapshotThrottle(private val minIntervalMillis: Long = 1_000) {
    private var lastPackage: String? = null
    private var lastAt = Long.MIN_VALUE / 2

    fun shouldSnapshot(packageName: String, kind: SnapshotKind, now: Long): Boolean {
        val due = kind == SnapshotKind.WINDOW_CHANGED || packageName != lastPackage || now - lastAt >= minIntervalMillis
        if (due) {
            lastPackage = packageName
            lastAt = now
        }
        return due
    }
}
