package cz.narsil.gamification.debug

import cz.narsil.core.AvatarState
import cz.narsil.core.GamificationEngine
import cz.narsil.core.fake.FakeGamificationEngine
import cz.narsil.gamification.RulesGamificationEngine

/**
 * Ladící akce gamifikace pro demo panel (222 → 123). Fungují nad fake i skutečnou implementací;
 * vrací false, když daná implementace akci neumí.
 */
object GamificationDebug {
    /** Demo „+500 XP“ (skutečný engine zapíše událost DEMO). */
    suspend fun addXp(engine: GamificationEngine, xp: Int = 500): Boolean = when (engine) {
        is RulesGamificationEngine -> {
            engine.addXp(xp)
            true
        }
        is FakeGamificationEngine -> {
            engine.addXp(xp)
            true
        }
        else -> false
    }

    /** Vnutí stav postavy; null = zpět podle pravidel (fake null ignoruje). */
    suspend fun setAvatar(engine: GamificationEngine, avatar: AvatarState?): Boolean = when (engine) {
        is RulesGamificationEngine -> {
            engine.setAvatar(avatar)
            true
        }
        is FakeGamificationEngine -> {
            if (avatar != null) engine.setAvatar(avatar)
            avatar != null
        }
        else -> false
    }

    /** Přepne postavu na další stav (EXHAUSTED → … → THRIVING → EXHAUSTED). Vrací nový stav. */
    suspend fun nextAvatar(engine: GamificationEngine): AvatarState {
        val states = AvatarState.entries
        val next = states[(engine.state.value.avatar.ordinal + 1) % states.size]
        setAvatar(engine, next)
        return next
    }
}
