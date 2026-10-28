package com.imadnote.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.imadnote.app.ImadNoteApp
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val container = (context.applicationContext as ImadNoteApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                val task = container.tasks.get(taskId)
                when (intent.action) {
                    ACTION_REMIND ->
                        if (task != null && !task.isCompleted) Notifications.showTaskReminder(context, task)
                    ACTION_COMPLETE -> {
                        if (task != null && !task.isCompleted) container.tasks.toggleComplete(task)
                        Notifications.cancel(context, taskId)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.imadnote.app.action.REMIND"
        const val ACTION_COMPLETE = "com.imadnote.app.action.COMPLETE"
        const val EXTRA_TASK_ID = "task_id"
    }
}

/** Re-registers alarms after events that clear them (reboot, app update, clock or permission changes). */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as ImadNoteApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.reminderScheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
