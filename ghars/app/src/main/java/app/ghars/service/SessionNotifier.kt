package app.ghars.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.ghars.MainActivity
import app.ghars.R
import app.ghars.core.designsystem.GharsLightColors
import app.ghars.domain.session.SessionState
import app.ghars.feature.tree.SpeciesCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.ui.graphics.toArgb

@Singleton
class SessionNotifier @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SESSION, context.getString(R.string.channel_session), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    private fun base(channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_sprout)
        .setColor(GharsLightColors.brand.toArgb())
        .setContentIntent(
            PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )

    /**
     * The permanent notification of the foreground service.
     * The countdown is drawn by the system, so it keeps moving without us updating it.
     */
    fun ongoing(state: SessionState?, remainingMs: Long): Notification {
        val builder = base(CHANNEL_SESSION)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        when (state) {
            is SessionState.Running -> builder
                .setContentTitle(context.getString(R.string.session_growing))
                .setContentText(context.getString(R.string.notification_time_left))
                .setWhen(System.currentTimeMillis() + remainingMs)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
            is SessionState.Paused -> builder
                .setContentTitle(context.getString(R.string.session_paused))
                .setContentText(minutesLeft(remainingMs).let { context.resources.getQuantityString(R.plurals.minutes_left, it, it) })
            else -> builder.setContentTitle(context.getString(R.string.app_name))
        }
        return builder.build()
    }

    fun updateOngoing(state: SessionState, remainingMs: Long) =
        manager.notify(ID_ONGOING, ongoing(state, remainingMs))

    fun showLeaveWarning() = manager.notify(
        ID_WARNING,
        base(CHANNEL_ALERTS)
            .setContentTitle(context.getString(R.string.warning_title))
            .setContentText(context.getString(R.string.warning_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter(WARNING_TIMEOUT_MS)
            .build(),
    )

    fun cancelLeaveWarning() = manager.cancel(ID_WARNING)

    fun showResult(finished: SessionState.Finished) {
        val text = if (finished is SessionState.Success) {
            context.getString(SpeciesCatalog.get(finished.session.speciesId).grownMessage)
        } else {
            context.getString(R.string.result_withered)
        }
        manager.notify(
            ID_RESULT,
            base(CHANNEL_ALERTS).setContentTitle(text).setAutoCancel(true).build(),
        )
    }

    fun cancelResult() = manager.cancel(ID_RESULT)

    companion object {
        const val ID_ONGOING = 1
        private const val ID_WARNING = 2
        private const val ID_RESULT = 3
        private const val CHANNEL_SESSION = "session"
        private const val CHANNEL_ALERTS = "alerts"
        private const val WARNING_TIMEOUT_MS = 15_000L

        /** Whole minutes left, rounded up so "1 minute" shows until the very end. */
        fun minutesLeft(remainingMs: Long): Int = ((remainingMs + 59_999) / 60_000).toInt()
    }
}
