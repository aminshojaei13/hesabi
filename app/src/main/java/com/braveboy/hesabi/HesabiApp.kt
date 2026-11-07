package com.braveboy.hesabi

import android.app.Application
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.reminder.ReminderScheduler
import com.braveboy.hesabi.di.AppContainer
import kotlinx.coroutines.launch

class HesabiApp : Application() {

    lateinit var container: AppContainer
        private set

    val applicationScope get() = container.applicationScope

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            container.ensureSeeded()
            val todayEpochDay = JalaliDate.today().toEpochDay()
            container.recurringTransactionProcessor.processDueTransactions(todayEpochDay)
            container.processSalaryAutoDeposit(todayEpochDay)
        }
        ReminderScheduler.schedule(this)
    }
}
