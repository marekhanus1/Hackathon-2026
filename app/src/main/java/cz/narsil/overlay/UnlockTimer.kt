package cz.narsil.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zaplacený čas (114): do [unlockedUntil] se nezasahuje. Detekci (111) to hlásí
 * [EchoOutcomeHandler] přes `AccessibilityTriggerSource.onOutcome`; tady je stav pro UI a demo panel.
 */
class UnlockTimer(private val clock: () -> Long = System::currentTimeMillis) {
    private val _until = MutableStateFlow(0L)
    val unlockedUntil: StateFlow<Long> = _until.asStateFlow()

    fun unlock(minutes: Int) {
        val base = maxOf(clock(), _until.value)
        _until.value = base + minutes * 60_000L
    }

    fun clear() {
        _until.value = 0
    }

    fun isUnlocked(): Boolean = clock() < _until.value

    fun remainingMillis(): Long = (_until.value - clock()).coerceAtLeast(0)
}
