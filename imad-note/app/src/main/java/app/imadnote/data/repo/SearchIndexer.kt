package app.imadnote.data.repo

import app.imadnote.data.db.SearchDao
import app.imadnote.data.db.SearchIndexEntity

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

    companion object {
        const val PAGE = "page"
        const val BLOCK = "block"
        const val TASK = "task"
        const val PROJECT = "project"
        const val RECORD = "record"
        const val EVENT = "event"
        const val ATTACHMENT = "attachment"
    }
}
