package cz.narsil.movement.steps

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import cz.narsil.core.data.DayKey
import java.time.Clock
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Záloha: `Sensor.TYPE_STEP_COUNTER` (kroky od startu telefonu). Kroky dne = rozdíl proti
 * první hodnotě dne, přes restart telefonu se sčítají ([StepCounterMath]). Kroky ušlé dnes
 * před prvním čtením senzor nezná. Potřebuje oprávnění `ACTIVITY_RECOGNITION` (žádá onboarding).
 */
class SensorStepsSource(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
) : StepsSource {

    private val sensorManager: SensorManager? = context.getSystemService(SensorManager::class.java)

    val isAvailable: Boolean get() = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    override suspend fun stepsToday(): Int? = runCatching {
        if (context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val raw = readRaw() ?: return null
        val today = DayKey.of(clock.millis(), clock.zone).toString()
        val saved = StepCounterMath.Saved(
            day = prefs.getString(KEY_DAY, null),
            baseline = prefs.getLong(KEY_BASELINE, 0),
            lastRaw = prefs.getLong(KEY_LAST_RAW, 0),
            accumulated = prefs.getLong(KEY_ACC, 0),
        )
        val next = StepCounterMath.update(saved, today, raw)
        prefs.edit()
            .putString(KEY_DAY, next.day)
            .putLong(KEY_BASELINE, next.baseline)
            .putLong(KEY_LAST_RAW, next.lastRaw)
            .putLong(KEY_ACC, next.accumulated)
            .apply()
        StepCounterMath.steps(next).toInt()
    }.getOrNull()

    /** Jedna hodnota senzoru (čeká nejvýš 3 s). */
    private suspend fun readRaw(): Long? {
        val sm = sensorManager ?: return null
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        return withTimeoutOrNull(3_000) {
            suspendCancellableCoroutine { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sm.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values.firstOrNull()?.toLong() ?: 0L)
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { sm.unregisterListener(listener) }
            }
        }
    }

    private companion object {
        const val PREFS = "narsil_step_counter"
        const val KEY_DAY = "day"
        const val KEY_BASELINE = "baseline"
        const val KEY_LAST_RAW = "last_raw"
        const val KEY_ACC = "accumulated"
    }
}

/** Čistý přepočet hodnot `TYPE_STEP_COUNTER` na kroky dne (testy bez Androidu). */
object StepCounterMath {
    data class Saved(val day: String?, val baseline: Long, val lastRaw: Long, val accumulated: Long)

    fun update(saved: Saved, today: String, raw: Long): Saved = when {
        saved.day != today -> Saved(day = today, baseline = raw, lastRaw = raw, accumulated = 0)
        // Restart telefonu: počítadlo začalo od nuly, dosavadní kroky dne se uloží.
        raw < saved.lastRaw -> saved.copy(baseline = 0, lastRaw = raw, accumulated = saved.accumulated + (saved.lastRaw - saved.baseline))
        else -> saved.copy(lastRaw = raw)
    }

    fun steps(saved: Saved): Long = (saved.accumulated + saved.lastRaw - saved.baseline).coerceAtLeast(0)
}
