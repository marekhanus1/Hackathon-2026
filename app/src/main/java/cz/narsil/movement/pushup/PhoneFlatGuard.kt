package cz.narsil.movement.pushup

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Stav umístění telefonu pro kliky na zemi.
 * Počítání je povolené pouze ve stavu [READY_ON_FLOOR].
 */
enum class PhonePlacement {
    READY_ON_FLOOR,
    NOT_FLAT,
    FACE_DOWN,
    MOVING,
}

data class FlatGuardConfig(
    /** Minimální osa Z akcelerometru (m/s²), kdy telefon leží displejem nahoru. */
    val minFaceUpZ: Float = 8.2f,
    /** Maximální náklon v osách X/Y (m/s²). */
    val maxTiltXY: Float = 4.8f,
    /** Maximální odchylka celkového zrychlení od 9,81 m/s² (pohyb rukou / zvednutí). */
    val maxAccelDeviation: Float = 1.8f,
    /** Maximální úhlová rychlost z gyroskopu (rad/s), kdy je telefon považován za nehybný. */
    val maxGyroRadPerSec: Float = 0.45f,
    /** Jak dlouho musí telefon ležet rovně a v klidu, než se odemkne počítání. */
    val stableFlatMillis: Long = 400L,
    /** Ochranná prodleva po jakémkoli pohybu telefonu rukou. */
    val movementCooldownMillis: Long = 700L,
)

/**
 * Čistá anti-cheat logika nad akcelerometrem a gyroskopem:
 * zaručuje, že telefon leží displejem nahoru na podlaze a uživatel s ním nehýbe v ruce.
 */
class PhoneFlatGuard(private val config: FlatGuardConfig = FlatGuardConfig()) {

    var placement: PhonePlacement = PhonePlacement.NOT_FLAT
        private set

    private var lastGyroNorm: Float = 0f
    private var lastMotionAt: Long = Long.MIN_VALUE
    private var flatSince: Long? = null

    fun reset() {
        placement = PhonePlacement.NOT_FLAT
        lastGyroNorm = 0f
        lastMotionAt = Long.MIN_VALUE
        flatSince = null
    }

    fun onGyroscope(gx: Float, gy: Float, gz: Float, timeMillis: Long) {
        val norm = sqrt(gx * gx + gy * gy + gz * gz)
        lastGyroNorm = norm
        if (norm > config.maxGyroRadPerSec) {
            lastMotionAt = timeMillis
            flatSince = null
            if (placement == PhonePlacement.READY_ON_FLOOR) {
                placement = PhonePlacement.MOVING
            }
        }
    }

    fun onAccelerometer(ax: Float, ay: Float, az: Float, timeMillis: Long) {
        val norm = sqrt(ax * ax + ay * ay + az * az)
        if (abs(norm - GRAVITY) > config.maxAccelDeviation) {
            lastMotionAt = timeMillis
            flatSince = null
            placement = PhonePlacement.MOVING
            return
        }
        if (az < -3.0f) {
            flatSince = null
            placement = PhonePlacement.FACE_DOWN
            return
        }
        if (az < config.minFaceUpZ || abs(ax) > config.maxTiltXY || abs(ay) > config.maxTiltXY) {
            flatSince = null
            placement = PhonePlacement.NOT_FLAT
            return
        }

        val cooldownElapsed = lastMotionAt == Long.MIN_VALUE || timeMillis - lastMotionAt >= config.movementCooldownMillis
        if (!cooldownElapsed || lastGyroNorm > config.maxGyroRadPerSec) {
            flatSince = null
            placement = PhonePlacement.MOVING
            return
        }

        val start = flatSince ?: timeMillis.also { flatSince = it }
        placement = if (timeMillis - start >= config.stableFlatMillis) {
            PhonePlacement.READY_ON_FLOOR
        } else {
            PhonePlacement.MOVING
        }
    }

    fun isReady(timeMillis: Long): Boolean {
        val cooldownElapsed = lastMotionAt == Long.MIN_VALUE || timeMillis - lastMotionAt >= config.movementCooldownMillis
        return placement == PhonePlacement.READY_ON_FLOOR && cooldownElapsed && lastGyroNorm <= config.maxGyroRadPerSec
    }

    private companion object {
        const val GRAVITY = 9.81f
    }
}
