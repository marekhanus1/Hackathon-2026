package cz.narsil.app.nav

import cz.narsil.core.PlanRules
import cz.narsil.core.PlanTask
import cz.narsil.core.TaskStatus
import cz.narsil.ui.screens.verify.TaskItem
import cz.narsil.ui.screens.verify.TaskItemStatus
import cz.narsil.ui.screens.verify.TaskType as UiTaskType

/** Úkol smlouvy z jádra → položka seznamů v UI (domov, výběr, záložky ověření; 334). */
internal fun PlanTask.toTaskItem(): TaskItem = TaskItem(
    id = id,
    title = title,
    type = UiTaskType.entries.firstOrNull { it.name == type.name } ?: UiTaskType.OTHER,
    rewardMinutes = rewardMinutes,
    status = when (status) {
        TaskStatus.VERIFIED -> TaskItemStatus.VERIFIED
        TaskStatus.VERIFY_LATER -> TaskItemStatus.VERIFY_LATER
        TaskStatus.REJECTED -> if (canRetry) TaskItemStatus.REJECTED else TaskItemStatus.EXHAUSTED
        TaskStatus.PENDING -> if (canRetry) TaskItemStatus.PENDING else TaskItemStatus.EXHAUSTED
    },
    attemptsUsed = attempts,
    maxAttempts = PlanRules.MAX_ATTEMPTS,
    goal = target,
)
