package cz.narsil.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Obrysové ikony konceptu E (334), převzaté z SVG návrhu; barvu dává `Icon(tint = …)`. */
object NeuIcons {
    val Home: ImageVector by lazy { icon("home", "M3 11l9-7 9 7v9H3z") }
    val Contract: ImageVector by lazy { icon("contract", "M6 3h9l4 4v14H6z", "M9 12h7M9 16h5") }
    val Badge: ImageVector by lazy { icon("badge", circle(12f, 9f, 6f), "M9 14l-2 7 5-3 5 3-2-7") }
    val Settings: ImageVector by lazy {
        icon(
            "settings",
            circle(12f, 12f, 3f),
            "M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1",
        )
    }
    val Back: ImageVector by lazy { icon("back", "M15 5l-7 7 7 7", width = 2.2f) }
    val Chevron: ImageVector by lazy { icon("chevron", "M9 5l7 7-7 7", width = 2.2f) }
    val Check: ImageVector by lazy { icon("check", "M5 12l5 5 9-10", width = 3f) }
    val Close: ImageVector by lazy { icon("close", "M6 6l12 12M18 6L6 18", width = 2.2f) }
    val Camera: ImageVector by lazy { icon("camera", "M4 8h3l2-3h6l2 3h3v11H4z", circle(12f, 13f, 3.5f)) }
    val Video: ImageVector by lazy { icon("video", "M5 6h11a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z", "M16 10l5-3v10l-5-3") }
    val Person: ImageVector by lazy { icon("person", circle(12f, 7f, 4f), "M4 21c1-4 4-6 8-6s7 2 8 6") }
    val Pause: ImageVector by lazy { icon("pause", "M8 7v10M16 7v10", width = 2.2f) }
    val Play: ImageVector by lazy { icon("play", "M8 5l11 7-11 7z", width = 2.2f) }

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun icon(name: String, vararg paths: String, width: Float = 2f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
