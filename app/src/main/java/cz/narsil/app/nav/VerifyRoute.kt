package cz.narsil.app.nav

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cz.narsil.app.ServiceLocator
import cz.narsil.core.PhotoKind
import cz.narsil.core.PlanRules
import cz.narsil.core.PlanTask
import cz.narsil.core.Proof
import cz.narsil.core.TaskStatus
import cz.narsil.core.VerificationMethod
import cz.narsil.ui.screens.verify.ProofMode
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
 * fotoaparát), vlastní popis (2–3 věty, LLM vrátí zpětnou vazbu) nebo Strava (pohybový úkol se ověří sám,
 * když je Strava připojená). Demo „vždy ANO“ jde přes DemoAwareTaskVerifier.
 */
@Composable
fun VerifyRoute(onBack: () -> Unit) {
    val sl = ServiceLocator
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val contract by sl.contractRepository.today.collectAsState()
    val events by sl.verification.events.collectAsState()
    val stravaConnected by sl.stravaRepository.isConnected.collectAsState()

    val task: PlanTask? = contract?.tasks?.firstOrNull { it.status != TaskStatus.VERIFIED }
        ?: contract?.tasks?.lastOrNull()
    if (task == null) {
        NoTask(onBack)
        return
    }

    var proofMode by rememberSaveable(task.id) {
        mutableStateOf(if (task.method == VerificationMethod.TEXT) ProofMode.TEXT else ProofMode.PHOTO)
    }
    var text by rememberSaveable(task.id) { mutableStateOf("") }
    var photo by rememberSaveable(task.id) { mutableStateOf<String?>(null) }
    var pendingPhoto by remember { mutableStateOf<Uri?>(null) }
    // „Zkusit znovu“ po zamítnutí: znovu formulář důkazu, dokud se nezapíše další pokus.
    var retrying by rememberSaveable(task.id, task.attempts) { mutableStateOf(false) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) photo = pendingPhoto?.toString()
    }

    fun submit(proof: Proof) = scope.launch { runCatching { sl.verification.submit(task, proof) } }

    // Pohybový úkol: se Stravou se ověří sám (SPEC 7), jinak záloha fotkou.
    LaunchedEffect(task.id, stravaConnected) {
        if (task.method == VerificationMethod.STRAVA && stravaConnected && task.status == TaskStatus.PENDING && task.attempts == 0) {
            submit(Proof.Strava)
        }
    }

    val event = events[task.id]
    val state = VerifyTaskUiState(
        taskTitle = task.title,
        taskGoal = task.target.ifBlank {
            if (task.method == VerificationMethod.STRAVA) "Aktivita ve Stravě (≥ 15 min), nebo snímek aktivity" else "Fotka, fotka prostředí nebo vlastní popis"
        },
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
    )
}

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
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Dnes nemáš žádný úkol k ověření.", style = MaterialTheme.typography.titleMedium)
        Text("Úkoly si naplánuješ ve večerní smlouvě.", style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onBack) { Text("Zpět") }
    }
}
