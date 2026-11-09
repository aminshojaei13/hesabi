package com.braveboy.hesabi

import android.app.Application
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.diagnostics.CrashLogger
import com.braveboy.hesabi.core.notifications.NotificationChannels
import com.braveboy.hesabi.core.receipt.ReceiptStorageManager
import com.braveboy.hesabi.core.reminder.ReminderScheduler
import com.braveboy.hesabi.di.AppContainer
import kotlinx.coroutines.launch

class HesabiApp : Application() {

    lateinit var container: AppContainer
        private set

    val applicationScope get() = container.applicationScope

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        container = AppContainer(this)
        NotificationChannels.ensure(this)

        applicationScope.launch {
            runCatching {
                container.ensureSeeded()
                val todayEpochDay = JalaliDate.today().toEpochDay()
                container.recurringTransactionProcessor.processDueTransactions(todayEpochDay)
                container.processSalaryAutoDeposit(todayEpochDay)
                container.purgeExpiredTrash()
                container.diagnosticsRepository.record("lifecycle", "app_start")
            }
        }
        ReminderScheduler.schedule(this)
    }
}
