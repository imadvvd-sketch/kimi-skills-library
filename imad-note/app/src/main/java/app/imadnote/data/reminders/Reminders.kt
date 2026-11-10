package app.imadnote.data.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.imadnote.ImadNoteApp
import app.imadnote.MainActivity
import app.imadnote.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Schedules local notifications. [key] identifies the owner, e.g. `task:<id>`; scheduling again replaces it. */
interface Reminders {
    fun schedule(key: String, at: Long, title: String, body: String)
    fun cancel(key: String)

    object None : Reminders {
        override fun schedule(key: String, at: Long, title: String, body: String) = Unit
        override fun cancel(key: String) = Unit
    }
}

/**
 * AlarmManager-backed reminders. Exact alarms are used when the user allowed them
 * (Settings › Alarms & reminders); otherwise Android may deliver within a few minutes.
 * Alarms are lost on reboot, so [BootReceiver] schedules them again from the database.
 */
class AlarmReminders(private val context: Context) : Reminders {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    private fun intent(key: String, title: String = "", body: String = "") =
        PendingIntent.getBroadcast(
            context, key.hashCode(),
            Intent(context, ReminderReceiver::class.java).setAction(ACTION).putExtra(EXTRA_KEY, key).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_BODY, body),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun schedule(key: String, at: Long, title: String, body: String) {
        if (at <= System.currentTimeMillis()) return cancel(key)
        val pending = intent(key, title, body)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    override fun cancel(key: String) = alarms.cancel(intent(key))

    companion object {
        const val ACTION = "app.imadnote.REMINDER"
        const val EXTRA_KEY = "key"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val CHANNEL = "reminders"

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_HIGH)
            )
        }

        fun canNotify(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!AlarmReminders.canNotify(context)) return
        AlarmReminders.ensureChannel(context)
        val key = intent.getStringExtra(AlarmReminders.EXTRA_KEY) ?: return
        val open = PendingIntent.getActivity(
            context, key.hashCode(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN, key).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, AlarmReminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(intent.getStringExtra(AlarmReminders.EXTRA_TITLE))
            .setContentText(intent.getStringExtra(AlarmReminders.EXTRA_BODY))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                (context.applicationContext as ImadNoteApp).container.rescheduleReminders()
            } finally {
                pending.finish()
            }
        }
    }
}
