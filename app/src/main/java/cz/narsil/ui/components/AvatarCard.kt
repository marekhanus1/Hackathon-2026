package cz.narsil.ui.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.R
import cz.narsil.ui.theme.NarsilTheme

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
 * Maps each [AvatarState] to its vector drawable illustration in `res/drawable/`.
 */
@DrawableRes
fun AvatarState.drawableRes(): Int = when (this) {
    AvatarState.TIRED -> R.drawable.avatar_state_1_tired
    AvatarState.RESTING -> R.drawable.avatar_state_2_resting
    AvatarState.BALANCED -> R.drawable.avatar_state_3_balanced
    AvatarState.ENERGIZED -> R.drawable.avatar_state_4_energized
    AvatarState.THRIVING -> R.drawable.avatar_state_5_thriving
}

/**
 * Displays the "Budoucí já" character card with an optional slot for custom illustrations.
 */
@Composable
fun AvatarCard(
    state: AvatarState,
    summary7Days: String,
    modifier: Modifier = Modifier,
    avatarContent: (@Composable (AvatarState) -> Unit)? = null
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    val containerColor = when (state) {
        AvatarState.TIRED -> colors.echoContainer
        AvatarState.RESTING -> MaterialTheme.colorScheme.surfaceVariant
        AvatarState.BALANCED -> colors.emberContainer
        AvatarState.ENERGIZED -> colors.positiveContainer
        AvatarState.THRIVING -> colors.positiveContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                if (avatarContent != null) {
                    avatarContent(state)
                } else {
                    DefaultAvatarEmblem(state = state)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
            ) {
                Text(
                    text = "Budoucí já · ${state.label}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = state.encouragement,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = spacing.xxs)
                ) {
                    Text(
                        text = summary7Days,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultAvatarEmblem(state: AvatarState) {
    Image(
        painter = painterResource(id = state.drawableRes()),
        contentDescription = state.label,
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    )
}

@Preview(name = "AvatarCard – Světlý", showBackground = true)
@Composable
private fun AvatarCardLightPreview() {
    NarsilTheme(darkTheme = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            AvatarCard(
                state = AvatarState.ENERGIZED,
                summary7Days = "Posledních 7 dní: 5 výher · 420 dřepů"
            )
        }
    }
}

@Preview(
    name = "AvatarCard – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AvatarCardDarkPreview() {
    NarsilTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            AvatarCard(
                state = AvatarState.TIRED,
                summary7Days = "Posledních 7 dní: 2 výhry · prostor na klid"
            )
        }
    }
}
