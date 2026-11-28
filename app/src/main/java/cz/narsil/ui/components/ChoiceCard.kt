package cz.narsil.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
 * Visual emphasis tones for intervention and contract choice cards.
 */
enum class ChoiceCardTone {
    POSITIVE,
    EMBER,
    ECHO,
    DEBT,
    NEUTRAL
}

private class ChoiceLook(val style: AeroStyle, val title: Color, val subtitle: Color, val badge: AeroStyle, val badgeText: Color)

private fun lookFor(tone: ChoiceCardTone): ChoiceLook = when (tone) {
    ChoiceCardTone.POSITIVE -> ChoiceLook(AeroStyle.GreenGloss, NeuColors.OnAccent, Color(0xFF0B5A26), AeroStyle.GreenBubble, NeuColors.OnAccent)
    ChoiceCardTone.EMBER -> ChoiceLook(AeroStyle.AmberGloss, Color(0xFF5A3A00), Color(0xFF6E4C0E), AeroStyle.Glass, Color(0xFF5A3A00))
    ChoiceCardTone.ECHO -> ChoiceLook(AeroStyle.Glass, NeuColors.Accent, NeuColors.Muted, AeroStyle.BlueGlass, NeuColors.Text)
    ChoiceCardTone.DEBT -> ChoiceLook(AeroStyle.RedGloss, Color.White, Color(0xF2FFFFFF), AeroStyle.Glass, NeuColors.Error)
    ChoiceCardTone.NEUTRAL -> ChoiceLook(AeroStyle.Glass, NeuColors.Text, NeuColors.Muted, AeroStyle.Frost, NeuColors.Label)
}

/**
 * Velká volba (Ozvěna, kroky smlouvy, demo panel) ve stylu Frutiger Aero.
 * [ChoiceCardTone.POSITIVE] je lesklé zelené tlačítko, ostatní tóny jantarový / červený lesk nebo sklo.
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
    val look = lookFor(tone)
    val shape = RoundedCornerShape(22.dp)
    OnGlass {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.55f)
                .aeroSurface(look.style, shape)
                .clip(shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                NeuText(
                    title,
                    size = 16.sp,
                    weight = if (tone == ChoiceCardTone.POSITIVE) FontWeight.ExtraBold else FontWeight.Bold,
                    color = look.title,
                )
                NeuText(subtitle, size = 13.sp, weight = FontWeight.Medium, color = look.subtitle, lineHeight = 18.sp)
            }

            if (!badgeText.isNullOrBlank()) {
                Box(
                    Modifier
                        .aeroSurface(look.badge, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    NeuText(badgeText, size = 12.sp, weight = FontWeight.ExtraBold, color = look.badgeText, maxLines = 1)
                }
            }
        }
    }
}

@Preview(name = "ChoiceCard – Aero", widthDp = 390, heightDp = 520)
@Composable
private fun ChoiceCardPreview() {
    NarsilTheme {
        AeroBackdrop {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceCard("Zavřít feed", "Chuť opadla, vracím se k sobě", {}, tone = ChoiceCardTone.POSITIVE, badgeText = "+50 XP")
                ChoiceCard("Udělat 20 dřepů hned", "Kamera spočítá opakování a odemkne 10 min", {}, tone = ChoiceCardTone.EMBER, badgeText = "+20 XP")
                ChoiceCard("Zaplatit kroky", "1 000 kroků ze dneška za 10 min", {}, tone = ChoiceCardTone.ECHO, badgeText = "+10 XP")
                ChoiceCard("Poprosit Parťáka o 10 min", "Kamarád ti pošle pár minut navíc", {}, tone = ChoiceCardTone.DEBT, badgeText = "0 XP")
                ChoiceCard("Pokračovat", "Bez odměny", {}, enabled = false)
            }
        }
    }
}
