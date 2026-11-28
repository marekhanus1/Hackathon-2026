package cz.narsil.overlay

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.sin

/**
 * Ukazatel blížícího se limitu (135, 148): 7 dp pruh se zaoblenými konci hned POD stavovým řádkem
 * (respektuje výřez kamery), tmavá stopa a obrys, ať je vidět na světlém i tmavém obsahu. Při 50 % a 80 %
 * krátce pulsuje, v posledních 20 % ukáže štítek „Ozvěna za 0:15“ (odpočítává sám mezi aktualizacemi).
 * Neblokuje dotyky ani fokus (`FLAG_NOT_TOUCHABLE`, `FLAG_NOT_FOCUSABLE`). Volat odkudkoli, běží na hlavním vlákně.
 */
class ScrollProgressOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var bar: BarView? = null

    val isShowing: Boolean get() = bar != null

    /**
     * [percent] 0–100 ukáže / posune pruh, null ho skryje. [remainingMillis] = čas do Ozvěny
     * (pro štítek v posledních 20 %), null = štítek bez času neukazovat.
     */
    fun update(percent: Int?, remainingMillis: Long? = null) = runOnMain {
        if (percent == null) return@runOnMain removeBar()
        val view = bar ?: addBar() ?: return@runOnMain
        view.setProgress(percent, remainingMillis)
    }

    fun dismiss() = runOnMain { removeBar() }

    private fun addBar(): BarView? {
        if (context !is AccessibilityService && !Settings.canDrawOverlays(context)) {
            Log.w("NarsilProgress", "ukazatel: bez oprávnění k překrytí")
            return null
        }
        val view = BarView(context)
        return runCatching { wm.addView(view, layoutParams()) }
            .onSuccess { Log.d("NarsilProgress", "ukazatel: okno přidáno") }
            .onFailure { Log.w(TAG, "ukazatel limitu: addView selhal", it) }
            .map { view.also { bar = it } }
            .getOrNull()
    }

    private fun removeBar() {
        val v = bar ?: return
        v.stop()
        runCatching { wm.removeViewImmediate(v) }
        bar = null
    }

    /** Horní okraj pod stavovým řádkem a výřezem kamery (px). */
    private fun topInset(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val insets = wm.currentWindowMetrics.windowInsets
                    .getInsetsIgnoringVisibility(WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout())
                if (insets.top > 0) return insets.top
            }
        }
        val res = context.resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) res.getDimensionPixelSize(id) else (24 * res.displayMetrics.density).toInt()
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val d = context.resources.displayMetrics.density
        val height = with(ScrollProgressStyle) { (TOP_GAP_DP + BAR_DP * 2 + 6f + LABEL_HEIGHT_DP) * d }.toInt()
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            if (context is AccessibilityService) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            y = topInset()
            title = "NarsilScrollProgress"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // y se počítá od horního okraje obrazovky i u výřezu (Nothing 3a), pruh pak leží pod ním.
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    /** Pruh zleva doprava; barva podle [ScrollProgressColors], chování podle [ScrollProgressStyle]. */
    private class BarView(context: Context) : View(context) {
        private val d = context.resources.displayMetrics.density
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = ScrollProgressColors.TRACK }
        private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * d
            color = ScrollProgressColors.OUTLINE
        }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ScrollProgressColors.LABEL_TEXT
            textSize = ScrollProgressStyle.LABEL_TEXT_SP * context.resources.displayMetrics.scaledDensity
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(2f * d, 0f, 0f, ScrollProgressColors.OUTLINE)
        }
        private val rect = RectF()

        private var percent: Int? = null
        private var deadline: Long? = null
        private var pulse = 0f // 0..1 během pulsu
        private var animator: ValueAnimator? = null
        private val tick = object : Runnable {
            override fun run() {
                invalidate()
                if (labelVisible()) postDelayed(this, 250)
            }
        }

        fun setProgress(value: Int, remainingMillis: Long?) {
            val v = value.coerceIn(0, 100)
            val previous = percent
            deadline = remainingMillis?.let { SystemClock.uptimeMillis() + it }
            if (v == previous) return
            percent = v
            fill.color = ScrollProgressColors.color(v)
            glow.color = (fill.color and 0x00FFFFFF) or 0x66000000
            if (ScrollProgressStyle.crossedPulse(previous, v) && previous != null) startPulse()
            removeCallbacks(tick)
            if (labelVisible()) post(tick)
            invalidate()
        }

        fun stop() {
            animator?.cancel()
            removeCallbacks(tick)
        }

        private fun labelVisible() = percent?.let(ScrollProgressStyle::showLabel) == true && deadline != null

        private fun startPulse() {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = ScrollProgressStyle.PULSE_MILLIS
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { pulse = sin(Math.PI * (it.animatedValue as Float)).toFloat(); invalidate() }
                start()
            }
        }

        override fun onDetachedFromWindow() {
            stop()
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            val p = percent ?: return
            val margin = ScrollProgressStyle.SIDE_MARGIN_DP * d
            val baseH = ScrollProgressStyle.BAR_DP * d
            val h = baseH * (1f + 0.8f * pulse)
            val centerY = ScrollProgressStyle.TOP_GAP_DP * d + baseH
            val left = margin
            val right = width - margin
            val top = centerY - h / 2
            val bottom = centerY + h / 2
            val r = h / 2

            // Stopa celé délky + obrys.
            rect.set(left, top, right, bottom)
            canvas.drawRoundRect(rect, r, r, track)
            // Vyplněná část (aspoň kolečko, ať jsou konce kulaté i na začátku).
            val fillRight = (left + (right - left) * p / 100f).coerceAtLeast(left + h)
            if (pulse > 0f) {
                val g = 3f * d * pulse
                rect.set(left - g, top - g, fillRight + g, bottom + g)
                canvas.drawRoundRect(rect, r + g, r + g, glow)
            }
            rect.set(left, top, fillRight, bottom)
            canvas.drawRoundRect(rect, r, r, fill)
            rect.set(left, top, right, bottom)
            canvas.drawRoundRect(rect, r, r, outline)

            // Štítek v posledních 20 %: pilulka pod pravým koncem pruhu.
            val end = deadline
            if (ScrollProgressStyle.showLabel(p) && end != null) {
                val label = ScrollProgressStyle.label(end - SystemClock.uptimeMillis())
                val padX = 10f * d
                val labelH = ScrollProgressStyle.LABEL_HEIGHT_DP * d
                val w = text.measureText(label) + padX * 2
                val lTop = centerY + baseH + 4f * d
                rect.set(right - w, lTop, right, lTop + labelH)
                canvas.drawRoundRect(rect, labelH / 2, labelH / 2, fill)
                canvas.drawRoundRect(rect, labelH / 2, labelH / 2, outline)
                val baseline = rect.centerY() - (text.descent() + text.ascent()) / 2
                canvas.drawText(label, rect.left + padX, baseline, text)
            }
        }
    }

    private companion object {
        const val TAG = "NarsilOverlay"
    }
}
