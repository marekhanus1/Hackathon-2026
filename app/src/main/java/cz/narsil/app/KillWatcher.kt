package cz.narsil.app

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cz.narsil.app.onboarding.OnboardingPrefs
import cz.narsil.app.onboarding.PermissionChecker
import cz.narsil.detection.accessibility.NarsilAccessibilityService

/**
 * Pozná, že Narsil vypnul systém (MIUI „AutoIdleKill“, „Vyčistit vše“, málo paměti), a jednou za čas
 * řekne uživateli, co nastavit – místo dlouhého návodu předem. Volá se při startu procesu ([NarsilApp]).
 * Ověřeno na Redmi Note 11 (11/2026): OneKeyClean = důvod 13, AutoIdleKill = 10 s popisem, paměť = 3.
 */
object KillWatcher {
    private const val PREFS = "kill_watcher"
    private const val KEY_LAST_EXIT = "last_exit"
    private const val KEY_LAST_NOTICE = "last_notice"
    private const val CHANNEL = "survival"
    private const val NOTIFICATION_ID = 4_101
    private const val NOTICE_EVERY_MILLIS = 12 * 60 * 60_000L

    // Hodnoty ApplicationExitInfo.REASON_* (API 30), ať jde čistá logika testovat bez Androidu.
    private const val REASON_LOW_MEMORY = 3
    private const val REASON_USER_REQUESTED = 10
    private const val REASON_OTHER = 13

    /** Ukončil proces systém nebo „čistič“ výrobce (ne pád, ne aktualizace, ne uživatel v Nastavení)? */
    fun isSystemKill(reason: Int, description: String?): Boolean = when (reason) {
        REASON_OTHER, REASON_LOW_MEMORY -> true
        // Force-stop z Nastavení je taky USER_REQUESTED: počítat jen, když ho udělal čistič výrobce.
        REASON_USER_REQUESTED -> description.orEmpty().let { d ->
            listOf("autoidle", "clean", "powerkeeper", "kill").any { d.contains(it, ignoreCase = true) }
        }
        else -> false
    }

    /** Má se teď ozvat? Jen po novém zabití, s dokončeným průvodcem a nejvýš jednou za 12 h. */
    fun shouldNotify(systemKill: Boolean, exitAt: Long, lastSeenExit: Long, lastNotice: Long, now: Long, onboardingDone: Boolean): Boolean =
        systemKill && onboardingDone && exitAt > lastSeenExit && now - lastNotice >= NOTICE_EVERY_MILLIS

    fun check(context: Context, now: Long = System.currentTimeMillis()) {
        if (Build.VERSION.SDK_INT < 30) return
        runCatching {
            val am = context.getSystemService(ActivityManager::class.java) ?: return
            val exit = am.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull() ?: return
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastSeen = prefs.getLong(KEY_LAST_EXIT, 0)
            val notify = shouldNotify(
                systemKill = isSystemKill(exit.reason, exit.description),
                exitAt = exit.timestamp,
                lastSeenExit = lastSeen,
                lastNotice = prefs.getLong(KEY_LAST_NOTICE, 0),
                now = now,
                onboardingDone = OnboardingPrefs.isDone(context),
            )
            prefs.edit().putLong(KEY_LAST_EXIT, maxOf(lastSeen, exit.timestamp)).apply()
            if (notify && NarsilAccessibilityService.isEnabled(context) && post(context)) {
                prefs.edit().putLong(KEY_LAST_NOTICE, now).apply()
            }
        }
    }

    private fun post(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Běh na pozadí", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Upozornění, když telefon Narsil vypne"
            },
        )
        val text = if (PermissionChecker.isMiui) {
            "Xiaomi Narsil vypnulo a hlídání feedu chvíli neběželo. Klepni a v průvodci zaškrtni kroky pro Xiaomi – hlavně zámek v posledních aplikacích."
        } else {
            "Telefon Narsil vypnul a hlídání feedu chvíli neběželo. Klepni a vypni pro Narsil optimalizaci baterie."
        }
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_OPEN_SETUP, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Telefon vypnul Narsil")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        return runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }.isSuccess
    }

    /** Extra pro [MainActivity]: otevřít průvodce nastavením (kroky pro Xiaomi). */
    const val EXTRA_OPEN_SETUP = "cz.narsil.OPEN_SETUP"
}
