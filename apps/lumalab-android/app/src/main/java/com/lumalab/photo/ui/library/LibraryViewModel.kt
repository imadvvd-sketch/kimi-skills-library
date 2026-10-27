package com.lumalab.photo.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumalab.photo.LumaLabApp
import com.lumalab.photo.data.Album
import com.lumalab.photo.data.Flag
import com.lumalab.photo.data.PhotoEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface LibraryFilter {
    data object All : LibraryFilter
    data object Picks : LibraryFilter
    data object Edited : LibraryFilter
    data object Rated : LibraryFilter
    data class InAlbum(val albumId: String) : LibraryFilter
}

data class LibraryUiState(
    val photos: List<PhotoEntry> = emptyList(),
    val totalCount: Int = 0,
    val albums: List<Album> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.All,
    val selection: Set<String> = emptySet(),
    val canPaste: Boolean = false,
    val importing: Boolean = false,
) {
    val selecting: Boolean get() = selection.isNotEmpty()
}

class LibraryViewModel(app: LumaLabApp) : ViewModel() {
    private val repo = app.repository
    private val filter = MutableStateFlow<LibraryFilter>(LibraryFilter.All)
    private val selection = MutableStateFlow<Set<String>>(emptySet())
    private val importing = MutableStateFlow(false)

    val state: StateFlow<LibraryUiState> = combine(repo.library, filter, selection, repo.clipboard, importing) { doc, f, sel, clip, busy ->
        val visible = doc.photos.filter { photo ->
            when (f) {
                LibraryFilter.All -> true
                LibraryFilter.Picks -> photo.flag == Flag.PICK
                LibraryFilter.Edited -> photo.isEdited
                LibraryFilter.Rated -> photo.rating > 0
                is LibraryFilter.InAlbum -> f.albumId in photo.albumIds
            }
        }
        val validFilter = if (f is LibraryFilter.InAlbum && doc.albums.none { it.id == f.albumId }) LibraryFilter.All else f
        LibraryUiState(
            photos = visible,
            totalCount = doc.photos.size,
            albums = doc.albums,
            filter = validFilter,
            selection = sel.filterTo(mutableSetOf()) { id -> doc.photos.any { it.id == id } },
            canPaste = clip != null,
            importing = busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun fileFor(entry: PhotoEntry): File = repo.fileFor(entry)

    fun setFilter(value: LibraryFilter) {
        filter.value = value
        selection.value = emptySet()
    }

    fun toggleSelection(id: String) = selection.update { if (id in it) it - id else it + id }

    fun selectAll() {
        selection.value = state.value.photos.mapTo(mutableSetOf()) { it.id }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            importing.value = true
            try {
                repo.import(uris)
            } finally {
                importing.value = false
            }
        }
    }

    fun deleteSelected() {
        repo.delete(selection.value)
        clearSelection()
    }

    fun pasteToSelected() {
        val look = repo.clipboard.value ?: return
        repo.pasteEdits(selection.value, look)
        clearSelection()
    }

    fun addSelectedToAlbum(albumId: String) {
        repo.setAlbumMembership(selection.value, albumId, member = true)
        clearSelection()
    }

    fun removeSelectedFromAlbum() {
        val f = filter.value as? LibraryFilter.InAlbum ?: return
        repo.setAlbumMembership(selection.value, f.albumId, member = false)
        clearSelection()
    }

    fun createAlbum(name: String, addSelection: Boolean) {
        if (name.isBlank()) return
        val album = repo.createAlbum(name)
        if (addSelection && selection.value.isNotEmpty()) addSelectedToAlbum(album.id)
    }

    fun deleteCurrentAlbum() {
        val f = filter.value as? LibraryFilter.InAlbum ?: return
        repo.deleteAlbum(f.albumId)
        filter.value = LibraryFilter.All
    }
}
