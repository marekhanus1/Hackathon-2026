package cz.narsil.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Five states of the "Budoucí já" avatar reflecting the last 7 days.
 */
enum class AvatarState(
    val level: Int,
    val label: String,
    val encouragement: String
) {
    TIRED(
        level = 1,
        label = "Unavené já",
        encouragement = "Krátký oddech pomůže nabrat síly. Každý nový večer je čistý štít."
    ),
    RESTING(
        level = 2,
        label = "Klidné já",
        encouragement = "Krok za krokem hledáš svůj rytmus. Každé vědomé zavření se počítá."
    ),
    BALANCED(
        level = 3,
        label = "Vyrovnané já",
        encouragement = "Máš dobrý balanc mezi odpočinkem a časem pro sebe."
    ),
    ENERGIZED(
        level = 4,
        label = "Soustředěné já",
        encouragement = "Pevné tempo a čistá hlava. Bereš si pozornost zpátky."
    ),
    THRIVING(
        level = 5,
        label = "V plné kondici",
        encouragement = "Znovu ukovaná síla. Držíš vlastní směr s lehkostí."
    )
}

/**
 * Karta „Budoucí já“ ve skle: zelená bublina s ilustrací (slot pro Štěpánovy obrázky) nebo koulí s úrovní,
 * tmavě modrý text a souhrn 7 dní v matné pilulce.
 */
@Composable
fun AvatarCard(
    state: AvatarState,
    summary7Days: String,
    modifier: Modifier = Modifier,
    avatarContent: (@Composable (AvatarState) -> Unit)? = null
) {
    NeuPanel(modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .aeroSurface(AeroStyle.GreenBubble, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (avatarContent != null) {
                    avatarContent(state)
                } else {
                    AeroOrb("${state.level}/5", size = 40.dp, fontSize = 12.sp)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NeuText("Budoucí já · ${state.label}", size = 15.sp, weight = FontWeight.Bold)
                NeuText(state.encouragement, size = 13.sp, weight = FontWeight.Medium, color = NeuColors.Muted, lineHeight = 18.sp)
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .aeroSurface(AeroStyle.Frost, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    NeuText(summary7Days, size = 11.sp, weight = FontWeight.Bold, color = NeuColors.Label)
                }
            }
        }
    }
}

@Preview(name = "AvatarCard – Aero", widthDp = 390, heightDp = 200)
@Composable
private fun AvatarCardPreview() {
    NarsilTheme {
        AeroBackdrop {
            AvatarCard(
                state = AvatarState.ENERGIZED,
                summary7Days = "Posledních 7 dní: 5 výher · 420 dřepů",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
