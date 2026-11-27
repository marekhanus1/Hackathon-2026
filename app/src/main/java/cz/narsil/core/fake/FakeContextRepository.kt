package cz.narsil.core.fake

import cz.narsil.core.ComfortContext
import cz.narsil.core.ContextRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake: „detekovaný“ stav se mění přes [setDetected], ruční přepínač má přednost. */
class FakeContextRepository(
    detected: ComfortContext = ComfortContext.HOME,
) : ContextRepository {
    private var detected = detected
    private val _override = MutableStateFlow<ComfortContext?>(null)
    private val _context = MutableStateFlow(detected)

    override val context: StateFlow<ComfortContext> = _context.asStateFlow()
    override val manualOverride: StateFlow<ComfortContext?> = _override.asStateFlow()

    override suspend fun setManualOverride(context: ComfortContext?) {
        _override.value = context
        recompute()
    }

    /** Simuluje změnu Wi-Fi. */
    fun setDetected(context: ComfortContext) {
        detected = context
        recompute()
    }

    private fun recompute() {
        _context.value = _override.value ?: detected
    }
}
