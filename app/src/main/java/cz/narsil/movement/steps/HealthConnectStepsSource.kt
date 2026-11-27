package cz.narsil.movement.steps

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import cz.narsil.core.data.DayKey
import java.time.Clock
import java.time.Instant

/**
 * Kroky dnes (od 4:00) z Health Connect. Nic nehází: bez aplikace Health Connect
 * nebo bez oprávnění vrací null (pak platí záloha [SensorStepsSource]).
 *
 * Oprávnění si vyžádá onboarding (122): `registerForActivityResult(HealthConnectStepsSource.permissionContract())`
 * a spustí s [PERMISSIONS]. Na Redmi může být potřeba Health Connect doinstalovat z Google Play ([isAvailable]).
 */
class HealthConnectStepsSource(
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) : StepsSource {

    private val client: HealthConnectClient? by lazy {
        if (isAvailable(context)) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null
    }

    suspend fun hasPermission(): Boolean = runCatching {
        client?.permissionController?.getGrantedPermissions()?.containsAll(PERMISSIONS) == true
    }.getOrDefault(false)

    override suspend fun stepsToday(): Int? = runCatching {
        val c = client ?: return null
        if (!hasPermission()) return null
        val start = Instant.ofEpochMilli(DayKey.startMillis(DayKey.of(clock.millis(), clock.zone), clock.zone))
        val end = Instant.ofEpochMilli(clock.millis())
        if (!end.isAfter(start)) return 0
        val result = c.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )
        (result[StepsRecord.COUNT_TOTAL] ?: 0L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }.getOrNull()

    companion object {
        val PERMISSIONS: Set<String> = setOf(HealthPermission.getReadPermission(StepsRecord::class))

        fun isAvailable(context: Context): Boolean =
            runCatching { HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE }
                .getOrDefault(false)

        /** Kontrakt pro žádost o oprávnění (onboarding 122). */
        fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
            PermissionController.createRequestPermissionResultContract()
    }
}
