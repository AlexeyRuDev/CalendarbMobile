package com.calendarb.widget

import android.content.Context
import com.calendarb.widget.data.AppDatabase
import com.calendarb.widget.data.Task
import java.time.LocalDate

/**
 * Единая точка доступа к данным для компонентов, которые не могут использовать
 * DI (RemoteViewsService, AppWidgetProvider, BroadcastReceiver).
 * Все методы синхронные — вызывать только из фоновых потоков.
 */
object DataProvider {

    private var cachedAppContext: Context? = null

    /** Вызывается из [App].onCreate() и из Activity/Service на всякий случай. */
    fun init(context: Context) {
        cachedAppContext = context.applicationContext
    }

    private val appContext: Context
        get() = cachedAppContext
            ?: error("DataProvider not initialized")

    private val dao by lazy { AppDatabase.get(appContext).taskDao() }

    fun tasksForDay(date: LocalDate): List<Task> = dao.tasksForDay(date.toEpochDay())

    /** Даты (epochDay), на которые есть задачи — виджет отмечает их точками. */
    fun datesWithTasks(): Set<Long> = dao.datesWithTasks().toSet()

    /** Задачи на сегодня, сгруппированные по важности (убывание). */
    fun todayTasks(): List<Task> = dao.todayTasksSortedByImportance(LocalDate.now().toEpochDay())

    /** Задачи диапазона дней [fromDay; toDay] — для сетки виджета. */
    fun tasksInRange(fromDay: Long, toDay: Long): List<Task> = dao.tasksInRange(fromDay, toDay)

    fun findById(id: Long): Task? = dao.findById(id)
}
