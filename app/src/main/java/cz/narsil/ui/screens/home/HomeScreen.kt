package cz.narsil.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.R
import cz.narsil.ui.components.NeuDivider
import cz.narsil.ui.components.NeuIcons
import cz.narsil.ui.components.NeuPanel
import cz.narsil.ui.components.NeuProgress
import cz.narsil.ui.components.NeuSmallButton
import cz.narsil.ui.components.NeuStat
import cz.narsil.ui.components.NeuTab
import cz.narsil.ui.components.NeuTabBar
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.screens.verify.TaskItem
import cz.narsil.ui.screens.verify.TaskItemStatus
import cz.narsil.ui.screens.verify.TaskType
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.neuInset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import cz.narsil.ui.components.AeroOrb
import cz.narsil.ui.components.NeuIconButton
import cz.narsil.ui.components.OnSky
import cz.narsil.ui.theme.AeroBackdrop
import cz.narsil.ui.theme.AeroStyle
import cz.narsil.ui.theme.aeroSurface
import java.text.NumberFormat
import java.util.Locale

/**
 * Domov ve stylu Frutiger Aero (narsil-frontend-v2): značka a stav ochrany, tlačítka cviku a nastavení,
 * nadpis „Tvoje úkoly“ se zelenou koulí hotových, zůstatek a pohyb ve skle, úkoly dnešní smlouvy
 * (posouvá se jen seznam) a dole přehled dne. Dok kreslí navigace (NarsilNavHost).
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenContract: () -> Unit,
    onOpenMirror: () -> Unit = {},
    onOpenBadges: () -> Unit = {},
    modifier: Modifier = Modifier,
    /** „Ověřit“ u konkrétního úkolu → ověření rovnou pro něj. */
    onVerifyTask: (Long) -> Unit = {},
    /** Obecný vstup do ověření → výběr úkolu. */
    onPickTask: () -> Unit = {},
    /** Zkratka cviku vpravo v hlavičce (dřepy / kliky); null = skrýt. */
    exerciseLabel: String? = null,
    onOpenExercise: () -> Unit = {},
    /** Ozubené kolo v hlavičce; null = skrýt. */
    onOpenSettings: (() -> Unit)? = null,
) {
    OnSky {
        Column(
            modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeHeader(state, exerciseLabel, onOpenExercise, onOpenSettings)
            BalancePanel(state)
            TitleRow(state, onPickTask)
            TasksPanel(state, onVerifyTask, onOpenContract, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HomeHeader(state: HomeUiState, exerciseLabel: String?, onOpenExercise: () -> Unit, onOpenSettings: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            NeuText("Narsil", size = 30.sp, weight = FontWeight.Bold, letterSpacing = (-1).sp, lineHeight = 32.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val dot = when (state.zoneStatus) {
                    ZoneStatus.HOME -> NeuColors.OnSkySuccess
                    ZoneStatus.AWAY -> NeuColors.Warning
                    ZoneStatus.UNKNOWN -> NeuColors.OnSkyError
                }
                Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                NeuText(state.zoneStatus.label, size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted, maxLines = 1)
            }
        }
        if (exerciseLabel != null) {
            val shape = RoundedCornerShape(15.dp)
            Row(
                Modifier
                    .height(46.dp)
                    .aeroSurface(AeroStyle.BlueGlass, shape)
                    .clip(shape)
                    .clickable(role = Role.Button, onClick = onOpenExercise)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(NeuIcons.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                NeuText(exerciseLabel, size = 15.sp, weight = FontWeight.Bold, maxLines = 1)
            }
        }
        if (onOpenSettings != null) NeuIconButton(NeuIcons.Settings, "Nastavení", onOpenSettings)
    }
}

@Composable
private fun BalancePanel(state: HomeUiState) {
    NeuPanel(radius = 22.dp, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(96.dp).aeroSurface(AeroStyle.GreenOrb, CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NeuText("${state.availableStepMinutes}", size = 30.sp, weight = FontWeight.ExtraBold, color = Color(0xFF075821), lineHeight = 32.sp)
                    NeuText("min zůstatek", size = 10.sp, weight = FontWeight.Bold, color = Color(0xFF075821))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                NeuText("Úroveň ${state.level} · ${state.levelTitle}", size = 15.sp, weight = FontWeight.Bold, maxLines = 1)
                NeuText(
                    "${formatNumber(state.currentXp)} XP · série ${state.streakDays} ${dayWord(state.streakDays)}",
                    size = 12.sp,
                    weight = FontWeight.Medium,
                    color = NeuColors.Muted,
                    maxLines = 1,
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NeuText("${formatNumber(state.stepsToday)} kroků", size = 12.sp, weight = FontWeight.Bold, color = NeuColors.Accent)
                        NeuText("cíl ${formatNumber(state.stepGoal)}", size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted)
                    }
                    NeuProgress(if (state.stepGoal > 0) state.stepsToday.toFloat() / state.stepGoal else 0f)
                }
                state.debtLabel?.let { NeuText(it, size = 11.sp, weight = FontWeight.Bold, color = NeuColors.Error, maxLines = 2) }
            }
        }
        NeuDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HomeStat("${state.reclaimedMinutesToday} min", "získaný čas", NeuColors.Success)
            HomeStat("${state.feedMinutesToday} min", "ve feedu", NeuColors.Accent)
            HomeStat("${state.streakDays} ${dayWord(state.streakDays)}", "série", NeuColors.Success)
        }
    }
}

