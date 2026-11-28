package cz.narsil.ui.screens.echo

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import cz.narsil.ui.components.OnGlass
import cz.narsil.ui.components.OnSky
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.aeroSurface
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.neuInset

/**
 * Ozvěna ve stylu Frutiger Aero (dříve E/334, 310): video ze včerejší smlouvy, pojmenování pocitu a volby
 * (pohyb, kroky, Parťák) ve vnořené vaně; hlavní akce „Zavřít feed“. Okno se neposouvá,
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
        NeuHeader(title = "Ozvěna · ${state.sourceAppName}")

        // Skleněná karta s videem: na skle tmavý text (OnGlass), video v tmavém rámu s bílým okrajem.
        OnGlass {
            Column(
                Modifier.fillMaxWidth().neuInset(NeuDepth.Normal, RoundedCornerShape(24.dp)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val videoShape = RoundedCornerShape(18.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(videoShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF0B5C8E), Color(0xFF06324F))))
                        .border(2.dp, Color(0xE6FFFFFF), videoShape),
                    contentAlignment = Alignment.Center,
                ) { OnSky { videoSlot() } }
                Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    NeuLabel("Tvoje včerejší Ozvěna")
                    NeuText(state.videoCaption, size = 14.sp, weight = FontWeight.Bold, lineHeight = 19.sp, maxLines = 3)
                }
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

        // Volby ve skle; text tmavě modrý (OnGlass), jinak by byl bílý na bílém skle.
        OnGlass { Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .neuInset(NeuDepth.Normal, RoundedCornerShape(24.dp))
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
            // 163: místo půjčky do zítřka prosba kamarádovi; odpověď je v MVP simulovaná.
            val left = (state.debtLimitMinutes - state.currentDebtMinutes).coerceAtLeast(0)
            EchoOption(
                short = "PA",
                title = when (state.friendRequest) {
                    FriendRequest.IDLE -> "Poprosit Parťáka o 10 min"
                    FriendRequest.ASKING -> "Píšu Parťákovi…"
                    FriendRequest.GRANTED -> "Parťák: „Jasně, máš 10 min“"
                },
                subtitle = when {
                    state.friendRequest == FriendRequest.ASKING -> "Čekám na odpověď"
                    state.friendRequest == FriendRequest.GRANTED -> "Odemykám 10 min"
                    state.debtLimitMinutes == 0 -> "Parťák je v Nastavení vypnutý"
                    state.canBorrow -> "Kamarád ti pošle pár minut navíc · dnes ještě $left min"
                    else -> "Dnes už ti dal ${state.currentDebtMinutes} min, víc nejde"
                },
                xp = "0 XP",
                enabled = state.canBorrow && state.friendRequest == FriendRequest.IDLE,
                onClick = onBorrow,
            )
        } }

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
        Modifier.size(64.dp).aeroSurface(AeroStyle.BlueOrb, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (state.isVideoPlaying) NeuIcons.Pause else NeuIcons.Play,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview(name = "Ozvěna – koncept E", widthDp = 390, heightDp = 844)
@Composable
private fun EchoScreenPreview() {
    NarsilTheme { AeroBackdrop {
        EchoScreen(
            state = EchoUiState(selectedFeeling = EchoFeeling.UNAVA),
            onSelectFeeling = {},
            onCloseFeed = {},
            onStartSquats = {},
            onPayWithSteps = {},
            onBorrow = {}
        )
    } }
}

@Preview(name = "Ozvěna – bez kroků, s dluhem", widthDp = 390, heightDp = 760)
@Composable
private fun EchoScreenDebtPreview() {
    NarsilTheme { AeroBackdrop {
        EchoScreen(
            state = EchoUiState(sourceAppName = "Instagram Reels", availableSteps = 620, currentDebtMinutes = 30, canBorrow = false),
            onSelectFeeling = {},
            onCloseFeed = {},
            onStartSquats = {},
            onPayWithSteps = {},
            onBorrow = {}
        )
    } }
}
