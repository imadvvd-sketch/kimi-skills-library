package app.ghars.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.ghars.domain.session.Clock
import app.ghars.domain.session.SessionState
import app.ghars.domain.session.SessionStateMachine
import app.ghars.session.SessionController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps a focus session alive while the app is not on screen.
 *
 * It does not own the timer (deadlines are absolute and live in [SessionController]);
 * it watches whether the user has left, shows the notifications, and wakes up at the deadline.
 */
@AndroidEntryPoint
class FocusService : Service() {

    @Inject lateinit var controller: SessionController
    @Inject lateinit var presence: PresenceMonitor
    @Inject lateinit var notifier: SessionNotifier
    @Inject lateinit var clock: Clock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var warningShown = false

    /** Android requires startForeground before a service started this way may stop. */
    private var foregroundStarted = false

    /** Screen and unlock changes are checked at once rather than on the next tick. */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            controller.tick(presence.isAway())
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scope.launch { controller.state.filterNotNull().collect(::onState) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must happen promptly on every start, including restarts by the system.
        val state = controller.state.value
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        val promoted = runCatching {
            ServiceCompat.startForeground(
                this, SessionNotifier.ID_ONGOING,
                notifier.ongoing(state, SessionStateMachine.remainingMs(state ?: SessionState.Idle, clock.elapsedMs())),
                type,
            )
        }.isSuccess
        foregroundStarted = true
        // The system may refuse (started from the background with nothing to show), or the
        // session may have ended between the request to start and now.
        if (!promoted || (state != null && state !is SessionState.Active)) {
            if (state != null) onState(state) else stopSelf()
            return START_NOT_STICKY
        }
        controller.tick(presence.isAway())
        return START_STICKY
    }

    private fun onState(state: SessionState) {
        val now = clock.elapsedMs()
        when (state) {
            is SessionState.Active -> {
                notifier.cancelResult()
                notifier.updateOngoing(state, SessionStateMachine.remainingMs(state, now))
                scheduleWakeUp(nextDeadline(state))
                val graceRunning = state is SessionState.Running && state.graceDeadline != null
                if (graceRunning && !warningShown) notifier.showLeaveWarning()
                if (!graceRunning && warningShown) notifier.cancelLeaveWarning()
                warningShown = graceRunning
                if (tickJob == null) tickJob = scope.launch {
                    while (isActive) {
                        controller.tick(presence.isAway())
                        delay(TICK_MS)
                    }
                }
            }
            is SessionState.Finished, SessionState.Idle -> {
                if (!foregroundStarted) return // onStartCommand will come back here
                tickJob?.cancel()
                tickJob = null
                cancelWakeUp()
                notifier.cancelLeaveWarning()
                warningShown = false
                // If they are looking at the app, the result screen says it all.
                if (state is SessionState.Finished && !presence.appInForeground) notifier.showResult(state)
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun nextDeadline(state: SessionState.Active): Long = when (state) {
        is SessionState.Running -> minOf(state.endsAt, state.graceDeadline ?: Long.MAX_VALUE)
        is SessionState.Paused -> state.resumeDeadline
    }

    /** With the screen off the tick loop sleeps; this alarm wakes us to finish on time. */
    private fun wakeUpIntent() = PendingIntent.getService(
        this, 0, Intent(this, FocusService::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun scheduleWakeUp(atElapsed: Long) {
        getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, wakeUpIntent())
    }

    private fun cancelWakeUp() = getSystemService(AlarmManager::class.java).cancel(wakeUpIntent())

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TICK_MS = 500L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FocusService::class.java))
        }
    }
}
