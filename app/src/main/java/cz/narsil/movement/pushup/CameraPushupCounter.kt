package cz.narsil.movement.pushup

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import cz.narsil.core.SquatCounter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.hypot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Počítadlo kliků na podlaze (implementuje [SquatCounter] pro snadné napojení):
 * - přední kamera: průměrný jas Y-roviny (zakrytí hrudníkem) + ML Kit Pose (přiblížení ramen/hlavy),
 * - proximity senzor: blízkost hrudníku nad displejem,
 * - anti-cheat: akcelerometr + gyroskop přes [PhoneFlatGuard] blokují pohyb telefonem v ruce.
 */
class CameraPushupCounter(
    context: Context,
    thresholds: PushupThresholds = PushupThresholds(),
) : SquatCounter, SensorEventListener {

    private val context = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(this.context)
    private val sensorManager = this.context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val reps = PushupRepCounter(thresholds)

    private val _count = MutableStateFlow(0)
    private val _running = MutableStateFlow(false)
    private val _depth = MutableStateFlow<Double?>(null)
    private val _placement = MutableStateFlow(PhonePlacement.NOT_FLAT)
    private val _proximityNear = MutableStateFlow(false)
    private val _personVisible = MutableStateFlow(false)

    override val count: StateFlow<Int> = _count.asStateFlow()
    override val isRunning: StateFlow<Boolean> = _running.asStateFlow()

    val depth: StateFlow<Double?> = _depth.asStateFlow()
    val placement: StateFlow<PhonePlacement> = _placement.asStateFlow()
    val proximityNear: StateFlow<Boolean> = _proximityNear.asStateFlow()
    val personVisible: StateFlow<Boolean> = _personVisible.asStateFlow()

    private var provider: ProcessCameraProvider? = null
    private var useCases: List<UseCase> = emptyList()
    private var detector: PoseDetector? = null
    private var analysisExecutor: ExecutorService? = null
    private var proximitySensor: Sensor? = null

    @Volatile
    private var active = false

    @Volatile
    private var currentProximityNear = false

    override fun start(lifecycleOwner: LifecycleOwner, previewSurfaceProvider: Any?) {
        stop()
        synchronized(reps) { reps.reset() }
        _count.value = 0
        _depth.value = null
        _placement.value = PhonePlacement.NOT_FLAT
        _proximityNear.value = false
        _personVisible.value = false
        currentProximityNear = false
        active = true

        registerSensors()

        val executor = Executors.newSingleThreadExecutor().also { analysisExecutor = it }
        val poseDetector = PoseDetection.getClient(
            PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build(),
        ).also { detector = it }

        runCatching {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (!active) return@addListener
                runCatching {
                    val cameraProvider = future.get()
                    val preview = (previewSurfaceProvider as? Preview.SurfaceProvider)?.let { surface ->
                        Preview.Builder().build().also { it.setSurfaceProvider(surface) }
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor) { image -> analyze(image, poseDetector) } }
                    val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    val cases = listOfNotNull(preview, analysis)
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, *cases.toTypedArray())
                    provider = cameraProvider
                    useCases = cases
                    _running.value = true
                }.onFailure { stop() }
            }, mainExecutor)
        }.onFailure { stop() }
    }

    override fun stop() {
        active = false
        runCatching { sensorManager?.unregisterListener(this) }
        runCatching { if (useCases.isNotEmpty()) provider?.unbind(*useCases.toTypedArray()) }
        useCases = emptyList()
        runCatching { detector?.close() }
        detector = null
        analysisExecutor?.shutdown()
        analysisExecutor = null
        _running.value = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!active || event == null) return
        val now = SystemClock.elapsedRealtime()
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                synchronized(reps) {
                    reps.onAccelerometer(event.values[0], event.values[1], event.values[2], now)
                    _placement.value = reps.placement
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                synchronized(reps) {
                    reps.onGyroscope(event.values[0], event.values[1], event.values[2], now)
                    _placement.value = reps.placement
                }
            }
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = proximitySensor?.maximumRange ?: 5f
                val near = event.values[0] < minOf(maxRange, PROXIMITY_THRESHOLD_CM)
                currentProximityNear = near
                _proximityNear.value = near
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun registerSensors() {
        val manager = sensorManager ?: return
        manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        proximitySensor = manager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.also {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyze(image: ImageProxy, poseDetector: PoseDetector) {
        val media = image.image
        if (!active || media == null) {
            image.close()
            return
        }
        val luma = computeAverageLuma(image)
        val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
        poseDetector.process(input)
            .addOnSuccessListener { pose -> onPose(pose, luma) }
            .addOnCompleteListener { image.close() }
    }

    private fun onPose(pose: Pose, averageLuma: Double?) {
        if (!active) return
        val span = extractUpperBodySpan(pose)
        val visible = span != null || pose.getPoseLandmark(PoseLandmark.NOSE)?.let {
            it.inFrameLikelihood >= VISIBLE_LIKELIHOOD
        } == true
        val frame = PushupFrame(
            upperBodySpan = span,
            averageLuma = averageLuma,
            proximityNear = currentProximityNear,
            personVisible = visible,
        )
        val now = SystemClock.elapsedRealtime()
        synchronized(reps) {
            val counted = reps.onFrame(frame, now)
            _personVisible.value = visible
            _placement.value = reps.placement
            if (counted) _count.value = reps.count
            _depth.value = reps.lastDepth
        }
    }

    private fun extractUpperBodySpan(pose: Pose): Double? {
        fun pairDist(aType: Int, bType: Int): Double? {
            val a = pose.getPoseLandmark(aType)?.takeIf { it.inFrameLikelihood >= VISIBLE_LIKELIHOOD } ?: return null
            val b = pose.getPoseLandmark(bType)?.takeIf { it.inFrameLikelihood >= VISIBLE_LIKELIHOOD } ?: return null
            return hypot((a.position.x - b.position.x).toDouble(), (a.position.y - b.position.y).toDouble())
        }
        return pairDist(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
            ?: pairDist(PoseLandmark.LEFT_EAR, PoseLandmark.RIGHT_EAR)
            ?: pairDist(PoseLandmark.LEFT_EYE, PoseLandmark.RIGHT_EYE)
    }

    private fun computeAverageLuma(image: ImageProxy): Double? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null

        var sum = 0L
        var samples = 0L
        val step = 8
        for (y in 0 until height step step) {
            val rowOffset = y * rowStride
            for (x in 0 until width step step) {
                val idx = rowOffset + x * pixelStride
                if (idx in 0 until buffer.limit()) {
                    sum += buffer.get(idx).toInt() and 0xFF
                    samples++
                }
            }
        }
        return if (samples > 0) sum.toDouble() / samples.toDouble() else null
    }

    private companion object {
        const val VISIBLE_LIKELIHOOD = 0.35f
        const val PROXIMITY_THRESHOLD_CM = 5.0f
    }
}
