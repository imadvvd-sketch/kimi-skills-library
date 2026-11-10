package app.imadnote.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.ForeignKey.Companion.SET_NULL
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// Conventions: ids are UUID strings; timestamps are epoch millis; calendar dates are epoch days.

@Serializable
@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val sortIndex: Int,
    val createdAt: Long,
)

enum class PageKind { PAGE, FOLDER }

@Serializable
@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(WorkspaceEntity::class, ["id"], ["workspaceId"], onDelete = CASCADE),
        ForeignKey(PageEntity::class, ["id"], ["parentId"], onDelete = CASCADE),
        ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = SET_NULL),
    ],
    indices = [Index("workspaceId"), Index("parentId"), Index("projectId")],
)
data class PageEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val parentId: String? = null,
    val projectId: String? = null,
    val kind: PageKind = PageKind.PAGE,
    val title: String = "",
    val icon: String? = null,
    val coverUri: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isInbox: Boolean = false,
    val isTrashed: Boolean = false,
    val sortIndex: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long? = null,
)

@Serializable
@Entity(
    tableName = "blocks",
    foreignKeys = [ForeignKey(PageEntity::class, ["id"], ["pageId"], onDelete = CASCADE)],
    indices = [Index("pageId", "sortIndex")],
)
data class BlockEntity(
    @PrimaryKey val id: String,
    val pageId: String,
    /** Stable type key, see [app.imadnote.domain.editor.BlockType]. */
    val type: String,
    val text: String = "",
    /** JSON list of inline format spans. */
    val spans: String = "[]",
    /** JSON object of type-specific properties (checked, language, url, table cells…). */
    val props: String = "{}",
    /** Nesting depth; children are the following blocks with a larger indent. */
    val indent: Int = 0,
    val sortIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class ProjectStatus { PLANNED, ACTIVE, ON_HOLD, DONE }

@Serializable
@Entity(
    tableName = "projects",
    foreignKeys = [ForeignKey(WorkspaceEntity::class, ["id"], ["workspaceId"], onDelete = CASCADE)],
    indices = [Index("workspaceId")],
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val description: String = "",
    val icon: String = "📁",
    val color: Long = 0xFF527DAA,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val startDate: Long? = null,
    val deadline: Long? = null,
    val sortIndex: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class TaskStatus { TODO, IN_PROGRESS, DONE }

@Serializable
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(WorkspaceEntity::class, ["id"], ["workspaceId"], onDelete = CASCADE),
        ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = SET_NULL),
        ForeignKey(TaskEntity::class, ["id"], ["parentId"], onDelete = CASCADE),
    ],
    indices = [Index("workspaceId"), Index("projectId"), Index("parentId"), Index("dueDate"), Index("completedAt")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val projectId: String? = null,
    /** Non-null for subtasks. */
    val parentId: String? = null,
    val title: String,
    val description: String = "",
    /** 0 none, 1 low, 2 medium, 3 high. */
    val priority: Int = 0,
    val status: TaskStatus = TaskStatus.TODO,
    /** Epoch day. */
    val dueDate: Long? = null,
    /** Minutes after midnight, only meaningful with [dueDate]. */
    val dueTime: Int? = null,
    val reminderAt: Long? = null,
    /** See [app.imadnote.domain.Recurrence] for the stored format. */
    val recurrence: String? = null,
    val completedAt: Long? = null,
    val sortIndex: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "labels",
    foreignKeys = [ForeignKey(WorkspaceEntity::class, ["id"], ["workspaceId"], onDelete = CASCADE)],
    indices = [Index("workspaceId")],
)
data class LabelEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val color: Long,
)

@Serializable
@Entity(
    tableName = "task_labels",
    primaryKeys = ["taskId", "labelId"],
    foreignKeys = [
        ForeignKey(TaskEntity::class, ["id"], ["taskId"], onDelete = CASCADE),
        ForeignKey(LabelEntity::class, ["id"], ["labelId"], onDelete = CASCADE),
    ],
    indices = [Index("labelId")],
)
data class TaskLabelCrossRef(val taskId: String, val labelId: String)

@Serializable
@Entity(
    tableName = "events",
    foreignKeys = [ForeignKey(WorkspaceEntity::class, ["id"], ["workspaceId"], onDelete = CASCADE)],
    indices = [Index("workspaceId"), Index("startAt")],
)
data class EventEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val title: String,
    val notes: String = "",
    val startAt: Long,
    val endAt: Long,
    val allDay: Boolean = false,
    val color: Long = 0xFF527DAA,
    val reminderAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

