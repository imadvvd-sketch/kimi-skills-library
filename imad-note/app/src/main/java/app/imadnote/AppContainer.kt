package app.imadnote

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import app.imadnote.core.Clock
import app.imadnote.data.SettingsStore
import app.imadnote.data.backup.BackupManager
import app.imadnote.data.reminders.AlarmReminders
import app.imadnote.data.reminders.Reminders
import app.imadnote.data.repo.CollectionRepository
import app.imadnote.data.repo.EventRepository
import app.imadnote.data.repo.ProjectRepository
import app.imadnote.data.repo.TaskRepository
import app.imadnote.data.db.AppDatabase
import app.imadnote.data.repo.AttachmentRepository
import app.imadnote.data.repo.PageRepository
import app.imadnote.data.repo.SearchIndexer
import app.imadnote.data.repo.WorkspaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container. The object graph is small and has a single scope (the process),
 * so a DI framework would add build time and indirection without a benefit.
 */
class AppContainer(
    context: Context,
    val db: AppDatabase = AppDatabase.build(context),
    val settings: SettingsStore = SettingsStore(context),
    val clock: Clock = Clock.System,
    val reminders: Reminders = AlarmReminders(context.applicationContext),
) {
    /** Outlives screens; used for writes that must finish even if the user leaves, such as the final autosave. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val indexer = SearchIndexer(db.search())
    val workspaces = WorkspaceRepository(db, settings, clock)
    val pages = PageRepository(db, indexer, clock)
    val attachments = AttachmentRepository(context.applicationContext, db, indexer, clock)
    val backup = BackupManager(db, indexer, { attachments.directory }, clock)
    val tasks = TaskRepository(db, indexer, reminders, clock)
    val projects = ProjectRepository(db, indexer, clock)
    val events = EventRepository(db, indexer, reminders, clock)
    val collections = CollectionRepository(db, indexer, clock)

    suspend fun rescheduleReminders() {
        tasks.rescheduleAll()
        events.rescheduleAll()
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
