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
 * ViewModel pro sovu Narsilku: hlídá limit proaktivních hlášek, šetří Gemini Free API
 * přes lokální odpovědi, cache a rate-limiter, a při limitu okamžitě přepne na offline fallback.
 */
class PetViewModel(
    context: Flow<PetContext>,
    private val replier: (suspend (String) -> PetApiResult)?,
    private val policy: PetSpeechPolicy,
    private val rateLimiter: PetRateLimiter? = null,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetUiState())
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()

    private var latestContext = PetContext()
    private var cacheDayKey: Long = Long.MIN_VALUE
    private val responseCache = LinkedHashMap<String, String>(CACHE_CAPACITY, 0.75f, true)

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
        val turnIndex = historySnapshot.size
        val userMsg = PetMessage(fromPet = false, text = trimmed)

        // 1. Lokální cesta (0 ms, 0 API tokenů): krize, pozdravy, poděkování a rychlé volby.
        val localReply = PetLines.tryLocalReply(trimmed, ctxSnapshot, turnIndex)
        if (localReply != null) {
            appendImmediateReply(userMsg, localReply)
            return
        }

        val millis = now()
        val dayKey = petDayKey(millis)
        val cacheKey = PetLines.normalizeKey(trimmed)

        // 2. Cache opakovaných dotazů v rámci dne (0 ms, 0 API tokenů).
        val cachedReply = getCachedReply(dayKey, cacheKey)
        if (cachedReply != null) {
            appendImmediateReply(userMsg, cachedReply)
            return
        }

        // 3. Rate-limit, cooldown po 429 nebo vypnuté API → okamžitý offline fallback bez čekání.
        if (replier == null || rateLimiter?.canCallApi(millis, dayKey) == false) {
            appendImmediateReply(userMsg, PetLines.reply(trimmed, ctxSnapshot, turnIndex))
            return
        }

        // 4. Gemini API volání s evidencí kvóty.
        rateLimiter?.recordCall(millis, dayKey)
        _uiState.update {
            it.copy(
                input = "",
                isThinking = true,
                messages = it.messages + userMsg,
            )
        }

        viewModelScope.launch {
            val prompt = buildPrompt(historySnapshot, ctxSnapshot, trimmed)
            val apiResult = try {
                replier.invoke(prompt)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                PetApiResult.Unavailable
            }

            val replyText = when (apiResult) {
                is PetApiResult.Success -> {
                    val shortened = PetLines.shorten(apiResult.text)
                    if (shortened.isNotBlank()) {
                        putCachedReply(dayKey, cacheKey, shortened)
                        shortened
                    } else {
                        PetLines.reply(trimmed, ctxSnapshot, turnIndex)
                    }
                }
                PetApiResult.RateLimited -> {
                    rateLimiter?.recordError(now(), isRateLimit = true)
                    PetLines.reply(trimmed, ctxSnapshot, turnIndex)
                }
                PetApiResult.Unavailable -> {
                    rateLimiter?.recordError(now(), isRateLimit = false)
                    PetLines.reply(trimmed, ctxSnapshot, turnIndex)
                }
            }

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
            "Jsi sova Narsilka, klidný parťák při odvykání scrollování. " +
                "Odpovídej česky, max 2 krátké věty, vřele a bez moralizování. " +
                "Při krizi doporuč Linku 116 123.",
        )
        val tasks = if (ctx.plannedTasks.isEmpty()) "žádné" else ctx.plannedTasks.take(3).joinToString(", ")
        appendLine(
            "Kontext: série ${ctx.streakDays} d, získáno ${ctx.reclaimedMinutes} min, " +
                "feed ${ctx.feedMinutes} min, dluh ${ctx.debtMinutes} min, plán: $tasks.",
        )
        val recent = history.takeLast(4)
        if (recent.isNotEmpty()) {
            recent.forEach { msg ->
                val who = if (msg.fromPet) "Sova" else "Uživatel"
                appendLine("$who: ${msg.text.take(120)}")
            }
        }
        appendLine("Uživatel: ${userText.take(200)}")
        append("Sova:")
    }

    private fun appendImmediateReply(userMsg: PetMessage, replyText: String) {
        _uiState.update {
            it.copy(
                input = "",
                messages = it.messages + userMsg + PetMessage(fromPet = true, text = replyText),
            )
        }
    }

    private fun getCachedReply(dayKey: Long, cacheKey: String): String? {
        if (cacheDayKey != dayKey) {
            cacheDayKey = dayKey
            responseCache.clear()
            return null
        }
        return responseCache[cacheKey]
    }

    private fun putCachedReply(dayKey: Long, cacheKey: String, reply: String) {
        if (cacheDayKey != dayKey) {
            cacheDayKey = dayKey
            responseCache.clear()
        }
        if (responseCache.size >= CACHE_CAPACITY && !responseCache.containsKey(cacheKey)) {
            val oldestKey = responseCache.keys.firstOrNull()
            if (oldestKey != null) responseCache.remove(oldestKey)
        }
        responseCache[cacheKey] = reply
    }

    private companion object {
        const val CACHE_CAPACITY = 20
    }
}
