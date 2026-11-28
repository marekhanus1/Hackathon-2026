package cz.narsil.detection.youtube

/**
 * Zastaralý end-screen (152): po otevření videa (intent, znovu totéž video) zůstane ve stromu YouTube
 * end-screen předchozího přehrání a [VideoPlayback.ended] je true hned na začátku. Konec proto platí,
 * jen když se na tomhle videu předtím aspoň jednou přečetlo přehrávání (`ended = false`).
 * Nové video = příchod na přehrávač odjinud, nebo jiný [VideoPlayback.videoKey]. Čistý Kotlin.
 */
class EndScreenFilter {
    private var onVideo = false
    private var key: String? = null
    private var sawPlaying = false

    /** [video] s `ended` jen po přehrávání; [isVideo] = povrch je VIDEO. */
    fun filter(isVideo: Boolean, video: VideoPlayback?): VideoPlayback? {
        if (!isVideo) {
            onVideo = false
            return video
        }
        val k = video?.videoKey
        if (!onVideo || (k != null && k != key)) {
            sawPlaying = false
            key = k
        }
        onVideo = true
        if (video == null) return null
        if (!video.ended) {
            sawPlaying = true
            return video
        }
        return if (sawPlaying) video else video.copy(ended = false)
    }
}
