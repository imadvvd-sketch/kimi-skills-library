package com.imadnote.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.imadnote.app.data.TaskDao
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.domain.TaskRules
import java.time.ZoneId

class ReminderScheduler(
    private val context: Context,
    private val taskDao: () -> TaskDao,
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(task: TaskEntity) {
        cancel(task.id)
        if (task.isCompleted || task.parentId != null) return
        val at = TaskRules.reminderTime(task.timing, ZoneId.systemDefault()) ?: return
        if (at <= System.currentTimeMillis()) return
        val pi = pendingIntent(task.id)
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: SecurityException) {
            // Exact-alarm access was revoked between the check and the call; fall back to inexact.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(taskId: String) {
        alarmManager.cancel(pendingIntent(taskId))
    }

    suspend fun rescheduleAll() {
        taskDao().withPendingReminders().forEach { schedule(it) }
    }

    private fun pendingIntent(taskId: String): PendingIntent {
        // The data URI makes each task's intent distinct, so alarms never overwrite each other.
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMIND)
            .setData(Uri.parse("imadnote://task/$taskId"))
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
