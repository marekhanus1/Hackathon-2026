package cz.narsil.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

/**
 * Překryvné okno přes celou obrazovku (113). Obsah je libovolný Compose (obrazovka Ozvěny 310).
 *
 * Typ okna: s kontextem [AccessibilityService] `TYPE_ACCESSIBILITY_OVERLAY` (nepotřebuje
 * „zobrazovat přes ostatní aplikace“ a na MIUI je spolehlivější), jinak `TYPE_APPLICATION_OVERLAY`
 * se `SYSTEM_ALERT_WINDOW`. Volat z hlavního vlákna (metody si to zajistí samy).
 */
class EchoOverlayWindow(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var view: ComposeView? = null
    private var owner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = view != null

    /** Lze okno ukázat? (accessibility kontext, nebo povolené překrývání) */
    fun canShow(): Boolean = context is AccessibilityService || Settings.canDrawOverlays(context)

    /** Ukáže okno s [content]; když už je vidět, jen vymění obsah. Vrací false, když chybí oprávnění. */
    fun show(content: @Composable () -> Unit): Boolean {
        if (!canShow()) {
            Log.w(TAG, "chybí oprávnění k překryvnému oknu")
            return false
        }
        runOnMain {
            view?.let {
                it.setContent(content)
                return@runOnMain
            }
            val lifecycle = OverlayLifecycleOwner()
            val compose = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent(content)
            }
            lifecycle.attachTo(compose)
            lifecycle.start()
            runCatching { wm.addView(compose, layoutParams()) }
                .onSuccess {
                    view = compose
                    owner = lifecycle
                }
                .onFailure {
                    Log.e(TAG, "addView selhal", it)
                    lifecycle.destroy()
                }
        }
        return true
    }

    fun dismiss() = runOnMain {
        val v = view ?: return@runOnMain
        runCatching { wm.removeViewImmediate(v) }
        owner?.destroy()
        view = null
        owner = null
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (context is AccessibilityService) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // Fokusovatelné okno: přebírá dotyky i tlačítko zpět, aplikace pod ním nejde ovládat.
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.CENTER
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    }

    private inline fun runOnMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post { block() }
    }

    private companion object {
        const val TAG = "NarsilOverlay"
    }
}
