package cz.narsil.overlay

import cz.narsil.core.DailyContract
import cz.narsil.core.EchoVideo
import cz.narsil.core.Exercise
import cz.narsil.core.InterventionTrigger
import cz.narsil.core.MovementRates
import cz.narsil.core.MovementState
import cz.narsil.ui.screens.echo.EchoUiState

/** Co overlay ukáže: stav obrazovky 310 + video / záložní text (SPEC 9). Čistý Kotlin kvůli testům. */
data class EchoContent(
    val ui: EchoUiState,
    val videoUri: String?,
    val fallbackText: String?,
)

object EchoStateBuilder {
    private val APP_NAMES = mapOf(
        "com.instagram.android" to "Instagram",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok",
        "com.ss.android.ugc.aweme" to "TikTok",
        "com.google.android.youtube" to "YouTube",
        "org.mozilla.firefox" to "Firefox",
    )

    fun appName(packageName: String): String = APP_NAMES[packageName] ?: packageName.substringAfterLast('.')

    fun build(
        trigger: InterventionTrigger,
        movement: MovementState,
        contract: DailyContract?,
        video: EchoVideo?,
        exercise: Exercise = Exercise.SQUATS,
    ): EchoContent {
        val firstTask = contract?.tasks?.firstOrNull()?.title ?: contract?.plannedContent?.firstOrNull()
        val fallback = firstTask?.let { "Dnes jsi chtěl: $it" }
        val minutes = (trigger.event.durationMillis / 60_000).toInt()
        val ui = EchoUiState(
            sourceAppName = appName(trigger.event.packageName),
            videoCaption = fallback ?: "Scrolluješ už $minutes min.",
            // Obrazovka 310 počítá s kroky; zůstatek v minutách převedeme zpět na kroky podle kurzu.
            availableSteps = movement.balanceMinutes * MovementRates.STEPS_PER_MINUTE,
            stepCostForTenMin = MovementRates.MINUTES_PER_UNLOCK * MovementRates.STEPS_PER_MINUTE,
            currentDebtMinutes = movement.debtMinutes,
            isVideoPlaying = video != null,
            exerciseOptionTitle = ExerciseText.echoOption(exercise),
            selectedExercise = exercise,
        )
        return EchoContent(ui, video?.uri, fallback)
    }
}
