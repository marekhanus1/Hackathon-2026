package cz.narsil.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.theme.NarsilTheme

/**
 * Visual emphasis tones for intervention and contract choice cards.
 */
enum class ChoiceCardTone {
    POSITIVE,
    EMBER,
    ECHO,
    DEBT,
    NEUTRAL
}

/**
 * Large tactile option card used primarily in the Ozvěna overlay and contract steps.
 * [ChoiceCardTone.POSITIVE] receives strong visual prominence to highlight closing the feed.
 */
@Composable
fun ChoiceCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ChoiceCardTone = ChoiceCardTone.NEUTRAL,
    badgeText: String? = null,
    enabled: Boolean = true
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    val containerColor = when (tone) {
        ChoiceCardTone.POSITIVE -> colors.positive
        ChoiceCardTone.EMBER -> colors.emberContainer
        ChoiceCardTone.ECHO -> colors.echoContainer
        ChoiceCardTone.DEBT -> colors.debtContainer
        ChoiceCardTone.NEUTRAL -> MaterialTheme.colorScheme.surface
    }

    val titleColor = when (tone) {
        ChoiceCardTone.POSITIVE -> colors.onPositive
        ChoiceCardTone.EMBER -> colors.onEmberContainer
        ChoiceCardTone.ECHO -> colors.onEchoContainer
        ChoiceCardTone.DEBT -> colors.onDebtContainer
        ChoiceCardTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }

    val subtitleColor = when (tone) {
        ChoiceCardTone.POSITIVE -> colors.onPositive.copy(alpha = 0.88f)
        ChoiceCardTone.EMBER -> colors.onEmberContainer.copy(alpha = 0.82f)
        ChoiceCardTone.ECHO -> colors.onEchoContainer.copy(alpha = 0.82f)
        ChoiceCardTone.DEBT -> colors.onDebtContainer.copy(alpha = 0.82f)
        ChoiceCardTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderStroke = when (tone) {
        ChoiceCardTone.POSITIVE -> BorderStroke(2.dp, colors.goldAccent.copy(alpha = 0.65f))
        ChoiceCardTone.EMBER -> BorderStroke(1.dp, colors.ember.copy(alpha = 0.4f))
        ChoiceCardTone.ECHO -> BorderStroke(1.dp, colors.echo.copy(alpha = 0.4f))
        ChoiceCardTone.DEBT -> BorderStroke(1.dp, colors.debt.copy(alpha = 0.4f))
        ChoiceCardTone.NEUTRAL -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val badgeContainer = when (tone) {
        ChoiceCardTone.POSITIVE -> colors.onPositive.copy(alpha = 0.20f)
        ChoiceCardTone.EMBER -> colors.ember.copy(alpha = 0.16f)
        ChoiceCardTone.ECHO -> colors.echo.copy(alpha = 0.16f)
        ChoiceCardTone.DEBT -> colors.debt.copy(alpha = 0.16f)
        ChoiceCardTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = titleColor
        ),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (tone == ChoiceCardTone.POSITIVE) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (tone == ChoiceCardTone.POSITIVE) FontWeight.Bold else FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            }

            if (!badgeText.isNullOrBlank()) {
                Surface(
                    shape = CircleShape,
                    color = badgeContainer,
                    contentColor = titleColor
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                    )
                }
            }
        }
    }
}

@Preview(name = "ChoiceCard – Světlý", showBackground = true)
@Composable
private fun ChoiceCardLightPreview() {
    NarsilTheme(darkTheme = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceCard(
                    title = "Zavřít feed",
                    subtitle = "Chuť opadla, vracím se k sobě",
                    badgeText = "+50 XP",
                    tone = ChoiceCardTone.POSITIVE,
                    onClick = {}
                )
                ChoiceCard(
                    title = "Udělat 20 dřepů hned",
                    subtitle = "Kamera spočítá opakování a odemkne 10 min",
                    badgeText = "+20 XP",
                    tone = ChoiceCardTone.EMBER,
                    onClick = {}
                )
                ChoiceCard(
                    title = "Zaplatit kroky",
                    subtitle = "1 000 kroků ze dneška za 10 min",
                    badgeText = "+10 XP",
                    tone = ChoiceCardTone.ECHO,
                    onClick = {}
                )
                ChoiceCard(
                    title = "Poprosit Parťáka o 10 min",
                    subtitle = "Kamarád ti pošle pár minut navíc",
                    badgeText = "0 XP",
                    tone = ChoiceCardTone.DEBT,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(
    name = "ChoiceCard – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ChoiceCardDarkPreview() {
    NarsilTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceCard(
                    title = "Zavřít feed",
                    subtitle = "Chuť opadla, vracím se k sobě",
                    badgeText = "+50 XP",
                    tone = ChoiceCardTone.POSITIVE,
                    onClick = {}
                )
                ChoiceCard(
                    title = "Udělat 20 dřepů hned",
                    subtitle = "Kamera spočítá opakování a odemkne 10 min",
                    badgeText = "+20 XP",
                    tone = ChoiceCardTone.EMBER,
                    onClick = {}
                )
                ChoiceCard(
                    title = "Poprosit Parťáka o 10 min",
                    subtitle = "Kamarád ti pošle pár minut navíc",
                    badgeText = "0 XP",
                    tone = ChoiceCardTone.DEBT,
                    onClick = {}
                )
            }
        }
    }
}
