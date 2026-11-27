package cz.narsil.contract

import android.content.Context
import cz.narsil.core.EchoVideo
import cz.narsil.core.EchoVideoRepository
import cz.narsil.core.data.ContractDao
import cz.narsil.core.data.ContractEntity
import cz.narsil.core.data.DayKey
import java.time.Clock
import java.time.LocalDate

/** Video z onboardingu (SPEC 9, záloha Ozvěny). Nahrává ho onboarding (122/320). */
interface OnboardingVideoStore {
    suspend fun video(): EchoVideo?
    suspend fun save(uri: String, recordedOn: LocalDate)
}

class PrefsOnboardingVideoStore(context: Context) : OnboardingVideoStore {
    private val prefs = context.applicationContext.getSharedPreferences("narsil_onboarding_video", Context.MODE_PRIVATE)

    override suspend fun video(): EchoVideo? = runCatching {
        val uri = prefs.getString(KEY_URI, null) ?: return null
        EchoVideo(uri, LocalDate.parse(prefs.getString(KEY_DATE, null)))
    }.getOrNull()

    override suspend fun save(uri: String, recordedOn: LocalDate) {
        prefs.edit().putString(KEY_URI, uri).putString(KEY_DATE, recordedOn.toString()).apply()
    }

    private companion object {
        const val KEY_URI = "uri"
        const val KEY_DATE = "date"
    }
}

/** Výběr videa pro Ozvěnu (SPEC 9). Čistý Kotlin. */
object EchoVideoPicker {
    /**
     * 1. video dnešní smlouvy (natočené včera večer – „druhý den se video smlouvy přehraje“),
     * 2. jinak nejnovější video ze smlouvy staršího dne,
     * 3. jinak video z onboardingu, jinak null (UI složí text ze smlouvy).
     */
    fun pick(todayContract: ContractEntity?, olderWithVideo: ContractEntity?, onboarding: EchoVideo?, zone: java.time.ZoneId): EchoVideo? =
        todayContract?.toEcho(zone) ?: olderWithVideo?.toEcho(zone) ?: onboarding

    private fun ContractEntity.toEcho(zone: java.time.ZoneId): EchoVideo? =
        videoUri?.takeIf { it.isNotBlank() }?.let { EchoVideo(it, DayKey.of(createdAtMillis, zone)) }
}

/** [EchoVideoRepository] nad Room (212). Video přehraje overlay (113). Nic nehází. */
class RoomEchoVideoRepository(
    private val dao: ContractDao,
    private val onboarding: OnboardingVideoStore? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
) : EchoVideoRepository {
    override suspend fun pickEchoVideo(): EchoVideo? = runCatching {
        val today = DayKey.of(clock.millis(), clock.zone).toString()
        EchoVideoPicker.pick(
            todayContract = dao.contract(today),
            olderWithVideo = dao.latestWithVideoBefore(today),
            onboarding = runCatching { onboarding?.video() }.getOrNull(),
            zone = clock.zone,
        )
    }.getOrNull()
}
