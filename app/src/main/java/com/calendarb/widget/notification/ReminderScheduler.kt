package com.calendarb.widget.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Планировщик ежедневного напоминания на 08:00.
 *
 * Используем будильник, который сам перепланируется на следующий день после срабатывания
 * ([DailyReminderReceiver]), а также восстанавливается после перезагрузки ([BootReceiver])
 * и смены даты/часового пояса.
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 9001

    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextOccurrence(LocalTime.of(8, 0))

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_CHECK_TODAY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Точный аларм нужен, чтобы уведомление приходило ровно в 8 утра.
        if (canScheduleExactAlarms(alarmManager)) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, pendingIntent),
                pendingIntent
            )
        } else {
            // Fallback, если пользователь не выдал SCHEDULE_EXACT_ALARM.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun canScheduleExactAlarms(am: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true

    /** Миллисекунды до ближайшего наступления [time] (сегодня или завтра). */
    private fun nextOccurrence(time: LocalTime): Long {
        val now = LocalDateTime.now()
        val target = LocalDate.now().atTime(time)
        val next = if (target.isAfter(now)) target else target.plusDays(1)
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
