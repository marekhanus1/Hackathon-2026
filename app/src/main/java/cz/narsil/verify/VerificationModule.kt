package cz.narsil.verify

import android.content.Context
import cz.narsil.core.ContractRepository
import cz.narsil.core.RewardService
import cz.narsil.core.SettingsRepository
import cz.narsil.core.StravaRepository
import cz.narsil.core.TaskVerifier
import cz.narsil.core.data.NarsilDatabase
import cz.narsil.core.data.toDomain
import cz.narsil.llm.client.MultiProviderLlmClient
import kotlinx.coroutines.CoroutineScope

/**
 * Sestavení ověření pro `ServiceLocator` (124). Ověřený úkol jde do [RewardService] (Šimon, 221):
 * minuty do zůstatku + 30 XP, každý úkol jen jednou (521).
 *
 * ```
 * val verifier = VerificationModule.verifier(appContext, settingsRepository, stravaRepository)
 * val verification = VerificationModule.service(contractRepository, verifier, rewardService,
 *     VerificationModule.pendingFromDatabase(appContext))
 * VerificationModule.networkTrigger(appContext, verification, appScope).start()
 * ```
 */
object VerificationModule {
    /** Skutečný ověřovač obalený demo přepínačem „vždy ANO“ (522, demo panel). */
    fun verifier(context: Context, settings: SettingsRepository, strava: StravaRepository?): TaskVerifier =
        cz.narsil.verify.demo.DemoAwareTaskVerifier(llmVerifier(context, settings, strava), settings)

    fun llmVerifier(context: Context, settings: SettingsRepository, strava: StravaRepository?): LlmTaskVerifier {
        val app = context.applicationContext
        return LlmTaskVerifier(
            llm = MultiProviderLlmClient(provider = { settings.settings.value.llmProvider }),
            photos = AndroidPhotoLoader(app),
            hashes = RoomProofHashCheck(NarsilDatabase.get(app).contractDao()),
            network = AndroidNetworkStatus(app),
            strava = strava,
        )
    }

    /** Ověření + předání odměně (521). [verifier] může být i fake (`FakeTaskVerifier`). */
    fun service(
        contracts: ContractRepository,
        verifier: TaskVerifier,
        reward: RewardService,
        pendingTasks: (suspend () -> List<cz.narsil.core.PlanTask>)? = null,
    ): TaskVerificationService = if (pendingTasks == null) {
        TaskVerificationService(contracts, verifier, onVerified = rewardHandoff(reward))
    } else {
        TaskVerificationService(contracts, verifier, onVerified = rewardHandoff(reward), pendingTasks = pendingTasks)
    }

    /** Čekající úkoly ze všech dnů (Room), ať se doověří i po přelomu dne ve 4:00. */
    fun pendingFromDatabase(context: Context): suspend () -> List<cz.narsil.core.PlanTask> {
        val dao = NarsilDatabase.get(context.applicationContext).contractDao()
        return { dao.tasksToVerifyLater().map { it.toDomain() } }
    }

    /** Předání výsledku odměnám: jen VERIFIED, chyba odměny ověření nezboří. */
    fun rewardHandoff(reward: RewardService): suspend (cz.narsil.core.PlanTask) -> Unit = { task ->
        if (task.status == cz.narsil.core.TaskStatus.VERIFIED) cz.narsil.llm.client.runCatchingCancellable { reward.reward(task) }
    }

    fun networkTrigger(context: Context, service: TaskVerificationService, scope: CoroutineScope) =
        NetworkRetryTrigger(context, service, scope)
}
