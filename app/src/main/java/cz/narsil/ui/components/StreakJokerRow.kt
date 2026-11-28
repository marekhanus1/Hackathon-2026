package cz.narsil.ui.components

import cz.narsil.ui.text.CzechPlural
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 * Série splněných smluv a žolíci (50 dřepů navíc) ve skle: aktivní žolík = zelená koule, prázdný = matné sklo.
 */
@Composable
fun StreakJokerRow(
    streakDays: Int,
    availableJokers: Int,
    maxJokers: Int = 3,
    modifier: Modifier = Modifier
) {
    val safeDays = streakDays.coerceAtLeast(0)
    val safeMax = maxJokers.coerceAtLeast(1)
    val safeJokers = availableJokers.coerceIn(0, safeMax)

    val dayUnit = CzechPlural.word(safeDays, "den", "dny", "dní")

    NeuPanel(modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                NeuText("Série: $safeDays $dayUnit", size = 15.sp, weight = FontWeight.Bold)
                NeuText(
                    "Žolík (50 dřepů navíc) chrání sérii při relapsu",
                    size = 12.sp,
                    weight = FontWeight.Medium,
                    color = NeuColors.Muted,
                    lineHeight = 16.sp,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(safeMax) { index ->
                    val isActive = index < safeJokers
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .aeroSurface(if (isActive) AeroStyle.GreenOrb else AeroStyle.Frost, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isActive) "Ž" else "·",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isActive) Color.White else NeuColors.Muted,
                                shadow = if (isActive) Shadow(Color(0xAA08722A), Offset(0f, 2f), 4f) else null,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "StreakJokerRow – Aero", widthDp = 390, heightDp = 140)
@Composable
private fun StreakJokerRowPreview() {
    NarsilTheme {
        AeroBackdrop {
            StreakJokerRow(streakDays = 6, availableJokers = 2, modifier = Modifier.padding(16.dp))
        }
    }
}
