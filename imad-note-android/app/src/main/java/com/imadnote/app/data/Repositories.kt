package com.imadnote.app.data

import android.net.Uri
import androidx.room.withTransaction
import com.imadnote.app.domain.Recurrence
import com.imadnote.app.domain.TaskRules
import com.imadnote.app.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class NoteRepository(
    private val db: AppDatabase,
    private val storage: AttachmentStorage,
) {
    private val dao = db.noteDao()

    fun observeActive(): Flow<List<NoteWithDetails>> = dao.observeActive()
    fun observeArchived(): Flow<List<NoteWithDetails>> = dao.observeArchived()
    fun observeByProject(projectId: String): Flow<List<NoteWithDetails>> = dao.observeByProject(projectId)
    fun observeTitles(): Flow<List<NoteTitle>> = dao.observeTitles()
    fun observeAttachments(noteId: String): Flow<List<AttachmentEntity>> = dao.observeAttachments(noteId)
    fun search(query: String): Flow<List<NoteWithDetails>> = dao.search(query)

    suspend fun get(id: String): NoteWithDetails? = dao.getWithDetails(id)
    suspend fun exists(id: String): Boolean = dao.count(id) > 0

    suspend fun save(note: NoteEntity, checklist: List<ChecklistItemEntity>) = dao.saveNote(note, checklist)

    suspend fun setPinned(id: String, pinned: Boolean) = dao.setPinned(id, pinned, now())
    suspend fun setArchived(id: String, archived: Boolean) = dao.setArchived(id, archived, now())

    /** Deletes the note, its checklist (cascade) and its image files. Linked tasks are kept and unlinked. */
    suspend fun delete(id: String) {
        val files = dao.attachmentsFor(id)
        dao.delete(id)
        files.forEach { storage.delete(it.fileName) }
    }

    suspend fun addImage(noteId: String, uri: Uri): AttachmentEntity =
        insertAttachment(noteId, storage.importUri(uri))

    suspend fun addCameraImage(noteId: String, file: File): AttachmentEntity =
        insertAttachment(noteId, storage.importCameraFile(file))

    private suspend fun insertAttachment(noteId: String, imported: AttachmentStorage.Imported): AttachmentEntity {
        val attachment = AttachmentEntity(
            noteId = noteId,
            fileName = imported.fileName,
            mimeType = imported.mimeType,
            createdAt = now(),
        )
        try {
            dao.insertAttachment(attachment)
        } catch (e: Exception) {
            storage.delete(imported.fileName)
            throw e
        }
        return attachment
    }

    suspend fun removeAttachment(attachment: AttachmentEntity) {
        dao.deleteAttachment(attachment.id)
        storage.delete(attachment.fileName)
    }

    fun fileFor(attachment: AttachmentEntity): File = storage.fileFor(attachment.fileName)

    fun newCameraFile(): File = storage.newCameraFile()

    suspend fun cleanOrphanFiles() = storage.cleanOrphans(dao.allAttachmentFileNames().toSet())
}

class TaskRepository(
    private val db: AppDatabase,
    private val scheduler: ReminderScheduler,
) {
    private val dao = db.taskDao()

    fun observeTopLevel(): Flow<List<TaskEntity>> = dao.observeTopLevel()
    fun observeByProject(projectId: String): Flow<List<TaskEntity>> = dao.observeByProject(projectId)
    fun observeByNote(noteId: String): Flow<List<TaskEntity>> = dao.observeByNote(noteId)
    fun observeSubtaskCounts(): Flow<List<SubtaskCount>> = dao.observeSubtaskCounts()
    fun search(query: String): Flow<List<TaskEntity>> = dao.search(query)

    suspend fun get(id: String): TaskEntity? = dao.get(id)
    suspend fun subtasks(parentId: String): List<TaskEntity> = dao.subtasksOf(parentId)

    /** Saves a task and replaces its subtasks with [subtasks] in one transaction, then (re)schedules its reminder. */
    suspend fun save(task: TaskEntity, subtasks: List<TaskEntity>) {
        db.withTransaction {
            dao.upsert(task)
            if (subtasks.isEmpty()) {
                dao.deleteAllSubtasks(task.id)
            } else {
                dao.deleteSubtasksExcept(task.id, subtasks.map { it.id })
                dao.upsertAll(subtasks.mapIndexed { i, s -> s.copy(parentId = task.id, position = i) })
            }
        }
        scheduler.schedule(task)
    }

    /**
     * Completing a recurring task marks this occurrence done and creates the next one,
     * so the history of completed occurrences is kept.
     */
    suspend fun toggleComplete(task: TaskEntity) {
        val now = now()
        if (task.isCompleted) {
            val reopened = task.copy(completedAt = null, updatedAt = now)
            dao.upsert(reopened)
            scheduler.schedule(reopened)
            return
        }
        val done = task.copy(completedAt = now, updatedAt = now)
        val zone = ZoneId.systemDefault()
        val nextDue = if (task.recurrence != Recurrence.NONE && task.dueAt != null) {
            TaskRules.nextOccurrence(task.dueAt, task.recurrence, zone, LocalDate.now(zone))
        } else {
            null
        }
        var next: TaskEntity? = null
        db.withTransaction {
            dao.upsert(done)
            if (nextDue != null) {
                next = task.copy(id = newId(), dueAt = nextDue, completedAt = null, createdAt = now, updatedAt = now)
                dao.upsert(next!!)
                val subs = dao.subtasksOf(task.id)
                if (subs.isNotEmpty()) {
                    dao.upsertAll(
                        subs.map { it.copy(id = newId(), parentId = next!!.id, completedAt = null, createdAt = now, updatedAt = now) },
                    )
                }
            }
        }
        scheduler.cancel(task.id)
        next?.let { scheduler.schedule(it) }
    }

    suspend fun delete(task: TaskEntity) {
        scheduler.cancel(task.id)
        dao.delete(task.id)
    }
}

class ProjectRepository(private val db: AppDatabase) {
    private val dao = db.projectDao()

    fun observeActive(): Flow<List<ProjectWithStats>> = dao.observeWithStats(archived = false)
    fun observeArchived(): Flow<List<ProjectWithStats>> = dao.observeWithStats(archived = true)
    fun observeOne(id: String): Flow<ProjectWithStats?> = dao.observeOne(id)
    fun observeList(): Flow<List<ProjectEntity>> = dao.observeActiveList()
    fun search(query: String): Flow<List<ProjectWithStats>> = dao.search(query)

    suspend fun get(id: String): ProjectEntity? = dao.get(id)
    suspend fun save(project: ProjectEntity) = dao.upsert(project)
    suspend fun setArchived(id: String, archived: Boolean) = dao.setArchived(id, archived, now())

    /** Notes and tasks are kept and simply unlinked (foreign keys use SET NULL). */
    suspend fun delete(id: String) = dao.delete(id)
}

internal fun now(): Long = System.currentTimeMillis()
