package cz.narsil.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.neuInset

/** Záložky spodního menu (334). */
enum class NeuTab(val label: String) {
    HOME("Domů"),
    CONTRACT("Smlouva"),
    BADGES("Odznaky"),
    SETTINGS("Nastavení");

    val icon: ImageVector
        get() = when (this) {
            HOME -> NeuIcons.Home
            CONTRACT -> NeuIcons.Contract
            BADGES -> NeuIcons.Badge
            SETTINGS -> NeuIcons.Settings
        }
}

/** Spodní menu: jedna vnořená lišta, aktivní záložka zamáčknutá hlouběji s jantarovým textem. */
@Composable
fun NeuTabBar(selected: NeuTab?, onSelect: (NeuTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().neuInset(NeuDepth.Normal, RoundedCornerShape(24.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NeuTab.entries.forEach { tab ->
            val active = tab == selected
            val shape = RoundedCornerShape(18.dp)
            val color = if (active) NeuColors.Accent else NeuColors.Muted
            Column(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .neuInset(if (active) NeuDepth.Deep else NeuDepth.Flat, shape)
                    .clip(shape)
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .semantics { this.selected = active },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                NeuText(
                    tab.label,
                    size = 12.sp,
                    weight = if (active) FontWeight.ExtraBold else FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                )
            }
        }
    }
}
