package cz.narsil.ui.screens.reward

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.components.LevelBadge
import cz.narsil.ui.components.NeuPrimaryButton
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.OnGlass
import cz.narsil.ui.components.XpProgressBar
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.aeroSurface
import cz.narsil.ui.theme.neuInset

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

    // Aero: průhledné (pozadí kreslí AeroBackdrop okna), jemné ztmavení a skleněná karta s bílým okrajem.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x2600305A))
            .padding(spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        OnGlass {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                    }
                    .neuInset(NeuDepth.Normal, RoundedCornerShape(28.dp))
                    .padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                // Zelená (úroveň: jantarová) lesklá koule s bílým počtem XP.
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .aeroSurface(if (state.leveledUp) AeroStyle.AmberGloss else AeroStyle.GreenOrb, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+$displayedXp",
                        style = TextStyle(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            shadow = Shadow(
                                if (state.leveledUp) Color(0xAA8A4B00) else Color(0xAA08722A),
                                Offset(0f, 2f),
                                5f
                            )
                        )
                    )
                }

                if (state.leveledUp) {
                    Box(
                        Modifier
                            .aeroSurface(AeroStyle.GreenBubble, CircleShape)
                            .padding(horizontal = spacing.md, vertical = spacing.xxs)
                    ) {
                        NeuText("Nová úroveň!", size = 14.sp, weight = FontWeight.Bold, color = NeuColors.OnAccent)
                    }
                }

                NeuText(
                    state.reasonTitle,
                    size = 22.sp,
                    weight = FontWeight.Bold,
                    align = TextAlign.Center,
                    lineHeight = 27.sp
                )

                NeuText(
                    state.encouragingSubtitle,
                    size = 14.sp,
                    weight = FontWeight.Medium,
                    color = NeuColors.Muted,
                    align = TextAlign.Center,
                    lineHeight = 19.sp
                )

                LevelBadge(
                    level = state.newLevel,
                    title = state.newLevelTitle
                )

                XpProgressBar(currentXp = state.totalXpAfter)

                NeuPrimaryButton("Pokračovat", onClick = onContinue)
            }
        }
    }
}

@Preview(name = "Odměna – Nová úroveň (Světlý)", showBackground = true)
@Composable
private fun RewardLevelUpLightPreview() {
    NarsilTheme(darkTheme = false) { AeroBackdrop {
        RewardOverlay(
            state = RewardUiState(),
            onContinue = {},
            animateOnStart = false
        )
    } }
}

@Preview(
    name = "Odměna – XP (Tmavý)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RewardXpDarkPreview() {
    NarsilTheme(darkTheme = true) { AeroBackdrop {
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
    } }
}
