package cz.narsil.ui.screens.echo

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.components.NeuBadgeIcon
import cz.narsil.ui.components.NeuDivider
import cz.narsil.ui.components.NeuHeader
import cz.narsil.ui.components.NeuIcons
import cz.narsil.ui.components.NeuLabel
import cz.narsil.ui.components.NeuPrimaryButton
import cz.narsil.ui.components.NeuScreen
import cz.narsil.ui.components.NeuSegmented
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.neuInset

/**
 * Ozvěna ve stylu E (334, dříve 310): video ze včerejší smlouvy, pojmenování pocitu a volby
 * (pohyb, kroky, půjčka) ve vnořené vaně; hlavní akce „Zavřít feed“. Okno se neposouvá,
 * při malé výšce se posouvá jen vana s volbami.
 */
@Composable
fun EchoScreen(
    state: EchoUiState,
    onSelectFeeling: (EchoFeeling) -> Unit,
    onCloseFeed: () -> Unit,
    onStartSquats: () -> Unit,
    onPayWithSteps: () -> Unit,
    onBorrow: () -> Unit,
    modifier: Modifier = Modifier,
    videoSlot: @Composable () -> Unit = { DefaultEchoVideoPlaceholder(state) }
) {
    val canPayWithSteps = state.availableSteps >= state.stepCostForTenMin
    NeuScreen(modifier.systemBarsPadding(), gap = 14.dp) {
        NeuHeader(
            title = "Ozvěna · ${state.sourceAppName}",
            trailing = if (state.currentDebtMinutes > 0) {
                {
                    Box(Modifier.neuInset(NeuDepth.Deep, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        NeuText("Dluh ${state.currentDebtMinutes} min", size = 12.sp, weight = FontWeight.ExtraBold, color = NeuColors.Error)
                    }
                }
            } else null,
        )

        Column(
            Modifier.fillMaxWidth().neuInset(NeuDepth.Normal, RoundedCornerShape(28.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().height(170.dp).neuInset(NeuDepth.Deep, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) { videoSlot() }
            Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                NeuLabel("Tvoje včerejší Ozvěna")
                NeuText(state.videoCaption, size = 14.sp, weight = FontWeight.Bold, lineHeight = 19.sp, maxLines = 3)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NeuText("Čemu se teď vyhýbáš?", Modifier.padding(horizontal = 4.dp), size = 13.sp, weight = FontWeight.ExtraBold)
            NeuSegmented(
                options = EchoFeeling.entries.toList(),
                selected = state.selectedFeeling,
                label = { it.label },
                onSelect = onSelectFeeling,
                height = 36.dp,
                outerRadius = 18.dp,
                innerRadius = 12.dp,
                fontSize = 11.sp,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .neuInset(NeuDepth.Normal, RoundedCornerShape(28.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            EchoOption(
                short = "DŘ",
                title = state.exerciseOptionTitle,
                subtitle = "Kamera spočítá opakování a odemkne 10 min",
                xp = "+20 XP",
                enabled = true,
                onClick = onStartSquats,
            )
            NeuDivider()
            EchoOption(
                short = "KR",
                title = "Zaplatit kroky",
                subtitle = if (canPayWithSteps) "${state.stepCostForTenMin} kroků za 10 min · máš ${state.availableSteps}"
                else "Potřebuješ ${state.stepCostForTenMin} kroků · zatím máš ${state.availableSteps}",
                xp = "+10 XP",
                enabled = canPayWithSteps,
                onClick = onPayWithSteps,
            )
            NeuDivider()
            EchoOption(
                short = "PŮ",
                title = "Půjčit si 10 min",
                subtitle = if (state.canBorrow) "Přičte se k zítřejšímu pohybovému cíli · dluh ${state.currentDebtMinutes} / ${state.debtLimitMinutes} min"
                else "Limit dluhu ${state.debtLimitMinutes} min · dlužíš ${state.currentDebtMinutes} min",
                xp = "0 XP",
                enabled = state.canBorrow,
                onClick = onBorrow,
            )
        }

        NeuPrimaryButton("Zavřít feed · +50 XP", onClick = onCloseFeed)
    }
}

@Composable
private fun EchoOption(short: String, title: String, subtitle: String, xp: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NeuBadgeIcon(short, size = 38.dp, radius = 12.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            NeuText(title, size = 14.sp, weight = FontWeight.ExtraBold, maxLines = 1)
            NeuText(subtitle, size = 11.sp, weight = FontWeight.SemiBold, color = NeuColors.Muted, maxLines = 2)
        }
        NeuText(xp, size = 12.sp, weight = FontWeight.ExtraBold, color = if (xp == "0 XP") NeuColors.Muted else NeuColors.Success)
    }
}

@Composable
private fun DefaultEchoVideoPlaceholder(state: EchoUiState) {
    Box(
        Modifier.size(64.dp).border(2.dp, NeuColors.Accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (state.isVideoPlaying) NeuIcons.Pause else NeuIcons.Play,
            contentDescription = null,
            tint = NeuColors.Accent,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Preview(name = "Ozvěna – koncept E", widthDp = 390, heightDp = 844)
@Composable
private fun EchoScreenPreview() {
    NarsilTheme {
        EchoScreen(
            state = EchoUiState(selectedFeeling = EchoFeeling.UNAVA),
            onSelectFeeling = {},
            onCloseFeed = {},
            onStartSquats = {},
            onPayWithSteps = {},
            onBorrow = {}
        )
    }
}

@Preview(name = "Ozvěna – bez kroků, s dluhem", widthDp = 390, heightDp = 760)
@Composable
private fun EchoScreenDebtPreview() {
    NarsilTheme {
        EchoScreen(
            state = EchoUiState(sourceAppName = "Instagram Reels", availableSteps = 620, currentDebtMinutes = 30, canBorrow = false),
            onSelectFeeling = {},
            onCloseFeed = {},
            onStartSquats = {},
            onPayWithSteps = {},
            onBorrow = {}
        )
    }
}
