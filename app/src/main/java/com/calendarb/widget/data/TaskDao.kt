package com.calendarb.widget.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: Task): Long

    @Update
    fun update(task: Task)

    @Delete
    fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun findById(id: Long): Task?

    /** Все задачи указанного дня, отсортированные по времени. */
    @Query("SELECT * FROM tasks WHERE dateEpochDay = :epochDay ORDER BY dateTimeSorted ASC")
    fun tasksForDay(epochDay: Long): List<Task>

    /** Задачи диапазона дней [fromDay; toDay] — используются сеткой виджета. */
    @Query(
        "SELECT * FROM tasks WHERE dateEpochDay BETWEEN :fromDay AND :toDay " +
            "ORDER BY dateTimeSorted ASC"
    )
    fun tasksInRange(fromDay: Long, toDay: Long): List<Task>

    /** Только даты (epochDay), на которые есть хотя бы одна незавершённая задача. */
    @Query("SELECT DISTINCT dateEpochDay FROM tasks")
    fun datesWithTasks(): List<Long>

    /** Все задачи на сегодня — для утреннего уведомления. */
    @Query("SELECT * FROM tasks WHERE dateEpochDay = :epochDay ORDER BY importance DESC, dateTimeSorted ASC")
    fun todayTasksSortedByImportance(epochDay: Long): List<Task>

    @Query("SELECT COUNT(*) FROM tasks")
    fun count(): Int
}
