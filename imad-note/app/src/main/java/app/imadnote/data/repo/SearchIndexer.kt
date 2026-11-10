package app.imadnote.data.repo

import app.imadnote.data.db.AppDatabase
import app.imadnote.data.db.AttachmentEntity
import app.imadnote.data.db.BlockEntity
import app.imadnote.data.db.EventEntity
import app.imadnote.data.db.ProjectEntity
import app.imadnote.data.db.PropertyType
import app.imadnote.data.db.SearchDao
import app.imadnote.data.db.SearchIndexEntity
import app.imadnote.data.db.TaskEntity
import app.imadnote.domain.editor.BlockJson
import app.imadnote.domain.editor.BlockType

/** Keeps the FTS table in step with content. Call inside the same transaction as the write. */
class SearchIndexer(private val dao: SearchDao) {
    suspend fun put(kind: String, refId: String, workspaceId: String, parentRef: String, title: String, body: String = "") {
        dao.deleteByRef(refId)
        if (title.isBlank() && body.isBlank()) return
        dao.insert(SearchIndexEntity(kind = kind, refId = refId, workspaceId = workspaceId, parentRef = parentRef, title = title, body = body))
    }

    suspend fun remove(refId: String) = dao.deleteByRef(refId)
    suspend fun remove(refIds: List<String>) = refIds.chunked(500).forEach { dao.deleteByRefs(it) }
    suspend fun removeChildren(parentRef: String) = dao.deleteByParent(parentRef)

    suspend fun putTask(task: TaskEntity) = put(TASK, task.id, task.workspaceId, task.parentId ?: task.id, task.title, task.description)
    suspend fun putProject(project: ProjectEntity) = put(PROJECT, project.id, project.workspaceId, project.id, project.name, project.description)
    suspend fun putEvent(event: EventEntity) = put(EVENT, event.id, event.workspaceId, event.id, event.title, event.notes)
    suspend fun putAttachment(attachment: AttachmentEntity) =
        put(ATTACHMENT, attachment.id, attachment.workspaceId, attachment.ownerId, attachment.fileName)

    /** Indexes a database record: its title cell is the title, the other text-like cells are the body. */
    suspend fun putRecord(db: AppDatabase, recordId: String) {
        val record = db.collections().record(recordId) ?: return remove(recordId)
        val collection = db.collections().get(record.collectionId) ?: return
        val properties = db.collections().properties(record.collectionId).associateBy { it.id }
        var title = ""
        val body = StringBuilder()
        for (cell in db.collections().cellsForRecord(recordId)) {
            val property = properties[cell.propertyId] ?: continue
            when (property.type) {
                PropertyType.TITLE -> title = cell.value
                PropertyType.TEXT, PropertyType.URL, PropertyType.EMAIL, PropertyType.PHONE, PropertyType.PERSON, PropertyType.NUMBER ->
                    body.append(cell.value).append(' ')
                else -> Unit
            }
        }
        put(RECORD, recordId, collection.workspaceId, record.collectionId, title, body.toString().trim())
    }

    /** Rebuilds the whole index from content tables; used after a restore. */
    suspend fun rebuild(db: AppDatabase) {
        dao.clear()
        val pages = db.pages().getAll().associateBy { it.id }
        for (page in pages.values) put(PAGE, page.id, page.workspaceId, page.id, page.title)
        for (block in db.blocks().getAll()) {
            val page = pages[block.pageId] ?: continue
            put(BLOCK, block.id, page.workspaceId, page.id, "", blockText(block))
        }
        db.tasks().getAll().forEach { putTask(it) }
        db.projects().getAll().forEach { putProject(it) }
        db.events().getAll().forEach { putEvent(it) }
        db.attachments().getAll().forEach { putAttachment(it) }
        db.collections().allRecords().forEach { putRecord(db, it.id) }
    }

    companion object {
        const val PAGE = "page"
        const val BLOCK = "block"
        const val TASK = "task"
        const val PROJECT = "project"
        const val RECORD = "record"
        const val EVENT = "event"
        const val ATTACHMENT = "attachment"

        fun blockText(entity: BlockEntity): String {
            val props = BlockJson.decodeProps(entity.props)
            return when (BlockType.fromKey(entity.type)) {
                BlockType.TABLE -> props.cells.flatten().joinToString(" ")
                BlockType.BOOKMARK, BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.FILE ->
                    listOfNotNull(props.caption, props.url).joinToString(" ")
                else -> entity.text
            }
        }
    }
}
