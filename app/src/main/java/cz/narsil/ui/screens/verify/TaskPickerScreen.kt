package cz.narsil.ui.screens.verify

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.narsil.ui.components.NeuBadgeIcon
import cz.narsil.ui.components.NeuHeader
import cz.narsil.ui.components.NeuIcons
import cz.narsil.ui.components.NeuLabel
import cz.narsil.ui.components.NeuPanel
import cz.narsil.ui.components.NeuPrimaryButton
import cz.narsil.ui.components.NeuScreen
import cz.narsil.ui.components.NeuText
import cz.narsil.ui.components.OnGlass
import androidx.compose.ui.graphics.Color
import cz.narsil.ui.theme.NarsilTheme
import cz.narsil.ui.theme.NeuColors
import cz.narsil.ui.theme.NeuDepth
import cz.narsil.ui.theme.neuInset

/**
 * Výběr úkolu k ověření (334): obecný vstup do ověření. Čekající úkoly nahoře, hotové pod nimi;
 * „Pokračovat k ověření“ otevře ověření vybraného úkolu.
 */
@Composable
fun TaskPickerScreen(
    tasks: List<TaskItem>,
    onBack: () -> Unit,
    onContinue: (Long) -> Unit,
    onOpenContract: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = tasks.filter { it.canVerify }
    val done = tasks.filter { !it.canVerify }
    var picked by rememberSaveable { mutableStateOf<Long?>(null) }
    val pickedTask = open.firstOrNull { it.id == picked } ?: open.firstOrNull()

    NeuScreen(modifier, gap = 16.dp) {
        NeuHeader(
            title = "Který úkol ověříš?",
            subtitle = if (open.isEmpty()) "Ze smlouvy na dnes" else "Ze smlouvy na dnes · ${waiting(open.size)}",
            onBack = onBack,
        )

        if (open.isEmpty()) {
            NeuPanel(Modifier.fillMaxWidth(), radius = 28.dp, padding = PaddingValues(20.dp), gap = 12.dp) {
                NeuText(
                    if (tasks.isEmpty()) "Dnes nemáš žádný úkol k ověření." else "Všechny dnešní úkoly jsou vyřízené.",
                    size = 15.sp,
                    weight = FontWeight.ExtraBold,
                )
                NeuText(
                    "Úkoly si naplánuješ ve večerní smlouvě.",
                    size = 13.sp,
                    weight = FontWeight.Medium,
                    color = NeuColors.Muted,
                )
            }
        } else {
            NeuPanel(
                Modifier.fillMaxWidth().weight(1f, fill = false),
                radius = 28.dp,
                padding = PaddingValues(10.dp),
                gap = 6.dp,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    open.forEach { task ->
                        PickerRow(task, selected = task.id == pickedTask?.id, onClick = { picked = task.id })
                    }
                }
            }
        }

        if (done.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NeuLabel("Hotovo", Modifier.padding(horizontal = 4.dp))
                done.take(3).forEach { task ->
                  OnGlass {
                    Row(
                        Modifier.fillMaxWidth().neuInset(NeuDepth.Normal, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (task.isDone) {
                            Icon(NeuIcons.Check, contentDescription = null, tint = NeuColors.Success, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(NeuIcons.Close, contentDescription = null, tint = NeuColors.Error, modifier = Modifier.size(18.dp))
                        }
                        NeuText(task.title, Modifier.weight(1f), size = 14.sp, weight = FontWeight.Bold, color = NeuColors.Muted, maxLines = 1)
                        NeuText(
                            when (task.status) {
                                TaskItemStatus.VERIFIED -> "+${task.rewardMinutes} min"
                                TaskItemStatus.VERIFY_LATER -> "čeká na síť"
                                else -> "bez pokusů"
                            },
                            size = 12.sp,
                            weight = FontWeight.Bold,
                            color = if (task.isDone) NeuColors.Success else NeuColors.Muted,
                        )
                    }
                  }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (pickedTask != null) {
            NeuText(
                "Vybraný úkol: ${pickedTask.title}. Důkaz zvolíš na dalším kroku.",
                Modifier.padding(horizontal = 4.dp),
                size = 12.sp,
                weight = FontWeight.Medium,
                color = NeuColors.Muted,
                lineHeight = 17.sp,
            )
            NeuPrimaryButton("Pokračovat k ověření", onClick = { onContinue(pickedTask.id) })
        } else {
            NeuPrimaryButton("Otevřít smlouvu", onClick = onOpenContract)
        }
    }
}

@Composable
private fun PickerRow(task: TaskItem, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .neuInset(if (selected) NeuDepth.Deep else NeuDepth.Flat, shape)
            .then(if (selected) Modifier.border(1.5.dp, Color(0xE6FFFFFF), shape) else Modifier)
            .clip(shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NeuBadgeIcon(task.short, size = 44.dp, radius = 14.dp, color = if (selected) NeuColors.Accent else NeuColors.Muted, fontSize = 12.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            NeuText(task.title, size = 15.sp, weight = FontWeight.ExtraBold, maxLines = 2)
            NeuText(
                if (task.status == TaskItemStatus.REJECTED) "Zamítnuto ${task.attemptsUsed}× · ${attemptsLeftText(task.attemptsLeft)}"
                else "${task.type.label} · fotka nebo popis",
                size = 12.sp,
                weight = FontWeight.SemiBold,
                color = if (task.isWarning) NeuColors.Error else NeuColors.Muted,
            )
        }
        NeuText("+${task.rewardMinutes} min", size = 13.sp, weight = FontWeight.ExtraBold, color = NeuColors.Success, align = TextAlign.End)
    }
}

private fun waiting(n: Int): String = when (n) {
    1 -> "1 čeká"
    in 2..4 -> "$n čekají"
    else -> "$n čeká"
}

@Preview(name = "Výběr úkolu – koncept E", widthDp = 390, heightDp = 844)
@Composable
private fun TaskPickerPreview() {
    NarsilTheme {
        TaskPickerScreen(
            tasks = listOf(
                TaskItem(1, "Běh 5 km", TaskType.MOVEMENT, status = TaskItemStatus.VERIFIED, attemptsUsed = 1),
                TaskItem(2, "Laboratorní úloha z databází", TaskType.SCHOOL),
                TaskItem(3, "Dopsat report do práce", TaskType.WORK, status = TaskItemStatus.REJECTED, attemptsUsed = 1),
                TaskItem(4, "Uklidit pokoj", TaskType.OTHER),
            ),
            onBack = {},
            onContinue = {},
            onOpenContract = {},
        )
    }
}
