package cz.narsil.ui.screens.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel pro sovu Narsilku: hlídá limit proaktivních hlášek, řeší chat přes LLM
 * a v krizi nebo při výpadku sítě přepíná na bezpečné offline odpovědi.
 */
class PetViewModel(
    context: Flow<PetContext>,
    private val replier: (suspend (String) -> String?)?,
    private val policy: PetSpeechPolicy,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetUiState())
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()

    private var latestContext = PetContext()

    init {
        viewModelScope.launch {
            context.collect { ctx ->
                latestContext = ctx
                val millis = now()
                val dayKey = petDayKey(millis)
                val hour = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).hour
                val mood = petMoodFor(ctx.avatar, hour)
                val candidate = PetLines.greeting(ctx, mood, dayKey.toInt())
                val muted = policy.isMuted(dayKey)

                _uiState.update { current ->
                    val shouldSpeak = !muted &&
                        candidate != current.bubbleText &&
                        policy.canSpeakUnprompted(dayKey)
                    if (shouldSpeak) {
                        policy.markSpoken(dayKey)
                    }
                    val nextBubble = if (shouldSpeak) candidate else current.bubbleText
                    val nextVisible = !muted && nextBubble != null
                    val nextMessages = if (current.messages.isEmpty()) {
                        listOf(PetMessage(fromPet = true, text = nextBubble ?: candidate))
                    } else {
                        current.messages
                    }
                    current.copy(
                        mood = mood,
                        bubbleText = nextBubble,
                        bubbleVisible = nextVisible,
                        messages = nextMessages,
                    )
                }
            }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value.isThinking) return

        val ctxSnapshot = latestContext
        val historySnapshot = _uiState.value.messages
        val userMsg = PetMessage(fromPet = false, text = trimmed)

        if (PetLines.isCrisis(trimmed)) {
            val crisisReply = PetLines.reply(trimmed, ctxSnapshot)
            _uiState.update {
                it.copy(
                    input = "",
                    messages = it.messages + userMsg + PetMessage(fromPet = true, text = crisisReply),
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                input = "",
                isThinking = true,
                messages = it.messages + userMsg,
            )
        }

        viewModelScope.launch {
            val prompt = buildPrompt(historySnapshot, ctxSnapshot, trimmed)
            val llmRaw = try {
                replier?.invoke(prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                null
            }
            val replyText = PetLines.shorten(
                llmRaw?.takeIf { it.isNotBlank() } ?: PetLines.reply(trimmed, ctxSnapshot),
            )
            _uiState.update {
                it.copy(
                    isThinking = false,
                    messages = it.messages + PetMessage(fromPet = true, text = replyText),
                )
            }
        }
    }

    fun muteToday() {
        policy.muteUntilTomorrow(petDayKey(now()))
        _uiState.update { it.copy(bubbleVisible = false) }
    }

    fun unmute() {
        policy.unmute()
        _uiState.update { it.copy(bubbleVisible = it.bubbleText != null) }
    }

    fun buildPrompt(history: List<PetMessage>, ctx: PetContext, userText: String): String = buildString {
        appendLine(
            "Jsi Narsilka, malá moudrá sova a parťák uživatele v odvykání nekonečného scrollování. " +
                "Mluvíš česky, maximálně 2 krátké věty, vřele a s lehkým humorem. " +
                "Nikdy nesoudíš, nemoralizuješ ani nevyčítáš čísla. " +
                "Nabízíš konkrétní malé kroky z uživatelova plánu. " +
                "Nejsi terapeut; při vážných potížích doporuč Linku první psychické pomoci 116 123.",
        )
        appendLine()
        appendLine("Kontext uživatele:")
        appendLine("- Série splněných smluv: ${ctx.streakDays} dní")
        appendLine("- Získaný čas dnes: ${ctx.reclaimedMinutes} min, feed dnes: ${ctx.feedMinutes} min")
        appendLine("- Kroky dnes: ${ctx.stepsToday}, dluh: ${ctx.debtMinutes} min")
        val tasks = if (ctx.plannedTasks.isEmpty()) "žádné zadané" else ctx.plannedTasks.joinToString(", ")
        appendLine("- Úkoly a plán dne: $tasks")
        appendLine()
        val recent = history.takeLast(6)
        if (recent.isNotEmpty()) {
            appendLine("Poslední zprávy:")
            recent.forEach { msg ->
                val who = if (msg.fromPet) "Narsilka" else "Uživatel"
                appendLine("$who: ${msg.text}")
            }
            appendLine()
        }
        appendLine("Uživatel: $userText")
        append("Narsilka:")
    }
}
