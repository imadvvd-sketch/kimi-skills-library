package com.lumalab.photo

import android.app.Application
import com.lumalab.photo.data.PhotoRepository
import com.lumalab.photo.image.ThumbnailCache

class LumaLabApp : Application() {
    lateinit var repository: PhotoRepository
        private set
    val thumbnails = ThumbnailCache()

    override fun onCreate() {
        super.onCreate()
        repository = PhotoRepository(this)
    }
}
