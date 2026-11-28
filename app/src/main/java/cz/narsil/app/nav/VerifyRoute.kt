package cz.narsil.app.nav

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import cz.narsil.app.ServiceLocator
import cz.narsil.core.PhotoKind
import cz.narsil.gamification.GamificationRules
import cz.narsil.core.PlanRules
import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.TaskStatus
import cz.narsil.core.VerificationMethod
import cz.narsil.ui.screens.verify.ProofMode
import cz.narsil.ui.screens.verify.TaskPickerScreen
import cz.narsil.ui.screens.verify.TaskType as UiTaskType
import cz.narsil.ui.screens.verify.VerificationStatus
import cz.narsil.ui.screens.verify.VerifyTaskScreen
import cz.narsil.ui.screens.verify.VerifyTaskUiState
import cz.narsil.verify.VerifyRules
import java.io.File
import kotlinx.coroutines.launch

/**
 * Ověření úkolu (324 + 520): Janova obrazovka nad skutečným [cz.narsil.verify.TaskVerificationService].
 * Úkol = první neověřený z dnešní smlouvy. Důkaz (541): fotka úkolu, fotka prostředí (systémový
 * fotoaparát) nebo vlastní popis (2–3 věty, LLM vrátí zpětnou vazbu). Strava se nepoužije (146), i pohybový
 * úkol se dokládá fotkou nebo popisem. Demo „vždy ANO“ jde přes DemoAwareTaskVerifier.
 */
@Composable
fun VerifyRoute(onBack: () -> Unit, initialTaskId: Long? = null) {
    val sl = ServiceLocator
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contract by sl.contractRepository.today.collectAsState()
    val events by sl.verification.events.collectAsState()

    // Zobrazený úkol se drží, dokud uživatel neklepne na „Další úkol“ – po ověření uvidí výsledek a odměnu.
    var shownTaskId by rememberSaveable { mutableStateOf(initialTaskId) }
    val tasks = contract?.tasks.orEmpty()
    val task: PlanTask? = pickVerifyTask(tasks, shownTaskId)
    if (task == null) {
        NoTask(onBack)
        return
    }
    LaunchedEffect(task.id) { shownTaskId = task.id }
    val nextTask = tasks.firstOrNull { it.id != task.id && it.status != TaskStatus.VERIFIED }

    var proofMode by rememberSaveable(task.id) {
        mutableStateOf(if (task.method == VerificationMethod.TEXT) ProofMode.TEXT else ProofMode.PHOTO)
    }
    var text by rememberSaveable(task.id) { mutableStateOf("") }
    var photo by rememberSaveable(task.id) { mutableStateOf<String?>(null) }
    var pendingPhoto by remember { mutableStateOf<Uri?>(null) }
    // „Zkusit znovu“ po zamítnutí: znovu formulář důkazu, dokud se nezapíše další pokus.
    var retrying by rememberSaveable(task.id, task.attempts) { mutableStateOf(false) }
    fun submit(proof: Proof) = scope.launch { runCatching { sl.verification.submit(task, proof) } }

    // „Vyfotit a ověřit“ (334): po vyfocení se důkaz rovnou odešle.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingPhoto?.toString()
        if (ok && uri != null) {
            photo = uri
            submit(Proof.Photo(uri, if (proofMode == ProofMode.ROOM) PhotoKind.ROOM else PhotoKind.TASK))
        }
    }

    // Po restartu aplikace vezme důvod (pochvalu / zamítnutí od AI) uložený u úkolu.
    val event = events[task.id] ?: remember(task.id, task.status) { sl.verification.eventFor(task) }
    val state = VerifyTaskUiState(
        taskTitle = task.title,
        taskGoal = task.target.ifBlank { "Fotka, fotka prostředí nebo vlastní popis" },
        taskType = UiTaskType.entries.firstOrNull { it.name == task.type.name } ?: UiTaskType.OTHER,
        rewardMinutes = task.rewardMinutes,
        proofMode = proofMode,
        photoCaptured = photo != null,
        textProof = text,
        maxTextLength = PlanRules.MAX_TEXT_PROOF_CHARS,
        minTextLength = VerifyRules.MIN_TEXT_CHARS,
        status = verifyStatus(task, running = event?.running == true, retrying = retrying),
        attemptsUsed = task.attempts,
        maxAttempts = PlanRules.MAX_ATTEMPTS,
        statusMessage = if (retrying) "" else event?.reason.orEmpty(),
        rewardXp = GamificationRules.XP_TASK,
        nextTaskTitle = nextTask?.title,
        tasks = tasks.map { it.toTaskItem() },
        selectedTaskId = task.id,
    )

    VerifyTaskScreen(
        state = state,
        onSelectProofMode = {
            if (it != proofMode) photo = null // fotka úkolu ≠ fotka prostředí
            proofMode = it
        },
        onTextProofChange = { text = it.take(PlanRules.MAX_TEXT_PROOF_CHARS) },
        onCapturePhoto = {
            val dir = File(context.cacheDir, "proofs").apply { mkdirs() }
            val file = File(dir, "proof_${task.id}_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingPhoto = uri
            runCatching { camera.launch(uri) }
        },
        onRetakePhoto = { photo = null },
        onSubmitProof = {
            when (proofMode) {
                ProofMode.TEXT -> submit(Proof.Text(text.trim()))
                ProofMode.PHOTO -> photo?.let { submit(Proof.Photo(it, PhotoKind.TASK)) }
                ProofMode.ROOM -> photo?.let { submit(Proof.Photo(it, PhotoKind.ROOM)) }
            }
        },
        onRetry = {
            if (task.status == TaskStatus.VERIFY_LATER) {
                scope.launch { runCatching { sl.verification.retryNow(task) } }
            } else {
                photo = null
                text = ""
                retrying = true
            }
        },
        onBack = onBack,
        onContinue = { if (nextTask != null) shownTaskId = nextTask.id else onBack() },
        onSelectTask = { shownTaskId = it },
    )
}