// ---- User-defined databases ("collections" in code to avoid clashing with the Room database) ----

@Serializable
@Entity(
    tableName = "collections",
    foreignKeys = [ForeignKey(WorkspaceEntity::class, ["id"], ["workspaceId"], onDelete = CASCADE)],
    indices = [Index("workspaceId")],
)
data class CollectionEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val icon: String = "🗂️",
    val description: String = "",
    val sortIndex: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class PropertyType {
    TITLE, TEXT, NUMBER, SELECT, MULTI_SELECT, STATUS, DATE, CHECKBOX, URL, EMAIL, PHONE,
    PERSON, RELATION, FORMULA, CREATED_TIME, LAST_EDITED_TIME,
}

@Serializable
@Entity(
    tableName = "properties",
    foreignKeys = [ForeignKey(CollectionEntity::class, ["id"], ["collectionId"], onDelete = CASCADE)],
    indices = [Index("collectionId")],
)
data class PropertyEntity(
    @PrimaryKey val id: String,
    val collectionId: String,
    val name: String,
    val type: PropertyType,
    /** JSON: select options, relation target collection, formula expression. */
    val config: String = "{}",
    val sortIndex: Int,
)

@Serializable
@Entity(
    tableName = "records",
    foreignKeys = [ForeignKey(CollectionEntity::class, ["id"], ["collectionId"], onDelete = CASCADE)],
    indices = [Index("collectionId")],
)
data class RecordEntity(
    @PrimaryKey val id: String,
    val collectionId: String,
    val sortIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "cells",
    primaryKeys = ["recordId", "propertyId"],
    foreignKeys = [
        ForeignKey(RecordEntity::class, ["id"], ["recordId"], onDelete = CASCADE),
        ForeignKey(PropertyEntity::class, ["id"], ["propertyId"], onDelete = CASCADE),
    ],
    indices = [Index("propertyId")],
)
data class CellEntity(val recordId: String, val propertyId: String, val value: String)

/** One row per link of a RELATION property; integrity is enforced on both ends. */
@Serializable
@Entity(
    tableName = "record_relations",
    primaryKeys = ["fromRecordId", "propertyId", "toRecordId"],
    foreignKeys = [
        ForeignKey(RecordEntity::class, ["id"], ["fromRecordId"], onDelete = CASCADE),
        ForeignKey(RecordEntity::class, ["id"], ["toRecordId"], onDelete = CASCADE),
        ForeignKey(PropertyEntity::class, ["id"], ["propertyId"], onDelete = CASCADE),
    ],
    indices = [Index("toRecordId"), Index("propertyId")],
)
data class RecordRelationEntity(val fromRecordId: String, val propertyId: String, val toRecordId: String)

enum class ViewType { TABLE, BOARD, CALENDAR, GALLERY, LIST }

@Serializable
@Entity(
    tableName = "collection_views",
    foreignKeys = [ForeignKey(CollectionEntity::class, ["id"], ["collectionId"], onDelete = CASCADE)],
    indices = [Index("collectionId")],
)
data class ViewEntity(
    @PrimaryKey val id: String,
    val collectionId: String,
    val name: String,
    val type: ViewType,
    /** JSON: filters, sorts, grouping, hidden properties. */
    val config: String = "{}",
    val sortIndex: Int,
)

enum class AttachmentOwner { PAGE, BLOCK, TASK, RECORD }

@Serializable
@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(WorkspaceEntity::class, ["id"], ["workspaceId"], onDelete = CASCADE)],
    indices = [Index("workspaceId"), Index("ownerType", "ownerId")],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val ownerType: AttachmentOwner,
    val ownerId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** Path relative to the app's private attachments directory. */
    val relativePath: String,
    val createdAt: Long,
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(@PrimaryKey val query: String, val searchedAt: Long)

/** Full-text index, maintained by the repositories on every write. */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61, notIndexed = ["kind", "refId", "workspaceId", "parentRef"])
@Entity(tableName = "search_index")
data class SearchIndexEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = 0,
    /** page, block, task, project, record, event, attachment. */
    val kind: String,
    val refId: String,
    val workspaceId: String,
    /** Id of the container to open (page for blocks, collection for records). */
    val parentRef: String,
    val title: String,
    val body: String,
)
