package com.calendarb.widget.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.calendarb.widget.R
import com.calendarb.widget.data.Task
import com.calendarb.widget.data.TaskImportance
import com.calendarb.widget.ui.MainActivity

/**
 * Показ утреннего уведомления со списком задач на сегодня.
 * Группировка по важности: сначала Высокие, затем Средние, затем Низкие;
 * каждая группа оформлена отдельным заголовком в BigTextStyle.
 */
object TaskNotifier {

    const val NOTIFICATION_ID = 42
    private const val CHANNEL_ID = "daily_tasks"

    fun notifyTodayTasks(context: Context, tasks: List<Task>) {
        createChannelIfNeeded(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val grouped = buildGroupedText(tasks)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                context.getString(R.string.notification_title, tasks.size)
            )
            .setContentText(firstLinePreview(tasks))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(grouped)
                    .setSummaryText(context.getString(R.string.notification_summary))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        if (NotificationManagerCompat.from(context)
                .areNotificationsEnabled()
        ) {
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Нет POST_NOTIFICATIONS — молча пропускаем.
            }
        }
    }

    /** Текст уведомления, сгруппированный по важности. */
    private fun buildGroupedText(tasks: List<Task>): String {
        val order = listOf(TaskImportance.HIGH, TaskImportance.MEDIUM, TaskImportance.LOW)
        return order.mapNotNull { importance ->
            val group = tasks.filter { it.importance == importance }
            if (group.isEmpty()) null
            else buildString {
                appendLine("${importance.title} (${group.size}):")
                group.forEach { task ->
                    appendLine("  ${task.hour}:${task.minute.toString().padStart(2, '0')} · ${task.title}")
                }
            }.trimEnd()
        }.joinToString("\n\n")
    }

    private fun firstLinePreview(tasks: List<Task>): String {
        val high = tasks.count { it.importance == TaskImportance.HIGH }
        return if (high > 0) {
            tasks.first { it.importance == TaskImportance.HIGH }.title
        } else {
            tasks.first().title
        }
    }

    private fun createChannelIfNeeded(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }
}
