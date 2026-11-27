package cz.narsil.movement.squat

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** Bod kloubu v obraze (pixely, y roste dolů) a jistota, že je v záběru (0–1). */
data class Joint(val x: Float, val y: Float, val likelihood: Float = 1f)

/** Kyčel, koleno a kotník jedné nohy. */
data class Leg(val hip: Joint, val knee: Joint, val ankle: Joint) {
    val likelihood: Float get() = minOf(hip.likelihood, knee.likelihood, ankle.likelihood)
}

/** Klouby jednoho snímku; kterýkoli může chybět (mimo záběr). */
data class PoseFrame(
    val leftShoulder: Joint? = null,
    val rightShoulder: Joint? = null,
    val leftHip: Joint? = null,
    val rightHip: Joint? = null,
    val leftKnee: Joint? = null,
    val rightKnee: Joint? = null,
    val leftAnkle: Joint? = null,
    val rightAnkle: Joint? = null,
)

/**
 * Prahy pro počítání (231: tolerantní pro sál – světlo, vzdálenost, úhel kamery).
 * Hloubka dřepu je 0 = stojí rovně, 1 = hluboký dřep (viz [SquatRepCounter.depthOf]).
 */
data class SquatThresholds(
    /** Úhel v koleni (°), který odpovídá stoji (hloubka 0). */
    val standAngle: Double = 170.0,
    /** Úhel v koleni (°), který odpovídá hlubokému dřepu (hloubka 1). */
    val deepAngle: Double = 90.0,
    /** Od této hloubky je dřep „dole“ (0,6 ≈ úhel 122°; SPEC původně 100° – tolerance pro sál). */
    val downDepth: Double = 0.6,
    /** Pod touto hloubkou je „nahoře“ (0,25 ≈ úhel 150°). */
    val upDepth: Double = 0.25,
    /** Kolik snímků po sobě musí poloha trvat (tlumení zákmitů). */
    val stableFrames: Int = 2,
    /** Nejkratší doba mezi dvěma dřepy. */
    val minRepMillis: Long = 600,
    /** Pod touto jistotou se kloub ignoruje (v šeru ML Kit hlásí nižší jistotu). */
    val minLikelihood: Float = 0.4f,
    /** Vyhlazení hloubky (EMA): 1 = bez vyhlazení. */
    val smoothing: Double = 0.6,
    /** Poměr stehno/trup ve stoji se kalibruje z prvních snímků; tolik snímků stačí ke kalibraci. */
    val calibrationFrames: Int = 5,
    /** Kolikrát se musí stehno (svisle) zkrátit proti stoji pro hloubku 1 (0 = stehno vodorovně). */
    val deepThighRatio: Double = 0.25,
) {
    companion object {
        /** Původní přísné prahy ze zadání 210 (dole < 100°, nahoře > 160°). */
        val STRICT = SquatThresholds(downDepth = 0.875, upDepth = 0.125)
    }
}

/**
 * Čistá logika počítání dřepů (210, 231). Bez Androidu – testuje se v unit testech.
 *
 * Signál = **hloubka dřepu** 0–1 ze dvou zdrojů, bere se vyšší:
 * 1. úhel kyčel–koleno–kotník (dobře funguje z boku),
 * 2. svislá délka stehna (kyčel → koleno) vůči trupu (rameno → kyčel), kalibrovaná ve stoji –
 *    funguje i **čelem ke kameře**, kde se 2D úhel v koleni skoro nemění, a i bez kotníků v záběru.
 *
 * Dřep = nahoře → dole → nahoře. Zákmity tlumí vyhlazení, [SquatThresholds.stableFrames]
 * a [SquatThresholds.minRepMillis]; mezi prahy se stav nemění (hystereze).
 */
class SquatRepCounter(private val thresholds: SquatThresholds = SquatThresholds()) {
    enum class Phase { UNKNOWN, UP, DOWN }

    var count: Int = 0
        private set
    var phase: Phase = Phase.UNKNOWN
        private set

    /** Poslední vyhlazená hloubka 0–1 (ladění, ukazatel na obrazovce). */
    var lastDepth: Double? = null
        private set

    /** Poslední úhel v koleni (°), když byla vidět noha. */
    var lastAngle: Double? = null
        private set

    /** Poměr svislé délky stehna a trupu ve stoji (kalibrace); null = zatím neznámý. */
    var standingThighRatio: Double? = null
        private set

    private var candidate: Phase = Phase.UNKNOWN
    private var candidateFrames = 0
    private var wasDown = false
    private var lastRepAt = Long.MIN_VALUE / 2
    private val calibration = ArrayList<Double>()

    fun reset() {
        count = 0
        phase = Phase.UNKNOWN
        lastDepth = null
        lastAngle = null
        standingThighRatio = null
        candidate = Phase.UNKNOWN
        candidateFrames = 0
        wasDown = false
        lastRepAt = Long.MIN_VALUE / 2
        calibration.clear()
    }

    /** Snímek s klouby. Vrací true, když přibyl dřep. */
    fun onFrame(frame: PoseFrame, timeMillis: Long): Boolean {
        val depth = depthOf(frame) ?: return false
        return onDepth(depth, timeMillis)
    }

