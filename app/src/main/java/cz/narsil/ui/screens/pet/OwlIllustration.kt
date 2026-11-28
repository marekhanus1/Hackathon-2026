package cz.narsil.ui.screens.pet

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vektorová ilustrace sovy Narsilky kreslená přes [Canvas] s jemným dýcháním.
 */
@Composable
fun OwlIllustration(
    mood: PetMood,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val colors = NarsilTheme.colors
    val bodyColor = colors.ember
    val bellyColor = colors.emberContainer
    val accentGold = colors.goldAccent
    val pupilColor = Color(0xFF1E1B18)

    val transition = rememberInfiniteTransition(label = "owl_breath")
    val scale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "owl_scale",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            val w = this.size.width
            val h = this.size.height

            // Ouška
            val leftEar = Path().apply {
                moveTo(w * 0.22f, h * 0.28f)
                lineTo(w * 0.19f, h * 0.10f)
                lineTo(w * 0.36f, h * 0.20f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(w * 0.78f, h * 0.28f)
                lineTo(w * 0.81f, h * 0.10f)
                lineTo(w * 0.64f, h * 0.20f)
                close()
            }
            drawPath(leftEar, color = bodyColor)
            drawPath(rightEar, color = bodyColor)

            // Kulaté tělo
            drawOval(
                color = bodyColor,
                topLeft = Offset(w * 0.14f, h * 0.16f),
                size = Size(w * 0.72f, h * 0.76f),
            )

            // Světlejší bříško
            drawOval(
                color = bellyColor,
                topLeft = Offset(w * 0.28f, h * 0.50f),
                size = Size(w * 0.44f, h * 0.36f),
            )

            // Nožičky
            val footStroke = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
            drawLine(accentGold, Offset(w * 0.38f, h * 0.90f), Offset(w * 0.38f, h * 0.96f), strokeWidth = footStroke.width, cap = StrokeCap.Round)
            drawLine(accentGold, Offset(w * 0.62f, h * 0.90f), Offset(w * 0.62f, h * 0.96f), strokeWidth = footStroke.width, cap = StrokeCap.Round)

            // Oči
            val leftEyeCenter = Offset(w * 0.36f, h * 0.40f)
            val rightEyeCenter = Offset(w * 0.64f, h * 0.40f)
            val eyeRadius = w * 0.13f

            drawCircle(color = Color.White, radius = eyeRadius, center = leftEyeCenter)
            drawCircle(color = Color.White, radius = eyeRadius, center = rightEyeCenter)

            drawMoodEyes(
                mood = mood,
                leftCenter = leftEyeCenter,
                rightCenter = rightEyeCenter,
                eyeRadius = eyeRadius,
                pupilColor = pupilColor,
                eyelidColor = bodyColor,
            )

            // Zobáček
            val beak = Path().apply {
                moveTo(w * 0.45f, h * 0.48f)
                lineTo(w * 0.55f, h * 0.48f)
                lineTo(w * 0.50f, h * 0.57f)
                close()
            }
            drawPath(beak, color = accentGold)

            // Zlatá hvězdička u hrdé sovy
            if (mood == PetMood.HRDA) {
                drawSparkleStar(
                    center = Offset(w * 0.84f, h * 0.18f),
                    outerRadius = w * 0.09f,
                    innerRadius = w * 0.04f,
                    color = accentGold,
                )
            }
        }

        if (mood == PetMood.SPI) {
            Text(
                text = "z",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.echo,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 2.dp),
            )
        }
    }
}

private fun DrawScope.drawMoodEyes(
    mood: PetMood,
    leftCenter: Offset,
    rightCenter: Offset,
    eyeRadius: Float,
    pupilColor: Color,
    eyelidColor: Color,
) {
    val lineStroke = Stroke(width = eyeRadius * 0.24f, cap = StrokeCap.Round)
    when (mood) {
        PetMood.SPI -> {
            listOf(leftCenter, rightCenter).forEach { c ->
                drawArc(
                    color = pupilColor,
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(c.x - eyeRadius * 0.65f, c.y - eyeRadius * 0.35f),
                    size = Size(eyeRadius * 1.3f, eyeRadius * 0.8f),
                    style = lineStroke,
                )
            }
        }
        PetMood.OSPALA -> {
            val pupilRadius = eyeRadius * 0.45f
            drawCircle(color = pupilColor, radius = pupilRadius, center = leftCenter.copy(y = leftCenter.y + eyeRadius * 0.12f))
            drawCircle(color = pupilColor, radius = pupilRadius, center = rightCenter.copy(y = rightCenter.y + eyeRadius * 0.12f))
            listOf(leftCenter, rightCenter).forEach { c ->
                drawArc(
                    color = eyelidColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(c.x - eyeRadius, c.y - eyeRadius),
                    size = Size(eyeRadius * 2f, eyeRadius * 1.9f),
                )
            }
        }
        PetMood.SPOKOJENA -> {
            listOf(leftCenter, rightCenter).forEach { c ->
                drawArc(
                    color = pupilColor,
                    startAngle = 195f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(c.x - eyeRadius * 0.65f, c.y - eyeRadius * 0.3f),
                    size = Size(eyeRadius * 1.3f, eyeRadius * 0.9f),
                    style = lineStroke,
                )
            }
        }
        PetMood.KLIDNA, PetMood.HRDA -> {
            val pupilRadius = eyeRadius * 0.50f
            val glintRadius = eyeRadius * 0.18f
            listOf(leftCenter, rightCenter).forEach { c ->
                drawCircle(color = pupilColor, radius = pupilRadius, center = c)
                drawCircle(
                    color = Color.White,
                    radius = glintRadius,
                    center = Offset(c.x - pupilRadius * 0.35f, c.y - pupilRadius * 0.35f),
                )
            }
        }
    }
}

private fun DrawScope.drawSparkleStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
) {
    val path = Path()
    val points = 8
    for (i in 0 until points) {
        val angle = (i * PI / 4.0) - (PI / 2.0)
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}

@Preview(name = "Sova – Světlý režim", showBackground = true)
@Composable
private fun OwlAllMoodsLightPreview() {
    NarsilTheme(darkTheme = false) {
        Surface {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PetMood.entries.forEach { mood ->
                    OwlIllustration(mood = mood, size = 64.dp)
                }
            }
        }
    }
}

@Preview(
    name = "Sova – Tmavý režim",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun OwlAllMoodsDarkPreview() {
    NarsilTheme(darkTheme = true) {
        Surface {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PetMood.entries.forEach { mood ->
                    OwlIllustration(mood = mood, size = 64.dp)
                }
            }
        }
    }
}
