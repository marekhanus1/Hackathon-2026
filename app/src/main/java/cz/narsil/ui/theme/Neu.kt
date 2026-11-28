package cz.narsil.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neumorfismus obráceně (koncept E, 334): nic nevystupuje, všechno je vnořené.
 * Podklad [Base], vnořené plochy [Inset], hlubší vnoření [Deep] pro aktivní / vybraný stav.
 */
object NeuColors {
    val Base = Color(0xFF272B34)
    val Inset = Color(0xFF23272F)
    val Deep = Color(0xFF20232A)
    val ShadowDark = Color(0xFF181B20)
    val ShadowLight = Color(0xFF2E333D)
    val DeepShadowDark = Color(0xFF15181C)
    val DeepShadowLight = Color(0xFF2B3039)
    val Divider = Color(0xFF30353F)
    val Dashed = Color(0xFF4A505C)

    val Accent = Color(0xFFF2A359)
    val OnAccent = Color(0xFF3A1A00)
    val Success = Color(0xFF5CC497)
    val OnSuccess = Color(0xFF10241B)
    val Error = Color(0xFFF59A8B)
    val Text = Color(0xFFE9ECF2)
    val Muted = Color(0xFFA3AAB8)
}

/**
 * Hloubka vnoření; odpovídá třídám `.in`, `.in-s`, `.in-d` z návrhu.
 * [Flat] = bez plochy i stínu (nevybraná položka v řadě záložek).
 */
enum class NeuDepth(
    internal val fill: Color,
    internal val dark: Color,
    internal val light: Color,
    internal val darkOffset: Dp,
    internal val darkBlur: Dp,
    internal val lightOffset: Dp,
    internal val lightBlur: Dp,
) {
    Normal(NeuColors.Inset, NeuColors.ShadowDark, NeuColors.ShadowLight, 6.dp, 12.dp, 6.dp, 12.dp),
    Shallow(NeuColors.Inset, NeuColors.ShadowDark, NeuColors.ShadowLight, 3.dp, 6.dp, 3.dp, 6.dp),
    Deep(NeuColors.Deep, NeuColors.DeepShadowDark, NeuColors.DeepShadowLight, 4.dp, 8.dp, 3.dp, 6.dp),
    Flat(Color.Transparent, Color.Transparent, Color.Transparent, 0.dp, 0.dp, 0.dp, 0.dp),
}

/**
 * Vnořená plocha: výplň + vnitřní stín (tmavý vlevo nahoře, světlý vpravo dole) uvnitř [shape].
 * Jediné místo, kde se vnitřní stín kreslí – obrazovky ho jen používají.
 *
 * Stín = tlustý rozmazaný obrys tvaru posunutý o offset a oříznutý tvarem; rozmazání (BlurMaskFilter)
 * funguje s hardwarovou akcelerací od API 28 (minSdk 29).
 */
fun Modifier.neuInset(
    depth: NeuDepth = NeuDepth.Normal,
    shape: Shape = RoundedCornerShape(24.dp),
): Modifier = if (depth == NeuDepth.Flat) this else drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val androidPath = path.asAndroidPath()

    fun shadowPaint(color: Color, blur: Dp): Paint = Paint().also { p ->
        p.asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = blur.toPx() * 2f
            this.color = color.toArgb()
            if (blur.toPx() > 0f) maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
    }
    val dark = shadowPaint(depth.dark, depth.darkBlur)
    val light = shadowPaint(depth.light, depth.lightBlur)
    val dOff = depth.darkOffset.toPx()
    val lOff = depth.lightOffset.toPx()

    onDrawBehind {
        drawPath(path, depth.fill)
        clipPath(path) {
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                native.translate(dOff, dOff)
                native.drawPath(androidPath, dark.asFrameworkPaint())
                native.restore()
                native.save()
                native.translate(-lOff, -lOff)
                native.drawPath(androidPath, light.asFrameworkPaint())
                native.restore()
            }
        }
    }
}
