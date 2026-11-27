package cz.narsil.movement.squat

import kotlin.math.abs
import kotlin.math.atan2

/** Bod kloubu v obraze (pixely) a jistota, že je v záběru (0–1). */
data class Joint(val x: Float, val y: Float, val likelihood: Float = 1f)

/** Kyčel, koleno a kotník jedné nohy. */
data class Leg(val hip: Joint, val knee: Joint, val ankle: Joint) {
    val likelihood: Float get() = minOf(hip.likelihood, knee.likelihood, ankle.likelihood)
}

/** Prahy pro počítání (ladí 231 v sále). */
data class SquatThresholds(
    /** Pod tímto úhlem v koleni je dřep „dole“. */
    val downAngle: Double = 100.0,
    /** Nad tímto úhlem je „nahoře“ (dřep dokončený). */
    val upAngle: Double = 160.0,
    /** Kolik snímků po sobě musí poloha trvat (tlumení zákmitů). */
    val stableFrames: Int = 2,
    /** Nejkratší doba mezi dvěma dřepy. */
    val minRepMillis: Long = 600,
    /** Pod touto jistotou se noha ignoruje. */
    val minLikelihood: Float = 0.5f,
    /** Vyhlazení úhlu (EMA): 1 = bez vyhlazení. */
    val smoothing: Double = 0.6,
)

/**
 * Čistá logika počítání dřepů (210): úhel kyčel–koleno–kotník, dole pod 100°, nahoře nad 160°.
 * Dřep = nahoře → dole → nahoře. Zákmity tlumí vyhlazení úhlu, [SquatThresholds.stableFrames]
 * a [SquatThresholds.minRepMillis]. Bez Androidu – testuje se v unit testech.
 */
class SquatRepCounter(private val thresholds: SquatThresholds = SquatThresholds()) {
    enum class Phase { UNKNOWN, UP, DOWN }

    var count: Int = 0
        private set
    var phase: Phase = Phase.UNKNOWN
        private set
    var lastAngle: Double? = null
        private set

    private var candidate: Phase = Phase.UNKNOWN
    private var candidateFrames = 0
    private var wasDown = false
    private var lastRepAt = Long.MIN_VALUE / 2

    fun reset() {
        count = 0
        phase = Phase.UNKNOWN
        lastAngle = null
        candidate = Phase.UNKNOWN
        candidateFrames = 0
        wasDown = false
        lastRepAt = Long.MIN_VALUE / 2
    }

    /** Snímek s oběma nohama (kterákoli může chybět). Vrací true, když přibyl dřep. */
    fun onFrame(left: Leg?, right: Leg?, timeMillis: Long): Boolean {
        val angle = kneeAngle(left, right) ?: return false
        return onAngle(angle, timeMillis)
    }

    /** Úhel v koleni ve stupních. Vrací true, když přibyl dřep. */
    fun onAngle(rawAngle: Double, timeMillis: Long): Boolean {
        val smoothed = lastAngle?.let { it + thresholds.smoothing * (rawAngle - it) } ?: rawAngle
        lastAngle = smoothed
        val observed = when {
            smoothed < thresholds.downAngle -> Phase.DOWN
            smoothed > thresholds.upAngle -> Phase.UP
            else -> return false // mezi prahy nic neměnit (hystereze)
        }
        if (observed == candidate) candidateFrames++ else {
            candidate = observed
            candidateFrames = 1
        }
        if (candidateFrames < thresholds.stableFrames || observed == phase) return false
        phase = observed
        if (observed == Phase.DOWN) {
            wasDown = true
            return false
        }
        // observed == UP
        if (wasDown && timeMillis - lastRepAt >= thresholds.minRepMillis) {
            wasDown = false
            lastRepAt = timeMillis
            count++
            return true
        }
        wasDown = false
        return false
    }

    /** Úhel z lépe viditelné nohy (nebo průměr, když jsou obě dobře vidět). */
    fun kneeAngle(left: Leg?, right: Leg?): Double? {
        val legs = listOfNotNull(left, right).filter { it.likelihood >= thresholds.minLikelihood }
        if (legs.isEmpty()) return null
        return legs.map { angleAt(it.hip, it.knee, it.ankle) }.average()
    }

    companion object {
        /** Úhel ABC ve vrcholu [b] ve stupních (0–180). */
        fun angleAt(a: Joint, b: Joint, c: Joint): Double {
            val deg = Math.toDegrees(
                atan2((c.y - b.y).toDouble(), (c.x - b.x).toDouble()) - atan2((a.y - b.y).toDouble(), (a.x - b.x).toDouble()),
            )
            val abs = abs(deg) % 360.0
            return if (abs > 180.0) 360.0 - abs else abs
        }
    }
}
