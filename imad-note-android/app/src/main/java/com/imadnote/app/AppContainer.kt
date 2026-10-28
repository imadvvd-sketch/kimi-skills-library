package com.imadnote.app

import android.content.Context
import com.imadnote.app.data.AppDatabase
import com.imadnote.app.data.AttachmentStorage
import com.imadnote.app.data.NoteRepository
import com.imadnote.app.data.ProjectRepository
import com.imadnote.app.data.QuickCaptureRepository
import com.imadnote.app.data.SettingsRepository
import com.imadnote.app.data.TaskRepository
import com.imadnote.app.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container; small enough that a DI framework would add more than it saves. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: AppDatabase = AppDatabase.build(context)
    val reminderScheduler = ReminderScheduler(context) { db.taskDao() }
    val notes = NoteRepository(db, AttachmentStorage(context))
    val tasks = TaskRepository(db, reminderScheduler)
    val projects = ProjectRepository(db)
    val settings = SettingsRepository(context)
    val quickCapture = QuickCaptureRepository(notes, tasks)
}
