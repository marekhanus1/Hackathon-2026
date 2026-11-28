package cz.narsil.ui.screens.badges

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.R
import cz.narsil.ui.components.AvatarCard
import cz.narsil.ui.components.AvatarState
import cz.narsil.ui.components.NarsilProgressBar
import cz.narsil.ui.components.drawableRes
import cz.narsil.ui.theme.NarsilTheme

/**
 * Badges, 5-state "Budoucí já" evolution strip, and Party Challenge leaderboard.
 * Ready for Štěpán's illustration assets (task 420) via [avatarIllustrationSlot].
 */
@Composable
fun BadgesScreen(
    state: BadgesUiState,
    modifier: Modifier = Modifier,
    avatarIllustrationSlot: (@Composable (AvatarState) -> Unit)? = null
) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = "Odznaky a parta",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            AvatarCard(
                state = state.currentAvatarState,
                summary7Days = state.avatarSummary7Days,
                avatarContent = avatarIllustrationSlot
            )

            // 5-state evolution overview
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = "5 stavů Budoucího já",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    AvatarState.entries.forEach { avatarStage ->
                        val isCurrent = avatarStage == state.currentAvatarState
                        Surface(
                            modifier = Modifier.width(108.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isCurrent) colors.emberContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) colors.ember else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(spacing.sm),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                            ) {
                                Image(
                                    painter = painterResource(id = avatarStage.drawableRes()),
                                    contentDescription = avatarStage.label,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "${avatarStage.level}/5 · ${avatarStage.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) colors.ember else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Badges list
            Text(
                text = "Odznaky",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                state.badges.forEach { badge ->
                    BadgeCard(badge = badge)
                }
            }

            // Party challenge & leaderboard
            Text(
                text = "Výzva party",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            PartyChallengeCard(state = state)
        }
    }
}

@Composable
private fun BadgeCard(badge: BadgeItem) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val safeTarget = badge.target.coerceAtLeast(1)
    val fraction = (badge.progress.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) colors.emberContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (badge.isUnlocked) colors.goldAccent else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = badgeDrawableRes(badge.id)),
                    contentDescription = badge.title,
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(if (badge.isUnlocked) 1f else 0.45f)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (badge.isUnlocked) colors.goldAccent else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (badge.isUnlocked) "Získáno" else "${badge.progress}/${badge.target}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.isUnlocked) colors.onEmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
                    )
                }
            }
            NarsilProgressBar(
                progress = fraction,
                fillColor = if (badge.isUnlocked) colors.goldAccent else colors.positive,
                height = 6.dp
            )
        }
    }
}

@DrawableRes
private fun badgeDrawableRes(badgeId: String): Int = when (badgeId) {
    "closed_10x" -> R.drawable.ic_badge_closed_10x
    "week_no_debt" -> R.drawable.ic_badge_week_no_debt
    "1000_squats" -> R.drawable.ic_badge_1000_squats
    "echo_keeper" -> R.drawable.ic_badge_echo_keeper
    "reforged" -> R.drawable.ic_badge_reforged
    else -> R.drawable.ic_badge_reforged
}

@Composable
private fun PartyChallengeCard(state: BadgesUiState) {
    val colors = NarsilTheme.colors
    val spacing = NarsilTheme.spacing
    val safeGoal = state.partyGoalSteps.coerceAtLeast(1)
    val fraction = (state.partyCurrentSteps.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Společný cíl kroků",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${state.partyCurrentSteps} / ${state.partyGoalSteps}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.ember
                )
            }

            NarsilProgressBar(
                progress = fraction,
                fillColor = colors.ember,
                height = 10.dp
            )

            Text(
                text = "Žebříček získaného času (tento týden)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            state.leaderboard.forEach { member ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = if (member.isCurrentUser) colors.positiveContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.sm, vertical = spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${member.rank}. ${member.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (member.isCurrentUser) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "${member.reclaimedMinutes} min",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (member.isCurrentUser) colors.positive else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Odznaky a parta – Světlý", showBackground = true)
@Composable
private fun BadgesLightPreview() {
    NarsilTheme(darkTheme = false) {
        BadgesScreen(state = BadgesUiState())
    }
}

@Preview(
    name = "Odznaky a parta – Tmavý",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun BadgesDarkPreview() {
    NarsilTheme(darkTheme = true) {
        BadgesScreen(state = BadgesUiState(currentAvatarState = AvatarState.THRIVING))
    }
}
