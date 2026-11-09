package com.braveboy.hesabi.data.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.braveboy.hesabi.core.reminder.ReminderScheduler

/**
 * Re-registers the periodic reminder after a reboot. WorkManager normally survives
 * reboots, but an explicit receiver removes the "first reminder never arrives"
 * window on devices with aggressive battery managers (L6).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            runCatching { ReminderScheduler.schedule(context.applicationContext) }
        }
    }
}