@Composable
private fun HomeStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NeuText(value, size = 16.sp, weight = FontWeight.Bold, color = color, maxLines = 1)
        NeuText(label, size = 11.sp, weight = FontWeight.Medium, color = NeuColors.Muted, maxLines = 1)
    }
}

@Composable
private fun TitleRow(state: HomeUiState, onPickTask: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = state.openTaskCount > 0, role = Role.Button, onClick = onPickTask),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            NeuText(
                if (state.tasks.isNotEmpty()) "Tvoje úkoly" else "Dnešní smlouva",
                size = 26.sp,
                weight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                maxLines = 1,
            )
            NeuText(
                when {
                    state.contractCompletedToday -> "Smlouva splněna · +100 XP"
                    state.tasks.isNotEmpty() -> "Klepni na úkol, který chceš ověřit."
                    else -> state.contractTitle
                },
                size = 13.sp,
                weight = FontWeight.Medium,
                color = NeuColors.Muted,
                maxLines = 2,
            )
        }
        if (state.tasks.isNotEmpty()) AeroOrb("${state.doneTaskCount}/${state.tasks.size}")
    }
}

@Composable
private fun TasksPanel(
    state: HomeUiState,
    onVerifyTask: (Long) -> Unit,
    onOpenContract: () -> Unit,
    modifier: Modifier,
) {
    NeuPanel(modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(horizontal = 4.dp, vertical = 4.dp), gap = 0.dp) {
        if (state.tasks.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NeuText(
                    if (state.contractSigned) "Na dnešek nemáš žádné úkoly. Přidej si je ve smlouvě – za ověřený úkol dostaneš minuty."
                    else "Večer si naplánuj zítřek: pohybový cíl, úkoly a 15s video pro Ozvěnu.",
                    size = 14.sp,
                    weight = FontWeight.Medium,
                    color = NeuColors.Muted,
                    align = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                NeuSmallButton(state.contractButton, onOpenContract, height = 44.dp)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(state.tasks, key = { _, t -> t.id }) { index, task ->
                    if (index > 0) NeuDivider()
                    HomeTaskRow(index, task, onVerify = { onVerifyTask(task.id) })
                }
            }
        }
        NeuDivider()
        NeuText(
            "Plánovaný obsah: ${state.plannedContentSummary}",
            Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            size = 11.sp,
            weight = FontWeight.Medium,
            color = NeuColors.Muted,
            maxLines = 1,
        )
    }
}

@Composable
private fun HomeTaskRow(index: Int, task: TaskItem, onVerify: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (task.isDone) Modifier.background(Brush.horizontalGradient(listOf(Color(0xB8C7F6B3), Color(0x99E6FFF7))))
                else Modifier,
            )
            .clickable(enabled = task.canVerify, role = Role.Button, onClick = onVerify)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
            if (task.isDone) {
                Icon(NeuIcons.Check, contentDescription = "Ověřeno", tint = NeuColors.Success, modifier = Modifier.size(20.dp))
            } else {
                NeuText("${index + 1}.", size = 17.sp, weight = FontWeight.Bold, color = NeuColors.Accent)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            NeuText(task.title, size = 15.sp, weight = FontWeight.Bold, maxLines = 2, lineHeight = 19.sp)
            NeuText(
                if (task.canVerify && !task.isWarning) "${task.type.label} · klepnutím otevřít ověření" else task.meta,
                size = 12.sp,
                weight = FontWeight.Medium,
                color = when {
                    task.isDone -> NeuColors.Success
                    task.isWarning -> NeuColors.Error
                    else -> NeuColors.Accent
                },
                maxLines = 1,
            )
        }
        NeuText(
            "+${task.rewardMinutes} min",
            size = 13.sp,
            weight = FontWeight.Bold,
            color = if (task.isDone || task.canVerify) NeuColors.Success else NeuColors.Muted,
            maxLines = 1,
        )
    }
}

private fun formatNumber(n: Int): String = NumberFormat.getIntegerInstance(Locale("cs", "CZ")).format(n)

private fun dayWord(n: Int): String = when (n) {
    1 -> "den"
    in 2..4 -> "dny"
    else -> "dní"
}

internal fun previewTasks(): List<TaskItem> = listOf(
    TaskItem(1, "Běh 5 km", TaskType.MOVEMENT, status = TaskItemStatus.VERIFIED, attemptsUsed = 1),
    TaskItem(2, "Laboratorní úloha z databází", TaskType.SCHOOL),
    TaskItem(3, "Dopsat report do práce", TaskType.WORK, status = TaskItemStatus.REJECTED, attemptsUsed = 1),
    TaskItem(4, "Uklidit pokoj", TaskType.OTHER),
)

@Preview(name = "Domov – Frutiger Aero", widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    NarsilTheme {
        AeroBackdrop { Column(Modifier.fillMaxSize()) {
            HomeScreen(
                state = HomeUiState(contractSigned = true, tasks = previewTasks()),
                onOpenContract = {},
                exerciseLabel = "Dřepy",
                modifier = Modifier.weight(1f),
            )
            NeuTabBar(NeuTab.HOME, {}, Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp))
        } }
    }
}
