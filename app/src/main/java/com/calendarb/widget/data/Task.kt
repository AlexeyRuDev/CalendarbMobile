package com.calendarb.widget.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Задача. Хранится в Room.
 *
 * @param dateEpochDay  день в формате LocalDate.toEpochDay() — по нему виджет отмечает даты
 * @param timeOfDay     отсортированное время "HH:mm" для отображения и порядка задач
 */
@Entity(
    tableName = "tasks",
    indices = [Index("dateEpochDay"), Index("dateTimeSorted")]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String?,
    val dateEpochDay: Long,
    val hour: Int,
    val minute: Int,
    val importance: TaskImportance
) {
    /** Стабильный ключ сортировки по датам и времени. */
    val dateTimeSorted: Long
        get() = dateEpochDay * SEC_IN_DAY + hour * SEC_IN_HOUR + minute * SEC_IN_MIN

    companion object {
        const val SEC_IN_MIN = 60L
        const val SEC_IN_HOUR = 60L * SEC_IN_MIN
        const val SEC_IN_DAY = 24L * SEC_IN_HOUR
    }
}
