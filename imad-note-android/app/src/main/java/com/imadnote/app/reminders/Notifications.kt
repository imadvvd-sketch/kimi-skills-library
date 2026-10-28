package com.imadnote.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.imadnote.app.MainActivity
import com.imadnote.app.R
import com.imadnote.app.data.TaskEntity

object Notifications {
    private const val CHANNEL_REMINDERS = "reminders"

    fun createChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.channel_reminders_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showTaskReminder(context: Context, task: TaskEntity) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setData(Uri.parse("imadnote://task/${task.id}"))
                .putExtra(MainActivity.EXTRA_OPEN_TASK, task.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val complete = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_COMPLETE)
                .setData(Uri.parse("imadnote://task/${task.id}"))
                .putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(task.description.ifBlank { context.getString(R.string.reminder_default_text) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.action_mark_done), complete)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(task.id, 1, notification)
        } catch (_: SecurityException) {
            // Permission revoked while the reminder was in flight.
        }
    }

    fun cancel(context: Context, taskId: String) {
        NotificationManagerCompat.from(context).cancel(taskId, 1)
    }
}
