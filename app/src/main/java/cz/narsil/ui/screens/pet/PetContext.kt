package cz.narsil.ui.screens.pet

import cz.narsil.core.AvatarState

/** Kontext dnešního dne předávaný sově (jen čísla a názvy úkolů, žádná data aplikací). */
data class PetContext(
    val avatar: AvatarState = AvatarState.OK,
    val streakDays: Int = 0,
    val reclaimedMinutes: Int = 0,
    val feedMinutes: Int = 0,
    val debtMinutes: Int = 0,
    val stepsToday: Int = 0,
    val plannedTasks: List<String> = emptyList(),
)
