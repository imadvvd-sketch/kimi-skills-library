package com.lumalab.photo.image

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.lumalab.core.ImageBuffer
import java.io.File
import java.io.OutputStream

object BitmapIO {
    /** Width and height after applying EXIF orientation, or null if the file is not an image. */
    fun orientedSize(file: File): Pair<Int, Int>? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        val swap = when (exifOrientation(file)) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_TRANSVERSE -> true
            else -> false
        }
        return if (swap) opts.outHeight to opts.outWidth else opts.outWidth to opts.outHeight
    }

    /** Decodes [file] upright with its longest edge at most [maxDim] pixels. */
    fun decode(file: File, maxDim: Int): ImageBuffer? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.path, opts) ?: return null
        val longest = maxOf(decoded.width, decoded.height)
        val scale = if (longest > maxDim) maxDim.toFloat() / longest else 1f
        val matrix = orientationMatrix(exifOrientation(file)).apply { postScale(scale, scale) }
        val upright = if (matrix.isIdentity) decoded else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it !== decoded) decoded.recycle()
            }
        }
        val software = if (upright.config == Bitmap.Config.ARGB_8888) upright else upright.copy(Bitmap.Config.ARGB_8888, false)
        return fromBitmap(software)
    }

    fun fromBitmap(bitmap: Bitmap): ImageBuffer {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return ImageBuffer(bitmap.width, bitmap.height, pixels)
    }

    fun toBitmap(buffer: ImageBuffer): Bitmap =
        Bitmap.createBitmap(buffer.pixels, buffer.width, buffer.height, Bitmap.Config.ARGB_8888)

    /** Writes a JPEG into Pictures/LumaLab and returns its content Uri. */
    fun saveToGallery(context: Context, buffer: ImageBuffer, quality: Int, baseName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$baseName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LumaLab")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore refused the new image")
        try {
            resolver.openOutputStream(uri)?.use { writeJpeg(buffer, quality, it) } ?: error("No output stream")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    /** Writes a JPEG to the shareable cache directory and returns a FileProvider Uri. */
    fun writeShareable(context: Context, buffer: ImageBuffer, quality: Int, baseName: String): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "$baseName.jpg")
        file.outputStream().use { writeJpeg(buffer, quality, it) }
        return FileProvider.getUriForFile(context, context.packageName + ".files", file)
    }

    private fun writeJpeg(buffer: ImageBuffer, quality: Int, out: OutputStream) {
        val bitmap = toBitmap(buffer)
        try {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)) { "JPEG encoding failed" }
        } finally {
            bitmap.recycle()
        }
    }

    private fun exifOrientation(file: File): Int = runCatching {
        ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun orientationMatrix(orientation: Int): Matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
        }
    }
}
