package com.calendarb.widget.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.calendarb.widget.DataProvider
import java.util.concurrent.Executors

/**
 * Срабатывает каждый день в 08:00 (плюс системные события смены даты/времени).
 * Если на текущий день есть задачи — показывает push-уведомление с группировкой
 * по важности. После показа планируется следующий запуск на завтра в 8:00.
 */
class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Перепланируем на следующее наступление 8:00 — makeDaily сам считает "завтра".
        ReminderScheduler.scheduleDailyReminder(context)

        // Broadcast receiver должен завершиться быстро → работу с БД уводим в фон.
        val appContext = context.applicationContext
        val pending = goAsync()
        Executors.newSingleThreadExecutor().execute {
            try {
                val tasks = DataProvider.todayTasks()
                if (tasks.isNotEmpty()) {
                    TaskNotifier.notifyTodayTasks(appContext, tasks)
                } else {
                    // Задач нет — уведомление не показываем, снимаем старое.
                    NotificationManagerCompat.from(appContext)
                        .cancel(TaskNotifier.NOTIFICATION_ID)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CHECK_TODAY = "com.calendarb.widget.ACTION_CHECK_TODAY"
    }
}
