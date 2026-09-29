package com.calendarb.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.YearMonth

/**
 * Состояние и действия виджета. Отображаемый месяц хранится в SharedPreferences,
 * чтобы переживать перерисовки RemoteViews и перезапуски процесса.
 */
object WidgetState {

    const val ACTION_PREV_MONTH = "com.calendarb.widget.ACTION_PREV_MONTH"
    const val ACTION_NEXT_MONTH = "com.calendarb.widget.ACTION_NEXT_MONTH"
    const val ACTION_TODAY = "com.calendarb.widget.ACTION_TODAY"
    const val ACTION_DAY_CLICK = "com.calendarb.widget.ACTION_DAY_CLICK"
    const val ACTION_DATA_CHANGED = "com.calendarb.widget.ACTION_DATA_CHANGED"

    private const val PREFS = "calendar_widget_prefs"
    private const val KEY_YEAR = "displayed_year"
    private const val KEY_MONTH = "displayed_month"

    const val EXTRA_EPOCH_DAY = "extra_epoch_day"

    fun displayedMonth(context: Context): YearMonth {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val y = prefs.getInt(KEY_YEAR, LocalDate.now().year)
        val m = prefs.getInt(KEY_MONTH, LocalDate.now().monthValue)
        return YearMonth.of(y, m)
    }

    fun setDisplayedMonth(context: Context, month: YearMonth) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_YEAR, month.year)
            .putInt(KEY_MONTH, month.monthValue)
            .apply()
    }

    /** Перерисовать все экземпляры виджета на рабочем столе. */
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CalendarWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    /** Сообщить виджету, что данные изменились — обновить все экземпляры. */
    fun notifyDataChanged(context: Context) {
        val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = ACTION_DATA_CHANGED
        }
        // Явная broadcast-рассылка на компонент провайдера: ContextCompat.sendBroadcast
        // не существует, поэтому используем стандартный Context.sendBroadcast.
        context.sendBroadcast(intent)
    }
}
