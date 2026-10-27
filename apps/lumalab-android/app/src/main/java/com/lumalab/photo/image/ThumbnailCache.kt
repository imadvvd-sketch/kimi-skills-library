package com.lumalab.photo.image

import android.graphics.Bitmap
import android.util.LruCache
import com.lumalab.core.EditSettings
import com.lumalab.core.ImageBuffer
import com.lumalab.core.Processor
import com.lumalab.photo.data.PhotoEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

/** Edited thumbnails for the library grid, keyed by photo and edit state. */
class ThumbnailCache {
    private val rendered = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val sources = object : LruCache<String, ImageBuffer>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBuffer) = value.pixels.size * 4
    }
    private val decodePermits = Semaphore(3)

    fun cached(entry: PhotoEntry): Bitmap? = rendered.get(key(entry.id, entry.edits))

    suspend fun load(entry: PhotoEntry, file: File, maxDim: Int = THUMB_SIZE): Bitmap? {
        val key = key(entry.id, entry.edits)
        rendered.get(key)?.let { return it }
        return decodePermits.withPermit {
            withContext(Dispatchers.Default) {
                val src = sources.get(entry.id) ?: BitmapIO.decode(file, maxDim)?.also { sources.put(entry.id, it) }
                    ?: return@withContext null
                val out = if (entry.edits.isDefault) src else Processor.process(src, entry.edits)
                BitmapIO.toBitmap(out).also { rendered.put(key, it) }
            }
        }
    }

    private fun key(id: String, edits: EditSettings) = "$id:${edits.hashCode()}"

    companion object {
        const val THUMB_SIZE = 360
    }
}
