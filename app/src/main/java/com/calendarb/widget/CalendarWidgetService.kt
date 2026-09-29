package com.calendarb.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Сервис, поставляющий [RemoteViews] для GridView виджета.
 * Каждая ячейка — это день месяца (или пустая ячейка-заполнитель в начале/конце).
 */
class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        DaysFactory(applicationContext)
}

/**
 * Фабрика ячеек календаря. Понедельник — первый день недели (ISO), как привычно
 * для русскоязычных пользователей. Дни с задачами помечаются точками под числом;
 * цвет точки соответствует максимальной важности задач на этот день.
 */
class DaysFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private data class DayCell(
        val date: LocalDate?,           // null → пустая заглушка
        val hasTasks: Boolean,
        val isToday: Boolean,
        val dotColorLow: Int,
        val dotColorMedium: Int,
        val dotColorHigh: Int
    )

    private var cells: List<DayCell> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        // Работа с БД идёт в фоновом потоке сервиса RemoteViews — здесь это допустимо.
        val month = WidgetState.displayedMonth(context)
        val datesWithTasks = DataProvider.datesWithTasks()

        val firstOfMonth = month.atDay(1)
        val leadingBlanks = (firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val totalCells = ((leadingBlanks + month.lengthOfMonth() + 6) / 7) * 7
        val today = LocalDate.now()

        val importanceByDay = buildMap<Long, Set<String>> {
            // Определяем цвета точек по важности задач на каждый день диапазона.
            val from = firstOfMonth.minusDays(leadingBlanks.toLong()).toEpochDay()
            val to = firstOfMonth.plusDays((totalCells - leadingBlanks).toLong()).toEpochDay()
            DataProvider.tasksInRange(from, to).groupBy { it.dateEpochDay }
                .forEach { (day, tasks) ->
                    put(day, tasks.map { it.importance.name }.toSet())
                }
        }

        cells = (0 until totalCells).map { index ->
            val dayNumber = index - leadingBlanks + 1
            if (dayNumber < 1 || dayNumber > month.lengthOfMonth()) {
                DayCell(null, false, false, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT)
            } else {
                val date = month.atDay(dayNumber)
                val imp = importanceByDay[date.toEpochDay()].orEmpty()
                DayCell(
                    date = date,
                    hasTasks = date.toEpochDay() in datesWithTasks,
                    isToday = date == today,
                    dotColorLow = if ("LOW" in imp) COLOR_LOW else Color.TRANSPARENT,
                    dotColorMedium = if ("MEDIUM" in imp) COLOR_MEDIUM else Color.TRANSPARENT,
                    dotColorHigh = if ("HIGH" in imp) COLOR_HIGH else Color.TRANSPARENT
                )
            }
        }
    }

    override fun getCount(): Int = cells.size

    override fun getViewAt(position: Int): RemoteViews {
        val cell = cells.getOrElse(position) { return RemoteViews(context.packageName, R.layout.widget_day_cell) }
        val views = RemoteViews(context.packageName, R.layout.widget_day_cell)

        if (cell.date == null) {
            views.setViewVisibility(R.id.day_number, View.INVISIBLE)
            hideDots(views)
            return views
        }

        views.setTextViewText(
            R.id.day_number,
            cell.date.format(DateTimeFormatter.ofPattern("d", Locale.getDefault()))
        )

        // Подсветка сегодняшнего дня и дней с задачами.
        when {
            cell.isToday -> views.setInt(R.id.day_root, "setBackgroundResource", R.drawable.bg_day_today)
            cell.hasTasks -> views.setInt(R.id.day_root, "setBackgroundResource", R.drawable.bg_day_has_tasks)
            else -> views.setInt(R.id.day_root, "setBackgroundResource", android.R.color.transparent)
        }
        views.setTextColor(
            R.id.day_number,
            if (cell.isToday) Color.parseColor("#FFFFFF") else Color.parseColor("#212121")
        )

        showDot(views, R.id.dot_low, cell.dotColorLow)
        showDot(views, R.id.dot_medium, cell.dotColorMedium)
        showDot(views, R.id.dot_high, cell.dotColorHigh)

        // Заполнение клика: открываем приложение на выбранную дату.
        val fillIn = Intent().putExtra(WidgetState.EXTRA_EPOCH_DAY, cell.date.toEpochDay())
        views.setOnClickFillInIntent(R.id.day_root, fillIn)

        return views
    }

    private fun showDot(views: RemoteViews, viewId: Int, color: Int) {
        if (color == Color.TRANSPARENT) {
            views.setViewVisibility(viewId, View.INVISIBLE)
        } else {
            views.setViewVisibility(viewId, View.VISIBLE)
            views.setInt(viewId, "setColorFilter", color)
        }
    }

    private fun hideDots(views: RemoteViews) {
        views.setViewVisibility(R.id.dot_low, View.INVISIBLE)
        views.setViewVisibility(R.id.dot_medium, View.INVISIBLE)
        views.setViewVisibility(R.id.dot_high, View.INVISIBLE)
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
    override fun onDestroy() {
        cells = emptyList()
    }

    companion object {
        val COLOR_LOW = Color.parseColor("#4CAF50")
        val COLOR_MEDIUM = Color.parseColor("#FFC107")
        val COLOR_HIGH = Color.parseColor("#F44336")
    }
}
