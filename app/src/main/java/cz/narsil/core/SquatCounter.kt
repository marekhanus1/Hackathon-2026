package cz.narsil.core

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow

/**
 * Počítání dřepů kamerou (210: CameraX + ML Kit Pose). Core nezávisí na CameraX:
 * náhled si UI vytvoří samo (PreviewView v AndroidView) a předá jeho
 * `surfaceProvider` jako [Any]; implementace ho přetypuje na
 * `androidx.camera.core.Preview.SurfaceProvider`. null = počítat bez náhledu.
 */
interface SquatCounter {
    /** Počet dřepů od posledního [start]. */
    val count: StateFlow<Int>

    /** Kamera běží a počítá. */
    val isRunning: StateFlow<Boolean>

    /** Vynuluje [count] a spustí kameru navázanou na [lifecycleOwner]. */
    fun start(lifecycleOwner: LifecycleOwner, previewSurfaceProvider: Any? = null)

    /** Zastaví kameru; [count] zůstane. */
    fun stop()
}
