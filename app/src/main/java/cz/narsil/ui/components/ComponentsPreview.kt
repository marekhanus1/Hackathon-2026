package cz.narsil.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cz.narsil.ui.theme.NarsilTheme

@Composable
private fun ComponentsGalleryContent() {
    val spacing = NarsilTheme.spacing

    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = "Sdílené komponenty Narsil",
                style = MaterialTheme.typography.headlineMedium
            )

            LevelBadge(level = 4, title = "Kovář klidu")

            XpProgressBar(currentXp = 1860)

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

@Preview(name = "Komponenty – Světlý režim", showBackground = true)
@Composable
private fun ComponentsGalleryLightPreview() {
    NarsilTheme(darkTheme = false) {
        ComponentsGalleryContent()
    }
}

@Preview(
    name = "Komponenty – Tmavý režim",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ComponentsGalleryDarkPreview() {
    NarsilTheme(darkTheme = true) {
        ComponentsGalleryContent()
    }
}
