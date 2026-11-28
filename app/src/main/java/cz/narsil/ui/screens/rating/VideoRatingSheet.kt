package cz.narsil.ui.screens.rating

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.components.NeuSmallButton
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.OnGlass
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.aeroSurface

/**
 * Spodní panel „Co ti to video dalo?“ (156): pět odpovědí, jedno ťuknutí. Po odpovědi jen poděkuje.
 * Neblokuje: okno nad YouTube je malé a samo zmizí (overlay), „Teď ne“ ho zavře hned.
 */
@Composable
fun VideoRatingSheet(
    state: VideoRatingUiState,
    onAnswer: (VideoValue) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = NarsilTheme.spacing
    val shape = RoundedCornerShape(26.dp)

    // Aero: neprůhledný skleněný panel (pod ním je cizí aplikace, ne nebe) s bílým okrajem a leskem nahoře.
    OnGlass {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .shadow(14.dp, shape, ambientColor = Color(0xFF004270), spotColor = Color(0xFF004270))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE3F6FF), Color(0xFFCDEFFC))))
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.Transparent), 0f, 6.dp.toPx()),
                        size = Size(size.width, 6.dp.toPx()),
                    )
                }
                .border(1.5.dp, Color.White, shape)
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(NeuColors.Dashed),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeuText(
                    state.question,
                    Modifier.weight(1f),
                    size = 20.sp,
                    weight = FontWeight.Bold,
                    lineHeight = 25.sp,
                )
                if (state.earnedXp > 0) {
                    Box(
                        Modifier
                            .aeroSurface(AeroStyle.GreenBubble, CircleShape)
                            .padding(horizontal = spacing.sm, vertical = spacing.xxs),
                    ) {
                        NeuText("+${state.earnedXp} XP", size = 13.sp, weight = FontWeight.Bold, color = NeuColors.OnAccent, maxLines = 1)
                    }
                }
            }
            NeuText(
                state.videoTitle,
                size = 13.sp,
                weight = FontWeight.Medium,
                color = NeuColors.Muted,
                maxLines = 1,
            )

            val answered = state.answered
            if (answered != null) {
                NeuText(
                    "${answered.emoji} Díky. Zapsáno do tvé mapy hodnoty.",
                    Modifier.padding(vertical = spacing.sm),
                    size = 16.sp,
                    weight = FontWeight.Bold,
                    color = NeuColors.Success,
                )
            } else {
                VideoValue.entries.forEach { value ->
                    val rowShape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aeroSurface(AeroStyle.Frost, rowShape)
                            .clip(rowShape)
                            .clickable { onAnswer(value) }
                            .padding(horizontal = spacing.md, vertical = spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NeuText(value.emoji, size = 16.sp)
                        NeuText(value.label, size = 15.sp, weight = FontWeight.Bold)
                    }
                }
                NeuSmallButton(
                    "Teď ne",
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    color = NeuColors.Muted,
                )
            }
        }
    }
}

@Preview(name = "Co ti video dalo – Světlý", showBackground = true)
@Composable
private fun VideoRatingLightPreview() {
    NarsilTheme(darkTheme = false) {
        VideoRatingSheet(state = VideoRatingUiState(), onAnswer = {}, onDismiss = {})
    }
}

@Preview(name = "Co ti video dalo – Tmavý, odpovězeno", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VideoRatingDarkPreview() {
    NarsilTheme(darkTheme = true) {
        VideoRatingSheet(
            state = VideoRatingUiState(videoTitle = "10 minut feedu na TikToku", question = "Co ti daly ty minuty?", answered = VideoValue.NOTHING),
            onAnswer = {},
            onDismiss = {},
        )
    }
}
