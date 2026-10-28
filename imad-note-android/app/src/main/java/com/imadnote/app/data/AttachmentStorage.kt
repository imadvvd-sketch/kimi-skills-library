package com.imadnote.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/** Copies images into app-private storage so attachments survive the source app or URI permission going away. */
class AttachmentStorage(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "attachments").apply { mkdirs() }
    private val cameraDir: File get() = File(context.cacheDir, "camera").apply { mkdirs() }

    fun fileFor(fileName: String): File = File(dir, fileName)

    data class Imported(val fileName: String, val mimeType: String)

    suspend fun importUri(uri: Uri): Imported = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val name = "${UUID.randomUUID()}.${extensionFor(mime)}"
        val target = File(dir, name)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open $uri")
            input.use { src -> target.outputStream().use { src.copyTo(it) } }
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        Imported(name, mime)
    }

    fun newCameraFile(): File = File(cameraDir, "capture_${UUID.randomUUID()}.jpg")

    suspend fun importCameraFile(file: File): Imported = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) throw IOException("Camera returned no image")
        val name = "${UUID.randomUUID()}.jpg"
        val target = File(dir, name)
        if (!file.renameTo(target)) {
            file.inputStream().use { src -> target.outputStream().use { src.copyTo(it) } }
            file.delete()
        }
        Imported(name, "image/jpeg")
    }

    suspend fun delete(fileName: String) = withContext(Dispatchers.IO) {
        File(dir, fileName).delete()
    }

    /** Removes files no row refers to (e.g. after a crash mid-import). Recent files are kept to avoid racing an import. */
    suspend fun cleanOrphans(known: Set<String>, olderThanMs: Long = 60 * 60 * 1000L) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        dir.listFiles()?.forEach { f ->
            if (f.name !in known && f.lastModified() < cutoff) f.delete()
        }
        cameraDir.listFiles()?.forEach { f -> if (f.lastModified() < cutoff) f.delete() }
    }

    private fun extensionFor(mime: String): String = when {
        mime.contains("png") -> "png"
        mime.contains("webp") -> "webp"
        mime.contains("gif") -> "gif"
        mime.contains("heic") || mime.contains("heif") -> "heic"
        else -> "jpg"
    }
}
