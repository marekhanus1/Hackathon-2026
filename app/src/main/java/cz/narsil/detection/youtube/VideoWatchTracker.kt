package cz.narsil.detection.youtube

/**
 * Dosledované video pro otázku „Co ti to video dalo?“ (156). Čistý Kotlin, čas jen z parametrů.
 *
 * Jedno zhlédnutí = jeden pobyt na přehrávači jednoho videa (Firefox: stejné `v=`; aplikace YouTube: od příchodu
 * na přehrávač do konce, po konci znovu přehrávání = další video). Hotové je, když:
 * - video skončí ([VideoPlayback.ended], už po filtru zastaralého end-screenu) po aspoň [minEndedMillis] sledování,
 * - nebo uživatel z videa odejde (jiné video, jiná stránka, jiná aplikace) po aspoň [minLeftMillis].
 * Každé zhlédnutí se ohlásí nejvýš jednou.
 */
class VideoWatchTracker(
    private val minEndedMillis: Long = MIN_ENDED_MILLIS,
    private val minLeftMillis: Long = MIN_LEFT_MILLIS,
) {
    /** Dosledované video. [title] null = název nečitelný (aplikace YouTube). */
    data class Finished(val title: String?, val packageName: String, val watchedMillis: Long, val ended: Boolean)

    private class Watch(val key: String?, val packageName: String, val startedAtMillis: Long) {
        var title: String? = null
        var lastSeenMillis: Long = startedAtMillis
        var ended = false
        var reported = false
    }

    private var watch: Watch? = null

    /**
     * Pozorování (snímek nebo tik). [onVideo] = přehrávač jednoho videa doma ve sledované aplikaci.
     * Vrací hotové zhlédnutí, jinak null.
     */
    fun observe(onVideo: Boolean, packageName: String, video: VideoPlayback?, now: Long): Finished? {
        val w = watch
        if (!onVideo) {
            watch = null
            return w?.let { left(it) }
        }
        val key = video?.videoKey
        val ended = video?.ended == true
        val isNew = when {
            w == null -> true
            w.packageName != packageName -> true
            key != null && w.key != null -> key != w.key
            else -> w.ended && !ended // aplikace: po konci znovu přehrává = další video
        }
        if (isNew) {
            val finished = w?.let { left(it) }
            watch = Watch(key, packageName, now).also { it.update(video, now) }
            return finished
        }
        w!!.update(video, now)
        if (ended && !w.reported && now - w.startedAtMillis >= minEndedMillis) {
            w.reported = true
            return w.finished(now, ended = true)
        }
        return null
    }

    private fun Watch.update(video: VideoPlayback?, now: Long) {
        lastSeenMillis = now
        video?.title?.takeIf { it.isNotBlank() }?.let { title = it }
        if (video?.ended == true) ended = true else if (video != null) ended = false
    }

    private fun left(w: Watch): Finished? {
        if (w.reported || w.lastSeenMillis - w.startedAtMillis < minLeftMillis) return null
        w.reported = true
        return w.finished(w.lastSeenMillis, ended = false)
    }

    private fun Watch.finished(now: Long, ended: Boolean) = Finished(title, packageName, now - startedAtMillis, ended)

    companion object {
        /** Kratší video, které doběhlo, se nehodnotí (jedno krátké video z odkazu). */
        const val MIN_ENDED_MILLIS = 60_000L

        /** Odchod z videa se hodnotí po 2 min sledování (brief 156). */
        const val MIN_LEFT_MILLIS = 2 * 60_000L
    }
}
