package com.calendarb.widget.ui

import android.graphics.Color
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.calendarb.widget.data.Task
import com.calendarb.widget.data.TaskImportance
import com.calendarb.widget.databinding.ItemTaskBinding

/**
 * Адаптер списка задач дня. Цвет полоски слева = важность задачи.
 */
class TaskListAdapter(
    private val onClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskListAdapter.TaskViewHolder>() {

    private var items: List<Task> = emptyList()

    fun submit(tasks: List<Task>) {
        items = tasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.textTaskTitle.text = task.title
            binding.textTaskDescription.text = task.description.orEmpty()
            binding.textTaskDescription.visibility =
                if (task.description.isNullOrBlank()) View.GONE
                else View.VISIBLE
            binding.textTaskTime.text = String.format(
                "%02d:%02d · %s", task.hour, task.minute, task.importance.title
            )
            binding.viewImportanceStripe.setBackgroundColor(colorFor(task.importance))
            binding.root.setOnClickListener { onClick(task) }
        }

        private fun colorFor(importance: TaskImportance): Int = when (importance) {
            TaskImportance.LOW -> Color.parseColor("#4CAF50")
            TaskImportance.MEDIUM -> Color.parseColor("#FFC107")
            TaskImportance.HIGH -> Color.parseColor("#F44336")
        }
    }
}
