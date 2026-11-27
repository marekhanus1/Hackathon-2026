package cz.narsil.detection.accessibility

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sběrnice mezi službou (110) a klasifikací (111). Proces aplikace je jeden, stačí singleton.
 */
object AccessibilityEvents {
    private val _snapshots = MutableSharedFlow<ForegroundSnapshot>(extraBufferCapacity = 64)
    private val _latest = MutableStateFlow<ForegroundSnapshot?>(null)
    private val _connected = MutableStateFlow(false)

    /** Každý snímek (hot, bez replay). */
    val snapshots: SharedFlow<ForegroundSnapshot> = _snapshots.asSharedFlow()

    /** Poslední snímek. */
    val latest: StateFlow<ForegroundSnapshot?> = _latest.asStateFlow()

    /** Služba běží (onServiceConnected … onUnbind/onDestroy). */
    val serviceConnected: StateFlow<Boolean> = _connected.asStateFlow()

    internal fun emit(snapshot: ForegroundSnapshot) {
        _latest.value = snapshot
        _snapshots.tryEmit(snapshot)
    }

    internal fun setConnected(value: Boolean) {
        _connected.value = value
    }
}
