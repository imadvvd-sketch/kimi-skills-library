package com.imadnote.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE projectId = :projectId AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeByProject(projectId: String): Flow<List<NoteWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM notes
        WHERE title LIKE '%' || :q || '%'
           OR content LIKE '%' || :q || '%'
           OR id IN (SELECT noteId FROM checklist_items WHERE text LIKE '%' || :q || '%')
        ORDER BY isArchived ASC, updatedAt DESC
        LIMIT 100
        """,
    )
    fun search(q: String): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getWithDetails(id: String): NoteWithDetails?

    @Query("SELECT id, title FROM notes WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun observeTitles(): Flow<List<NoteTitle>>

    @Query("SELECT COUNT(*) FROM notes WHERE id = :id")
    suspend fun count(id: String): Int

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, now: Long)

    @Query("UPDATE notes SET isArchived = :archived, isPinned = 0, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun clearChecklist(noteId: String)

    @Insert
    suspend fun insertChecklist(items: List<ChecklistItemEntity>)

    /** Upsert (not REPLACE) so the note row is never deleted and children are not cascaded away. */
    @Transaction
    suspend fun saveNote(note: NoteEntity, checklist: List<ChecklistItemEntity>) {
        upsert(note)
        clearChecklist(note.id)
        if (checklist.isNotEmpty()) insertChecklist(checklist)
    }

    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachment(id: String)

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt")
    suspend fun attachmentsFor(noteId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt")
    fun observeAttachments(noteId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT fileName FROM attachments")
    suspend fun allAttachmentFileNames(): List<String>
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE parentId IS NULL ORDER BY position, createdAt")
    fun observeTopLevel(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND projectId = :projectId ORDER BY position, createdAt")
    fun observeByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND noteId = :noteId ORDER BY completedAt IS NOT NULL, createdAt")
    fun observeByNote(noteId: String): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT parentId, COUNT(*) AS total, SUM(CASE WHEN completedAt IS NOT NULL THEN 1 ELSE 0 END) AS done
        FROM tasks WHERE parentId IS NOT NULL GROUP BY parentId
        """,
    )
    fun observeSubtaskCounts(): Flow<List<SubtaskCount>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE title LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%'
        ORDER BY completedAt IS NOT NULL, updatedAt DESC
        LIMIT 100
        """,
    )
    fun search(q: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE parentId = :parentId ORDER BY position, createdAt")
    suspend fun subtasksOf(parentId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE completedAt IS NULL AND reminderOffsetMin IS NOT NULL AND dueAt IS NOT NULL")
    suspend fun withPendingReminders(): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks WHERE parentId = :parentId AND id NOT IN (:keepIds)")
    suspend fun deleteSubtasksExcept(parentId: String, keepIds: List<String>)

    @Query("DELETE FROM tasks WHERE parentId = :parentId")
    suspend fun deleteAllSubtasks(parentId: String)
}

@Dao
interface ProjectDao {
    @Query(
        """
        SELECT p.*,
          (SELECT COUNT(*) FROM tasks t WHERE t.projectId = p.id AND t.parentId IS NULL) AS totalTasks,
          (SELECT COUNT(*) FROM tasks t WHERE t.projectId = p.id AND t.parentId IS NULL AND t.completedAt IS NOT NULL) AS doneTasks
        FROM projects p
        WHERE p.isArchived = :archived
        ORDER BY CASE WHEN p.deadline IS NULL THEN 1 ELSE 0 END, p.deadline, p.updatedAt DESC
        """,
    )
    fun observeWithStats(archived: Boolean): Flow<List<ProjectWithStats>>

    @Query(
        """
        SELECT p.*,
          (SELECT COUNT(*) FROM tasks t WHERE t.projectId = p.id AND t.parentId IS NULL) AS totalTasks,
          (SELECT COUNT(*) FROM tasks t WHERE t.projectId = p.id AND t.parentId IS NULL AND t.completedAt IS NOT NULL) AS doneTasks
        FROM projects p WHERE p.id = :id
        """,
    )
    fun observeOne(id: String): Flow<ProjectWithStats?>

    @Query(
        """
        SELECT p.*,
          (SELECT COUNT(*) FROM tasks t WHERE t.projectId = p.id AND t.parentId IS NULL) AS totalTasks,
          (SELECT COUNT(*) FROM tasks t WHERE t.projectId = p.id AND t.parentId IS NULL AND t.completedAt IS NOT NULL) AS doneTasks
        FROM projects p
        WHERE p.name LIKE '%' || :q || '%' OR p.description LIKE '%' || :q || '%'
        ORDER BY p.isArchived, p.updatedAt DESC LIMIT 50
        """,
    )
    fun search(q: String): Flow<List<ProjectWithStats>>

    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActiveList(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun get(id: String): ProjectEntity?

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Query("UPDATE projects SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)
}
