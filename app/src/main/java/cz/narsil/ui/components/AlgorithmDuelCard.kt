package cz.narsil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.aeroSurface

/**
 * Denní souboj ve skle: získaný čas (zeleně) proti času ve feedu (modře), lesklý dělený pruh a stav souboje.
 */
@Composable
fun AlgorithmDuelCard(
    reclaimedMinutes: Int,
    feedMinutes: Int,
    wonDaysCount: Int,
    modifier: Modifier = Modifier
) {
    val safeReclaimed = reclaimedMinutes.coerceAtLeast(0)
    val safeFeed = feedMinutes.coerceAtLeast(0)
    val total = (safeReclaimed + safeFeed).coerceAtLeast(1)
    val reclaimedShare = if (safeReclaimed + safeFeed == 0) {
        0.5f
    } else {
        (safeReclaimed.toFloat() / total.toFloat()).coerceIn(0.08f, 0.92f)
    }

    val statusText = when {
        safeReclaimed > safeFeed -> "Dnes vedeš"
        safeReclaimed == safeFeed -> "Vyrovnaný souboj"
        else -> "Algoritmus má náskok"
    }
    val leading = safeReclaimed >= safeFeed

    NeuPanel(modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(16.dp), gap = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuText("Souboj s algoritmem", Modifier.weight(1f), size = 15.sp, weight = FontWeight.Bold, maxLines = 1)
            Box(
                Modifier
                    .aeroSurface(if (leading) AeroStyle.GreenBubble else AeroStyle.BlueGlass, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                NeuText(statusText, size = 11.sp, weight = FontWeight.ExtraBold, color = if (leading) NeuColors.OnAccent else NeuColors.Text, maxLines = 1)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                NeuText("$safeReclaimed min", size = 24.sp, weight = FontWeight.Bold, color = Color(0xFF0C8731))
                NeuText("Získaný čas", size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted)
            }
            Column(horizontalAlignment = Alignment.End) {
                NeuText("$safeFeed min", size = 24.sp, weight = FontWeight.Bold, color = Color(0xFF087FB0))
                NeuText("Čas ve feedu", size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(Color(0x24005B96))
        ) {
            Box(
                modifier = Modifier
                    .weight(reclaimedShare)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(Color(0xFFADF87A), Color(0xFF22AF42))))
            ) { Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0x99FFFFFF))) }
            Box(
                modifier = Modifier
                    .weight(1f - reclaimedShare)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(Color(0xFF9EE3FF), Color(0xFF1C8FD0))))
            ) { Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0x80FFFFFF))) }
        }

        NeuText("Vyhrané dny celkem: $wonDaysCount", size = 12.sp, weight = FontWeight.Bold, color = NeuColors.Label)
    }
}

@Preview(name = "AlgorithmDuelCard – Aero", widthDp = 390, heightDp = 220)
@Composable
private fun AlgorithmDuelCardPreview() {
    NarsilTheme {
        AeroBackdrop {
            AlgorithmDuelCard(reclaimedMinutes = 45, feedMinutes = 18, wonDaysCount = 12, modifier = Modifier.padding(16.dp))
        }
    }
}
