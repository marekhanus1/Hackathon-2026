package cz.narsil.overlay

import cz.narsil.core.InterventionTrigger
import cz.narsil.ui.screens.echo.EchoFeeling

/**
 * Rozdělaná Ozvěna mimo instanci služby přístupnosti (141). Okno (TYPE_ACCESSIBILITY_OVERLAY) zmizí se
 * starou instancí služby; nový [EchoOverlayController] podle tohohle stavu okno obnoví. Žije v aplikačním
 * objektu (`EchoOverlayStarter`), přežije tedy výměnu služby, ne pád procesu. Čistý Kotlin kvůli testům.
 *
 * Pojistka: zásah starší než [maxAgeMillis] se neobnovuje (uživatel mezitím dělal něco jiného);
 * volající pak detekci řekne, že okno zmizelo bez volby, aby mohla zasáhnout znovu.
 */
class EchoSessionStore(private val maxAgeMillis: Long = MAX_AGE_MILLIS) {
    /** Na které obrazovce byla Ozvěna. Odměna se neobnovuje: volba už je zpracovaná. */
    enum class Phase { ECHO, EXERCISE }

    data class Pending(val trigger: InterventionTrigger, val shownAtMillis: Long, val feeling: EchoFeeling?, val phase: Phase)

    sealed interface Restore {
        data class Show(val pending: Pending) : Restore
        data class Expired(val pending: Pending) : Restore
    }

    @Volatile
    var pending: Pending? = null
        private set

    /** Nová Ozvěna na obrazovce; když už jde o stejný zásah (obnova), zachová čas a pocit. */
    fun begin(trigger: InterventionTrigger, nowMillis: Long) {
        val p = pending
        if (p != null && p.trigger == trigger) return
        pending = Pending(trigger, nowMillis, null, Phase.ECHO)
    }

    fun setFeeling(feeling: EchoFeeling?) {
        pending = pending?.copy(feeling = feeling)
    }

    fun setPhase(phase: Phase) {
        pending = pending?.copy(phase = phase)
    }

    /** Volba padla (nebo okno zavřel uživatel): nic k obnově. */
    fun clear() {
        pending = null
    }

    /** Při startu nového ovladače: co obnovit. Prošlý zásah se smaže. null = nic. */
    fun takeForRestore(nowMillis: Long): Restore? {
        val p = pending ?: return null
        if (nowMillis - p.shownAtMillis > maxAgeMillis) {
            pending = null
            return Restore.Expired(p)
        }
        return Restore.Show(p)
    }

    companion object {
        /** Déle rozdělanou Ozvěnu neobnovovat (služba se po přeinstalaci připojí do pár sekund). */
        const val MAX_AGE_MILLIS = 10 * 60_000L
    }
}
