package cz.narsil.ui.screens.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.components.AvatarState
import cz.narsil.ui.components.NeuDivider
import cz.narsil.ui.components.NeuHeader
import cz.narsil.ui.components.NeuIcons
import cz.narsil.ui.components.NeuLabel
import cz.narsil.ui.components.NeuPanel
import cz.narsil.ui.components.NeuProgress
import cz.narsil.ui.components.NeuScreen
import cz.narsil.ui.components.NeuSmallButton
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.neuInset
import java.text.NumberFormat
import java.util.Locale

/**
 * Odznaky a parta ve stylu E (334, dříve 323): budoucí já, mřížka odznaků s detailem vybraného,
 * společný cíl party a žebříček získaného času. Posouvá se jen žebříček uvnitř vany.
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

    NeuScreen(modifier, gap = 14.dp) {
        NeuHeader(
            title = "Odznaky a parta",
            trailing = onOpenMirror?.let { open -> { NeuSmallButton("Zrcadlo", open, height = 38.dp) } },
        )

        FutureSelfPanel(state)

        Box(
            Modifier.fillMaxWidth().neuInset(NeuDepth.Normal, RoundedCornerShape(28.dp)).padding(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.badges.take(6).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { badge ->
                            BadgeCell(badge, selected = badge.id == picked?.id, onClick = { pickedId = badge.id }, modifier = Modifier.weight(1f))
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }

        if (picked != null) {
            Box(Modifier.fillMaxWidth().neuInset(NeuDepth.Shallow, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
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

        NeuPanel(Modifier.weight(1f).fillMaxWidth(), radius = 28.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), gap = 8.dp) {
            NeuText("Výzva party · společný cíl kroků", size = 14.sp, weight = FontWeight.ExtraBold, maxLines = 1)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NeuProgress(
                    if (state.partyGoalSteps > 0) state.partyCurrentSteps.toFloat() / state.partyGoalSteps else 0f,
                    color = NeuColors.Success,
                )
                NeuText(
                    "${formatNumber(state.partyCurrentSteps)} / ${formatNumber(state.partyGoalSteps)} kroků",
                    size = 11.sp,
                    weight = FontWeight.Bold,
                    color = NeuColors.Muted,
                )
            }
            NeuLabel("Získaný čas tento týden", Modifier.padding(top = 4.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(state.leaderboard, key = { "${it.rank}-${it.name}" }) { p ->
                    NeuDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        NeuText("${p.rank}.", Modifier.width(22.dp), size = 12.sp, weight = FontWeight.ExtraBold, color = NeuColors.Muted)
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
    NeuPanel(Modifier.fillMaxWidth(), radius = 24.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).neuInset(NeuDepth.Deep, CircleShape), contentAlignment = Alignment.Center) {
                Icon(NeuIcons.Person, contentDescription = null, tint = NeuColors.Accent, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NeuText(state.currentAvatarState.label, size = 15.sp, weight = FontWeight.ExtraBold, maxLines = 1)
                    NeuText("$level / $max", size = 12.sp, weight = FontWeight.ExtraBold, color = NeuColors.Accent)
                }
                Row(
                    Modifier.fillMaxWidth().height(10.dp).neuInset(NeuDepth.Deep, RoundedCornerShape(5.dp)).padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    repeat(max) { i ->
                        Box(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                                .then(if (i < level) Modifier.background(NeuColors.Accent) else Modifier),
                        )
                    }
                }
                NeuText(state.avatarSummary7Days, size = 11.sp, weight = FontWeight.Medium, color = NeuColors.Muted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BadgeCell(badge: BadgeItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .neuInset(if (selected) NeuDepth.Deep else NeuDepth.Flat, shape)
            .then(if (selected) Modifier.border(1.dp, NeuColors.Accent, shape) else Modifier)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .neuInset(NeuDepth.Deep, CircleShape)
                .then(if (badge.isUnlocked) Modifier.border(2.dp, NeuColors.Accent, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            NeuText(badgeShort(badge), size = 12.sp, weight = FontWeight.ExtraBold, color = if (badge.isUnlocked) NeuColors.Accent else NeuColors.Muted)
        }
        NeuText(
            badge.title,
            size = 11.sp,
            weight = FontWeight.Bold,
            color = if (badge.isUnlocked) NeuColors.Text else NeuColors.Muted,
            align = TextAlign.Center,
            lineHeight = 13.sp,
            maxLines = 2,
        )
        NeuText(
            if (badge.isUnlocked) "Získáno" else "${formatNumber(badge.progress)} / ${formatNumber(badge.target)}",
            size = 10.sp,
            weight = FontWeight.Bold,
            color = if (badge.isUnlocked) NeuColors.Success else NeuColors.Muted,
            maxLines = 1,
        )
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

@Preview(name = "Odznaky – koncept E", widthDp = 390, heightDp = 760)
@Composable
private fun BadgesPreview() {
    NarsilTheme { BadgesScreen(state = BadgesUiState(), onOpenMirror = {}) }
}
