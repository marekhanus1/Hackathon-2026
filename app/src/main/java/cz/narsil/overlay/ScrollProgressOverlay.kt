package cz.narsil.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Tenký pruh u horního okraje obrazovky (135): jak blízko je limit scrollování, než přijde Ozvěna.
 * Neblokuje dotyky ani fokus (`FLAG_NOT_TOUCHABLE`, `FLAG_NOT_FOCUSABLE`), kreslí obyčejný [View]
 * (žádný Compose ani animace) a překreslí se jen při změně procenta. Volat odkudkoli, běží na hlavním vlákně.
 */
class ScrollProgressOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var bar: BarView? = null

    val isShowing: Boolean get() = bar != null

    /** [percent] 0–100 ukáže / posune pruh, null ho skryje. */
    fun update(percent: Int?) = runOnMain {
        if (percent == null) return@runOnMain removeBar()
        val view = bar ?: addBar() ?: return@runOnMain
        view.percent = percent
    }

    fun dismiss() = runOnMain { removeBar() }

    private fun addBar(): BarView? {
        if (context !is AccessibilityService && !Settings.canDrawOverlays(context)) return null
        val view = BarView(context)
        return runCatching { wm.addView(view, layoutParams()) }
            .onFailure { Log.w(TAG, "ukazatel limitu: addView selhal", it) }
            .map { view.also { bar = it } }
            .getOrNull()
    }

    private fun removeBar() {
        val v = bar ?: return
        runCatching { wm.removeViewImmediate(v) }
        bar = null
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        (HEIGHT_DP * context.resources.displayMetrics.density).toInt().coerceAtLeast(2),
        if (context is AccessibilityService) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        title = "NarsilScrollProgress"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Až k hornímu okraji i u výřezu kamery (Nothing 3a).
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    /** Pruh zleva doprava; barva a krytí podle [ScrollProgressColors]. */
    private class BarView(context: Context) : View(context) {
        private val paint = Paint().apply { style = Paint.Style.FILL }

        var percent: Int = -1
            set(value) {
                val v = value.coerceIn(0, 100)
                if (v == field) return
                field = v
                paint.color = ScrollProgressColors.color(v)
                invalidate()
            }

        override fun onDraw(canvas: Canvas) {
            if (percent <= 0) return
            canvas.drawRect(0f, 0f, width * percent / 100f, height.toFloat(), paint)
        }
    }

    private companion object {
        const val TAG = "NarsilOverlay"
        const val HEIGHT_DP = 3f
    }
}
