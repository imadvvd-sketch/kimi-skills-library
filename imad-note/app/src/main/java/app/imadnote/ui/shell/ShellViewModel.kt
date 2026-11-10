package app.imadnote.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.imadnote.AppContainer
import app.imadnote.data.ThemeMode
import app.imadnote.data.db.PageEntity
import app.imadnote.data.db.PageKind
import app.imadnote.data.db.WorkspaceEntity
import app.imadnote.domain.editor.Block
import app.imadnote.core.newId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Pages of the current workspace arranged as a tree. `null` while loading. */
data class PageTree(val all: List<PageEntity>) {
    private val byParent = all.groupBy { it.parentId }
    private val ids = all.mapTo(HashSet()) { it.id }

    /** Top-level pages, excluding inbox captures. Pages whose parent is in the trash also surface here. */
    val roots: List<PageEntity> = all.filter { (it.parentId == null || it.parentId !in ids) && !it.isInbox }
    val inbox: List<PageEntity> = all.filter { it.isInbox }.sortedByDescending { it.createdAt }
    val favorites: List<PageEntity> = all.filter { it.isFavorite }
    val pinned: List<PageEntity> = all.filter { it.isPinned }
    val recent: List<PageEntity> = all.filter { it.lastOpenedAt != null && it.kind == PageKind.PAGE }.sortedByDescending { it.lastOpenedAt }.take(8)

    fun children(id: String): List<PageEntity> = byParent[id].orEmpty()

    fun descendants(id: String): Set<String> {
        val out = HashSet<String>()
        val queue = ArrayDeque(listOf(id))
        while (queue.isNotEmpty()) children(queue.removeFirst()).forEach { if (out.add(it.id)) queue += it.id }
        return out
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModel(private val container: AppContainer, personalWorkspaceName: String) : ViewModel() {
    private val workspaceRepo = container.workspaces
    private val pages = container.pages

    val workspaces: StateFlow<List<WorkspaceEntity>> = workspaceRepo.workspaces.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val workspace: StateFlow<WorkspaceEntity?> = workspaceRepo.current.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val themeMode: StateFlow<ThemeMode?> = container.settings.themeMode.map<ThemeMode, ThemeMode?> { it }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val tree: StateFlow<PageTree?> = workspaceRepo.current
        .flatMapLatest { ws -> if (ws == null) flowOf(null) else pages.observeActive(ws.id).map(::PageTree) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val trashed: StateFlow<List<PageEntity>> = workspaceRepo.current
        .flatMapLatest { ws -> if (ws == null) flowOf(emptyList()) else pages.observeTrashed(ws.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { workspaceRepo.ensureDefault(personalWorkspaceName) }
    }

    fun createPage(parentId: String? = null, kind: PageKind = PageKind.PAGE, title: String = "", onCreated: (PageEntity) -> Unit = {}) {
        val workspaceId = workspace.value?.id ?: return
        viewModelScope.launch { onCreated(pages.create(workspaceId, parentId, kind, title)) }
    }

    /** Quick capture: the first line becomes the title, the rest the body. */
    fun capture(text: String) {
        val workspaceId = workspace.value?.id ?: return
        val lines = text.trim().lines()
        if (lines.firstOrNull().isNullOrBlank()) return
        viewModelScope.launch {
            pages.create(
                workspaceId, title = lines.first().trim(), isInbox = true,
                content = lines.drop(1).filter { it.isNotBlank() }.map { Block(newId(), text = it.trim()) },
            )
        }
    }

    enum class CaptureKind { NOTE, TASK, CHECKLIST, IDEA, BOOKMARK }

    /** One-step capture from the Inbox or a share. [label] titles checklists and link pages. */
    fun capture(kind: CaptureKind, text: String, label: String = "") {
        val ws = workspace.value?.id ?: return
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            when (kind) {
                CaptureKind.NOTE -> capture(clean)
                CaptureKind.TASK -> {
                    val parsed = app.imadnote.domain.tasks.TaskQuickParser.parse(clean, java.time.LocalDate.now())
                    container.tasks.create(ws, parsed.title, dueDate = parsed.dueDate, priority = parsed.priority.level, labelNames = parsed.labels)
                }
                CaptureKind.CHECKLIST -> pages.create(
                    ws, title = label, isInbox = true, icon = "☑️",
                    content = clean.lines().filter { it.isNotBlank() }.map { Block(newId(), app.imadnote.domain.editor.BlockType.TODO, it.trim().removePrefix("- ").removePrefix("[ ] ")) },
                )
                CaptureKind.IDEA -> {
                    val lines = clean.lines()
                    pages.create(ws, title = lines.first().trim(), icon = "💡", isInbox = true, content = lines.drop(1).filter { it.isNotBlank() }.map { Block(newId(), text = it.trim()) })
                }
                CaptureKind.BOOKMARK -> {
                    val url = clean.substringBefore(' ').let { if ("://" in it) it else "https://$it" }
                    val host = runCatching { java.net.URI(url).host }.getOrNull()?.removePrefix("www.") ?: url
                    pages.create(
                        ws, title = host, icon = "🔖", isInbox = true,
                        content = listOf(Block(newId(), app.imadnote.domain.editor.BlockType.BOOKMARK, props = app.imadnote.domain.editor.BlockProps(url = url, caption = clean.substringAfter(' ', "").ifBlank { null }))),
                    )
                }
            }
        }
    }

    /** Saves a picked image or a recorded audio file as a new Inbox page holding one media block. */
    fun captureMedia(uri: android.net.Uri, type: app.imadnote.domain.editor.BlockType, title: String, icon: String, onDone: () -> Unit = {}) {
        val ws = workspace.value?.id ?: return
        viewModelScope.launch {
            val blockId = newId()
            val attachment = container.attachments.import(uri, ws, app.imadnote.data.db.AttachmentOwner.BLOCK, blockId) ?: return@launch
            pages.create(ws, title = title, icon = icon, isInbox = true, content = listOf(Block(blockId, type, props = app.imadnote.domain.editor.BlockProps(attachmentId = attachment.id, caption = attachment.fileName))))
            onDone()
        }
    }

    /** A route to open once the UI is ready, e.g. from a notification tap. */
    val pendingRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    fun editPage(id: String, transform: (PageEntity) -> PageEntity) { viewModelScope.launch { pages.edit(id, transform) } }
    fun movePage(id: String, parentId: String?) { viewModelScope.launch { pages.move(id, parentId) } }
    fun duplicatePage(id: String) { viewModelScope.launch { pages.duplicate(id) } }
    fun trashPage(id: String) { viewModelScope.launch { pages.trash(id) } }
    fun restorePage(id: String) { viewModelScope.launch { pages.restore(id) } }
    fun deletePageForever(id: String) { viewModelScope.launch { pages.deleteForever(id) } }

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { container.settings.setThemeMode(mode) } }

    fun selectWorkspace(id: String) { viewModelScope.launch { workspaceRepo.select(id) } }
    fun createWorkspace(name: String) { viewModelScope.launch { workspaceRepo.create(name, "🗂️") } }
    fun renameWorkspace(workspace: WorkspaceEntity, name: String) { viewModelScope.launch { workspaceRepo.update(workspace.copy(name = name)) } }
    fun setWorkspaceIcon(workspace: WorkspaceEntity, icon: String) { viewModelScope.launch { workspaceRepo.update(workspace.copy(icon = icon)) } }
    fun deleteWorkspace(id: String) { viewModelScope.launch { workspaceRepo.delete(id) } }
}
