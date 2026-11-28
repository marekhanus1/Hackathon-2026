package cz.narsil.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Frutiger Aero (narsil-frontend-v2): sklo na modrém nebi s rybami a trávou.
 *
 * Jména zůstala z neumorfismu (334), aby obrazovky nemusely měnit API: [Text] a [Muted] jsou barvy
 * textu NA SKLE (tmavě modrá), text přímo na nebi kreslí [cz.narsil.ui.components.NeuText] bíle se stínem
 * uvnitř [cz.narsil.ui.components.OnSky].
 */
object NeuColors {
    /** Obrazovky jsou průhledné, pozadí kreslí [AeroBackdrop]. */
    val Base = Color.Transparent
    val Sky = Color(0xFF159DE9)
    val Inset = Color(0xEBFFFFFF)
    val Deep = Color(0xFFDDF6FF)
    val ShadowDark = Color(0x33004270)
    val ShadowLight = Color(0xF2FFFFFF)
    val DeepShadowDark = Color(0x2E005040)
    val DeepShadowLight = Color(0xFFFFFFFF)
    val Divider = Color(0x2B0076B0)
    val Dashed = Color(0x520078B5)

    /** Aero modrá: čísla, zvýraznění na skle. */
    val Accent = Color(0xFF0A7FC0)
    /** Text na zeleném lesklém tlačítku / bublině. */
    val OnAccent = Color(0xFF064F20)
    val Success = Color(0xFF168231)
    val OnSuccess = Color(0xFFFFFFFF)
    val Error = Color(0xFFC2412F)
    val Text = Color(0xFF063B5C)
    val Muted = Color(0xFF3F6B78)
    val Label = Color(0xFF075B79)

    val OnSky = Color(0xFFFFFFFF)
    val OnSkyMuted = Color(0xF2FFFFFF)
    val SkyTextShadow = Color(0xCC00578E)
    /** Světlé varianty pro text na nebi. */
    val OnSkyAccent = Color(0xFFD8F6FF)
    val OnSkySuccess = Color(0xFFBAFF73)
    val OnSkyError = Color(0xFFFFD2C8)
    val Warning = Color(0xFFFFD166)
}

/**
 * Druh plochy. Jména zůstala z neumorfismu:
 * [Normal] = skleněný panel, [Shallow] = modré skleněné tlačítko, [Deep] = zelená lesklá bublina
 * (vybraná záložka, malé tlačítko, ikona odznaku), [Flat] = nic.
 */
enum class NeuDepth { Normal, Shallow, Deep, Flat }

/** Plocha podle [depth]; jediné místo, které obrazovky volají. */
fun Modifier.neuInset(
    depth: NeuDepth = NeuDepth.Normal,
    shape: Shape = RoundedCornerShape(24.dp),
): Modifier = when (depth) {
    NeuDepth.Normal -> aeroSurface(AeroStyle.Glass, shape)
    NeuDepth.Shallow -> aeroSurface(AeroStyle.BlueGlass, shape)
    NeuDepth.Deep -> aeroSurface(AeroStyle.GreenBubble, shape)
    NeuDepth.Flat -> this
}
