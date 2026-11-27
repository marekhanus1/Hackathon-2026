package cz.narsil.movement.steps

/** Zdroj dnešních kroků (od 4:00). null = zdroj není dostupný (chybí oprávnění, aplikace, senzor). */
fun interface StepsSource {
    suspend fun stepsToday(): Int?
}

/**
 * Health Connect, záloha senzor kroků (211, SPEC 5). Bere vyšší z dostupných hodnot
 * (Health Connect bývá pozadu, senzor zná jen kroky od prvního čtení dne).
 */
class CombinedStepsSource(private vararg val sources: StepsSource) : StepsSource {
    override suspend fun stepsToday(): Int? =
        sources.mapNotNull { runCatching { it.stepsToday() }.getOrNull() }.maxOrNull()
}
