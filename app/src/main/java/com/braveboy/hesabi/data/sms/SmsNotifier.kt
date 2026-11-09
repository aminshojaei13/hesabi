package com.braveboy.hesabi.data.sms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.braveboy.hesabi.MainActivity
import com.braveboy.hesabi.R
import com.braveboy.hesabi.core.notifications.NotificationChannels

/** Lightweight "1 new transaction found" notification for a captured bank SMS. */
object SmsNotifier {

    private const val NOTIFICATION_ID = 3101

    fun notifyCaptured(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationChannels.ensure(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SMS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.SMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("۱ تراکنش تازه پیدا شد")
            .setContentText("برای بررسی و تأیید، حسابی را باز کن.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    const val EXTRA_OPEN_SMS = "open_sms"
}
