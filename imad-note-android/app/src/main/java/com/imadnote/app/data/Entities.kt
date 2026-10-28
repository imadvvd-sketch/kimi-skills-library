package com.imadnote.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.imadnote.app.domain.Recurrence
import com.imadnote.app.domain.TaskRules
import com.imadnote.app.domain.TaskTiming
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val description: String = "",
    val color: Int = 0,
    val deadline: Long? = null,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("projectId"), Index("updatedAt")],
)
data class NoteEntity(
    @PrimaryKey val id: String = newId(),
    val title: String = "",
    val content: String = "",
    val color: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val projectId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("noteId")],
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String = newId(),
    val noteId: String,
    val text: String,
    val isChecked: Boolean = false,
    val position: Int,
)

/** Image stored in app-private storage; the row is the only owner of the file. */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("noteId")],
)
data class AttachmentEntity(
    @PrimaryKey val id: String = newId(),
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val createdAt: Long,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("projectId"), Index("parentId"), Index("noteId"), Index("dueAt")],
)
data class TaskEntity(
    @PrimaryKey val id: String = newId(),
    val title: String,
    val description: String = "",
    val priority: Int = 0,
    val dueAt: Long? = null,
    val hasTime: Boolean = false,
    val reminderOffsetMin: Int? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val completedAt: Long? = null,
    val projectId: String? = null,
    val parentId: String? = null,
    val noteId: String? = null,
    val position: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isCompleted: Boolean get() = completedAt != null
    val timing: TaskTiming get() = TaskTiming(dueAt, hasTime, completedAt, reminderOffsetMin)
}

data class NoteWithDetails(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "id", entityColumn = "noteId")
    val checklist: List<ChecklistItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "noteId")
    val attachments: List<AttachmentEntity>,
) {
    val sortedChecklist: List<ChecklistItemEntity> get() = checklist.sortedBy { it.position }
    val sortedAttachments: List<AttachmentEntity> get() = attachments.sortedBy { it.createdAt }
}

data class ProjectWithStats(
    @Embedded val project: ProjectEntity,
    val totalTasks: Int,
    val doneTasks: Int,
) {
    val progress: Float get() = TaskRules.progress(doneTasks, totalTasks)
}

data class SubtaskCount(val parentId: String, val total: Int, val done: Int)

data class NoteTitle(val id: String, val title: String)
