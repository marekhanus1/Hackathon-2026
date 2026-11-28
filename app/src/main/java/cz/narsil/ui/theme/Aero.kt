package cz.narsil.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.narsil.R
import kotlin.math.max

/**
 * Plochy Frutiger Aero podle narsil-frontend-v2 (styles.css):
 * - [Glass]       `.glass-panel`       bílé sklo s modrým nádechem
 * - [Frost]       vnořené pole na skle (input, textarea)
 * - [BlueGlass]   `.aero-icon-button`  modré sklo na nebi
 * - [GreenBubble] `.dock button.active` bílo-zelená bublina (vybraná položka, malé tlačítko)
 * - [GreenGloss]  `.primary-aero`      lesklé zelené tlačítko s ostrým zlomem
 * - [GreenOrb]    `.task-count`        zelená koule
 * - [BlueOrb]     `.verify-orb`        modrá skleněná koule
 * - [AmberGloss] / [RedGloss]          varování / nevratná akce ve stejném lesku
 * - [Dock]        `.dock`              zelená skleněná lišta menu
 * - [Track]       `.progress`          průsvitná drážka ukazatele
 */
enum class AeroStyle { Glass, Frost, BlueGlass, GreenBubble, GreenGloss, GreenOrb, BlueOrb, AmberGloss, RedGloss, Dock, Track }

private class AeroLook(
    val shadow: Color?,
    val shadowDy: Dp,
    val shadowBlur: Dp,
    val border: Color?,
    val borderWidth: Dp,
    val highlight: Float,
    val bottomTint: Color?,
)

private fun look(style: AeroStyle): AeroLook = when (style) {
    AeroStyle.Glass -> AeroLook(Color(0x33004270), 5.dp, 12.dp, Color(0xEBFFFFFF), 1.dp, 0.95f, Color(0x140082BF))
    AeroStyle.Frost -> AeroLook(null, 0.dp, 0.dp, Color(0x400078B5), 1.dp, 0f, null)
    AeroStyle.BlueGlass -> AeroLook(Color(0x40004281), 3.dp, 7.dp, Color(0xE0FFFFFF), 1.dp, 0.8f, null)
    AeroStyle.GreenBubble -> AeroLook(Color(0x2E005041), 3.dp, 7.dp, Color(0xEBFFFFFF), 1.dp, 1f, null)
    AeroStyle.GreenGloss, AeroStyle.AmberGloss, AeroStyle.RedGloss ->
        AeroLook(Color(0x40004D25), 4.dp, 9.dp, Color.White, 2.dp, 0.8f, null)
    AeroStyle.GreenOrb -> AeroLook(Color(0x42003E4B), 4.dp, 8.dp, Color.White, 2.dp, 0f, null)
    AeroStyle.BlueOrb -> AeroLook(Color(0x47003E74), 8.dp, 20.dp, Color.White, 3.dp, 0f, null)
    AeroStyle.Dock -> AeroLook(Color(0x52004134), 7.dp, 18.dp, Color(0xE6FFFFFF), 1.dp, 0.88f, null)
    AeroStyle.Track -> AeroLook(null, 0.dp, 0.dp, null, 0.dp, 0f, null)
}

