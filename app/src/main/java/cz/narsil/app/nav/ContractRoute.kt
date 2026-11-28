package cz.narsil.app.nav

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.narsil.app.ServiceLocator
import cz.narsil.contract.ContractDraft
import cz.narsil.contract.ContractSigner
import cz.narsil.contract.ContractVideoRecorder
import cz.narsil.core.PlanRules
import cz.narsil.core.data.DayKey
import cz.narsil.ui.screens.contract.ContractScreen
import cz.narsil.ui.screens.contract.ContractStep
import cz.narsil.ui.screens.contract.ContractViewModel
import cz.narsil.ui.screens.contract.MovementGoalType
import kotlinx.coroutines.launch

/**
 * Večerní smlouva (313 + 212 + 139): Janova obrazovka se skutečným nahráváním videa
 * ([ContractVideoRecorder]) a uložením přes [ContractSigner] – cíl, plánovaný obsah, úkoly a video.
 * Video smlouvy pak přehraje Ozvěna (SPEC 9, `RoomEchoVideoRepository`).
 */
@Composable
fun ContractRoute(onBack: () -> Unit) {
    val sl = ServiceLocator
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: ContractViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val signer = remember { ContractSigner(sl.contractRepository, sl.settingsRepository) }
    val recorder = remember { ContractVideoRecorder(context) }
    val recording by recorder.state.collectAsState()
    // Texty „dnes“ / „zítra“ podle dne, na který se smlouva uloží (DayKey.contractDateFor).
    val appSettings by sl.settingsRepository.settings.collectAsState()
    val targetIsToday = remember(appSettings.demoContractToday) {
        signer.targetDate() == DayKey.of(System.currentTimeMillis())
    }
    var cameraAllowed by remember { mutableStateOf(hasCameraPermission(context)) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        cameraAllowed = hasCameraPermission(context)
    }

    // Smlouva na ten den už existuje → předvyplnit (úprava večer, video zůstane).
    val existingVideo = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val existing = runCatching { sl.contractRepository.forDate(signer.targetDate()) }.getOrNull()
        if (existing != null) {
            existingVideo.value = existing.videoUri
            vm.prefill(existing, videoSeconds = videoDurationSeconds(existing.videoUri))
        }
    }
    // Ke kroku s videem požádat o kameru (mikrofon je nepovinný – video jde i bez zvuku).
    LaunchedEffect(state.currentStep) {
        if (state.currentStep == ContractStep.ECHO_VIDEO && !cameraAllowed) {
            cameraPermission.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }
    LaunchedEffect(recording) {
        vm.onRecordingState(
            isRecording = recording.isRecording,
            seconds = recording.elapsedSeconds,
            hasVideo = recording.videoUri != null,
            error = recording.error,
        )
    }

    ContractScreen(
        state = state.copy(maxVideoSeconds = PlanRules.MAX_VIDEO_SECONDS, targetIsToday = targetIsToday),
        onSelectGoalType = vm::selectGoalType,
        onSetGoalAmount = vm::setGoalAmount,
        onAdjustGoalAmount = vm::adjustGoalAmount,
        onDraftChange = vm::updateDraftItemText,
        onAddPlannedItem = vm::addPlannedItem,
        onRemovePlannedItem = vm::removePlannedItem,
        onToggleRecording = {
            when {
                !cameraAllowed -> cameraPermission.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                recording.isRecording -> recorder.stop()
                else -> {
                    recorder.discard()
                    recorder.start(signer.targetDate())
                }
            }
        },
        onNextStep = vm::nextStep,
        onPreviousStep = vm::previousStep,
        onSubmitContract = {
            if (recording.isRecording) recorder.stop()
            scope.launch {
                val previous = runCatching { sl.contractRepository.forDate(signer.targetDate()) }.getOrNull()
                signer.sign(
                    ContractDraft(
                        stepGoal = if (state.goalType == MovementGoalType.STEPS) state.goalAmount else previous?.stepGoal ?: PlanRules.DEFAULT_STEP_GOAL,
                        plannedContent = state.plannedItems,
                        tasks = mergeTasks(previous?.tasks.orEmpty(), state.tasks.map { it.toPlanTask() }),
                        videoUri = recording.videoUri ?: existingVideo.value,
                    ),
                )
                vm.submitContract()
            }
        },
        onBackToHome = onBack,
        cameraPreviewSlot = { RecorderPreview(recorder, cameraAllowed) },
        onSelectTaskType = vm::selectTaskType,
        onTaskDraftChange = vm::updateTaskDraftText,
        onAddTask = { type, title -> vm.addTask(type, title) },
        onRemoveTask = vm::removeTask,
    )
}

/**
 * Úkoly z obrazovky nahradí ty uložené, ale už rozpracované (ověřené, pokusy, důkaz) se zachovají
 * podle názvu – úprava smlouvy večer nesmaže stav ověření.
 */
internal fun mergeTasks(previous: List<cz.narsil.core.PlanTask>, drafted: List<cz.narsil.core.PlanTask>): List<cz.narsil.core.PlanTask> =
    drafted.map { d -> previous.firstOrNull { it.title.equals(d.title, ignoreCase = true) && it.type == d.type } ?: d }

/** Délka uloženého videa v sekundách (0 = nejde zjistit). */
internal suspend fun videoDurationSeconds(uri: String?): Int {
    val path = uri?.let { android.net.Uri.parse(it).path } ?: return 0
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(path)
                val ms = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                ((ms + 500) / 1000).toInt()
            } finally {
                retriever.release()
            }
        }.getOrDefault(0)
    }
}
