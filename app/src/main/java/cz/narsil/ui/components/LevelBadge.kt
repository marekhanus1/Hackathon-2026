package cz.narsil.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
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
 * Pilulka s úrovní a volitelným českým titulem: bílo-zelená bublina se zelenou koulí s číslem.
 */
@Composable
fun LevelBadge(
    level: Int,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .aeroSurface(AeroStyle.GreenBubble, RoundedCornerShape(50))
            .padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .aeroSurface(AeroStyle.GreenOrb, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level.toString(),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    shadow = Shadow(Color(0xAA08722A), Offset(0f, 2f), 4f),
                ),
            )
        }

        val label = if (title.isNullOrBlank()) {
            "Úroveň $level"
        } else {
            "Úroveň $level · $title"
        }
        Text(
            text = label,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeuColors.OnAccent),
            maxLines = 1,
        )
    }
}

@Preview(name = "LevelBadge – Aero", widthDp = 360, heightDp = 90)
@Composable
private fun LevelBadgePreview() {
    NarsilTheme {
        AeroBackdrop {
            LevelBadge(level = 4, title = "Kovář klidu", modifier = Modifier.padding(16.dp))
        }
    }
}
