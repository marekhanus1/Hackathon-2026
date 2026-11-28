package cz.narsil.overlay

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.narsil.gamification.GamificationRules
import cz.narsil.ui.screens.rating.VideoRatingSheet
import cz.narsil.ui.screens.rating.VideoRatingUiState
import cz.narsil.ui.screens.rating.VideoValue
import cz.narsil.ui.theme.NarsilTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Panel „Co ti to video dalo?“ (156) přes YouTube / feed: malé okno u dolního okraje ([EchoOverlayWindow]
 * s `bottomPanel`), neblokuje, samo zmizí. Odpověď → [onAnswer] (zápis do `video_ratings` a +5 XP).
 * Ozvěna má přednost: přes ni se panel neukáže a když se objeví, panel se zavře ([dismiss]).
 */
class VideoFeedbackController(
    private val window: EchoOverlayWindow,
    private val scope: CoroutineScope,
    /** Ukazuje se teď Ozvěna (nebo odměna)? */
    private val isEchoShowing: () -> Boolean,
    /** Jsme doma (ComfortContext.HOME)? Venku se nikdy neptá. */
    private val isHome: () -> Boolean,
    /** Začátek dnešního dne (4:00) pro strop otázek. */
    private val dayStartMillis: () -> Long,
    /** Uloží odpověď, vrací přidané XP. Nesmí házet do UI (chyby se jen zalogují). */
    private val onAnswer: suspend (title: String, value: VideoValue) -> Int,
    private val limiter: VideoFeedback.Limiter = VideoFeedback.Limiter(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var autoDismiss: Job? = null

    /**
     * Zeptá se na [title] (uloží se jako název do mapy hodnoty), v panelu ukáže [caption].
     * Vrací false, když se teď ptát nesmí (Ozvěna, venku, strop, už se ptá).
     */
    fun ask(title: String, caption: String = title, question: String = QUESTION_VIDEO): Boolean {
        if (window.isShowing || isEchoShowing() || !isHome()) return false
        if (!limiter.tryAsk(clock(), dayStartMillis())) {
            Log.i(TAG, "otázka na video vynechána (strop nebo odstup)")
            return false
        }
        val ui = MutableStateFlow(VideoRatingUiState(videoTitle = caption, question = question, earnedXp = GamificationRules.XP_VIDEO_RATED))
        val shown = window.show {
            val state by ui.collectAsState()
            NarsilTheme {
                VideoRatingSheet(
                    state = state,
                    onAnswer = { value ->
                        if (ui.value.answered != null) return@VideoRatingSheet
                        ui.value = ui.value.copy(answered = value)
                        autoDismiss?.cancel()
                        scope.launch {
                            val xp = runCatching { onAnswer(title, value) }
                                .onFailure { Log.e(TAG, "zápis odpovědi selhal", it) }
                                .getOrDefault(0)
                            Log.i(TAG, "odpověď: ${value.name} na „$title“, +$xp XP")
                            delay(VideoFeedback.THANKS_MILLIS)
                            window.dismiss()
                        }
                    },
                    onDismiss = { dismiss() },
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
        if (!shown) return false
        Log.i(TAG, "otázka „$question“: $caption")
        autoDismiss?.cancel()
        autoDismiss = scope.launch {
            delay(VideoFeedback.AUTO_DISMISS_MILLIS)
            if (ui.value.answered == null) window.dismiss()
        }
        return true
    }

    fun dismiss() {
        autoDismiss?.cancel()
        window.dismiss()
    }

    companion object {
        const val QUESTION_VIDEO = "Co ti to video dalo?"
        const val QUESTION_FEED = "Co ti daly ty minuty?"
        private const val TAG = "NarsilFeedback"
    }
}
