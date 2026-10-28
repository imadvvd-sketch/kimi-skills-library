package com.imadnote.app

import android.app.Application
import com.imadnote.app.reminders.Notifications
import kotlinx.coroutines.launch

class ImadNoteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.appScope.launch {
            runCatching { container.notes.cleanOrphanFiles() }
            runCatching { container.reminderScheduler.rescheduleAll() }
        }
    }
}
