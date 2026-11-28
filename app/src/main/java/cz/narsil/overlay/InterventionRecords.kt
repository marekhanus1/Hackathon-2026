package cz.narsil.overlay

import cz.narsil.core.data.InterventionEntity

/** Výsledek Ozvěny → řádek v Room (121). Pocit malými písmeny bez diakritiky jako v mock datech („nuda“, „unava“…). */
object InterventionRecords {
    fun entity(r: EchoResult, atMillis: Long) = InterventionEntity(
        atMillis = atMillis,
        packageName = r.trigger.event.packageName,
        reason = r.trigger.reason.name,
        feeling = r.feeling?.name?.lowercase(),
        outcome = r.outcome.name,
        xpGained = r.xp,
    )
}
