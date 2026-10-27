package com.lumalab.photo.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.lumalab.core.EditSettings
import com.lumalab.photo.image.BitmapIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Local photo library: originals are copied into app storage and every edit is kept
 * as a non-destructive [EditSettings] document in a single JSON file.
 */
class PhotoRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val photoDir = File(context.filesDir, "photos").apply { mkdirs() }
    private val libraryFile = File(context.filesDir, "library.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()

    private val _library = MutableStateFlow(load())
    val library: StateFlow<LibraryDocument> = _library.asStateFlow()

    /** Settings copied with "Copy settings", shared between the library and the editor. */
    private val _clipboard = MutableStateFlow<EditSettings?>(null)
    val clipboard: StateFlow<EditSettings?> = _clipboard.asStateFlow()

    fun fileFor(entry: PhotoEntry): File = File(photoDir, entry.fileName)

    fun photo(id: String): PhotoEntry? = _library.value.photos.firstOrNull { it.id == id }

    suspend fun import(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        val added = uris.mapNotNull { uri ->
            runCatching {
                val id = UUID.randomUUID().toString()
                val ext = when (context.contentResolver.getType(uri)) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    "image/heic", "image/heif" -> "heic"
                    else -> "jpg"
                }
                val file = File(photoDir, "$id.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                } ?: return@mapNotNull null
                val (w, h) = BitmapIO.orientedSize(file) ?: run {
                    file.delete()
                    return@mapNotNull null
                }
                PhotoEntry(id = id, fileName = file.name, importedAt = System.currentTimeMillis(), width = w, height = h)
            }.onFailure { Log.w(TAG, "Import failed for $uri", it) }.getOrNull()
        }
        if (added.isNotEmpty()) mutate { it.copy(photos = added.reversed() + it.photos) }
        added.size
    }

    fun delete(ids: Set<String>) {
        val removed = _library.value.photos.filter { it.id in ids }
        mutate { doc -> doc.copy(photos = doc.photos.filterNot { it.id in ids }) }
        scope.launch { removed.forEach { fileFor(it).delete() } }
    }

    fun updateEdits(id: String, edits: EditSettings) = updatePhoto(id) {
        if (it.edits == edits) it else it.copy(edits = edits, editedAt = System.currentTimeMillis())
    }

    fun pasteEdits(ids: Set<String>, look: EditSettings) = mutate { doc ->
        doc.copy(photos = doc.photos.map {
            if (it.id in ids) it.copy(edits = EditSettings.applyLook(it.edits, look), editedAt = System.currentTimeMillis()) else it
        })
    }

    fun setRating(id: String, rating: Int) = updatePhoto(id) { it.copy(rating = rating.coerceIn(0, 5)) }

    fun setFlag(id: String, flag: Flag) = updatePhoto(id) { it.copy(flag = flag) }

    fun copySettings(settings: EditSettings) {
        _clipboard.value = settings.withoutGeometry()
    }

    fun createAlbum(name: String): Album {
        val album = Album(UUID.randomUUID().toString(), name.trim(), System.currentTimeMillis())
        mutate { it.copy(albums = it.albums + album) }
        return album
    }

    fun deleteAlbum(albumId: String) = mutate { doc ->
        doc.copy(
            albums = doc.albums.filterNot { it.id == albumId },
            photos = doc.photos.map { if (albumId in it.albumIds) it.copy(albumIds = it.albumIds - albumId) else it },
        )
    }

    fun setAlbumMembership(ids: Set<String>, albumId: String, member: Boolean) = mutate { doc ->
        doc.copy(photos = doc.photos.map {
            if (it.id !in ids) it else it.copy(albumIds = if (member) it.albumIds + albumId else it.albumIds - albumId)
        })
    }

    fun savePreset(name: String, settings: EditSettings) = mutate {
        it.copy(presets = it.presets + UserPreset(UUID.randomUUID().toString(), name.trim(), settings.withoutGeometry()))
    }

    fun deletePreset(id: String) = mutate { doc -> doc.copy(presets = doc.presets.filterNot { it.id == id }) }

    private fun updatePhoto(id: String, transform: (PhotoEntry) -> PhotoEntry) = mutate { doc ->
        doc.copy(photos = doc.photos.map { if (it.id == id) transform(it) else it })
    }

    private fun mutate(transform: (LibraryDocument) -> LibraryDocument) {
        _library.update(transform)
        scope.launch { persist() }
    }

    private suspend fun persist() = writeLock.withLock {
        val snapshot = _library.value
        val tmp = File(libraryFile.parentFile, "library.json.tmp")
        tmp.writeText(json.encodeToString(LibraryDocument.serializer(), snapshot))
        if (!tmp.renameTo(libraryFile)) {
            libraryFile.delete()
            tmp.renameTo(libraryFile)
        }
    }

    private fun load(): LibraryDocument = runCatching {
        if (libraryFile.exists()) json.decodeFromString(LibraryDocument.serializer(), libraryFile.readText()) else LibraryDocument()
    }.onFailure { Log.e(TAG, "Library could not be read", it) }.getOrDefault(LibraryDocument())

    private companion object {
        const val TAG = "PhotoRepository"
    }
}
