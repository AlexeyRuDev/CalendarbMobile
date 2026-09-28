package com.calendarb.widget.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.calendarb.widget.data.Task
import com.calendarb.widget.data.TaskRepository
import com.calendarb.widget.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Список задач выбранного дня. Открывается по тапу на дату в виджете
 * (extra [EXTRA_SELECTED_DATE]) или как обычная иконка приложения — тогда показываем сегодня.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskListAdapter
    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val epochDay = intent?.getLongExtra(EXTRA_SELECTED_DATE, -1L) ?: -1L
        if (epochDay >= 0) selectedDate = LocalDate.ofEpochDay(epochDay)

        adapter = TaskListAdapter { task -> openEditor(task.id) }
        binding.recyclerTasks.layoutManager = LinearLayoutManager(this)
        binding.recyclerTasks.adapter = adapter

        binding.fabAdd.setOnClickListener { openEditor(null) }
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {
        binding.textSelectedDate.text = selectedDate.format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
        ).replaceFirstChar { it.uppercase(Locale.getDefault()) }

        val repository = TaskRepository.get(this)
        lifecycleScope.launch {
            val tasks: List<Task> = withContext(Dispatchers.IO) {
                repository.tasksForDay(selectedDate)
            }
            adapter.submit(tasks)
            binding.textEmpty.visibility = if (tasks.isEmpty()) View.VISIBLE
            else View.GONE
        }
    }

    private fun openEditor(taskId: Long?) {
        val intent = Intent(this, TaskEditActivity::class.java).apply {
            if (taskId != null) putExtra(TaskEditActivity.EXTRA_TASK_ID, taskId)
            else putExtra(TaskEditActivity.EXTRA_DATE, selectedDate.toEpochDay())
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_SELECTED_DATE = "extra_selected_date"
    }
}
