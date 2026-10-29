package com.braveboy.hesabi.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.braveboy.hesabi.HesabiApp
import com.braveboy.hesabi.core.date.JalaliDate
import kotlinx.coroutines.launch

/**
 * Optional, user-enabled SMS ingestion. The receiver is deliberately thin: it joins
 * multipart messages, hands them to the repository and finishes the broadcast.
 *
 * Privacy: the raw body never leaves the device and is never written to logs.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val app = context.applicationContext as? HesabiApp ?: return
        val sender = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        if (body.isBlank()) return

        val pendingResult = goAsync()
        val today = JalaliDate.today().toEpochDay()
        app.applicationScope.launch {
            try {
                if (app.container.settingsRepository.current().smsCaptureEnabled) {
                    app.container.smsRepository.ingest(
                        sender = sender,
                        body = body,
                        receivedAtMillis = System.currentTimeMillis(),
                        todayEpochDay = today,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
