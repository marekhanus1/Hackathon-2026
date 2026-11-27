package cz.narsil.ui.screens.reward

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.ui.components.LevelBadge
import cz.narsil.ui.components.XpProgressBar
import cz.narsil.ui.theme.NarsilTheme

/**
 * Lightweight, Redmi-smooth spring celebration card for XP rewards and level-ups.
 */
@Composable
fun RewardOverlay(
    state: RewardUiState,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    animateOnStart: Boolean = true
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    var appeared by remember { mutableStateOf(!animateOnStart) }
    LaunchedEffect(Unit) {
        appeared = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "rewardScale"
    )

    val displayedXp by animateIntAsState(
        targetValue = if (appeared) state.gainedXp else 0,
        animationSpec = tween(durationMillis = 550),
        label = "xpCountUp"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                    },
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (state.leveledUp) colors.goldAccent else colors.positive
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (state.leveledUp) colors.emberContainer else colors.positiveContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$displayedXp",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (state.leveledUp) colors.onEmberContainer else colors.onPositiveContainer
                        )
                    }

                    if (state.leveledUp) {
                        Surface(
                            shape = CircleShape,
                            color = colors.goldAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Nová úroveň!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.ember,
                                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xxs)
                            )
                        }
                    }

                    Text(
                        text = state.reasonTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = state.encouragingSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    LevelBadge(
                        level = state.newLevel,
                        title = state.newLevelTitle
                    )

                    XpProgressBar(currentXp = state.totalXpAfter)

                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.positive,
                            contentColor = colors.onPositive
                        )
                    ) {
                        Text("Pokračovat")
                    }
                }
            }
        }
    }
}

@Preview(name = "Odměna – Nová úroveň (Světlý)", showBackground = true)
@Composable
private fun RewardLevelUpLightPreview() {
    NarsilTheme(darkTheme = false) {
        RewardOverlay(
            state = RewardUiState(),
            onContinue = {},
            animateOnStart = false
        )
    }
}

@Preview(
    name = "Odměna – XP (Tmavý)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RewardXpDarkPreview() {
    NarsilTheme(darkTheme = true) {
        RewardOverlay(
            state = RewardUiState(
                gainedXp = 20,
                totalXpAfter = 1880,
                previousLevel = 4,
                newLevel = 4,
                newLevelTitle = "Kovář klidu",
                reasonTitle = "20 dřepů hotovo!",
                encouragingSubtitle = "Skvělá práce. Získal jsi 10 minut i čerstvou energii."
            ),
            onContinue = {},
            animateOnStart = false
        )
    }
}
