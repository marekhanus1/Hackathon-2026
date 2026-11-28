package cz.narsil.contract

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import cz.narsil.core.PlanRules
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stav nahrávání večerního videa.
 *
 * @param isReady kamera je navázaná, jde nahrávat
 * @param videoUri hotové video (file://), null = zatím nic
 */
data class RecordingState(
    val isReady: Boolean = false,
    val isRecording: Boolean = false,
    val elapsedSeconds: Int = 0,
    val maxSeconds: Int = PlanRules.MAX_VIDEO_SECONDS,
    val videoUri: String? = null,
    val error: String? = null,
)

/**
 * Nahrání ≤ 15s večerního videa přední kamerou (212). Video zůstává jen v telefonu
 * (`filesDir/contract_videos`), URI jde do `DailyContract.videoUri`.
 *
 * Použití na obrazovce smlouvy (313, Jan): PreviewView v AndroidView →
 * `recorder.bind(lifecycleOwner, previewView.surfaceProvider)`, tlačítko → [start] / [stop],
 * stav z [state]. Po 15 s se nahrávání zastaví samo. Volat na hlavním vlákně. Nic nehází.
 */
class ContractVideoRecorder(context: Context) {
    private val context = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(this.context)

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var provider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var preview: Preview? = null
    private var recording: Recording? = null

    /** Naváže náhled a nahrávání na [lifecycleOwner]. [previewSurfaceProvider] = `PreviewView.surfaceProvider` nebo null. */
    fun bind(lifecycleOwner: LifecycleOwner, previewSurfaceProvider: Any? = null) {
        runCatching {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                runCatching {
                    val cameraProvider = future.get()
                    val newPreview = Preview.Builder().build().also { p ->
                        (previewSurfaceProvider as? Preview.SurfaceProvider)?.let(p::setSurfaceProvider)
                    }
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                        .build()
                    val capture = VideoCapture.withOutput(recorder)
                    val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    preview?.let { cameraProvider.unbind(it) }
                    videoCapture?.let { cameraProvider.unbind(it) }
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, newPreview, capture)
                    provider = cameraProvider
                    preview = newPreview
                    videoCapture = capture
                    _state.update { it.copy(isReady = true, error = null) }
                }.onFailure { _state.update { it.copy(isReady = false, error = "Kamera není k dispozici") } }
            }, mainExecutor)
        }.onFailure { _state.update { it.copy(isReady = false, error = "Kamera není k dispozici") } }
    }

    /** Začne nahrávat video ke smlouvě na den [date]; [prefix] = „smlouva“, nebo „onboarding“ (139). */
    @SuppressLint("MissingPermission")
    fun start(date: LocalDate, prefix: String = "smlouva") {
        val capture = videoCapture
        if (capture == null || recording != null) return
        runCatching {
            val dir = File(context.filesDir, VIDEO_DIR).apply { mkdirs() }
            val file = File(dir, "$prefix-$date-${System.currentTimeMillis()}.mp4")
            val options = FileOutputOptions.Builder(file)
                .setDurationLimitMillis(PlanRules.MAX_VIDEO_SECONDS * 1_000L)
                .build()
            var pending = capture.output.prepareRecording(context, options)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                pending = pending.withAudioEnabled()
            }
            _state.update { it.copy(isRecording = true, elapsedSeconds = 0, videoUri = null, error = null) }
            recording = pending.start(mainExecutor) { event -> onEvent(event, file) }
        }.onFailure {
            recording = null
            _state.update { it.copy(isRecording = false, error = "Nahrávání se nepovedlo") }
        }
    }

    /** Zastaví nahrávání; video se dokončí asynchronně ([RecordingState.videoUri]). */
    fun stop() {
        runCatching { recording?.stop() }
    }

    /** Zahodí hotové video (natočit znovu). */
    fun discard() {
        _state.value.videoUri?.let { uri -> runCatching { Uri.parse(uri).path?.let { File(it).delete() } } }
        _state.update { it.copy(videoUri = null, elapsedSeconds = 0) }
    }

    /** Uvolní kameru (při opuštění obrazovky). */
    fun unbind() {
        stop()
        runCatching {
            preview?.let { provider?.unbind(it) }
            videoCapture?.let { provider?.unbind(it) }
        }
        preview = null
        videoCapture = null
        _state.update { it.copy(isReady = false) }
    }

    private fun onEvent(event: VideoRecordEvent, file: File) {
        when (event) {
            is VideoRecordEvent.Status -> {
                val seconds = (event.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
                _state.update { it.copy(elapsedSeconds = seconds.coerceAtMost(PlanRules.MAX_VIDEO_SECONDS)) }
            }
            is VideoRecordEvent.Finalize -> {
                recording = null
                val ok = !event.hasError() || event.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
                if (ok && file.exists() && file.length() > 0) {
                    _state.update { it.copy(isRecording = false, videoUri = Uri.fromFile(file).toString(), error = null) }
                } else {
                    runCatching { file.delete() }
                    _state.update { it.copy(isRecording = false, error = "Nahrávání se nepovedlo") }
                }
            }
            else -> Unit
        }
    }

    private companion object {
        const val VIDEO_DIR = "contract_videos"
    }
}
