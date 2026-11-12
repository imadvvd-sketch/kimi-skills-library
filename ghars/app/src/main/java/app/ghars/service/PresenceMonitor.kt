package app.ghars.service

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the device signals that [LeavePolicy] needs. No permissions required. */
@Singleton
class PresenceMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val power = context.getSystemService(PowerManager::class.java)
    private val keyguard = context.getSystemService(KeyguardManager::class.java)
    private val audio = context.getSystemService(AudioManager::class.java)

    val appInForeground: Boolean
        get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    fun isAway(): Boolean = LeavePolicy.isAway(
        appInForeground = appInForeground,
        screenInteractive = power.isInteractive,
        keyguardLocked = keyguard.isKeyguardLocked,
        // Ringing or talking, on the phone line or a calling app.
        inCall = audio.mode != AudioManager.MODE_NORMAL,
    )
}
