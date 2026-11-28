package cz.narsil.movement.pushup

/**
 * Vstupní data jednoho snímku pro počítání kliků ze země (kamera + proximity).
 *
 * @param upperBodySpan vzdálenost ramen / hlavy v obraze (px) – při přiblížení hrudníku roste
 * @param averageLuma průměrný jas z Y-roviny kamery (0–255) – při těsném přiblížení hrudníku klesá
 * @param proximityNear true, pokud senzor přiblížení hlásí objekt těsně nad displejem (dotek / < 5 cm)
 * @param personVisible true, pokud ML Kit na snímku vidí horní část těla nebo obličej
 */
data class PushupFrame(
    val upperBodySpan: Double? = null,
    val averageLuma: Double? = null,
    val proximityNear: Boolean = false,
    val personVisible: Boolean = false,
)

data class PushupThresholds(
    val downDepth: Double = 0.60,
    val upDepth: Double = 0.25,
    val stableFrames: Int = 2,
    val minRepMillis: Long = 550L,
    val smoothing: Double = 0.60,
    val calibrationFrames: Int = 5,
    /** Kolikrát musí narůst šířka ramen/obličeje v obraze při dole oproti horní poloze. */
    val downSpanMultiplier: Double = 1.55,
    /** Jaký relativní pokles jasu kamery (0–1) znamená plné přiblížení hrudníku. */
    val lumaDropFraction: Double = 0.45,
    /** Jak dlouho po posledním spatření člověka platí proximity/luma (proti mávání rukou nad prázdným stolem). */
    val personFreshMillis: Long = 4_000L,
    val requirePersonRecently: Boolean = true,
)

/**
 * Čistá logika počítání kliků s telefonem na zemi pod hrudníkem.
 * Kombinuje proximity, zakrytí světla v kameře (luma) a zvětšení horní části těla (ML Kit),
 * přičemž každý klik přísně podmiňuje tím, že [PhoneFlatGuard] potvrzuje nehybný telefon na podlaze.
 */
class PushupRepCounter(
    private val thresholds: PushupThresholds = PushupThresholds(),
    private val flatGuard: PhoneFlatGuard = PhoneFlatGuard(),
) {
    enum class Phase { UNKNOWN, UP, DOWN }

    var count: Int = 0
        private set
    var phase: Phase = Phase.UNKNOWN
        private set
    var lastDepth: Double? = null
        private set
    var standingSpan: Double? = null
        private set
    var standingLuma: Double? = null
        private set

    val placement: PhonePlacement
        get() = flatGuard.placement

    private var candidate: Phase = Phase.UNKNOWN
    private var candidateFrames = 0
    private var wasDown = false
    private var lastRepAt = Long.MIN_VALUE / 2
    private var lastPersonSeenAt = Long.MIN_VALUE
    private val spanCalibration = ArrayList<Double>()
    private val lumaCalibration = ArrayList<Double>()

    fun reset() {
        count = 0
        phase = Phase.UNKNOWN
        lastDepth = null
        standingSpan = null
        standingLuma = null
        candidate = Phase.UNKNOWN
        candidateFrames = 0
        wasDown = false
        lastRepAt = Long.MIN_VALUE / 2
        lastPersonSeenAt = Long.MIN_VALUE
        spanCalibration.clear()
        lumaCalibration.clear()
        flatGuard.reset()
    }

    fun onAccelerometer(ax: Float, ay: Float, az: Float, timeMillis: Long) {
        flatGuard.onAccelerometer(ax, ay, az, timeMillis)
        if (!flatGuard.isReady(timeMillis)) {
            invalidateActiveRep()
        }
    }

    fun onGyroscope(gx: Float, gy: Float, gz: Float, timeMillis: Long) {
        flatGuard.onGyroscope(gx, gy, gz, timeMillis)
        if (!flatGuard.isReady(timeMillis)) {
            invalidateActiveRep()
        }
    }

    fun onFrame(frame: PushupFrame, timeMillis: Long): Boolean {
        if (frame.personVisible) {
            lastPersonSeenAt = timeMillis
        }
        if (!flatGuard.isReady(timeMillis)) {
            invalidateActiveRep()
            return false
        }
        if (thresholds.requirePersonRecently && !isPersonFresh(timeMillis)) {
            return false
        }
        val depth = depthOf(frame) ?: return false
        return onDepth(depth, timeMillis)
    }

    fun onDepth(rawDepth: Double, timeMillis: Long): Boolean {
        val raw = rawDepth.coerceIn(0.0, 1.0)
        val smoothed = lastDepth?.let { it + thresholds.smoothing * (raw - it) } ?: raw
        lastDepth = smoothed

        val observed = when {
            smoothed >= thresholds.downDepth -> Phase.DOWN
            smoothed <= thresholds.upDepth -> Phase.UP
            else -> return false
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

    fun depthOf(frame: PushupFrame): Double? {
        val byProximity = if (frame.proximityNear) 1.0 else null
        val byLuma = lumaDepth(frame.averageLuma)
        val bySpan = spanDepth(frame.upperBodySpan)
        return listOfNotNull(byProximity, byLuma, bySpan).maxOrNull()
    }

    private fun spanDepth(span: Double?): Double? {
        if (span == null || span < 8.0) return null
        val baseline = standingSpan
        if (baseline == null) {
            spanCalibration += span
            if (spanCalibration.size >= thresholds.calibrationFrames) {
                standingSpan = spanCalibration.sorted()[spanCalibration.size / 2]
            }
            return null
        }
        val downTarget = baseline * thresholds.downSpanMultiplier
        if (downTarget <= baseline) return null
        return ((span - baseline) / (downTarget - baseline)).coerceIn(0.0, 1.0)
    }

    private fun lumaDepth(luma: Double?): Double? {
        if (luma == null || luma < 0.0) return null
        val baseline = standingLuma
        if (baseline == null) {
            if (luma >= 12.0) {
                lumaCalibration += luma
                if (lumaCalibration.size >= thresholds.calibrationFrames) {
                    standingLuma = lumaCalibration.sorted()[lumaCalibration.size / 2]
                }
            }
            return null
        }
        if (luma > baseline) {
            standingLuma = baseline + 0.1 * (luma - baseline)
        }
        val ref = standingLuma ?: return null
        if (ref < 12.0) return null
        val drop = ref - luma
        return (drop / (ref * thresholds.lumaDropFraction)).coerceIn(0.0, 1.0)
    }

    private fun isPersonFresh(timeMillis: Long): Boolean =
        lastPersonSeenAt != Long.MIN_VALUE && timeMillis - lastPersonSeenAt <= thresholds.personFreshMillis

    private fun invalidateActiveRep() {
        wasDown = false
        candidate = Phase.UNKNOWN
        candidateFrames = 0
        phase = Phase.UNKNOWN
    }
}
