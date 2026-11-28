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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.aeroSurface

/**
 * Záložky doku podle narsil-frontend-v2: Úkoly · Zrcadlo · Odznaky · Plán.
 * Nastavení je ozubené kolo v hlavičce domova, plán (smlouva) se otevírá přes celou obrazovku.
 */
enum class NeuTab(val label: String) {
    HOME("Úkoly"),
    MIRROR("Zrcadlo"),
    BADGES("Odznaky"),
    CONTRACT("Plán");

    val icon: ImageVector
        get() = when (this) {
            HOME -> NeuIcons.Check
            MIRROR -> NeuIcons.Mirror
            BADGES -> NeuIcons.Star
            CONTRACT -> NeuIcons.Pen
        }
}

private val DockText = Color(0xFF07596A)

/** Spodní dok (`.dock`): zelené sklo, aktivní záložka je bílo-zelená bublina. */
@Composable
fun NeuTabBar(selected: NeuTab?, onSelect: (NeuTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().aeroSurface(AeroStyle.Dock, RoundedCornerShape(24.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NeuTab.entries.forEach { tab ->
            val active = tab == selected
            val shape = RoundedCornerShape(17.dp)
            Column(
                Modifier
                    .weight(1f)
                    .height(58.dp)
                    .then(if (active) Modifier.aeroSurface(AeroStyle.GreenBubble, shape) else Modifier)
                    .clip(shape)
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .semantics { this.selected = active },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            ) {
                Icon(tab.icon, contentDescription = null, tint = DockText, modifier = Modifier.size(22.dp))
                OnGlass { NeuText(tab.label, size = 11.sp, weight = FontWeight.Bold, color = DockText, maxLines = 1) }
            }
        }
    }
}
