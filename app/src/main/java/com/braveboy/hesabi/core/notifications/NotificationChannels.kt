package com.braveboy.hesabi.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

/** Channels are created once at app start so Settings can show them immediately. */
object NotificationChannels {

    const val REMINDERS = "hesabi_reminders_channel"
    const val SMS = "hesabi_sms_channel"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(SMS) == null) {
            val smsChannel = NotificationChannel(
                SMS,
                "پیامک‌های بانکی",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "اعلان پیدا شدن تراکنش تازهٔ بانکی برای تأیید"
                enableVibration(false)
            }
            manager.createNotificationChannel(smsChannel)
        }
        if (manager.getNotificationChannel(REMINDERS) == null) {
            val channel = NotificationChannel(
                REMINDERS,
                "یادآورهای مالی",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "یادآوری سررسید چک‌ها، اقساط و تعهدات"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
