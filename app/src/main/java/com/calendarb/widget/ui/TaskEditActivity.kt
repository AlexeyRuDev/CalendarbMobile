package com.calendarb.widget.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.calendarb.widget.CalendarWidgetProvider
import com.calendarb.widget.R
import com.calendarb.widget.WidgetState
import com.calendarb.widget.data.Task
import com.calendarb.widget.data.TaskImportance
import com.calendarb.widget.data.TaskRepository
import com.calendarb.widget.databinding.ActivityTaskEditBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Форма добавления/редактирования задачи.
 * Минимально нужное: заголовок (обязателен), описание, дата+время, важность.
 */
class TaskEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskEditBinding
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

    private var editingTask: Task? = null
    private var selectedDate: LocalDate = LocalDate.now()
    private var hour: Int = 9
    private var minute: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (!intent.hasExtra(EXTRA_TASK_ID)) {
            val epochDay = intent.getLongExtra(EXTRA_DATE, -1L)
            if (epochDay >= 0) selectedDate = LocalDate.ofEpochDay(epochDay)
        }

        if (taskId >= 0) {
            supportActionBar?.title = getString(R.string.edit_task)
            lifecycleScope.launch {
                val task = withContext(Dispatchers.IO) {
                    TaskRepository.get(this@TaskEditActivity).findById(taskId)
                }
                if (task == null) {
                    finish()
                    return@launch
                }
                editingTask = task
                fillForm(task)
            }
        } else {
            supportActionBar?.title = getString(R.string.add_task)
            refreshDateTimeButtons()
        }

        binding.buttonDate.setOnClickListener { pickDate() }
        binding.buttonTime.setOnClickListener { pickTime() }
        binding.buttonSave.setOnClickListener { save() }
        binding.buttonDelete.setOnClickListener { delete() }
    }

    private fun fillForm(task: Task) {
        binding.inputTitle.setText(task.title)
        binding.inputDescription.setText(task.description.orEmpty())
        selectedDate = LocalDate.ofEpochDay(task.dateEpochDay)
        hour = task.hour
        minute = task.minute
        binding.groupImportance.check(
            when (task.importance) {
                TaskImportance.LOW -> R.id.radio_low
                TaskImportance.MEDIUM -> R.id.radio_medium
                TaskImportance.HIGH -> R.id.radio_high
            }
        )
        binding.buttonDelete.visibility = View.VISIBLE
        refreshDateTimeButtons()
    }

    private fun refreshDateTimeButtons() {
        binding.buttonDate.text = selectedDate.format(dateFormat)
        binding.buttonTime.text = String.format("%02d:%02d", hour, minute)
    }

    private fun pickDate() {
        DatePickerDialog(
            this,
            { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d)
                refreshDateTimeButtons()
            },
            selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
        ).show()
    }

    private fun pickTime() {
        TimePickerDialog(
            this,
            { _, h, min ->
                hour = h
                minute = min
                refreshDateTimeButtons()
            },
            hour, minute, true
        ).show()
    }

    private fun selectedImportance(): TaskImportance = when (binding.groupImportance.checkedRadioButtonId) {
        R.id.radio_high -> TaskImportance.HIGH
        R.id.radio_medium -> TaskImportance.MEDIUM
        else -> TaskImportance.LOW
    }

    private fun save() {
        val taskTitle = binding.inputTitle.text?.toString()?.trim().orEmpty()
        if (taskTitle.isEmpty()) {
            Toast.makeText(this, R.string.error_title_required, Toast.LENGTH_SHORT).show()
            return
        }
        val description = binding.inputDescription.text?.toString()?.trim()?.ifEmpty { null }
        val importance = selectedImportance()
        val repository = TaskRepository.get(this)
        val existing = editingTask

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (existing != null) {
                    repository.updateTask(
                        existing.copy(
                            title = taskTitle,
                            description = description,
                            dateEpochDay = selectedDate.toEpochDay(),
                            hour = hour,
                            minute = minute,
                            importance = importance
                        )
                    )
                } else {
                    repository.addTask(taskTitle, description, selectedDate, hour, minute, importance)
                }
            }
            afterDataChanged()
            finish()
        }
    }

    private fun delete() {
        val task = editingTask ?: return
        val repository = TaskRepository.get(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { repository.deleteTask(task) }
            afterDataChanged()
            finish()
        }
    }

    /** Перерисовываем виджет и уведомляем приложение об изменении данных. */
    private fun afterDataChanged() {
        CalendarWidgetProvider.invalidate(this)
        WidgetState.notifyDataChanged(this)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_DATE = "extra_date"
    }
}
