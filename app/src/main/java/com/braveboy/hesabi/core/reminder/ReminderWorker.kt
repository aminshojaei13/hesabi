package com.braveboy.hesabi.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.braveboy.hesabi.HesabiApp
import com.braveboy.hesabi.MainActivity
import com.braveboy.hesabi.R
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.notifications.NotificationChannels
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.ChequeStatus

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = context.applicationContext as? HesabiApp ?: return Result.success()
        val container = app.container

        val today = JalaliDate.today().toEpochDay()

        // Automatically process due recurring transactions and salary auto-deposit if enabled
        val maintenance = runCatching {
            container.recurringTransactionProcessor.processDueTransactions(today)
            container.processSalaryAutoDeposit(today)
            container.purgeExpiredTrash()
        }
        if (maintenance.isFailure) {
            // Transient DB/lock problems retry with backoff instead of silently skipping
            // the whole reminder round (M21).
            return Result.retry()
        }

        val settings = container.settingsRepository.current()
        if (!settings.remindersEnabled) return Result.success()

        NotificationChannels.ensure(context)

        val dueWindowCheques = today + 2
        val dueWindowInstallments = today + 3

        val cheques = container.chequeRepository.getAll()
        val upcomingCheques = cheques.filter {
            it.status == ChequeStatus.PENDING && it.dueEpochDay in today..dueWindowCheques
        }

        val loans = container.loanRepository.getAllWithInstallments()
        val upcomingInstallments = loans.flatMap { loanWithInst ->
            loanWithInst.installments.filter { !it.isPaid && it.dueEpochDay in today..dueWindowInstallments }
                .map { inst -> loanWithInst.loan to inst }
        }

        var notificationId = 2001

        upcomingCheques.forEach { cheque ->
            val dueJalali = JalaliDate.fromEpochDay(cheque.dueEpochDay)
            val daysLeft = cheque.dueEpochDay - today
            val dueLabel = when (daysLeft) {
                0L -> "امروز"
                1L -> "فردا"
                else -> "تا ۲ روز دیگر"
            }
            sendNotification(
                id = notificationId++,
                title = "سررسید چک: ${cheque.bankName} ($dueLabel)",
                body = "چک به مبلغ ${Money.format(cheque.amountToman)} در وجه/از ${cheque.payeeOrDrawerName} در تاریخ ${dueJalali.formatShort()} سررسید می‌شود.",
            )
        }

        upcomingInstallments.forEach { (loan, inst) ->
            val dueJalali = JalaliDate.fromEpochDay(inst.dueEpochDay)
            val daysLeft = inst.dueEpochDay - today
            val dueLabel = when (daysLeft) {
                0L -> "امروز"
                1L -> "فردا"
                else -> "تا ۳ روز دیگر"
            }
            sendNotification(
                id = notificationId++,
                title = "سررسید قسط ${loan.title} ($dueLabel)",
                body = "قسط شماره ${inst.installmentNumber} به مبلغ ${Money.format(inst.amountToman)} در تاریخ ${dueJalali.formatShort()} سررسید می‌شود.",
            )
        }

        return Result.success()
    }

    private fun sendNotification(id: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = NotificationChannels.REMINDERS
        const val WORK_NAME = "hesabi_reminders_periodic_work"
    }
}
