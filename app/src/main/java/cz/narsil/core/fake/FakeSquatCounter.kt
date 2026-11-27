package cz.narsil.core.fake

import androidx.lifecycle.LifecycleOwner
import cz.narsil.core.MovementRates
import cz.narsil.core.SquatCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Fake bez kamery. Dřepy přidává ručně [addSquat], nebo – když dostane [scope] –
 * sám jeden za [intervalMillis] až do [target].
 */
class FakeSquatCounter(
    private val scope: CoroutineScope? = null,
    private val intervalMillis: Long = 1_200,
    private val target: Int = MovementRates.SQUATS_PER_UNLOCK,
) : SquatCounter {
    private val _count = MutableStateFlow(0)
    private val _running = MutableStateFlow(false)
    private var job: Job? = null

    override val count: StateFlow<Int> = _count.asStateFlow()
    override val isRunning: StateFlow<Boolean> = _running.asStateFlow()

    override fun start(lifecycleOwner: LifecycleOwner, previewSurfaceProvider: Any?) = start()

    /** Start bez lifecycle (testy, náhledy). */
    fun start() {
        job?.cancel()
        _count.value = 0
        _running.value = true
        job = scope?.launch {
            while (_count.value < target) {
                delay(intervalMillis)
                _count.value++
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        _running.value = false
    }

    fun addSquat() {
        if (_running.value) _count.value++
    }
}
