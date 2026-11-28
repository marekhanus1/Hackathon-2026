package cz.narsil.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.NarsilTheme

@Composable
private fun ComponentsGalleryContent() {
    AeroBackdrop {
        OnSky {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NeuText("Sdílené komponenty Narsil", size = 24.sp, weight = FontWeight.Bold)

                LevelBadge(level = 4, title = "Kovář klidu")

                NeuPanel(Modifier.fillMaxWidth()) { XpProgressBar(currentXp = 1860) }

                AvatarCard(
                    state = AvatarState.ENERGIZED,
                    summary7Days = "Posledních 7 dní: 5 výher · 420 dřepů"
                )

                AlgorithmDuelCard(
                    reclaimedMinutes = 42,
                    feedMinutes = 16,
                    wonDaysCount = 11
                )

                StreakJokerRow(
                    streakDays = 6,
                    availableJokers = 2
                )

                ChoiceCard(
                    title = "Zavřít feed",
                    subtitle = "Chuť opadla, vracím se ke svému dni",
                    badgeText = "+50 XP",
                    tone = ChoiceCardTone.POSITIVE,
                    onClick = {}
                )

                ChoiceCard(
                    title = "Udělat 20 dřepů hned",
                    subtitle = "Kamera spočítá dřepy a odemkne 10 minut",
                    badgeText = "+20 XP",
                    tone = ChoiceCardTone.EMBER,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Komponenty – Aero", widthDp = 390, heightDp = 1100)
@Composable
private fun ComponentsGalleryPreview() {
    NarsilTheme {
        ComponentsGalleryContent()
    }
}
