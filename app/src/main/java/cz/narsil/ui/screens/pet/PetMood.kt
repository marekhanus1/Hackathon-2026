package cz.narsil.ui.screens.pet

import cz.narsil.core.AvatarState

/** Nálady sovy Narsilky odvozené z [AvatarState] a nočního času. */
enum class PetMood {
    SPI,
    OSPALA,
    KLIDNA,
    SPOKOJENA,
    HRDA,
}

/** Určí náladu sovy podle stavu postavy a místní hodiny (23–5 → [PetMood.SPI]). */
fun petMoodFor(avatar: AvatarState, hour: Int): PetMood {
    val normalizedHour = ((hour % 24) + 24) % 24
    if (normalizedHour >= 23 || normalizedHour <= 5) return PetMood.SPI
    return when (avatar) {
        AvatarState.EXHAUSTED, AvatarState.TIRED -> PetMood.OSPALA
        AvatarState.OK -> PetMood.KLIDNA
        AvatarState.FIT -> PetMood.SPOKOJENA
        AvatarState.THRIVING -> PetMood.HRDA
    }
}
