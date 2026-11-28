package cz.narsil.ui.screens.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.components.AeroOrb
import cz.narsil.ui.components.AvatarState
import cz.narsil.ui.components.NeuBadgeIcon
import cz.narsil.ui.components.NeuDivider
import cz.narsil.ui.components.NeuHeader
import cz.narsil.ui.components.NeuIcons
import cz.narsil.ui.components.NeuLabel
import cz.narsil.ui.components.NeuPanel
import cz.narsil.ui.components.NeuProgress
import cz.narsil.ui.components.NeuSmallButton
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.OnGlass
import cz.narsil.ui.components.OnSky
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.aeroSurface
import java.text.NumberFormat
import java.util.Locale

/**
 * Odznaky a parta ve stylu Frutiger Aero (narsil-frontend-v2, `#screen-badges`): nadpis na nebi se zelenou
 * koulí, budoucí já, mřížka 2 sloupců skleněných dlaždic se zelenou ikonou a detailem vybrané, společný cíl
 * party se zeleným leskem a žebříček získaného času. Obrazovka je průhledná a posouvá se celá.
 */
@Composable
fun BadgesScreen(
    state: BadgesUiState,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") avatarIllustrationSlot: (@Composable (AvatarState) -> Unit)? = null,
    /** Vstup do Zrcadla (statistiky); null = skrýt. */
    onOpenMirror: (() -> Unit)? = null,
) {
    var pickedId by rememberSaveable { mutableStateOf<String?>(null) }
    val picked = state.badges.firstOrNull { it.id == pickedId } ?: state.badges.firstOrNull()

    OnSky {
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeuHeader(
                title = "Odznaky a parta",
                subtitle = "Tvoje malé výhry proti algoritmu.",
                trailing = {
                    if (onOpenMirror != null) NeuSmallButton("Zrcadlo", onOpenMirror, height = 38.dp)
                    else AeroOrb("★")
                },
            )

            FutureSelfPanel(state)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.badges.take(6).chunked(2).forEach { row ->
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { badge ->
                            BadgeCell(
                                badge,
                                selected = badge.id == picked?.id,
                                onClick = { pickedId = badge.id },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        repeat(2 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }

            if (picked != null) {
                NeuPanel(Modifier.fillMaxWidth(), radius = 18.dp, padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                    NeuText(
                        "${picked.title}: ${picked.description}",
                        size = 12.sp,
                        weight = FontWeight.Medium,
                        color = NeuColors.Muted,
                        lineHeight = 17.sp,
                        maxLines = 3,
                    )
                }
            }

            NeuPanel(Modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(15.dp), gap = 8.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    NeuText("Výzva party · společný cíl kroků", Modifier.weight(1f), size = 14.sp, weight = FontWeight.Bold, maxLines = 2)
                    NeuText(
                        "${formatNumber(state.partyCurrentSteps)} / ${formatNumber(state.partyGoalSteps)} kroků",
                        size = 11.sp,
                        weight = FontWeight.Medium,
                        color = NeuColors.Muted,
                        maxLines = 1,
                    )
                }
                NeuProgress(
                    if (state.partyGoalSteps > 0) state.partyCurrentSteps.toFloat() / state.partyGoalSteps else 0f,
                    color = NeuColors.Success,
                    height = 13.dp,
                )
            }

            NeuPanel(Modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), gap = 4.dp) {
                NeuLabel("Získaný čas tento týden")
                state.leaderboard.forEach { p ->
                    NeuDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        NeuText("${p.rank}.", Modifier.width(24.dp), size = 13.sp, weight = FontWeight.ExtraBold, color = NeuColors.Accent)
                        NeuText(
                            p.name,
                            Modifier.weight(1f),
                            size = 13.sp,
                            weight = if (p.isCurrentUser) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (p.isCurrentUser) NeuColors.Accent else NeuColors.Text,
                            maxLines = 1,
                        )
                        NeuText("${p.reclaimedMinutes} min", size = 12.sp, weight = FontWeight.ExtraBold, color = NeuColors.Success)
                    }
                }
            }
        }
    }
}

@Composable
private fun FutureSelfPanel(state: BadgesUiState) {
    val level = state.currentAvatarState.level
    val max = AvatarState.entries.size
    NeuPanel(Modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).aeroSurface(AeroStyle.GreenOrb, CircleShape), contentAlignment = Alignment.Center) {
                Icon(NeuIcons.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NeuText(state.currentAvatarState.label, size = 15.sp, weight = FontWeight.Bold, maxLines = 1)
                    NeuText("$level / $max", size = 12.sp, weight = FontWeight.ExtraBold, color = NeuColors.Accent)
                }
                Row(
                    Modifier.fillMaxWidth().height(12.dp).aeroSurface(AeroStyle.Track, RoundedCornerShape(6.dp)).padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    repeat(max) { i ->
                        Box(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp))
                                .then(
                                    if (i < level) Modifier.background(Brush.verticalGradient(listOf(Color(0xFFADF87A), Color(0xFF22AF42))))
                                    else Modifier,
                                ),
                        )
                    }
                }
                NeuText(state.avatarSummary7Days, size = 11.sp, weight = FontWeight.Medium, color = NeuColors.Muted, maxLines = 1)
            }
        }
    }
}

/** Skleněná dlaždice odznaku (`.aero-app`): zelená ikona, tučný název, zelený stav. Vybraná = zelená bublina. */
@Composable
private fun BadgeCell(badge: BadgeItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    OnGlass {
        Column(
            modifier
                .aeroSurface(if (selected) AeroStyle.GreenBubble else AeroStyle.Glass, shape)
                .clip(shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NeuBadgeIcon(
                badgeShort(badge),
                size = 56.dp,
                radius = 18.dp,
                fontSize = 17.sp,
                color = if (badge.isUnlocked) NeuColors.Success else NeuColors.Muted,
            )
            NeuText(
                badge.title,
                Modifier.padding(top = 3.dp),
                size = 13.sp,
                weight = FontWeight.Bold,
                color = if (badge.isUnlocked) NeuColors.Text else NeuColors.Muted,
                align = TextAlign.Center,
                lineHeight = 16.sp,
                maxLines = 2,
            )
            Spacer(Modifier.weight(1f))
            NeuText(
                if (badge.isUnlocked) "Získáno" else "${formatNumber(badge.progress)} / ${formatNumber(badge.target)}",
                size = 12.sp,
                weight = FontWeight.Bold,
                color = if (badge.isUnlocked) NeuColors.Success else NeuColors.Muted,
                maxLines = 1,
            )
        }
    }
}

/** Zkratka odznaku do kolečka: cíl („10×“, „7d“, „1k“), jinak první písmena. */
private fun badgeShort(badge: BadgeItem): String {
    val id = badge.id.lowercase()
    return when {
        badge.target >= 1000 -> "${badge.target / 1000}k"
        "day" in id || "week" in id || "streak" in id -> "${badge.target}d"
        "echo" in id -> "OZ"
        badge.target > 1 -> "${badge.target}×"
        else -> badge.title.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
    }
}

private fun formatNumber(n: Int): String = NumberFormat.getIntegerInstance(Locale("cs", "CZ")).format(n)

@Preview(name = "Odznaky – Aero", widthDp = 390, heightDp = 900)
@Composable
private fun BadgesPreview() {
    NarsilTheme { AeroBackdrop { BadgesScreen(state = BadgesUiState()) } }
}
