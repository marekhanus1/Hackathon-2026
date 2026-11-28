package cz.narsil.ui.screens.home

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.text.NumberFormat
import java.util.Locale

/**
 * Domov ve stylu E (334): vejde se bez posouvání stránky. Nahoře úroveň, pak zůstatek a pohyb,
 * pod tím todo list úkolů dnešní smlouvy (posouvá se jen seznam uvnitř vany) s „Ověřit“ u čekajících.
 * Spodní menu kreslí navigace (NarsilNavHost), ne obrazovka.
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
) {
    Column(
        modifier
            .fillMaxSize()
            .background(NeuColors.Base)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HomeHeader(state, exerciseLabel, onOpenExercise)
        BalancePanel(state)
        TasksPanel(state, onVerifyTask, onPickTask, onOpenContract, Modifier.weight(1f))
    }
}

@Composable
private fun HomeHeader(state: HomeUiState, exerciseLabel: String?, onOpenExercise: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).neuInset(NeuDepth.Shallow, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            NeuText("N", size = 18.sp, weight = FontWeight.ExtraBold, color = NeuColors.Accent)
        }
        Column(Modifier.weight(1f)) {
            NeuText("Úroveň ${state.level} · ${state.levelTitle}", size = 17.sp, weight = FontWeight.Bold, maxLines = 1)
            NeuText(
                "${formatNumber(state.currentXp)} XP · série ${state.streakDays} ${dayWord(state.streakDays)}",
                size = 12.sp,
                weight = FontWeight.Medium,
                color = NeuColors.Muted,
                maxLines = 1,
            )
        }
        if (exerciseLabel != null) NeuSmallButton(exerciseLabel, onOpenExercise, height = 38.dp)
    }
}

@Composable
private fun BalancePanel(state: HomeUiState) {
    NeuPanel(radius = 28.dp, padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(116.dp).border(3.dp, NeuColors.Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NeuText("${state.availableStepMinutes}", size = 34.sp, weight = FontWeight.ExtraBold, color = NeuColors.Accent)
                    NeuText("min zůstatek", size = 12.sp, weight = FontWeight.SemiBold, color = NeuColors.Muted)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(
                            when (state.zoneStatus) {
                                ZoneStatus.HOME -> NeuColors.Success
                                ZoneStatus.AWAY -> NeuColors.Muted
                                ZoneStatus.UNKNOWN -> NeuColors.Error
                            },
                        ),
                    )
                    NeuText(state.zoneStatus.label, size = 12.sp, weight = FontWeight.SemiBold, maxLines = 1)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NeuText("${formatNumber(state.stepsToday)} kroků", size = 12.sp, weight = FontWeight.Bold)
                        NeuText("cíl ${formatNumber(state.stepGoal)}", size = 12.sp, weight = FontWeight.Medium, color = NeuColors.Muted)
                    }
                    NeuProgress(if (state.stepGoal > 0) state.stepsToday.toFloat() / state.stepGoal else 0f)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    NeuStat("${state.reclaimedMinutesToday}", "min získáno", color = NeuColors.Success)
                    NeuStat("${state.feedMinutesToday}", "min ve feedu")
                }
                state.debtLabel?.let { NeuText(it, size = 11.sp, weight = FontWeight.Bold, color = NeuColors.Error, maxLines = 2) }
            }
        }
    }
}

@Composable
private fun TasksPanel(
    state: HomeUiState,
    onVerifyTask: (Long) -> Unit,
    onPickTask: () -> Unit,
    onOpenContract: () -> Unit,
    modifier: Modifier,
) {
    NeuPanel(modifier.fillMaxWidth(), radius = 28.dp, padding = PaddingValues(16.dp), gap = 4.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = state.openTaskCount > 0, role = Role.Button, onClick = onPickTask)
                .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeuText(
                if (state.tasks.isNotEmpty()) "Dnešní smlouva" else state.contractTitle,
                Modifier.weight(1f),
                size = 16.sp,
                weight = FontWeight.ExtraBold,
                maxLines = 2,
            )
            when {
                state.contractCompletedToday -> NeuText("Splněno · +100 XP", size = 13.sp, weight = FontWeight.Bold, color = NeuColors.Success)
                state.tasks.isNotEmpty() -> NeuText(
                    "${state.doneTaskCount} / ${state.tasks.size} hotovo",
                    size = 13.sp,
                    weight = FontWeight.Bold,
                    color = NeuColors.Muted,
                )
            }
        }
        if (state.tasks.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NeuText(
                    if (state.contractSigned) "Na dnešek nemáš žádné úkoly. Přidej si je ve smlouvě – za ověřený úkol dostaneš minuty."
                    else "Večer si naplánuj zítřek: pohybový cíl, úkoly a 15s video pro Ozvěnu.",
                    size = 13.sp,
                    weight = FontWeight.Medium,
                    color = NeuColors.Muted,
                    align = TextAlign.Center,
                    lineHeight = 19.sp,
                )
                NeuSmallButton(state.contractButton, onOpenContract, height = 42.dp)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(state.tasks, key = { _, t -> t.id }) { _, task ->
                    NeuDivider()
                    HomeTaskRow(task, onVerify = { onVerifyTask(task.id) })
                }
            }
        }
        NeuDivider()
        NeuText(
            "Plánovaný obsah: ${state.plannedContentSummary}",
            Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp),
            size = 11.sp,
            weight = FontWeight.Medium,
            color = NeuColors.Muted,
            maxLines = 1,
        )
    }
}

@Composable
private fun HomeTaskRow(task: TaskItem, onVerify: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = task.canVerify, role = Role.Button, onClick = onVerify)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (task.isDone) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(NeuColors.Success), contentAlignment = Alignment.Center) {
                Icon(NeuIcons.Check, contentDescription = "Ověřeno", tint = NeuColors.OnSuccess, modifier = Modifier.size(16.dp))
            }
        } else {
            Box(Modifier.size(30.dp).neuInset(NeuDepth.Deep, CircleShape))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            NeuText(task.title, size = 14.sp, weight = FontWeight.Bold, maxLines = 1)
            NeuText(
                task.meta,
                size = 11.sp,
                weight = FontWeight.SemiBold,
                color = when {
                    task.isDone -> NeuColors.Success
                    task.isWarning -> NeuColors.Error
                    else -> NeuColors.Muted
                },
                maxLines = 1,
            )
        }
        when {
            task.canVerify -> NeuSmallButton("Ověřit", onVerify)
            task.isDone -> NeuText("+${task.rewardMinutes} min", Modifier.padding(end = 4.dp), size = 12.sp, weight = FontWeight.ExtraBold, color = NeuColors.Success)
            else -> Spacer(Modifier.height(36.dp))
        }
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

@Preview(name = "Domov – koncept E", widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    NarsilTheme {
        Column(Modifier.fillMaxSize().background(NeuColors.Base)) {
            HomeScreen(
                state = HomeUiState(contractSigned = true, tasks = previewTasks()),
                onOpenContract = {},
                exerciseLabel = "Dřepy",
                modifier = Modifier.weight(1f),
            )
            NeuTabBar(NeuTab.HOME, {}, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp))
        }
    }
}
