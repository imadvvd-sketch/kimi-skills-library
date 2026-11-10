package app.imadnote.data.backup

import androidx.room.withTransaction
import app.imadnote.core.Clock
import app.imadnote.data.db.AppDatabase
import app.imadnote.data.db.AttachmentEntity
import app.imadnote.data.db.BlockEntity
import app.imadnote.data.db.CellEntity
import app.imadnote.data.db.CollectionEntity
import app.imadnote.data.db.EventEntity
import app.imadnote.data.db.LabelEntity
import app.imadnote.data.db.PageEntity
import app.imadnote.data.db.ProjectEntity
import app.imadnote.data.db.PropertyEntity
import app.imadnote.data.db.RecordEntity
import app.imadnote.data.db.RecordRelationEntity
import app.imadnote.data.db.TaskEntity
import app.imadnote.data.db.TaskLabelCrossRef
import app.imadnote.data.db.ViewEntity
import app.imadnote.data.db.WorkspaceEntity
import app.imadnote.data.repo.SearchIndexer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupData(
    val format: Int = BackupManager.FORMAT,
    val createdAt: Long,
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val pages: List<PageEntity> = emptyList(),
    val blocks: List<BlockEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val labels: List<LabelEntity> = emptyList(),
    val taskLabels: List<TaskLabelCrossRef> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val collections: List<CollectionEntity> = emptyList(),
    val properties: List<PropertyEntity> = emptyList(),
    val records: List<RecordEntity> = emptyList(),
    val cells: List<CellEntity> = emptyList(),
    val relations: List<RecordRelationEntity> = emptyList(),
    val views: List<ViewEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
)

data class BackupSummary(val pages: Int, val tasks: Int, val records: Int, val attachments: Int)

class BackupException(message: String) : Exception(message)

/**
 * A backup is a ZIP file holding `backup.json` (every content table) and the attachment files
 * under `attachments/`. Restoring replaces all content on the device; the search index is rebuilt.
 */
class BackupManager(
    private val db: AppDatabase,
    private val indexer: SearchIndexer,
    private val attachmentsDir: () -> File,
    private val clock: Clock,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun snapshot(): BackupData = db.withTransaction {
        val collections = db.collections()
        BackupData(
            createdAt = clock.now(),
            workspaces = db.workspaces().getAll(),
            pages = db.pages().getAll(),
            blocks = db.blocks().getAll(),
            projects = db.projects().getAll(),
            tasks = db.tasks().getAll(),
            labels = db.labels().getAll(),
            taskLabels = db.labels().getAllCrossRefs(),
            events = db.events().getAll(),
            collections = collections.allCollections(),
            properties = collections.allProperties(),
            records = collections.allRecords(),
            cells = collections.allCells(),
            relations = collections.allRelations(),
            views = collections.allViews(),
            attachments = db.attachments().getAll(),
        )
    }

    suspend fun export(output: OutputStream): BackupSummary = withContext(Dispatchers.IO) {
        val data = snapshot()
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(json.encodeToString(data).toByteArray())
            zip.closeEntry()
            val dir = attachmentsDir()
            for (attachment in data.attachments) {
                val file = File(dir, attachment.relativePath)
                if (!file.isFile) continue
                zip.putNextEntry(ZipEntry(ATTACHMENT_PREFIX + attachment.relativePath))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        data.summary()
    }

    /** Replaces everything on the device with the backup. Nothing changes if the file is invalid. */
    suspend fun restore(input: InputStream): BackupSummary = withContext(Dispatchers.IO) {
        val staging = File(attachmentsDir().parentFile, "restore-staging").apply { deleteRecursively(); mkdirs() }
        try {
            var data: BackupData? = null
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    when {
                        entry.isDirectory -> Unit
                        name == DATA_ENTRY -> data = runCatching { json.decodeFromString<BackupData>(zip.readBytes().decodeToString()) }
                            .getOrElse { throw BackupException("The backup file is damaged.") }
                        name.startsWith(ATTACHMENT_PREFIX) -> {
                            val relative = name.removePrefix(ATTACHMENT_PREFIX)
                            // Reject path traversal ("../") and nested paths: attachments are stored flat.
                            if (relative.isEmpty() || '/' in relative || '\\' in relative || relative.startsWith(".")) continue
                            File(staging, relative).outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
            val backup = data ?: throw BackupException("This file is not an IMAD NOTE backup.")
            if (backup.format > FORMAT) throw BackupException("This backup was made by a newer version of the app.")
            if (backup.workspaces.isEmpty()) throw BackupException("The backup contains no workspace.")

            db.withTransaction {
                // Parents and children arrive in arbitrary order; check foreign keys only at commit.
                db.openHelper.writableDatabase.execSQL("PRAGMA defer_foreign_keys = ON")
                db.openHelper.writableDatabase.execSQL("DELETE FROM workspaces") // cascades to all content
                backup.workspaces.forEach { db.workspaces().upsert(it) }
                db.projects().upsertAll(backup.projects)
                db.pages().upsertAll(backup.pages)
                db.blocks().upsertAll(backup.blocks)
                db.tasks().upsertAll(backup.tasks)
                db.labels().upsertAll(backup.labels)
                db.labels().linkAll(backup.taskLabels)
                db.events().upsertAll(backup.events)
                db.collections().apply {
                    upsertCollections(backup.collections)
                    upsertProperties(backup.properties)
                    upsertRecords(backup.records)
                    upsertCells(backup.cells)
                    linkAll(backup.relations)
                    upsertViews(backup.views)
                }
                db.attachments().upsertAll(backup.attachments)
                indexer.rebuild(db)
            }

            val dir = attachmentsDir()
            dir.listFiles()?.forEach { it.delete() }
            staging.listFiles()?.forEach { it.copyTo(File(dir, it.name), overwrite = true) }
            backup.summary()
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun BackupData.summary() = BackupSummary(pages.size, tasks.size, records.size, attachments.size)

    companion object {
        const val FORMAT = 1
        const val DATA_ENTRY = "backup.json"
        const val ATTACHMENT_PREFIX = "attachments/"
    }
}
