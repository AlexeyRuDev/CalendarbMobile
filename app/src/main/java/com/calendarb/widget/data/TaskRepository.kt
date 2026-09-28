package com.calendarb.widget.data

import android.content.Context
import androidx.room.Room
import java.time.LocalDate

/**
 * Тонкая обёртка над DAO. Все методы синхронные и должны вызываться
 * из фоновых потоков (виджет-провайдер, будильник) или корутин (UI).
 */
class TaskRepository private constructor(context: Context) {

    private val dao: TaskDao = Room.databaseBuilder(
        context.applicationContext, AppDatabase::class.java, "calendarb.db"
    ).build().taskDao()

    fun addTask(
        title: String,
        description: String?,
        date: LocalDate,
        hour: Int,
        minute: Int,
        importance: TaskImportance
    ): Long = dao.insert(
        Task(
            title = title.trim(),
            description = description?.trim()?.ifEmpty { null },
            dateEpochDay = date.toEpochDay(),
            hour = hour,
            minute = minute,
            importance = importance
        )
    )

    fun updateTask(task: Task) = dao.update(task)

    fun deleteTask(task: Task) = dao.delete(task)

    fun findById(id: Long): Task? = dao.findById(id)

    fun tasksForDay(date: LocalDate): List<Task> = dao.tasksForDay(date.toEpochDay())

    /** Задачи диапазона дней [fromDay; toDay] — для сетки виджета. */
    fun tasksInRange(fromDay: Long, toDay: Long): List<Task> = dao.tasksInRange(fromDay, toDay)

    /** Даты, на которые есть задачи — виджет отмечает их точками. */
    fun datesWithTasks(): Set<Long> = dao.datesWithTasks().toSet()

    fun todayTasks(): List<Task> = dao.todayTasksSortedByImportance(LocalDate.now().toEpochDay())

    companion object {
        @Volatile
        private var instance: TaskRepository? = null

        fun get(context: Context): TaskRepository =
            instance ?: synchronized(this) {
                instance ?: TaskRepository(context).also { instance = it }
            }
    }
}
