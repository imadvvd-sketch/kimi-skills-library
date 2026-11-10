package app.imadnote.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces ORDER BY sortIndex, createdAt")
    fun observeAll(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces ORDER BY sortIndex, createdAt")
    suspend fun getAll(): List<WorkspaceEntity>

    @Query("SELECT * FROM workspaces WHERE id = :id")
    suspend fun get(id: String): WorkspaceEntity?

    @Query("SELECT COUNT(*) FROM workspaces")
    suspend fun count(): Int

    @Upsert suspend fun upsert(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE workspaceId = :workspaceId AND isTrashed = 0 ORDER BY sortIndex, createdAt")
    fun observeActive(workspaceId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE workspaceId = :workspaceId AND isTrashed = 1 ORDER BY updatedAt DESC")
    fun observeTrashed(workspaceId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE id = :id")
    fun observe(id: String): Flow<PageEntity?>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun get(id: String): PageEntity?

    @Query("SELECT * FROM pages WHERE parentId = :parentId ORDER BY sortIndex, createdAt")
    suspend fun children(parentId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE projectId = :projectId AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun observeByProject(projectId: String): Flow<List<PageEntity>>

    @Query(
        "SELECT COALESCE(MAX(sortIndex), -1) + 1 FROM pages WHERE workspaceId = :workspaceId " +
            "AND ((:parentId IS NULL AND parentId IS NULL) OR parentId = :parentId)"
    )
    suspend fun nextSortIndex(workspaceId: String, parentId: String?): Int

    @Upsert suspend fun upsert(page: PageEntity)

    @Upsert suspend fun upsertAll(pages: List<PageEntity>)

    @Query("UPDATE pages SET lastOpenedAt = :at WHERE id = :id")
    suspend fun markOpened(id: String, at: Long)

    @Query("UPDATE pages SET isTrashed = :trashed, updatedAt = :at WHERE id IN (:ids)")
    suspend fun setTrashed(ids: List<String>, trashed: Boolean, at: Long)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM pages")
    suspend fun getAll(): List<PageEntity>
}

@Dao
interface BlockDao {
    @Query("SELECT * FROM blocks WHERE pageId = :pageId ORDER BY sortIndex")
    suspend fun forPage(pageId: String): List<BlockEntity>

    @Query("SELECT * FROM blocks WHERE pageId = :pageId ORDER BY sortIndex")
    fun observeForPage(pageId: String): Flow<List<BlockEntity>>

    @Query("SELECT id FROM blocks WHERE pageId = :pageId")
    suspend fun idsForPage(pageId: String): List<String>

    @Upsert suspend fun upsertAll(blocks: List<BlockEntity>)

    @Query("DELETE FROM blocks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM blocks")
    suspend fun getAll(): List<BlockEntity>
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE workspaceId = :workspaceId ORDER BY sortIndex, createdAt")
    fun observeAll(workspaceId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observe(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun get(id: String): ProjectEntity?

    @Upsert suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<ProjectEntity>

    @Upsert suspend fun upsertAll(projects: List<ProjectEntity>)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE workspaceId = :workspaceId ORDER BY sortIndex, createdAt")
    fun observeAll(workspaceId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observe(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE parentId = :parentId ORDER BY sortIndex, createdAt")
    suspend fun subtasks(parentId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE reminderAt IS NOT NULL AND reminderAt > :after AND completedAt IS NULL")
    suspend fun pendingReminders(after: Long): List<TaskEntity>

    @Query("SELECT COALESCE(MAX(sortIndex), -1) + 1 FROM tasks WHERE workspaceId = :workspaceId")
    suspend fun nextSortIndex(workspaceId: String): Int

    @Upsert suspend fun upsert(task: TaskEntity)

    @Upsert suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>
}

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels WHERE workspaceId = :workspaceId ORDER BY name COLLATE NOCASE")
    fun observeAll(workspaceId: String): Flow<List<LabelEntity>>

    @Query("SELECT * FROM task_labels")
    fun observeCrossRefs(): Flow<List<TaskLabelCrossRef>>

    @Upsert suspend fun upsert(label: LabelEntity)

    @Query("DELETE FROM labels WHERE id = :id")
    suspend fun delete(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(ref: TaskLabelCrossRef)

    @Delete suspend fun unlink(ref: TaskLabelCrossRef)

    @Query("DELETE FROM task_labels WHERE taskId = :taskId")
    suspend fun clearForTask(taskId: String)

    @Query("SELECT * FROM labels")
    suspend fun getAll(): List<LabelEntity>

    @Query("SELECT * FROM task_labels")
    suspend fun getAllCrossRefs(): List<TaskLabelCrossRef>

    @Upsert suspend fun upsertAll(labels: List<LabelEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkAll(refs: List<TaskLabelCrossRef>)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE workspaceId = :workspaceId AND startAt < :to AND endAt >= :from ORDER BY startAt")
    fun observeBetween(workspaceId: String, from: Long, to: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: String): EventEntity?

    @Upsert suspend fun upsert(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM events")
    suspend fun getAll(): List<EventEntity>

    @Upsert suspend fun upsertAll(events: List<EventEntity>)
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections WHERE workspaceId = :workspaceId ORDER BY sortIndex, createdAt")
    fun observeAll(workspaceId: String): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observe(id: String): Flow<CollectionEntity?>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun get(id: String): CollectionEntity?

    @Upsert suspend fun upsert(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM properties WHERE collectionId = :collectionId ORDER BY sortIndex")
    fun observeProperties(collectionId: String): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE collectionId = :collectionId ORDER BY sortIndex")
    suspend fun properties(collectionId: String): List<PropertyEntity>

    @Upsert suspend fun upsertProperty(property: PropertyEntity)

    @Query("DELETE FROM properties WHERE id = :id")
    suspend fun deleteProperty(id: String)

    @Query("SELECT * FROM records WHERE collectionId = :collectionId ORDER BY sortIndex, createdAt")
    fun observeRecords(collectionId: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE collectionId = :collectionId ORDER BY sortIndex, createdAt")
    suspend fun records(collectionId: String): List<RecordEntity>

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun record(id: String): RecordEntity?

    @Upsert suspend fun upsertRecord(record: RecordEntity)

    @Query("UPDATE records SET updatedAt = :at WHERE id = :id")
    suspend fun touchRecord(id: String, at: Long)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecord(id: String)

    @Query("SELECT c.* FROM cells c JOIN records r ON r.id = c.recordId WHERE r.collectionId = :collectionId")
    fun observeCells(collectionId: String): Flow<List<CellEntity>>

    @Query("SELECT * FROM cells WHERE recordId = :recordId")
    suspend fun cellsForRecord(recordId: String): List<CellEntity>

    @Upsert suspend fun upsertCell(cell: CellEntity)

    @Upsert suspend fun upsertCells(cells: List<CellEntity>)

    @Query("DELETE FROM cells WHERE recordId = :recordId AND propertyId = :propertyId")
    suspend fun deleteCell(recordId: String, propertyId: String)

    @Query(
        "SELECT rr.* FROM record_relations rr JOIN records r ON r.id = rr.fromRecordId " +
            "WHERE r.collectionId = :collectionId"
    )
    fun observeRelations(collectionId: String): Flow<List<RecordRelationEntity>>

    @Query("SELECT * FROM record_relations WHERE fromRecordId = :recordId")
    suspend fun relationsFrom(recordId: String): List<RecordRelationEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(relation: RecordRelationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkAll(relations: List<RecordRelationEntity>)

    @Delete suspend fun unlink(relation: RecordRelationEntity)

    @Query("SELECT * FROM collection_views WHERE collectionId = :collectionId ORDER BY sortIndex")
    fun observeViews(collectionId: String): Flow<List<ViewEntity>>

    @Query("SELECT * FROM collection_views WHERE collectionId = :collectionId ORDER BY sortIndex")
    suspend fun views(collectionId: String): List<ViewEntity>

    @Upsert suspend fun upsertView(view: ViewEntity)

    @Query("DELETE FROM collection_views WHERE id = :id")
    suspend fun deleteView(id: String)

    /** Titles of every record in the workspace; used to show relations. */
    @Query(
        "SELECT r.id AS recordId, r.collectionId AS collectionId, c.value AS title FROM records r " +
            "JOIN collections col ON col.id = r.collectionId " +
            "LEFT JOIN properties p ON p.collectionId = r.collectionId AND p.type = 'TITLE' " +
            "LEFT JOIN cells c ON c.recordId = r.id AND c.propertyId = p.id WHERE col.workspaceId = :workspaceId"
    )
    fun observeRecordTitles(workspaceId: String): Flow<List<RecordTitle>>

    @Query("SELECT * FROM record_relations WHERE propertyId = :propertyId")
    suspend fun relationsForProperty(propertyId: String): List<RecordRelationEntity>

    @Query("DELETE FROM record_relations WHERE fromRecordId = :recordId AND propertyId = :propertyId")
    suspend fun clearRelations(recordId: String, propertyId: String)

    @Query("DELETE FROM cells WHERE propertyId = :propertyId")
    suspend fun clearCells(propertyId: String)

    @Query("SELECT COUNT(*) FROM records WHERE collectionId = :collectionId")
    fun observeRecordCount(collectionId: String): Flow<Int>

    // Whole-table reads/writes for backup and restore.
    @Query("SELECT * FROM collections") suspend fun allCollections(): List<CollectionEntity>
    @Query("SELECT * FROM properties") suspend fun allProperties(): List<PropertyEntity>
    @Query("SELECT * FROM records") suspend fun allRecords(): List<RecordEntity>
    @Query("SELECT * FROM cells") suspend fun allCells(): List<CellEntity>
    @Query("SELECT * FROM record_relations") suspend fun allRelations(): List<RecordRelationEntity>
    @Query("SELECT * FROM collection_views") suspend fun allViews(): List<ViewEntity>
    @Upsert suspend fun upsertCollections(items: List<CollectionEntity>)
    @Upsert suspend fun upsertProperties(items: List<PropertyEntity>)
    @Upsert suspend fun upsertRecords(items: List<RecordEntity>)
    @Upsert suspend fun upsertViews(items: List<ViewEntity>)
}

data class RecordTitle(val recordId: String, val collectionId: String, val title: String?)

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun observeAll(workspaceId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt")
    fun observeFor(ownerType: AttachmentOwner, ownerId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun get(id: String): AttachmentEntity?

    @Upsert suspend fun upsert(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM attachments")
    suspend fun getAll(): List<AttachmentEntity>

    @Upsert suspend fun upsertAll(items: List<AttachmentEntity>)
}

data class SearchHit(
    val kind: String,
    val refId: String,
    val parentRef: String,
    val title: String,
    val snippet: String,
)

@Dao
interface SearchDao {
    @Insert suspend fun insert(entry: SearchIndexEntity)

    @Insert suspend fun insertAll(entries: List<SearchIndexEntity>)

    @Query("DELETE FROM search_index WHERE refId = :refId")
    suspend fun deleteByRef(refId: String)

    @Query("DELETE FROM search_index WHERE refId IN (:refIds)")
    suspend fun deleteByRefs(refIds: List<String>)

    @Query("DELETE FROM search_index WHERE parentRef = :parentRef")
    suspend fun deleteByParent(parentRef: String)

    @Query("DELETE FROM search_index")
    suspend fun clear()

    /** [query] must be an FTS MATCH expression; \u0002 and \u0003 delimit the matched terms in the snippet. */
    @Query(
        "SELECT kind, refId, parentRef, title, " +
            "snippet(search_index, char(2), char(3), '…', -1, 12) AS snippet " +
            "FROM search_index WHERE search_index MATCH :query AND workspaceId = :workspaceId LIMIT :limit"
    )
    suspend fun search(workspaceId: String, query: String, limit: Int = 60): List<SearchHit>

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun observeHistory(limit: Int = 12): Flow<List<SearchHistoryEntity>>

    @Upsert suspend fun remember(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun forget(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}