private fun brush(style: AeroStyle, size: Size): Brush {
    val w = size.width
    val h = size.height
    val r = max(w, h).coerceAtLeast(1f)
    return when (style) {
        AeroStyle.Glass -> Brush.linearGradient(
            listOf(Color(0xF2FFFFFF), Color(0xD9D6FAFF)), Offset.Zero, Offset(w, h * 1.2f),
        )
        AeroStyle.Frost -> Brush.linearGradient(listOf(Color(0xD9FFFFFF), Color(0xC7F3FDFF)))
        AeroStyle.BlueGlass -> Brush.linearGradient(
            0f to Color(0xC7FFFFFF), 0.48f to Color(0x6B6DDCFF), 1f to Color(0x61006DC6),
            start = Offset.Zero, end = Offset(w, h * 1.4f),
        )
        AeroStyle.GreenBubble -> Brush.radialGradient(
            0f to Color.White, 0.47f to Color(0xD1AAF7DE), 1f to Color(0xBD3DC46D),
            center = Offset(w * 0.35f, h * 0.2f), radius = r * 0.9f,
        )
        AeroStyle.GreenGloss -> Brush.verticalGradient(
            0f to Color(0xFFAAF57A), 0.52f to Color(0xFF36C246), 0.53f to Color(0xFF159437), 1f to Color(0xFF48D159),
        )
        AeroStyle.AmberGloss -> Brush.verticalGradient(
            0f to Color(0xFFFFEBAD), 0.52f to Color(0xFFFFC94F), 0.53f to Color(0xFFF2A51C), 1f to Color(0xFFFFD36B),
        )
        AeroStyle.RedGloss -> Brush.verticalGradient(
            0f to Color(0xFFFFC2B5), 0.52f to Color(0xFFF26B53), 0.53f to Color(0xFFD9432E), 1f to Color(0xFFF2826B),
        )
        AeroStyle.GreenOrb -> Brush.radialGradient(
            0f to Color(0xFFF8FFD8), 0.4f to Color(0xFFA7F46D), 0.73f to Color(0xFF35BF3C), 1f to Color(0xFF0C7931),
            center = Offset(w * 0.3f, h * 0.24f), radius = r * 0.85f,
        )
        AeroStyle.BlueOrb -> Brush.radialGradient(
            0f to Color.White, 0.05f to Color.White, 0.27f to Color(0xB3D1FDFF), 0.66f to Color(0xA644C2F0), 1f to Color(0x9E005BAB),
            center = Offset(w * 0.3f, h * 0.23f), radius = r * 0.85f,
        )
        AeroStyle.Dock -> Brush.verticalGradient(listOf(Color(0xE6E1FFEE), Color(0xB84CDA51)))
        AeroStyle.Track -> Brush.linearGradient(listOf(Color(0x24005B96), Color(0x24005B96)))
    }
}

/** Nakreslí plochu [style] za obsah v [shape] (stín, výplň, lesk nahoře, bílý okraj). */
fun Modifier.aeroSurface(style: AeroStyle, shape: Shape): Modifier = drawWithCache {
    val look = look(style)
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val fill = brush(style, size)
    val shadowPaint = look.shadow?.let { c ->
        Paint().also { p ->
            p.asFrameworkPaint().apply {
                isAntiAlias = true
                color = c.toArgb()
                if (look.shadowBlur.toPx() > 0f) maskFilter = BlurMaskFilter(look.shadowBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        }
    }
    val dy = look.shadowDy.toPx()
    val hl = 2.5.dp.toPx()
    val tint = 16.dp.toPx()
    val bw = look.borderWidth.toPx()
    onDrawBehind {
        if (shadowPaint != null) drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.translate(0f, dy)
            native.drawPath(path.asAndroidPath(), shadowPaint.asFrameworkPaint())
            native.restore()
        }
        drawPath(path, fill)
        if (look.highlight > 0f || look.bottomTint != null) clipPath(path) {
            if (look.highlight > 0f) drawRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = look.highlight), Color.Transparent), 0f, hl * 1.6f),
                size = Size(size.width, hl * 1.6f),
            )
            look.bottomTint?.let {
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, it), size.height - tint, size.height),
                    topLeft = Offset(0f, size.height - tint),
                    size = Size(size.width, tint),
                )
            }
        }
        if (look.border != null && bw > 0f) drawPath(path, look.border, style = Stroke(bw))
    }
}

/** Pozadí celé aplikace a překryvných oken: obrázek, ztmavení nahoře a dole a tři bubliny. */
@Composable
fun AeroBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(NeuColors.Sky)) {
        Image(
            painterResource(R.drawable.aero_background),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color(0x38003888), 0.29f to Color.Transparent, 0.62f to Color(0x14001A2D), 1f to Color(0x52004A29),
                ),
            ),
        )
        AeroBubble(78.dp, Modifier.align(Alignment.TopEnd).offset(x = 18.dp, y = 120.dp))
        AeroBubble(36.dp, Modifier.align(Alignment.TopStart).offset(x = 10.dp, y = 300.dp))
        AeroBubble(48.dp, Modifier.align(Alignment.BottomEnd).offset(x = (-22).dp, y = (-150).dp))
        content()
    }
}

/** Mýdlová bublina na pozadí (`.background-bubbles i`). */
@Composable
fun AeroBubble(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    0f to Color(0xE6FFFFFF), 0.36f to Color(0x215AD2FF), 0.72f to Color(0x380059B0), 1f to Color(0x7AFFFFFF),
                ),
            )
            .border(1.dp, Color(0xBFFFFFFF), CircleShape),
    )
}
