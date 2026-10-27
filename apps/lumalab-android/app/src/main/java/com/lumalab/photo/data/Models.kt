package com.lumalab.photo.data

import com.lumalab.core.EditSettings
import kotlinx.serialization.Serializable

enum class Flag { NONE, PICK, REJECT }

@Serializable
data class PhotoEntry(
    val id: String,
    val fileName: String,
    val importedAt: Long,
    val width: Int,
    val height: Int,
    val rating: Int = 0,
    val flag: Flag = Flag.NONE,
    val edits: EditSettings = EditSettings(),
    val editedAt: Long = 0L,
    val albumIds: Set<String> = emptySet(),
) {
    val isEdited: Boolean get() = !edits.isDefault
}

@Serializable
data class Album(val id: String, val name: String, val createdAt: Long)

@Serializable
data class UserPreset(val id: String, val name: String, val settings: EditSettings)

@Serializable
data class LibraryDocument(
    val version: Int = 1,
    val photos: List<PhotoEntry> = emptyList(),
    val albums: List<Album> = emptyList(),
    val presets: List<UserPreset> = emptyList(),
)
