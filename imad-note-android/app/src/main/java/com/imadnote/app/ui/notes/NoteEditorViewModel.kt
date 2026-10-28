package com.imadnote.app.ui.notes

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imadnote.app.AppContainer
import com.imadnote.app.R
import com.imadnote.app.data.AttachmentEntity
import com.imadnote.app.data.ChecklistItemEntity
import com.imadnote.app.data.NoteEntity
import com.imadnote.app.data.newId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

data class ChecklistDraft(val id: String = newId(), val text: String = "", val checked: Boolean = false)

data class NoteEditorState(
    val loaded: Boolean = false,
    val title: String = "",
    val content: String = "",
    val color: Int = 0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val projectId: String? = null,
    val checklist: List<ChecklistDraft> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val updatedAt: Long? = null,
    val busy: Boolean = false,
    val errorRes: Int? = null,
) {
    val isBlank: Boolean
        get() = title.isBlank() && content.isBlank() && checklist.all { it.text.isBlank() } && attachments.isEmpty()
}

/**
 * Keeps the whole note in memory while editing and autosaves a snapshot shortly after each change,
 * so typing never round-trips through the database and nothing is lost if the app is killed.
 */
class NoteEditorViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val argId: String = handle["id"] ?: "new"
    val noteId: String = handle.get<String>("draftId") ?: (if (argId == "new") newId() else argId).also { handle["draftId"] = it }
    val mode: String = handle["mode"] ?: ""
    val isNew: Boolean = argId == "new"

    private val _state = MutableStateFlow(NoteEditorState())
    val state = _state.asStateFlow()
    val projects = c.projects.observeList().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val linkedTasks = c.tasks.observeByNote(noteId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var createdAt = System.currentTimeMillis()
    private var rowExists = false
    private var dirty = false
    private var finished = false
    private var autosave: Job? = null
    private val saveLock = Mutex()

    init {
        val presetProject = handle.get<String>("projectId")?.takeIf { it.isNotEmpty() }
        viewModelScope.launch {
            val existing = c.notes.get(noteId)
            if (existing != null) {
                rowExists = true
                createdAt = existing.note.createdAt
                val n = existing.note
                _state.value = NoteEditorState(
                    loaded = true,
                    title = n.title,
                    content = n.content,
                    color = n.color,
                    pinned = n.isPinned,
                    archived = n.isArchived,
                    projectId = n.projectId,
                    checklist = existing.sortedChecklist.map { ChecklistDraft(it.id, it.text, it.isChecked) },
                    attachments = existing.sortedAttachments,
                    updatedAt = n.updatedAt,
                )
            } else {
                _state.value = NoteEditorState(
                    loaded = true,
                    projectId = presetProject,
                    color = if (mode == MODE_IDEA) 1 else 0,
                    checklist = if (mode == MODE_CHECKLIST) listOf(ChecklistDraft()) else emptyList(),
                )
            }
        }
    }

    fun update(transform: (NoteEditorState) -> NoteEditorState) {
        dirty = true
        _state.update(transform)
        autosave?.cancel()
        autosave = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            persist()
        }
    }

    fun addChecklistItem(afterId: String? = null): String {
        val item = ChecklistDraft()
        update { s ->
            val index = afterId?.let { id -> s.checklist.indexOfFirst { it.id == id } } ?: -1
            val list = s.checklist.toMutableList()
            if (index >= 0) list.add(index + 1, item) else list.add(item)
            s.copy(checklist = list)
        }
        return item.id
    }

    fun updateChecklistItem(id: String, transform: (ChecklistDraft) -> ChecklistDraft) =
        update { s -> s.copy(checklist = s.checklist.map { if (it.id == id) transform(it) else it }) }

    fun removeChecklistItem(id: String) = update { s -> s.copy(checklist = s.checklist.filterNot { it.id == id }) }

    fun clearError() = _state.update { it.copy(errorRes = null) }

    private fun snapshot(s: NoteEditorState): Pair<NoteEntity, List<ChecklistItemEntity>> {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            id = noteId,
            title = s.title,
            content = s.content,
            color = s.color,
            isPinned = s.pinned && !s.archived,
            isArchived = s.archived,
            projectId = s.projectId,
            createdAt = createdAt,
            updatedAt = now,
        )
        val items = s.checklist.mapIndexed { i, d ->
            ChecklistItemEntity(id = d.id, noteId = noteId, text = d.text, isChecked = d.checked, position = i)
        }
        return note to items
    }

    /** Writes the current snapshot. A brand-new blank note is not written unless [force] is set. */
    private suspend fun persist(force: Boolean = false) = saveLock.withLock {
        val s = _state.value
        if (!s.loaded) return@withLock
        if (!force && !dirty) return@withLock
        if (!force && !rowExists && s.isBlank) return@withLock
        val (note, items) = snapshot(s)
        c.notes.save(note, items)
        rowExists = true
        dirty = false
        _state.update { it.copy(updatedAt = note.updatedAt) }
    }

    /** Attachments and linked tasks reference the note row, so it must exist first. */
    suspend fun ensureSaved() {
        if (!rowExists) persist(force = true) else persist()
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            ensureSaved()
            var failed = false
            for (uri in uris) {
                try {
                    val a = c.notes.addImage(noteId, uri)
                    _state.update { it.copy(attachments = it.attachments + a) }
                } catch (e: Exception) {
                    failed = true
                }
            }
            _state.update { it.copy(busy = false, errorRes = if (failed) R.string.error_image_import else null) }
        }
    }

    fun newCameraFile(): File = c.notes.newCameraFile()

    fun addCameraImage(file: File) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            ensureSaved()
            try {
                val a = c.notes.addCameraImage(noteId, file)
                _state.update { it.copy(attachments = it.attachments + a, busy = false) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, errorRes = R.string.error_camera) }
            }
        }
    }

    fun removeAttachment(a: AttachmentEntity) {
        viewModelScope.launch {
            c.notes.removeAttachment(a)
            _state.update { s -> s.copy(attachments = s.attachments.filterNot { it.id == a.id }) }
        }
    }

    val taskActions = com.imadnote.app.ui.components.TaskActions(c.tasks, viewModelScope)

    fun setArchived(archived: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(archived = archived, pinned = false) }
            dirty = true
            persist(force = true)
            finished = true
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            finished = true
            autosave?.cancel()
            if (rowExists) c.notes.delete(noteId)
            onDone()
        }
    }

    fun prepareLinkedTask(onReady: (String) -> Unit) {
        viewModelScope.launch {
            ensureSaved()
            onReady(noteId)
        }
    }

    fun close(onDone: () -> Unit) {
        viewModelScope.launch {
            finishEditing()
            finished = true
            onDone()
        }
    }

    private suspend fun finishEditing() {
        autosave?.cancel()
        if (_state.value.isBlank) {
            // Empty notes are discarded instead of cluttering the list.
            if (rowExists) c.notes.delete(noteId)
        } else {
            persist()
        }
    }

    override fun onCleared() {
        if (!finished) c.appScope.launch { finishEditing() }
    }

    companion object {
        const val MODE_CHECKLIST = "checklist"
        const val MODE_IMAGE = "image"
        const val MODE_IDEA = "idea"
        private const val AUTOSAVE_DELAY_MS = 600L
    }
}
