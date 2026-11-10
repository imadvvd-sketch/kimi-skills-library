package app.imadnote.data.repo

import androidx.room.withTransaction
import app.imadnote.core.Clock
import app.imadnote.core.newId
import app.imadnote.core.startOfDayMillis
import app.imadnote.data.db.AppDatabase
import app.imadnote.data.db.LabelEntity
import app.imadnote.data.db.ProjectEntity
import app.imadnote.data.db.TaskEntity
import app.imadnote.data.db.TaskLabelCrossRef
import app.imadnote.data.db.TaskStatus
import app.imadnote.data.reminders.Reminders
import app.imadnote.domain.tasks.Recurrence
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class TaskRepository(
    private val db: AppDatabase,
    private val indexer: SearchIndexer,
    private val reminders: Reminders,
    private val clock: Clock,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val tasks = db.tasks()
    private val labels = db.labels()

    fun observeAll(workspaceId: String): Flow<List<TaskEntity>> = tasks.observeAll(workspaceId)
    fun observe(id: String): Flow<TaskEntity?> = tasks.observe(id)
    fun observeLabels(workspaceId: String): Flow<List<LabelEntity>> = labels.observeAll(workspaceId)
    fun observeTaskLabels(): Flow<List<TaskLabelCrossRef>> = labels.observeCrossRefs()
    suspend fun get(id: String) = tasks.get(id)

    suspend fun create(
        workspaceId: String,
        title: String,
        projectId: String? = null,
        parentId: String? = null,
        dueDate: LocalDate? = null,
        priority: Int = 0,
        labelNames: List<String> = emptyList(),
    ): TaskEntity = db.withTransaction {
        val now = clock.now()
        val task = TaskEntity(
            id = newId(), workspaceId = workspaceId, projectId = projectId, parentId = parentId,
            title = title.trim(), priority = priority, dueDate = dueDate?.toEpochDay(),
            sortIndex = tasks.nextSortIndex(workspaceId), createdAt = now, updatedAt = now,
        )
        tasks.upsert(task)
        indexer.putTask(task)
        for (name in labelNames) labels.link(TaskLabelCrossRef(task.id, labelNamed(workspaceId, name).id))
        task
    }

    private suspend fun labelNamed(workspaceId: String, name: String): LabelEntity =
        labels.getAll().firstOrNull { it.workspaceId == workspaceId && it.name.equals(name, ignoreCase = true) }
            ?: LabelEntity(newId(), workspaceId, name.trim(), LABEL_COLORS[(name.hashCode() and 0x7fffffff) % LABEL_COLORS.size]).also { labels.upsert(it) }

    suspend fun update(task: TaskEntity) = db.withTransaction {
        val clean = task.copy(
            title = task.title.trim().ifEmpty { tasks.get(task.id)?.title ?: task.title },
            // A reminder only makes sense for an open task.
            completedAt = if (task.status == TaskStatus.DONE) task.completedAt ?: clock.now() else null,
            updatedAt = clock.now(),
        )
        tasks.upsert(clean)
        indexer.putTask(clean)
        syncReminder(clean)
    }

    suspend fun edit(id: String, transform: (TaskEntity) -> TaskEntity) = db.withTransaction {
        val current = tasks.get(id) ?: return@withTransaction
        val changed = transform(current)
        if (changed != current) update(changed)
    }

    /**
     * Completing a repeating task marks this occurrence done and creates the next one, carrying over
     * subtasks (unchecked), labels and the reminder offset. Returns the new occurrence, if any.
     */
    suspend fun setCompleted(id: String, done: Boolean): TaskEntity? = db.withTransaction {
        val task = tasks.get(id) ?: return@withTransaction null
        if (!done) {
            update(task.copy(status = TaskStatus.TODO, completedAt = null))
            return@withTransaction null
        }
        if (task.status == TaskStatus.DONE) return@withTransaction null
        update(task.copy(status = TaskStatus.DONE, completedAt = clock.now()))
        val rule = Recurrence.parse(task.recurrence)
        val due = task.dueDate?.let(LocalDate::ofEpochDay)
        if (rule == null || due == null || task.parentId != null) return@withTransaction null

        val nextDue = rule.next(due)
        val now = clock.now()
        val shift = nextDue.startOfDayMillis(zone()) - due.startOfDayMillis(zone())
        val next = task.copy(
            id = newId(), status = TaskStatus.TODO, completedAt = null, dueDate = nextDue.toEpochDay(),
            reminderAt = task.reminderAt?.plus(shift), createdAt = now, updatedAt = now,
        )
        tasks.upsert(next)
        indexer.putTask(next)
        syncReminder(next)
        for (sub in tasks.subtasks(task.id)) {
            val copy = sub.copy(id = newId(), parentId = next.id, status = TaskStatus.TODO, completedAt = null, createdAt = now, updatedAt = now)
            tasks.upsert(copy)
            indexer.putTask(copy)
        }
        for (ref in labels.getAllCrossRefs().filter { it.taskId == task.id }) labels.link(TaskLabelCrossRef(next.id, ref.labelId))
        next
    }

    suspend fun delete(id: String) = db.withTransaction {
        val ids = listOf(id) + tasks.subtasks(id).map { it.id }
        ids.forEach { reminders.cancel(key(it)) }
        indexer.remove(ids)
        tasks.delete(id) // subtasks and label links cascade
    }

    suspend fun setLabels(taskId: String, labelIds: Set<String>) = db.withTransaction {
        labels.clearForTask(taskId)
        labelIds.forEach { labels.link(TaskLabelCrossRef(taskId, it)) }
    }

    suspend fun createLabel(workspaceId: String, name: String): LabelEntity = db.withTransaction { labelNamed(workspaceId, name) }
    suspend fun deleteLabel(id: String) = labels.delete(id)

    private fun syncReminder(task: TaskEntity) {
        val at = task.reminderAt
        if (at == null || task.status == TaskStatus.DONE) reminders.cancel(key(task.id))
        else reminders.schedule(key(task.id), at, task.title, task.description.lineSequence().firstOrNull().orEmpty())
    }

    suspend fun rescheduleAll() {
        for (task in tasks.pendingReminders(clock.now())) syncReminder(task)
    }

    companion object {
        fun key(taskId: String) = "task:$taskId"
        val LABEL_COLORS = listOf(0xFF527DAA, 0xFF2E9D73, 0xFFE5A43B, 0xFFD9534F, 0xFF7B55C7, 0xFF3A9BB5, 0xFFB7791F, 0xFF62748A)
    }
}

