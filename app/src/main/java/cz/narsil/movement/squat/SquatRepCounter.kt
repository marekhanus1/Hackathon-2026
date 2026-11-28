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
    /** Od této hloubky je dřep „dole“ (0,55 ≈ úhel 126°; tolerantní pro sál i šikmý záběr). */
    val downDepth: Double = 0.55,
    /** Pod touto hloubkou je „nahoře“ (0,25 ≈ úhel 150°). */
    val upDepth: Double = 0.25,
    /** Kolik snímků po sobě musí poloha trvat (tlumení zákmitů). */
    val stableFrames: Int = 2,
    /** Nejkratší doba mezi dvěma dřepy. */
    val minRepMillis: Long = 550,
    /** Pod touto jistotou se kloub ignoruje (v šeru ML Kit hlásí nižší jistotu). */
    val minLikelihood: Float = 0.35f,
    /** Vyhlazení hloubky (EMA): 1 = bez vyhlazení. */
    val smoothing: Double = 0.55,
    /** Poměr ve stoji se kalibruje z prvních snímků; tolik snímků stačí ke kalibraci. */
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
 * Signál = **hloubka dřepu** 0–1 ze tří zdrojů:
 * 1. úhel kyčel–koleno–kotník (z boku bere prioritně lépe viditelnou bližší nohu, ne zakrytou),
 * 2. svislá délka stehna vůči holeni (odolná proti předklonu trupu i vzdálenosti zepředu),
 * 3. záloha: svislá délka stehna vůči trupu (s pamětí stoje proti předklonu), když nejsou vidět kotníky.
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

    /** Poměr svislé délky stehna a holeni ve stoji (kalibrace); null = zatím neznámý. */
    var standingShinRatio: Double? = null
        private set

    private var standingTorso: Double? = null
    private var candidate: Phase = Phase.UNKNOWN
    private var candidateFrames = 0
    private var wasDown = false
    private var lastRepAt = Long.MIN_VALUE / 2
    private val torsoCalibration = ArrayList<Double>()
    private val shinCalibration = ArrayList<Double>()

    fun reset() {
        count = 0
        phase = Phase.UNKNOWN
        lastDepth = null
        lastAngle = null
        standingThighRatio = null
        standingShinRatio = null
        standingTorso = null
        candidate = Phase.UNKNOWN
        candidateFrames = 0
        wasDown = false
        lastRepAt = Long.MIN_VALUE / 2
        torsoCalibration.clear()
        shinCalibration.clear()
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

    /**
     * Hloubka dřepu ze snímku.
     * Když jsou vidět kotníky, kombinuje úhel kolene a poměr stehno/holeň (odolný vůči předklonu).
     * Bez kotníků použije zálohu stehno/trup s referencí stoje.
     */
    fun depthOf(frame: PoseFrame): Double? {
        val angle = kneeAngle(frame)
        if (angle != null) lastAngle = angle
        val byAngle = angle?.let(::angleDepth)
        val byShin = shinThighDepth(frame)
        return if (byShin != null) {
            listOfNotNull(byAngle, byShin).maxOrNull()
        } else {
            val byTorso = torsoThighDepth(frame)
            listOfNotNull(byAngle, byTorso).maxOrNull()
        }
    }

    /** Úhel z dobře viditelných nohou (z boku bere dominantní bližší nohu, jinak vážený průměr). */
    fun kneeAngle(frame: PoseFrame): Double? {
        val left = leg(frame.leftHip, frame.leftKnee, frame.leftAnkle)
        val right = leg(frame.rightHip, frame.rightKnee, frame.rightAnkle)
        return when {
            left == null && right == null -> null
            left != null && right == null -> angleAt(left.hip, left.knee, left.ankle)
            right != null && left == null -> angleAt(right.hip, right.knee, right.ankle)
            left != null && right != null -> {
                if (abs(left.likelihood - right.likelihood) > SIDE_CONFIDENCE_GAP) {
                    val primary = if (left.likelihood >= right.likelihood) left else right
                    angleAt(primary.hip, primary.knee, primary.ankle)
                } else {
                    val wl = left.likelihood.toDouble()
                    val wr = right.likelihood.toDouble()
                    val al = angleAt(left.hip, left.knee, left.ankle)
                    val ar = angleAt(right.hip, right.knee, right.ankle)
                    (al * wl + ar * wr) / (wl + wr)
                }
            }
            else -> null
        }
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
     * Hloubka ze svislé délky stehna vůči holeni. Holeň zůstává při dřepu svislá a nemění
     * délku při předklonu trupu ani při různé vzdálenosti od kamery.
     */
    private fun shinThighDepth(frame: PoseFrame): Double? {
        val ratio = shinThighRatio(frame) ?: return null
        val standing = standingShinRatio
        if (standing == null) {
            shinCalibration += ratio.coerceIn(0.65, 1.45)
            if (shinCalibration.size >= thresholds.calibrationFrames) {
                standingShinRatio = shinCalibration.sorted()[shinCalibration.size / 2]
            }
            return null
        }
        if (ratio > standing && ratio <= 1.35) {
            standingShinRatio = standing + UPWARD_CALIBRATION_EMA * (ratio - standing)
        }
        val ref = standingShinRatio ?: return null
        val deep = ref * thresholds.deepThighRatio
        return ((ref - ratio) / (ref - deep)).coerceIn(0.0, 1.0)
    }

    private fun shinThighRatio(frame: PoseFrame): Double? {
        fun legRatio(l: Leg): Double? {
            val shin = hypot((l.ankle.x - l.knee.x).toDouble(), (l.ankle.y - l.knee.y).toDouble())
            if (shin < 10.0) return null
            return ((l.knee.y - l.hip.y) / shin).coerceAtLeast(0.0)
        }
        val left = leg(frame.leftHip, frame.leftKnee, frame.leftAnkle)
        val right = leg(frame.rightHip, frame.rightKnee, frame.rightAnkle)
        val leftRatio = left?.let(::legRatio)
        val rightRatio = right?.let(::legRatio)
        return when {
            leftRatio == null && rightRatio == null -> null
            leftRatio != null && rightRatio == null -> leftRatio
            rightRatio != null && leftRatio == null -> rightRatio
            left != null && right != null && leftRatio != null && rightRatio != null -> {
                if (abs(left.likelihood - right.likelihood) > SIDE_CONFIDENCE_GAP) {
                    if (left.likelihood >= right.likelihood) leftRatio else rightRatio
                } else {
                    (leftRatio + rightRatio) / 2.0
                }
            }
            else -> null
        }
    }

    /**
     * Záložní hloubka ze svislé délky stehna vůči trupu (když kotníky nejsou v záběru).
     * Používá paměť délky trupu ve stoji, aby předklon trupu nesrazil jmenovatel.
     */
    private fun torsoThighDepth(frame: PoseFrame): Double? {
        val measured = torsoMeasurements(frame) ?: return null
        val (thighDrop, torsoLen) = measured
        val currentStandingTorso = standingTorso
        val refTorso = if (currentStandingTorso == null) {
            standingTorso = torsoLen
            torsoLen
        } else {
            if (torsoLen > currentStandingTorso) {
                standingTorso = currentStandingTorso + UPWARD_CALIBRATION_EMA * (torsoLen - currentStandingTorso)
            }
            maxOf(torsoLen, standingTorso ?: torsoLen)
        }
        val ratio = (thighDrop / refTorso).coerceAtLeast(0.0)
        val standing = standingThighRatio
        if (standing == null) {
            torsoCalibration += ratio.coerceIn(0.55, 1.30)
            if (torsoCalibration.size >= thresholds.calibrationFrames) {
                standingThighRatio = torsoCalibration.sorted()[torsoCalibration.size / 2]
            }
            return null
        }
        if (ratio > standing && ratio <= 1.25) {
            standingThighRatio = standing + UPWARD_CALIBRATION_EMA * (ratio - standing)
        }
        val ref = standingThighRatio ?: return null
        val deep = ref * thresholds.deepThighRatio
        return ((ref - ratio) / (ref - deep)).coerceIn(0.0, 1.0)
    }

    private fun torsoMeasurements(frame: PoseFrame): Pair<Double, Double>? {
        val shoulder = selectPoint(frame.leftShoulder, frame.rightShoulder) ?: return null
        val hip = selectPoint(frame.leftHip, frame.rightHip) ?: return null
        val knee = selectPoint(frame.leftKnee, frame.rightKnee) ?: return null
        val torso = hypot((hip.x - shoulder.x).toDouble(), (hip.y - shoulder.y).toDouble())
        if (torso < 1.0) return null
        val thighDrop = (knee.y - hip.y).toDouble().coerceAtLeast(0.0)
        return thighDrop to torso
    }

    private fun ok(j: Joint?): Joint? = j?.takeIf { it.likelihood >= thresholds.minLikelihood }

    private fun selectPoint(a: Joint?, b: Joint?): Joint? {
        val pa = ok(a)
        val pb = ok(b)
        return when {
            pa != null && pb != null -> when {
                pa.likelihood - pb.likelihood > SIDE_CONFIDENCE_GAP -> pa
                pb.likelihood - pa.likelihood > SIDE_CONFIDENCE_GAP -> pb
                else -> Joint((pa.x + pb.x) / 2f, (pa.y + pb.y) / 2f, minOf(pa.likelihood, pb.likelihood))
            }
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
        private const val SIDE_CONFIDENCE_GAP = 0.15f
        private const val UPWARD_CALIBRATION_EMA = 0.15

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