    /** Zpětná kompatibilita (210): jen nohy, bez ramen. */
    fun onFrame(left: Leg?, right: Leg?, timeMillis: Long): Boolean = onFrame(
        PoseFrame(
            leftHip = left?.hip, rightHip = right?.hip,
            leftKnee = left?.knee, rightKnee = right?.knee,
            leftAnkle = left?.ankle, rightAnkle = right?.ankle,
        ),
        timeMillis,
    )

    /** Úhel v koleni ve stupních → hloubka → počítání. Vrací true, když přibyl dřep. */
    fun onAngle(angle: Double, timeMillis: Long): Boolean = onDepth(angleDepth(angle), timeMillis)

    /** Hloubka 0–1 → stavový automat. Vrací true, když přibyl dřep. */
    fun onDepth(rawDepth: Double, timeMillis: Long): Boolean {
        val raw = rawDepth.coerceIn(0.0, 1.0)
        val smoothed = lastDepth?.let { it + thresholds.smoothing * (raw - it) } ?: raw
        lastDepth = smoothed
        val observed = when {
            smoothed >= thresholds.downDepth -> Phase.DOWN
            smoothed <= thresholds.upDepth -> Phase.UP
            else -> return false // mezi prahy nic neměnit (hystereze)
        }
        if (observed == candidate) {
            candidateFrames++
        } else {
            candidate = observed
            candidateFrames = 1
        }
        if (candidateFrames < thresholds.stableFrames || observed == phase) return false
        phase = observed
        if (observed == Phase.DOWN) {
            wasDown = true
            return false
        }
        if (wasDown && timeMillis - lastRepAt >= thresholds.minRepMillis) {
            wasDown = false
            lastRepAt = timeMillis
            count++
            return true
        }
        wasDown = false
        return false
    }

    /** Hloubka dřepu ze snímku (vyšší z úhlu a ze stehna), null = nohy nejsou vidět. */
    fun depthOf(frame: PoseFrame): Double? {
        val angle = kneeAngle(frame)
        if (angle != null) lastAngle = angle
        val byAngle = angle?.let(::angleDepth)
        val byThigh = thighDepth(frame)
        return listOfNotNull(byAngle, byThigh).maxOrNull()
    }

    /** Úhel z dobře viditelných nohou (průměr, když jsou vidět obě). */
    fun kneeAngle(frame: PoseFrame): Double? {
        val legs = listOfNotNull(
            leg(frame.leftHip, frame.leftKnee, frame.leftAnkle),
            leg(frame.rightHip, frame.rightKnee, frame.rightAnkle),
        )
        if (legs.isEmpty()) return null
        return legs.map { angleAt(it.hip, it.knee, it.ankle) }.average()
    }

    /** Zpětná kompatibilita (210). */
    fun kneeAngle(left: Leg?, right: Leg?): Double? = kneeAngle(
        PoseFrame(
            leftHip = left?.hip, rightHip = right?.hip,
            leftKnee = left?.knee, rightKnee = right?.knee,
            leftAnkle = left?.ankle, rightAnkle = right?.ankle,
        ),
    )

    fun angleDepth(angle: Double): Double =
        ((thresholds.standAngle - angle) / (thresholds.standAngle - thresholds.deepAngle)).coerceIn(0.0, 1.0)

    /**
     * Hloubka ze svislé délky stehna vůči trupu. Ve stoji je stehno svisle dlouhé, v dřepu se
     * (v obraze) zkracuje k nule. Poměr ve stoji se kalibruje (medián prvních snímků, pak maximum).
     */
    private fun thighDepth(frame: PoseFrame): Double? {
        val ratio = thighRatio(frame) ?: return null
        val standing = standingThighRatio
        if (standing == null) {
            calibration += ratio
            if (calibration.size >= thresholds.calibrationFrames) {
                standingThighRatio = calibration.sorted()[calibration.size / 2]
            }
            return null
        }
        if (ratio > standing) standingThighRatio = ratio // narovnal se víc než při kalibraci
        val ref = standingThighRatio ?: return null
        val deep = ref * thresholds.deepThighRatio
        return ((ref - ratio) / (ref - deep)).coerceIn(0.0, 1.0)
    }

    private fun thighRatio(frame: PoseFrame): Double? {
        val shoulder = mid(frame.leftShoulder, frame.rightShoulder) ?: return null
        val hip = mid(frame.leftHip, frame.rightHip) ?: return null
        val knee = mid(frame.leftKnee, frame.rightKnee) ?: return null
        val torso = hypot((hip.x - shoulder.x).toDouble(), (hip.y - shoulder.y).toDouble())
        if (torso < 1.0) return null
        return ((knee.y - hip.y) / torso).coerceAtLeast(0.0)
    }

    private fun ok(j: Joint?): Joint? = j?.takeIf { it.likelihood >= thresholds.minLikelihood }

    private fun mid(a: Joint?, b: Joint?): Joint? {
        val pa = ok(a)
        val pb = ok(b)
        return when {
            pa != null && pb != null -> Joint((pa.x + pb.x) / 2, (pa.y + pb.y) / 2, minOf(pa.likelihood, pb.likelihood))
            else -> pa ?: pb
        }
    }

    private fun leg(hip: Joint?, knee: Joint?, ankle: Joint?): Leg? {
        val h = ok(hip) ?: return null
        val k = ok(knee) ?: return null
        val a = ok(ankle) ?: return null
        return Leg(h, k, a)
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
