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
    private val _service = MutableStateFlow<NarsilAccessibilityService?>(null)

    /** Každý snímek (hot, bez replay). */
    val snapshots: SharedFlow<ForegroundSnapshot> = _snapshots.asSharedFlow()

    /** Poslední snímek. */
    val latest: StateFlow<ForegroundSnapshot?> = _latest.asStateFlow()

    /** Služba běží (onServiceConnected … onUnbind/onDestroy). */
    val serviceConnected: StateFlow<Boolean> = _connected.asStateFlow()

    /**
     * Aktuálně připojená instance služby. Po přeinstalaci se může připojit nová instance dřív,
     * než se zničí stará, proto posluchači (Ozvěna) sledují instanci, ne jen příznak.
     */
    val service: StateFlow<NarsilAccessibilityService?> = _service.asStateFlow()

    internal fun emit(snapshot: ForegroundSnapshot) {
        _latest.value = snapshot
        _snapshots.tryEmit(snapshot)
    }

    internal fun attach(service: NarsilAccessibilityService) {
        _service.value = service
        _connected.value = true
    }

    /** Odpojí jen [service], když je pořád aktuální (stará instance nesmí shodit novou). */
    internal fun detach(service: NarsilAccessibilityService) {
        if (_service.compareAndSet(service, null)) _connected.value = false
    }
}
