package com.calendarb.widget.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * После перезагрузки устройства будильники сбрасываются — восстанавливаем
 * ежедневное напоминание на 8:00.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.scheduleDailyReminder(context)
        }
    }
}
