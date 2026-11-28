package cz.narsil.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import cz.narsil.detection.accessibility.NarsilAccessibilityService

/**
 * Tichá služba na popředí („Narsil hlídá feed“): proces s ní má vyšší prioritu, takže ho systém
 * (hlavně MIUI úsporný režim) neukončuje tak ochotně. Sama nic nedělá – detekce běží ve službě přístupnosti.
 * Spouští se jen se zapnutou přístupností; když start z pozadí systém zakáže, nic se nestane.
 */
class GuardService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!NarsilAccessibilityService.isEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val ok = runCatching {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification(this),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
            )
        }.isSuccess
        if (!ok) stopSelf()
        return if (ok) START_STICKY else START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL = "guard"
        private const val NOTIFICATION_ID = 4_102

        /** Spustí službu, když dává smysl (přístupnost zapnutá). Nikdy nehází. */
        fun start(context: Context) {
            if (!NarsilAccessibilityService.isEnabled(context)) return
            runCatching { ContextCompat.startForegroundService(context, Intent(context, GuardService::class.java)) }
        }

        private fun notification(context: Context): android.app.Notification {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(CHANNEL, "Hlídání feedu", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Trvalá tichá notifikace, aby telefon Narsil nevypínal"
                    setShowBadge(false)
                },
            )
            val open = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Narsil hlídá feed")
                .setContentText("Zprávy ani chaty nečte. Venku mlčí.")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setSilent(true)
                .setShowWhen(false)
                .setContentIntent(open)
                .build()
        }
    }
}
