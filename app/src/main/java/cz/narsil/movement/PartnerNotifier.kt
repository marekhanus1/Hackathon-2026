package cz.narsil.movement

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Parťák (SPEC 5): půjčka nad limit dluhu → zpráva kamarádovi. V MVP ukázková notifikace
 * na tomto telefonu („tohle by dostal Parťák“). Nic nehází; bez oprávnění notifikací mlčí.
 */
class PartnerNotifier(private val context: Context) {

    fun notifyDebtOverLimit(debtMinutes: Int) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Parťák", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Zprávy pro Parťáka, když dluh přesáhne limit"
                },
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Zpráva pro Parťáka")
                .setContentText("Kamarád chtěl půjčit další minuty, dluh je už $debtMinutes min. Napiš mu!")
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val CHANNEL_ID = "partner"
        const val NOTIFICATION_ID = 2_101
    }
}
