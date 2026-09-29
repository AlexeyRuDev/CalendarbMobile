package com.calendarb.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.calendarb.widget.notification.ReminderScheduler
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Виджет «Календарь с задачами».
 *
 * Сетка дней собирается через [CalendarWidgetService] (GridView + RemoteViewsFactory),
 * потому что App Widget не умеет вложенные списки напрямую. Задачи отмечаются точками:
 * дата подсвечивается, если на неё есть хотя бы одна задача.
 */
class CalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // При первом обновлении планируем ежедневное напоминание на 8:00.
        ReminderScheduler.scheduleDailyReminder(context)

        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Клик по дню приходит только сюда (в onUpdate попадёт ACTION_DAY_CLICK с null-ids).
        if (intent.action == WidgetState.ACTION_DAY_CLICK) {
            val epochDay = intent.getLongExtra(WidgetState.EXTRA_EPOCH_DAY, -1L)
            if (epochDay >= 0) openDay(context, LocalDate.ofEpochDay(epochDay))
            return
        }

        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CalendarWidgetProvider::class.java))

        when (intent.action) {
            WidgetState.ACTION_PREV_MONTH -> {
                WidgetState.setDisplayedMonth(context, WidgetState.displayedMonth(context).minusMonths(1))
                refreshAll(context, manager, ids)
            }
            WidgetState.ACTION_NEXT_MONTH -> {
                WidgetState.setDisplayedMonth(context, WidgetState.displayedMonth(context).plusMonths(1))
                refreshAll(context, manager, ids)
            }
            WidgetState.ACTION_TODAY -> {
                WidgetState.setDisplayedMonth(context, YearMonth.now())
                refreshAll(context, manager, ids)
            }
            WidgetState.ACTION_DATA_CHANGED -> refreshAll(context, manager, ids)
            else -> Unit
        }
    }

    private fun refreshAll(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateWidget(context, manager, id)
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val month = WidgetState.displayedMonth(context)
        val views = RemoteViews(context.packageName, R.layout.widget_calendar)

        views.setTextViewText(
            R.id.widget_month_title,
            month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
                .replaceFirstChar { it.uppercase(Locale.getDefault()) }
        )

        // Привязываем сетку к сервису-фабрике. Uri уникален для виджета,
        // чтобы система не переиспользовала кэш другой инстанции.
        val serviceIntent = Intent(context, CalendarWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_days_grid, serviceIntent)
        views.setEmptyView(R.id.widget_days_grid, R.id.widget_empty)

        // Тап по пустой области дня → открыть MainActivity на эту дату.
        val clickTemplate = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = WidgetState.ACTION_DAY_CLICK
            // Extra подставляется динамически фабрикой через setOnClickFillInIntent.
        }
        val pendingIntentFlags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or
            android.app.PendingIntent.FLAG_MUTABLE
        val clickPI = android.app.PendingIntent.getBroadcast(
            context, 0, clickTemplate, pendingIntentFlags
        )
        views.setPendingIntentTemplate(R.id.widget_days_grid, clickPI)

        navButton(context, views, R.id.widget_prev, WidgetState.ACTION_PREV_MONTH, 101)
        navButton(context, views, R.id.widget_next, WidgetState.ACTION_NEXT_MONTH, 102)
        navButton(context, views, R.id.widget_today, WidgetState.ACTION_TODAY, 103)

        // Кнопка «плюс» — быстрый способ добавить задачу прямо из виджета.
        val addIntent = Intent(context, com.calendarb.widget.ui.TaskEditActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(com.calendarb.widget.ui.TaskEditActivity.EXTRA_DATE, LocalDate.now().toEpochDay())
        }
        views.setOnClickPendingIntent(
            R.id.widget_add_button,
            android.app.PendingIntent.getActivity(
                context, 104, addIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        )

        manager.updateAppWidget(widgetId, views)
        manager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_days_grid)
    }

    private fun navButton(
        context: Context,
        views: RemoteViews,
        viewId: Int,
        action: String,
        requestCode: Int
    ) {
        val intent = Intent(context, CalendarWidgetProvider::class.java).apply { this.action = action }
        val pi = android.app.PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(viewId, pi)
    }

    private fun openDay(context: Context, date: LocalDate) {
        val intent = Intent(context, com.calendarb.widget.ui.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(com.calendarb.widget.ui.MainActivity.EXTRA_SELECTED_DATE, date.toEpochDay())
        }
        context.startActivity(intent)
    }

    companion object {
        /** Вызывается после изменения данных (добавили/удалили задачу). */
        fun invalidate(context: Context) {
            WidgetState.updateAll(context)
        }
    }
}
