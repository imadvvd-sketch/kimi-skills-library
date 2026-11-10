package app.imadnote.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.imadnote.AppContainer
import app.imadnote.core.newId
import app.imadnote.data.db.AttachmentOwner
import app.imadnote.data.db.PageEntity
import app.imadnote.domain.editor.Block
import app.imadnote.domain.editor.BlockProps
import app.imadnote.domain.editor.BlockType
import app.imadnote.domain.editor.Caret
import app.imadnote.domain.editor.EditResult
import app.imadnote.domain.editor.EditorDoc
import app.imadnote.domain.editor.EditorHistory
import app.imadnote.domain.editor.InlineStyle
import app.imadnote.domain.editor.Markdown
import app.imadnote.domain.editor.RichText
import app.imadnote.domain.editor.TextAlignment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusRequest(val blockId: String, val offset: Int, val nonce: Long)

data class EditorUiState(
    val doc: EditorDoc? = null,
    val focusedBlockId: String? = null,
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    /** Formatting toggled with a collapsed caret, applied to the next typed text. */
    val pendingStyles: Map<InlineStyle, Boolean> = emptyMap(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val saved: Boolean = true,
) {
    val focusedBlock: Block? get() = focusedBlockId?.let { id -> doc?.block(id) }

    fun isStyleActive(style: InlineStyle): Boolean {
        val block = focusedBlock ?: return false
        if (selectionStart == selectionEnd) pendingStyles[style]?.let { return it }
        return RichText.isActive(block.spans, selectionStart, selectionEnd, style)
    }
}

class EditorViewModel(private val pageId: String, private val container: AppContainer) : ViewModel() {
    private val pages = container.pages
    private val history = EditorHistory()
    private var saveJob: Job? = null
    private var titleJob: Job? = null
    private var nonce = 0L

    val page: StateFlow<PageEntity?> = pages.observe(pageId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Compose snapshot state rather than a Flow: the text fields must see an edit in the same frame
    // it was typed, otherwise the keyboard's composing text is reset. All writes happen on the main thread.
    var state by mutableStateOf(EditorUiState())
        private set

    var focus by mutableStateOf<FocusRequest?>(null)
        private set

    private inline fun update(transform: (EditorUiState) -> EditorUiState) {
        state = transform(state)
    }

    init {
        viewModelScope.launch {
            pages.markOpened(pageId)
            val doc = pages.loadDoc(pageId).let { if (it.blocks.isEmpty()) EditorDoc.empty(newId()) else it }
            update { it.copy(doc = doc) }
        }
    }

    // ---- plumbing ----

    private val doc get() = state.doc

    private fun commit(before: EditorDoc, after: EditorDoc, caret: Caret? = null, typingBlockId: String? = null) {
        if (after == before) {
            caret?.let(::requestFocus)
            return
        }
        history.record(before, typingBlockId)
        val safe = ensureTrailingText(after)
        update { it.copy(doc = safe, canUndo = history.canUndo, canRedo = history.canRedo, saved = false) }
        caret?.let(::requestFocus)
        scheduleSave()
    }

    private fun commit(before: EditorDoc, result: EditResult) = commit(before, result.doc, result.caret)

    /** The page always ends with a text block so there is somewhere to keep typing. */
    private fun ensureTrailingText(doc: EditorDoc): EditorDoc =
        if (doc.blocks.lastOrNull()?.type?.hasText == true) doc else doc.copy(blocks = doc.blocks + Block(newId()))

    private fun requestFocus(caret: Caret) {
        focus = FocusRequest(caret.blockId, caret.offset, ++nonce)
        update { it.copy(focusedBlockId = caret.blockId, selectionStart = caret.offset, selectionEnd = caret.offset, pendingStyles = emptyMap()) }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            save()
        }
    }

    private suspend fun save() {
        val snapshot = doc ?: return
        pages.saveDoc(pageId, snapshot)
        if (doc === snapshot) update { it.copy(saved = true) }
    }

    /** Writes pending changes immediately; called when the screen stops or closes. */
    fun flush() {
        saveJob?.cancel()
        titleJob?.cancel()
        if (state.saved && pendingTitle == null) return
        container.appScope.launch {
            writeTitle()
            if (!state.saved) save()
        }
    }

    override fun onCleared() = flush()

    // ---- page metadata ----

    private var pendingTitle: String? = null

    fun setTitle(title: String) {
        pendingTitle = title
        titleJob?.cancel()
        titleJob = viewModelScope.launch {
            delay(300)
            writeTitle()
        }
    }

    private suspend fun writeTitle() {
        val title = pendingTitle ?: return
        pendingTitle = null
        pages.edit(pageId) { it.copy(title = title) }
    }

    fun updatePage(transform: (PageEntity) -> PageEntity) {
        viewModelScope.launch { pages.edit(pageId, transform) }
    }

    fun setCover(uri: android.net.Uri?) {
        viewModelScope.launch {
            val current = pages.get(pageId) ?: return@launch
            current.coverUri?.let { container.attachments.delete(it) }
            val attachment = uri?.let { container.attachments.import(it, current.workspaceId, AttachmentOwner.PAGE, pageId) }
            pages.edit(pageId) { it.copy(coverUri = attachment?.id) }
        }
    }

    fun trash(onDone: () -> Unit) {
        saveJob?.cancel()
        titleJob?.cancel()
        viewModelScope.launch {
            writeTitle()
            save()
            pages.trash(pageId)
            onDone()
        }
    }

    fun duplicate(onDone: (String) -> Unit) {
        titleJob?.cancel()
        viewModelScope.launch {
            writeTitle()
            save()
            pages.duplicate(pageId)?.let { onDone(it.id) }
        }
    }

    // ---- selection ----

    fun onSelectionChange(blockId: String, start: Int, end: Int) {
        update {
            val moved = it.focusedBlockId != blockId || it.selectionStart != start || it.selectionEnd != end
            if (!moved) it else it.copy(focusedBlockId = blockId, selectionStart = start, selectionEnd = end, pendingStyles = emptyMap())
        }
    }

    fun selectBlock(blockId: String) = update { it.copy(focusedBlockId = blockId, selectionStart = 0, selectionEnd = 0) }

    // ---- text editing ----

    fun onTextChange(blockId: String, newText: String, selStart: Int, selEnd: Int) {
        val before = doc ?: return
        val block = before.block(blockId) ?: return
        if (block.text == newText) return onSelectionChange(blockId, selStart, selEnd)

        if (!block.type.isMultiline && '\n' in newText) return handleLineBreak(before, block, newText)

        var spans = if (block.type == BlockType.CODE || block.type == BlockType.EQUATION) emptyList() else RichText.adjustForEdit(block.text, newText, block.spans)
        val pending = state.pendingStyles
        val inserted = newText.length - block.text.length
        if (pending.isNotEmpty() && inserted > 0 && selStart == selEnd && block.type != BlockType.CODE) {
            val from = selStart - inserted
            for ((style, on) in pending) {
                spans = if (on) RichText.add(spans, from, selStart, style) else RichText.remove(spans, from, selStart, style)
            }
        }

        if (block.type == BlockType.PARAGRAPH) {
            Markdown.shortcutFor(newText)?.let { shortcut ->
                val converted = ensureTrailingText(before.setText(blockId, shortcut.remaining, emptyList()).convert(blockId, shortcut.type))
                return commit(before, converted, Caret(nextTextBlockId(converted, blockId), shortcut.remaining.length.takeIf { shortcut.type.hasText } ?: 0))
            }
        }

        val after = before.setText(blockId, newText, spans)
        history.record(before, typingBlockId = blockId)
        update {
            it.copy(
                doc = after, focusedBlockId = blockId, selectionStart = selStart, selectionEnd = selEnd,
                canUndo = history.canUndo, canRedo = history.canRedo, saved = false,
                // Keep pending styles while typing so a whole word gets the format.
            )
        }
        scheduleSave()
    }

    /** First text block at or after [blockId]; [doc] must already end with a text block. */
    private fun nextTextBlockId(doc: EditorDoc, blockId: String): String =
        doc.blocks.drop(doc.indexOf(blockId).coerceAtLeast(0)).first { it.type.hasText }.id

    /** Enter key (a single newline) splits the block; pasted multi-line text becomes several blocks. */
    private fun handleLineBreak(before: EditorDoc, block: Block, newText: String) {
        val old = block.text
        var prefix = 0
        val max = minOf(old.length, newText.length)
        while (prefix < max && old[prefix] == newText[prefix]) prefix++
        var suffix = 0
        while (suffix < max - prefix && old[old.length - 1 - suffix] == newText[newText.length - 1 - suffix]) suffix++
        val inserted = newText.substring(prefix, newText.length - suffix)
        val removedEnd = old.length - suffix

        // Remove any replaced selection first, then work from a collapsed caret at `prefix`.
        val collapsedText = old.removeRange(prefix, removedEnd)
        val collapsed = before.setText(block.id, collapsedText, RichText.adjustForEdit(old, collapsedText, block.spans))

        if (inserted == "\n") return commit(before, collapsed.split(block.id, prefix, newId()))

        val lines = inserted.split('\n')
        val split = collapsed.split(block.id, prefix, newId())
        // `split` may have left the list instead of splitting when the block was empty; fall back to plain insert.
        val tailId = split.caret?.blockId?.takeIf { it != block.id && split.doc.blocks.size > collapsed.blocks.size }
        var working = if (tailId != null) split.doc else collapsed
        working = working.update(block.id) { it.copy(text = it.text + lines.first()) }
        val middle = Markdown.parse(lines.drop(1).dropLast(1).joinToString("\n"), ::newId).takeIf { lines.size > 2 }.orEmpty()
        var anchor = block.id
        for (pasted in middle) {
            working = working.copy(blocks = working.blocks.toMutableList().also { it.add(working.indexOf(anchor) + 1, pasted.copy(indent = pasted.indent + block.indent)) }).fixIndents()
            anchor = pasted.id
        }
        val last = lines.last()
        val caret: Caret
        if (tailId != null) {
            working = working.update(tailId) { tail ->
                tail.copy(text = last + tail.text, spans = tail.spans.map { it.copy(start = it.start + last.length, end = it.end + last.length) })
            }
            caret = Caret(tailId, last.length)
        } else {
            working = working.update(block.id) { it.copy(text = it.text + " " + last) }
            caret = Caret(block.id, working.block(block.id)!!.text.length)
        }
        commit(before, working, caret)
    }

    fun onBackspaceAtStart(blockId: String) {
        val before = doc ?: return
        val result = before.mergeBackward(blockId)
        commit(before, result.doc, result.caret ?: before.blocks.getOrNull(before.indexOf(blockId) + 1)?.takeIf { it.type.hasText }?.let { Caret(it.id, 0) })
    }

    // ---- inline formatting ----

    fun toggleStyle(style: InlineStyle) {
        val s = state
        val before = s.doc ?: return
        val block = s.focusedBlock ?: return
        if (!block.type.hasText || block.type == BlockType.CODE || block.type == BlockType.EQUATION) return
        if (s.selectionStart == s.selectionEnd) {
            update { it.copy(pendingStyles = it.pendingStyles + (style to !it.isStyleActive(style))) }
            return
        }
        commit(before, before.update(block.id) { it.copy(spans = RichText.toggle(it.spans, s.selectionStart, s.selectionEnd, style)) })
    }

    /** Sets or clears ([value] null) a text colour or highlight on the selection, or on the whole block without one. */
    fun setColor(style: InlineStyle, value: String?) {
        val s = state
        val before = s.doc ?: return
        val block = s.focusedBlock ?: return
        if (!block.type.hasText || block.type == BlockType.CODE) return
        val (start, end) = if (s.selectionStart == s.selectionEnd) 0 to block.text.length else s.selectionStart to s.selectionEnd
        commit(before, before.update(block.id) {
            it.copy(spans = if (value == null) RichText.remove(it.spans, start, end, style) else RichText.add(it.spans, start, end, style, value))
        })
    }

    // ---- block operations ----

    private inline fun onFocused(action: (EditorDoc, Block) -> Unit) {
        val before = doc ?: return
        val block = state.focusedBlock ?: return
        action(before, block)
    }

    fun convertFocused(type: BlockType) = onFocused { before, block -> convert(before, block.id, type) }

    private fun convert(before: EditorDoc, blockId: String, type: BlockType) {
        val converted = ensureTrailingText(before.convert(blockId, type))
        val target = nextTextBlockId(converted, blockId)
        commit(before, converted, Caret(target, if (target == blockId) converted.block(blockId)!!.text.length else 0))
    }

    /** Slash command: replaces the "/query" text of the block with the chosen block type. Undo restores the typed text. */
    fun applySlashCommand(blockId: String, type: BlockType) {
        val before = doc ?: return
        val converted = ensureTrailingText(before.setText(blockId, "", emptyList()).convert(blockId, type))
        commit(before, converted, Caret(nextTextBlockId(converted, blockId), 0))
    }

    fun insertAfterFocused(type: BlockType) {
        val before = doc ?: return
        val anchor = state.focusedBlockId ?: before.blocks.lastOrNull()?.id
        val block = Block(newId(), type).let { EditorDoc(listOf(it)).convert(it.id, type).blocks.first() }
        val after = ensureTrailingText(before.insertAfter(anchor, block))
        commit(before, after, Caret(nextTextBlockId(after, block.id), 0))
    }

    fun deleteFocused() = onFocused { before, block -> commit(before, before.delete(block.id)) }
    fun duplicateFocused() = onFocused { before, block -> commit(before, before.duplicate(block.id, ::newId)) }
    fun moveFocused(up: Boolean) = onFocused { before, block -> commit(before, before.move(block.id, up)) }
    fun indentFocused(delta: Int) = onFocused { before, block -> commit(before, before.indent(block.id, delta)) }

    fun cycleAlignment() = onFocused { before, block ->
        val next = TextAlignment.entries[(block.props.align.ordinal + 1) % TextAlignment.entries.size]
        commit(before, before.update(block.id) { it.copy(props = it.props.copy(align = next)) })
    }

    /** [typing] coalesces rapid edits (table cells, captions) into one undo step. */
    fun updateProps(blockId: String, typing: Boolean = false, transform: (BlockProps) -> BlockProps) {
        val before = doc ?: return
        commit(before, before.update(blockId) { it.copy(props = transform(it.props)) }, typingBlockId = blockId.takeIf { typing })
    }

    fun consumeFocus(nonce: Long) {
        if (focus?.nonce == nonce) focus = null
    }

    fun attachFile(blockId: String, uri: android.net.Uri) {
        viewModelScope.launch {
            val workspaceId = pages.get(pageId)?.workspaceId ?: return@launch
            val attachment = container.attachments.import(uri, workspaceId, AttachmentOwner.BLOCK, blockId) ?: return@launch
            updateProps(blockId) { it.copy(attachmentId = attachment.id, caption = it.caption ?: attachment.fileName) }
        }
    }

    fun focusEnd() {
        val last = doc?.blocks?.lastOrNull { it.type.hasText } ?: return
        requestFocus(Caret(last.id, last.text.length))
    }

    fun focusBlock(blockId: String) {
        val block = doc?.block(blockId) ?: return
        if (block.type.hasText) requestFocus(Caret(block.id, block.text.length)) else selectBlock(blockId)
    }

    // ---- history ----

    fun undo() { doc?.let { restore(history.undo(it)) } }
    fun redo() { doc?.let { restore(history.redo(it)) } }

    private fun restore(target: EditorDoc?) {
        target ?: return
        update { it.copy(doc = target, canUndo = history.canUndo, canRedo = history.canRedo, saved = false, pendingStyles = emptyMap()) }
        scheduleSave()
    }

    fun exportText(): String = listOfNotNull(page.value?.title?.takeIf { it.isNotBlank() }?.let { "# $it" }, doc?.let(Markdown::export)).joinToString("\n\n")

    companion object {
        const val AUTOSAVE_DELAY_MS = 600L
    }
}
