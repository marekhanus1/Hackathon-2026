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
 * Parťák (SPEC 5, 163): kamarád, kterého poprosíš o pár minut navíc. V MVP bez serveru:
 * odpověď je simulovaná a zprávy jsou ukázkové notifikace na tomto telefonu.
 * Nic nehází; bez oprávnění notifikací mlčí.
 */
class PartnerNotifier(private val context: Context) {

    fun notifyDebtOverLimit(debtMinutes: Int) =
        notify("Kamarád tě dnes prosil už o $debtMinutes min. Napiš mu, jestli je všechno v pohodě!")

    /** Simulovaná odpověď Parťáka na prosbu z Ozvěny (163). */
    fun notifyFriendGift(minutes: Int) =
        notify("Jasně, tady máš $minutes min. Ale pak už fakt dost 😉", title = "Parťák ti poslal $minutes min")

    /** Pozastavení ochrany v nastavení (133). */
    fun notifyProtectionPaused() =
        notify("Kamarád si v Narsilu pozastavil ochranu do rána. Zeptej se, jestli je všechno v pohodě.")

    private fun notify(text: String, title: String = "Zpráva pro Parťáka") {
        runCatching {
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Parťák", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Parťák: odpovědi na prosby o minuty a zprávy pro něj"
                },
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
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
