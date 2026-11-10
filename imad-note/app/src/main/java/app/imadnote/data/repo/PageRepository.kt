package app.imadnote.data.repo

import androidx.room.withTransaction
import app.imadnote.core.Clock
import app.imadnote.core.newId
import app.imadnote.data.db.AppDatabase
import app.imadnote.data.db.BlockEntity
import app.imadnote.data.db.PageEntity
import app.imadnote.data.db.PageKind
import app.imadnote.domain.editor.Block
import app.imadnote.domain.editor.BlockJson
import app.imadnote.domain.editor.BlockType
import app.imadnote.domain.editor.EditorDoc
import kotlinx.coroutines.flow.Flow

class PageRepository(
    private val db: AppDatabase,
    private val indexer: SearchIndexer,
    private val clock: Clock,
) {
    private val pages = db.pages()
    private val blocks = db.blocks()

    fun observeActive(workspaceId: String): Flow<List<PageEntity>> = pages.observeActive(workspaceId)
    fun observeTrashed(workspaceId: String): Flow<List<PageEntity>> = pages.observeTrashed(workspaceId)
    fun observe(id: String): Flow<PageEntity?> = pages.observe(id)
    fun observeByProject(projectId: String): Flow<List<PageEntity>> = pages.observeByProject(projectId)
    suspend fun get(id: String): PageEntity? = pages.get(id)

    suspend fun create(
        workspaceId: String,
        parentId: String? = null,
        kind: PageKind = PageKind.PAGE,
        title: String = "",
        icon: String? = null,
        isInbox: Boolean = false,
        projectId: String? = null,
        content: List<Block> = emptyList(),
    ): PageEntity = db.withTransaction {
        val now = clock.now()
        val page = PageEntity(
            id = newId(), workspaceId = workspaceId, parentId = parentId, projectId = projectId, kind = kind,
            title = title, icon = icon, isInbox = isInbox, sortIndex = pages.nextSortIndex(workspaceId, parentId),
            createdAt = now, updatedAt = now,
        )
        pages.upsert(page)
        indexPage(page)
        if (kind == PageKind.PAGE) saveDocLocked(page, EditorDoc(content.ifEmpty { listOf(Block(newId())) }), now)
        page
    }

    /** Updates metadata (title, icon, cover, flags). */
    suspend fun update(page: PageEntity) = db.withTransaction {
        val updated = page.copy(updatedAt = clock.now())
        pages.upsert(updated)
        indexPage(updated)
    }

    /** Atomic read-modify-write of page metadata, so concurrent edits of different fields cannot overwrite each other. */
    suspend fun edit(id: String, transform: (PageEntity) -> PageEntity) = db.withTransaction {
        val current = pages.get(id) ?: return@withTransaction
        val changed = transform(current)
        if (changed != current) update(changed)
    }

    suspend fun markOpened(id: String) = pages.markOpened(id, clock.now())

    /** Moves a page under [newParentId] (null for top level). Moving into its own subtree is rejected. */
    suspend fun move(id: String, newParentId: String?): Boolean = db.withTransaction {
        val page = pages.get(id) ?: return@withTransaction false
        var cursor = newParentId
        while (cursor != null) {
            if (cursor == id) return@withTransaction false
            cursor = pages.get(cursor)?.parentId
        }
        pages.upsert(page.copy(parentId = newParentId, isInbox = false, sortIndex = pages.nextSortIndex(page.workspaceId, newParentId), updatedAt = clock.now()))
        true
    }

    private suspend fun subtreeIds(id: String): List<String> {
        val out = ArrayList<String>()
        val queue = ArrayDeque(listOf(id))
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            out += next
            queue += pages.children(next).map { it.id }
        }
        return out
    }

    suspend fun trash(id: String) = db.withTransaction { pages.setTrashed(subtreeIds(id), true, clock.now()) }

    suspend fun restore(id: String) = db.withTransaction {
        val page = pages.get(id) ?: return@withTransaction
        // A page restored while its parent is still in the trash would be unreachable, so lift it to the top level.
        val parent = page.parentId?.let { pages.get(it) }
        if (parent != null && parent.isTrashed) pages.upsert(page.copy(parentId = null))
        pages.setTrashed(subtreeIds(id), false, clock.now())
    }

    suspend fun deleteForever(id: String) = db.withTransaction {
        val ids = subtreeIds(id)
        for (pageId in ids) {
            indexer.removeChildren(pageId)
            indexer.remove(pageId)
        }
        pages.delete(id) // children and blocks cascade
    }

    suspend fun duplicate(id: String): PageEntity? = db.withTransaction {
        val source = pages.get(id) ?: return@withTransaction null
        val doc = loadDoc(id)
        create(
            workspaceId = source.workspaceId, parentId = source.parentId, kind = source.kind,
            title = source.title, icon = source.icon, projectId = source.projectId,
            content = doc.blocks.map { it.copy(id = newId()) },
        ).also { copy -> pages.upsert(copy.copy(coverUri = source.coverUri)) }
    }

    suspend fun loadDoc(pageId: String): EditorDoc = EditorDoc(blocks.forPage(pageId).map { it.toBlock() })

    /** Autosave entry point: persists the whole document atomically and refreshes the search index. */
    suspend fun saveDoc(pageId: String, doc: EditorDoc) = db.withTransaction {
        val page = pages.get(pageId) ?: return@withTransaction
        val now = clock.now()
        saveDocLocked(page, doc, now)
        pages.upsert(page.copy(updatedAt = now))
    }

    private suspend fun saveDocLocked(page: PageEntity, doc: EditorDoc, now: Long) {
        val existing = blocks.forPage(page.id).associateBy { it.id }
        val keep = HashSet<String>()
        val changed = ArrayList<BlockEntity>()
        doc.blocks.forEachIndexed { index, block ->
            keep += block.id
            val old = existing[block.id]
            val entity = BlockEntity(
                id = block.id, pageId = page.id, type = block.type.key, text = block.text,
                spans = BlockJson.encodeSpans(block.spans), props = BlockJson.encodeProps(block.props),
                indent = block.indent, sortIndex = index, createdAt = old?.createdAt ?: now, updatedAt = now,
            )
            if (old == null || old.copy(updatedAt = now) != entity) {
                changed += entity
                indexer.put(SearchIndexer.BLOCK, block.id, page.workspaceId, page.id, title = "", body = SearchIndexer.blockText(entity))
            }
        }
        val removed = existing.keys - keep
        if (removed.isNotEmpty()) {
            blocks.deleteByIds(removed.toList())
            indexer.remove(removed.toList())
        }
        if (changed.isNotEmpty()) blocks.upsertAll(changed)
    }

    private suspend fun indexPage(page: PageEntity) =
        indexer.put(SearchIndexer.PAGE, page.id, page.workspaceId, page.id, title = page.title)

    private fun BlockEntity.toBlock() = Block(
        id = id, type = BlockType.fromKey(type), text = text,
        spans = BlockJson.decodeSpans(spans), props = BlockJson.decodeProps(props), indent = indent,
    )
}
