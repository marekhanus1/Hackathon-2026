package cz.narsil.app.onboarding

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import cz.narsil.detection.accessibility.NarsilAccessibilityService

/** Krok průvodce (122). [runtime] = oprávnění žádané systémovým dialogem, jinak se otevře nastavení. */
enum class PermissionStep(
    val title: String,
    val why: String,
    val required: Boolean,
    val runtime: List<String> = emptyList(),
) {
    ACCESSIBILITY(
        "Detekce scrollování",
        "Narsil pozná nekonečný feed. Zprávy a chaty nikdy nečte.",
        required = true,
    ),
    // Ozvěna i ukazatel jdou přes okno služby přístupnosti (TYPE_ACCESSIBILITY_OVERLAY), které tohle oprávnění
    // nepotřebuje. Zůstává jen jako záloha v Nastavení, onboarding ho neukazuje.
    OVERLAY("Okno Ozvěny", "Záloha, když služba přístupnosti neběží. Běžně není potřeba.", required = false),
    LOCATION(
        "Poloha kvůli Wi-Fi",
        "Android prozradí název Wi-Fi jen s polohou. Polohu nikam neposíláme.",
        required = true,
        runtime = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    ),
    // Kamera až při použití (dřepy v Ozvěně se zeptají samy přes CameraPermissionActivity).
    CAMERA("Kamera", "Počítá dřepy a natočí večerní video. Obraz zůstává v telefonu.", required = false, runtime = listOf(Manifest.permission.CAMERA)),
    MICROPHONE("Mikrofon", "Zvuk k večernímu videu.", required = false, runtime = listOf(Manifest.permission.RECORD_AUDIO)),
    STEPS("Kroky", "Kroky jsou tvoje měna za neplánovaný obsah.", required = false, runtime = listOf(Manifest.permission.ACTIVITY_RECOGNITION)),
    HEALTH_CONNECT("Health Connect", "Přesnější kroky z Health Connect.", required = false),
    NOTIFICATIONS(
        "Notifikace",
        "Zpráva Parťákovi a připomínka večerní smlouvy.",
        required = false,
        runtime = if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList(),
    ),
    BATTERY("Běh na pozadí", "Vypni optimalizaci baterie, ať systém Narsil neukončí.", required = false),
    MIUI_AUTOSTART(
        "MIUI: automatické spouštění a okna na pozadí",
        "Na Xiaomi zapni Automatické spouštění a Zobrazit vyskakovací okna na pozadí.",
        required = false,
    ),
}

/** Stav oprávnění a otevírání systémových nastavení (122). Nic nehází. */
object PermissionChecker {

    val isMiui: Boolean
        get() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
            Build.BRAND.equals("Redmi", ignoreCase = true) || Build.BRAND.equals("POCO", ignoreCase = true)

    /** Kroky, které se na tomhle telefonu ukazují. */
    fun steps(): List<PermissionStep> = PermissionStep.entries.filter { step ->
        when (step) {
            PermissionStep.MIUI_AUTOSTART -> isMiui
            PermissionStep.NOTIFICATIONS -> Build.VERSION.SDK_INT >= 33
            else -> true
        }
    }

    /** Je krok splněný? null = nejde ověřit (MIUI automatické spouštění). Health Connect řeší [isHealthGranted]. */
    fun isGranted(context: Context, step: PermissionStep): Boolean? = when (step) {
        PermissionStep.ACCESSIBILITY -> NarsilAccessibilityService.isEnabled(context)
        PermissionStep.OVERLAY -> Settings.canDrawOverlays(context)
        PermissionStep.BATTERY -> context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true
        PermissionStep.MIUI_AUTOSTART -> null
        PermissionStep.HEALTH_CONNECT -> null
        else -> step.runtime.isEmpty() || step.runtime.any { has(context, it) }
    }

    fun has(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** Intent do nastavení pro krok bez systémového dialogu. */
    fun settingsIntent(context: Context, step: PermissionStep): Intent {
        val pkg = Uri.fromParts("package", context.packageName, null)
        return when (step) {
            PermissionStep.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            PermissionStep.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg)
            PermissionStep.BATTERY -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            PermissionStep.MIUI_AUTOSTART -> Intent().setComponent(
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            )
            else -> appDetails(context)
        }
    }

    /** Informace o aplikaci: na Androidu 13+ odtud „Povolit omezená nastavení“ (⋮) pro přístupnost. */
    fun appDetails(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    /** Otevře nastavení; když konkrétní obrazovka neexistuje (MIUI, výrobce), otevře informace o aplikaci. */
    fun open(context: Context, step: PermissionStep) {
        val primary = settingsIntent(context, step).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(primary) }
            .recoverCatching { context.startActivity(appDetails(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