class ProjectRepository(
    private val db: AppDatabase,
    private val indexer: SearchIndexer,
    private val clock: Clock,
) {
    private val projects = db.projects()

    fun observeAll(workspaceId: String): Flow<List<ProjectEntity>> = projects.observeAll(workspaceId)
    fun observe(id: String): Flow<ProjectEntity?> = projects.observe(id)

    suspend fun create(workspaceId: String, name: String, icon: String = "📁", color: Long = 0xFF527DAA, description: String = ""): ProjectEntity =
        db.withTransaction {
            val now = clock.now()
            val project = ProjectEntity(
                newId(), workspaceId, name.trim().ifEmpty { "Project" }, description, icon, color,
                sortIndex = projects.getAll().count { it.workspaceId == workspaceId }, createdAt = now, updatedAt = now,
            )
            projects.upsert(project)
            indexer.putProject(project)
            project
        }

    suspend fun edit(id: String, transform: (ProjectEntity) -> ProjectEntity) = db.withTransaction {
        val current = projects.get(id) ?: return@withTransaction
        val changed = transform(current).let { it.copy(name = it.name.trim().ifEmpty { current.name }, updatedAt = clock.now()) }
        projects.upsert(changed)
        indexer.putProject(changed)
    }

    /** Deletes the project; its tasks and notes stay and simply lose the link. */
    suspend fun delete(id: String) = db.withTransaction {
        indexer.remove(id)
        projects.delete(id)
    }
}

class EventRepository(
    private val db: AppDatabase,
    private val indexer: SearchIndexer,
    private val reminders: Reminders,
    private val clock: Clock,
) {
    private val events = db.events()

    fun observeBetween(workspaceId: String, from: Long, to: Long) = events.observeBetween(workspaceId, from, to)
    suspend fun get(id: String) = events.get(id)

    suspend fun save(event: app.imadnote.data.db.EventEntity): app.imadnote.data.db.EventEntity = db.withTransaction {
        val now = clock.now()
        val clean = event.copy(
            id = event.id.ifEmpty { newId() },
            title = event.title.trim(),
            endAt = maxOf(event.endAt, event.startAt),
            createdAt = events.get(event.id)?.createdAt ?: now,
            updatedAt = now,
        )
        events.upsert(clean)
        indexer.putEvent(clean)
        val at = clean.reminderAt
        if (at == null) reminders.cancel(key(clean.id)) else reminders.schedule(key(clean.id), at, clean.title, clean.notes)
        clean
    }

    suspend fun delete(id: String) = db.withTransaction {
        reminders.cancel(key(id))
        indexer.remove(id)
        events.delete(id)
    }

    suspend fun rescheduleAll() {
        for (event in events.getAll()) {
            val at = event.reminderAt ?: continue
            if (at > clock.now()) reminders.schedule(key(event.id), at, event.title, event.notes)
        }
    }

    companion object {
        fun key(eventId: String) = "event:$eventId"
    }
}