/** Úkol k ověření: ten, který je právě zobrazený, jinak první neověřený, jinak poslední. */
internal fun pickVerifyTask(tasks: List<PlanTask>, shownTaskId: Long?): PlanTask? =
    tasks.firstOrNull { it.id == shownTaskId }
        ?: tasks.firstOrNull { it.status != TaskStatus.VERIFIED }
        ?: tasks.lastOrNull()

/**
 * Stav obrazovky ověření. Po „Zkusit znovu“ ([retrying]) se u zamítnutého úkolu znovu ukáže formulář
 * důkazu, dokud zbývají pokusy a nezapíše se další pokus.
 */
internal fun verifyStatus(task: PlanTask, running: Boolean, retrying: Boolean): VerificationStatus = when {
    running -> VerificationStatus.VERIFYING
    task.status == TaskStatus.VERIFIED -> VerificationStatus.VERIFIED
    task.status == TaskStatus.REJECTED && retrying && task.canRetry -> VerificationStatus.PENDING
    task.status == TaskStatus.REJECTED -> VerificationStatus.REJECTED
    task.status == TaskStatus.VERIFY_LATER -> VerificationStatus.VERIFY_LATER
    else -> VerificationStatus.PENDING
}

@Composable
private fun NoTask(onBack: () -> Unit) {
    TaskPickerScreen(tasks = emptyList(), onBack = onBack, onContinue = {}, onOpenContract = onBack)
}

/** Výběr úkolu k ověření (334) nad úkoly dnešní smlouvy. */
@Composable
fun TaskPickerRoute(onBack: () -> Unit, onContinue: (Long) -> Unit, onOpenContract: () -> Unit) {
    val contract by ServiceLocator.contractRepository.today.collectAsState()
    TaskPickerScreen(
        tasks = contract?.tasks.orEmpty().map { it.toTaskItem() },
        onBack = onBack,
        onContinue = onContinue,
        onOpenContract = onOpenContract,
    )
}
