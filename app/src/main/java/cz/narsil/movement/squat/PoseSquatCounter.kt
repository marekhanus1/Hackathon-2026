package cz.narsil.movement.squat

import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Skutečný [SquatCounter] (210): CameraX (přední kamera) + ML Kit Pose Detection v režimu streamu.
 * Každý snímek → úhel kyčel–koleno–kotník → [SquatRepCounter]. Analýza běží na vlastním vlákně
 * a zahazuje staré snímky (stíhá i Redmi). [start]/[stop] volat na hlavním vlákně. Nic nehází.
 *
 * Náhled: UI předá `PreviewView.surfaceProvider` (nebo použije [SquatCameraPreview]).
 */
class PoseSquatCounter(
    context: Context,
    thresholds: SquatThresholds = SquatThresholds(),
) : SquatCounter {
    private val context = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(this.context)
    private val reps = SquatRepCounter(thresholds)

    private val _count = MutableStateFlow(0)
    private val _running = MutableStateFlow(false)
    private val _kneeAngle = MutableStateFlow<Double?>(null)
    private val _personVisible = MutableStateFlow(false)
    private val _depth = MutableStateFlow<Double?>(null)

    override val count: StateFlow<Int> = _count.asStateFlow()
    override val isRunning: StateFlow<Boolean> = _running.asStateFlow()

    /** Vyhlazený úhel v koleni (ladění prahů v 231). */
    val kneeAngle: StateFlow<Double?> = _kneeAngle.asStateFlow()

    /** Je člověk (nohy) vidět v záběru – UI může ukázat „ustup dál od telefonu“. */
    val personVisible: StateFlow<Boolean> = _personVisible.asStateFlow()

    /** Vyhlazená hloubka dřepu 0–1 (ukazatel pro ladění v sále, [SquatTuningOverlay]). */
    val depth: StateFlow<Double?> = _depth.asStateFlow()

    private var provider: ProcessCameraProvider? = null
    private var useCases: List<UseCase> = emptyList()
    private var detector: PoseDetector? = null
    private var analysisExecutor: ExecutorService? = null

    @Volatile
    private var active = false

    override fun start(lifecycleOwner: LifecycleOwner, previewSurfaceProvider: Any?) {
        stop()
        synchronized(reps) { reps.reset() }
        _count.value = 0
        _kneeAngle.value = null
        _depth.value = null
        active = true
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
        runCatching { if (useCases.isNotEmpty()) provider?.unbind(*useCases.toTypedArray()) }
        useCases = emptyList()
        runCatching { detector?.close() }
        detector = null
        analysisExecutor?.shutdown()
        analysisExecutor = null
        _running.value = false
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyze(image: ImageProxy, poseDetector: PoseDetector) {
        val media = image.image
        if (!active || media == null) {
            image.close()
            return
        }
        val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
        poseDetector.process(input)
            .addOnSuccessListener { pose -> onPose(pose) }
            .addOnCompleteListener { image.close() }
    }

    private fun onPose(pose: Pose) {
        if (!active) return
        fun joint(type: Int): Joint? = pose.getPoseLandmark(type)?.let { Joint(it.position.x, it.position.y, it.inFrameLikelihood) }
        val frame = PoseFrame(
            leftShoulder = joint(PoseLandmark.LEFT_SHOULDER),
            rightShoulder = joint(PoseLandmark.RIGHT_SHOULDER),
            leftHip = joint(PoseLandmark.LEFT_HIP),
            rightHip = joint(PoseLandmark.RIGHT_HIP),
            leftKnee = joint(PoseLandmark.LEFT_KNEE),
            rightKnee = joint(PoseLandmark.RIGHT_KNEE),
            leftAnkle = joint(PoseLandmark.LEFT_ANKLE),
            rightAnkle = joint(PoseLandmark.RIGHT_ANKLE),
        )
        val now = SystemClock.elapsedRealtime()
        synchronized(reps) {
            val counted = reps.onFrame(frame, now)
            fun seen(vararg j: Joint?) = j.any { (it?.likelihood ?: 0f) >= VISIBLE_LIKELIHOOD }
            _personVisible.value = seen(frame.leftHip, frame.rightHip) && seen(frame.leftKnee, frame.rightKnee)
            if (counted) _count.value = reps.count
            _kneeAngle.value = reps.lastAngle
            _depth.value = reps.lastDepth
        }
    }

    private companion object {
        const val VISIBLE_LIKELIHOOD = 0.35f
    }
}
