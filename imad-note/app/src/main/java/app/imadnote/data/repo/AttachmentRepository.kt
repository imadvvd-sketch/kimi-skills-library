package app.imadnote.data.repo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import app.imadnote.core.Clock
import app.imadnote.core.newId
import app.imadnote.data.db.AppDatabase
import app.imadnote.data.db.AttachmentEntity
import app.imadnote.data.db.AttachmentOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Attachments are copied into app-private storage so they stay available offline and after the
 * source file is moved or deleted. No storage permission is needed: files are picked through the
 * system picker and shared out through a FileProvider.
 */
class AttachmentRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val indexer: SearchIndexer,
    private val clock: Clock,
) {
    private val dao = db.attachments()
    val directory: File get() = File(context.filesDir, "attachments").apply { mkdirs() }

    fun observeAll(workspaceId: String): Flow<List<AttachmentEntity>> = dao.observeAll(workspaceId)
    fun observeFor(owner: AttachmentOwner, ownerId: String): Flow<List<AttachmentEntity>> = dao.observeFor(owner, ownerId)
    suspend fun get(id: String): AttachmentEntity? = dao.get(id)

    fun file(attachment: AttachmentEntity) = File(directory, attachment.relativePath)

    fun shareUri(attachment: AttachmentEntity): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file(attachment))

    /** Copies the picked document into private storage. Returns null if it cannot be read. */
    suspend fun import(uri: Uri, workspaceId: String, owner: AttachmentOwner, ownerId: String): AttachmentEntity? =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            var name: String? = null
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) name = cursor.getString(0)
                }
            }
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            val extension = name?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() && it.length <= 8 }
                ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(mime).orEmpty()
            val id = newId()
            val relative = if (extension.isEmpty()) id else "$id.$extension"
            val target = File(directory, relative)
            val copied = runCatching {
                resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } != null
            }.getOrDefault(false)
            if (!copied) {
                target.delete()
                return@withContext null
            }
            val entity = AttachmentEntity(
                id = id, workspaceId = workspaceId, ownerType = owner, ownerId = ownerId,
                fileName = name ?: relative, mimeType = mime, sizeBytes = target.length(),
                relativePath = relative, createdAt = clock.now(),
            )
            dao.upsert(entity)
            indexer.put(SearchIndexer.ATTACHMENT, id, workspaceId, ownerId, title = entity.fileName)
            entity
        }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val attachment = dao.get(id) ?: return@withContext
        file(attachment).delete()
        dao.delete(id)
        indexer.remove(id)
    }
}
