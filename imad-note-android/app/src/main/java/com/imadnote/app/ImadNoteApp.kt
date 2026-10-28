package com.imadnote.app

import android.app.Application
import android.content.Context
import com.imadnote.app.reminders.Notifications
import kotlinx.coroutines.launch

class ImadNoteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        // Notifications and other non-UI strings follow the in-app language too.
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.appScope.launch {
            runCatching { container.notes.cleanOrphanFiles() }
            runCatching { container.reminderScheduler.rescheduleAll() }
        }
    }
}
