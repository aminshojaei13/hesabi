package com.braveboy.hesabi

import android.app.Application
import com.braveboy.hesabi.core.reminder.ReminderScheduler
import com.braveboy.hesabi.di.AppContainer

class HesabiApp : Application() {

    lateinit var container: AppContainer
        private set

    val applicationScope get() = container.applicationScope

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.schedule(this)
    }
}
