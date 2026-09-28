package com.calendarb.widget.data

/**
 * Важность задачи. Порядок объявления = порядок возрастания важности,
 * поэтому `ordinal` используется и для сортировки, и для группировки в уведомлении.
 */
enum class TaskImportance(val title: String) {
    LOW("Низкая"),
    MEDIUM("Средняя"),
    HIGH("Высокая")
}
