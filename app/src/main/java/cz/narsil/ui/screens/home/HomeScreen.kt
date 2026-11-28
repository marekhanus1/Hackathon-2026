package cz.narsil.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.narsil.app.R
import cz.narsil.ui.text.CzechPlural
import cz.narsil.ui.theme.NarsilTheme

private val AeroInk = Color(0xFF073B59)
private val AeroBlue = Color(0xFF0079BE)
private val AeroGreen = Color(0xFF178C36)

/**
 * Task-first home screen. The supplied Frutiger Aero artwork is the real app background;
 * today's tasks stay readable in a single glass list and each row opens its own verification.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenContract: () -> Unit,
    onVerifyTask: (Long) -> Unit = {},
    onOpenSquat: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenMirror: () -> Unit = {},
    onOpenBadges: () -> Unit = {},
    exerciseLabel: String = "Dřepy",
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.frutiger_aero_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x55002F70),
                        0.28f to Color.Transparent,
                        0.72f to Color(0x18001428),
                        1f to Color(0x66002623),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AeroHeader(
                zoneStatus = state.zoneStatus,
                exerciseLabel = exerciseLabel,
                onOpenSquat = onOpenSquat,
                onOpenSettings = onOpenSettings,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Tvoje úkoly",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "Klepni na úkol, který chceš ověřit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
                TaskCounter(state.tasks)
            }

            TaskList(
                tasks = state.tasks,
                onVerifyTask = onVerifyTask,
                onOpenContract = onOpenContract,
            )

            AeroStats(state)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassAction("Zrcadlo", onOpenMirror, Modifier.weight(1f))
                GlassAction("Odznaky", onOpenBadges, Modifier.weight(1f))
                GlassAction("Upravit plán", onOpenContract, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AeroHeader(
    zoneStatus: ZoneStatus,
    exerciseLabel: String,
    onOpenSquat: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Narsil",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = when (zoneStatus) {
                    ZoneStatus.HOME -> "Ochrana aktivní"
                    ZoneStatus.AWAY -> "Venku · Narsil mlčí"
                    ZoneStatus.UNKNOWN -> "Zóna nezjištěna"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onOpenSquat) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = Color.White,
                )
                Text(
                    text = exerciseLabel,
                    color = Color.White,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Nastavení",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun TaskCounter(tasks: List<HomeTaskUiState>) {
    val verified = tasks.count { it.verified }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.9f),
        contentColor = AeroGreen,
    ) {
        Text(
            text = "$verified/${tasks.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TaskList(
    tasks: List<HomeTaskUiState>,
    onVerifyTask: (Long) -> Unit,
    onOpenContract: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.9f),
        contentColor = AeroInk,
        shadowElevation = 3.dp,
    ) {
        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Dnes zatím nemáš žádné úkoly.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Přidej je do večerní smlouvy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AeroInk.copy(alpha = 0.72f),
                )
                TextButton(onClick = onOpenContract) { Text("Přidat úkoly") }
            }
        } else {
            Column {
                tasks.forEachIndexed { index, task ->
                    TaskRow(
                        task = task,
                        index = index + 1,
                        onClick = { onVerifyTask(task.id) },
                    )
                    if (index < tasks.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = AeroBlue.copy(alpha = 0.16f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: HomeTaskUiState, index: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !task.verified,
        color = if (task.verified) Color(0xFFE4F7DF).copy(alpha = 0.72f) else Color.Transparent,
        contentColor = AeroInk,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (task.verified) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Ověřeno",
                    tint = AeroGreen,
                )
            } else {
                Text(
                    text = "$index.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AeroBlue,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (task.verified) "Ověřeno" else "Klepnutím otevřít ověření",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.verified) AeroGreen else AeroBlue,
                )
            }
            Text(
                text = "+${task.rewardMinutes} min",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = AeroGreen,
            )
        }
    }
}

@Composable
private fun AeroStats(state: HomeUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.82f),
        contentColor = AeroInk,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AeroMetric("${state.reclaimedMinutesToday} min", "získaný čas")
            AeroMetric(CzechPlural.days(state.streakDays), "série")
            AeroMetric("${state.stepsToday}", "kroků")
        }
    }
}

@Composable
private fun AeroMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AeroGreen,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AeroInk.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun GlassAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.82f),
        contentColor = AeroBlue,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(name = "Domů – Frutiger Aero", showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NarsilTheme(darkTheme = false) {
        HomeScreen(
            state = HomeUiState(),
            onOpenContract = {},
        )
    }
}
