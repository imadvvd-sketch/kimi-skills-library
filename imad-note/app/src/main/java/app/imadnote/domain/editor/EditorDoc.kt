package app.imadnote.domain.editor

const val MAX_INDENT = 6

/** Where the caret should go after an operation. */
data class Caret(val blockId: String, val offset: Int)

data class EditResult(val doc: EditorDoc, val caret: Caret? = null)

/**
 * Immutable document model. Every editing action is a pure function returning a new document,
 * which makes undo/redo a matter of keeping previous instances (see [EditorHistory]).
 */
data class EditorDoc(val blocks: List<Block>) {

    fun indexOf(id: String) = blocks.indexOfFirst { it.id == id }
    fun block(id: String) = blocks.firstOrNull { it.id == id }

    /** Index one past the last descendant of the block at [index] (descendants have a larger indent). */
    fun subtreeEnd(index: Int): Int {
        val indent = blocks[index].indent
        var end = index + 1
        while (end < blocks.size && blocks[end].indent > indent) end++
        return end
    }

    private fun replace(index: Int, block: Block) = copy(blocks = blocks.toMutableList().also { it[index] = block })

    fun update(id: String, transform: (Block) -> Block): EditorDoc {
        val index = indexOf(id)
        return if (index < 0) this else replace(index, transform(blocks[index]))
    }

    fun setText(id: String, text: String, spans: List<Span>): EditorDoc =
        update(id) { it.copy(text = text, spans = RichText.clamp(spans, text.length)) }

    /** Enter key. Lists continue; an empty list item or heading falls back to a paragraph. */
    fun split(id: String, offset: Int, newId: String): EditResult {
        val index = indexOf(id)
        if (index < 0) return EditResult(this)
        val block = blocks[index]
        if (block.type.isList && block.text.isEmpty()) {
            val changed = if (block.indent > 0) block.copy(indent = block.indent - 1) else block.copy(type = BlockType.PARAGRAPH, props = BlockProps())
            return EditResult(replace(index, changed), Caret(id, 0))
        }
        val at = offset.coerceIn(0, block.text.length)
        val (headSpans, tailSpans) = RichText.split(block.spans, at)
        val head = block.copy(text = block.text.substring(0, at), spans = headSpans)
        val continues = block.type.isList || (block.type == BlockType.QUOTE && at > 0)
        val tail = Block(
            id = newId,
            type = if (continues) block.type else BlockType.PARAGRAPH,
            text = block.text.substring(at),
            spans = tailSpans,
            props = BlockProps(align = block.props.align),
            // New content under an open toggle becomes its child.
            indent = if (block.type == BlockType.TOGGLE && !block.props.collapsed && at == block.text.length && at > 0) {
                minOf(block.indent + 1, MAX_INDENT)
            } else block.indent,
        )
        val insertAt = if (block.type == BlockType.TOGGLE && block.props.collapsed) subtreeEnd(index) else index + 1
        val list = blocks.toMutableList()
        list[index] = head
        list.add(insertAt, if (tail.indent > block.indent) tail.copy(type = BlockType.PARAGRAPH) else tail)
        return EditResult(copy(blocks = list), Caret(newId, 0))
    }

    /** Backspace at the start of a block. */
    fun mergeBackward(id: String): EditResult {
        val index = indexOf(id)
        if (index < 0) return EditResult(this)
        val block = blocks[index]
        if (block.type != BlockType.PARAGRAPH) {
            return EditResult(replace(index, block.copy(type = BlockType.PARAGRAPH, props = BlockProps(align = block.props.align))), Caret(id, 0))
        }
        if (block.indent > 0) return EditResult(replace(index, block.copy(indent = block.indent - 1)), Caret(id, 0))
        if (index == 0) return EditResult(this, Caret(id, 0))
        val previous = blocks[index - 1]
        val list = blocks.toMutableList()
        if (!previous.type.hasText) {
            // Removing an empty paragraph after a non-text block keeps the block; otherwise delete the block before.
            if (block.text.isEmpty() && index < blocks.lastIndex) {
                list.removeAt(index)
                return EditResult(copy(blocks = list), null)
            }
            list.removeAt(index - 1)
            return EditResult(copy(blocks = list), Caret(id, 0))
        }
        list[index - 1] = previous.copy(
            text = previous.text + block.text,
            spans = RichText.join(previous.spans, previous.text.length, block.spans),
        )
        list.removeAt(index)
        return EditResult(copy(blocks = list), Caret(previous.id, previous.text.length))
    }

    fun insertAfter(id: String?, block: Block): EditorDoc {
        val index = id?.let(::indexOf) ?: -1
        val list = blocks.toMutableList()
        if (index < 0) list.add(block) else list.add(subtreeEndIfCollapsed(index), block.copy(indent = blocks[index].indent))
        return copy(blocks = list)
    }

    private fun subtreeEndIfCollapsed(index: Int) =
        if (blocks[index].type == BlockType.TOGGLE && blocks[index].props.collapsed) subtreeEnd(index) else index + 1

    fun convert(id: String, type: BlockType): EditorDoc = update(id) { block ->
        val props = when (type) {
            BlockType.CALLOUT -> block.props.copy(emoji = block.props.emoji ?: "💡")
            BlockType.TABLE -> if (block.props.cells.isEmpty()) block.props.copy(cells = List(3) { List(3) { "" } }) else block.props
            else -> block.props
        }
        block.copy(
            type = type,
            props = props,
            text = if (type.hasText) block.text else "",
            spans = if (type.hasText && type != BlockType.CODE && type != BlockType.EQUATION) block.spans else emptyList(),
        )
    }

    /** Deletes the block; children of a deleted block are promoted one level. */
    fun delete(id: String): EditResult {
        val index = indexOf(id)
        if (index < 0) return EditResult(this)
        val end = subtreeEnd(index)
        val list = blocks.toMutableList()
        for (i in index + 1 until end) list[i] = list[i].copy(indent = list[i].indent - 1)
        list.removeAt(index)
        val neighbour = list.getOrNull(index - 1) ?: list.getOrNull(index)
        return EditResult(copy(blocks = list), neighbour?.takeIf { it.type.hasText }?.let { Caret(it.id, it.text.length) })
    }

    fun duplicate(id: String, newIds: () -> String): EditResult {
        val index = indexOf(id)
        if (index < 0) return EditResult(this)
        val end = subtreeEnd(index)
        val copies = blocks.subList(index, end).map { it.copy(id = newIds()) }
        val list = blocks.toMutableList().also { it.addAll(end, copies) }
        return EditResult(copy(blocks = list), Caret(copies.first().id, copies.first().text.length))
    }

    /** Moves the block and its descendants above the previous sibling subtree, or below the next one. */
    fun move(id: String, up: Boolean): EditorDoc {
        val index = indexOf(id)
        if (index < 0) return this
        val end = subtreeEnd(index)
        val indent = blocks[index].indent
        val list = blocks.toMutableList()
        val moving = list.subList(index, end).toList()
        if (up) {
            if (index == 0) return this
            var target = index - 1
            while (target > 0 && blocks[target].indent > indent) target--
            repeat(moving.size) { list.removeAt(index) }
            list.addAll(target, moving)
        } else {
            if (end >= blocks.size) return this
            val nextEnd = if (blocks[end].indent >= indent) subtreeEnd(end) else end + 1
            repeat(moving.size) { list.removeAt(index) }
            list.addAll(nextEnd - moving.size, moving)
        }
        return copy(blocks = list).fixIndents()
    }

    /** Drag and drop: moves a block subtree so it starts at [targetIndex] of the remaining list. */
    fun moveTo(id: String, targetIndex: Int): EditorDoc {
        val index = indexOf(id)
        if (index < 0) return this
        val end = subtreeEnd(index)
        val list = blocks.toMutableList()
        val moving = list.subList(index, end).toList()
        repeat(moving.size) { list.removeAt(index) }
        list.addAll(targetIndex.coerceIn(0, list.size), moving)
        return copy(blocks = list).fixIndents()
    }

    fun indent(id: String, delta: Int): EditorDoc {
        val index = indexOf(id)
        if (index < 0) return this
        val block = blocks[index]
        val maxAllowed = if (index == 0) 0 else minOf(blocks[index - 1].indent + 1, MAX_INDENT)
        val target = (block.indent + delta).coerceIn(0, maxAllowed)
        val applied = target - block.indent
        if (applied == 0) return this
        val end = subtreeEnd(index)
        val list = blocks.toMutableList()
        for (i in index until end) list[i] = list[i].copy(indent = (list[i].indent + applied).coerceIn(0, MAX_INDENT))
        return copy(blocks = list)
    }

    /** A block may be at most one level deeper than the block before it. */
    fun fixIndents(): EditorDoc {
        var previous = -1
        var changed = false
        val list = blocks.map { block ->
            val indent = block.indent.coerceIn(0, previous + 1)
            previous = indent
            if (indent != block.indent) { changed = true; block.copy(indent = indent) } else block
        }
        return if (changed) copy(blocks = list) else this
    }

    /** Blocks not hidden inside a collapsed toggle. */
    fun visibleBlocks(): List<Block> {
        val out = ArrayList<Block>(blocks.size)
        var hiddenBelow: Int? = null
        for (block in blocks) {
            val limit = hiddenBelow
            if (limit != null) {
                if (block.indent > limit) continue
                hiddenBelow = null
            }
            out += block
            if (block.type == BlockType.TOGGLE && block.props.collapsed) hiddenBelow = block.indent
        }
        return out
    }

    /** 1-based ordinal for numbered list items, keyed by block id. */
    fun numbering(): Map<String, Int> {
        val out = HashMap<String, Int>()
        val counters = IntArray(MAX_INDENT + 2)
        for (block in blocks) {
            for (deeper in block.indent + 1 until counters.size) counters[deeper] = 0
            if (block.type == BlockType.NUMBERED) {
                counters[block.indent]++
                out[block.id] = counters[block.indent]
            } else {
                counters[block.indent] = 0
            }
        }
        return out
    }

    fun plainText(): String = blocks.joinToString("\n") { block ->
        when (block.type) {
            BlockType.TABLE -> block.props.cells.joinToString("\n") { it.joinToString(" ") }
            BlockType.BOOKMARK -> listOfNotNull(block.props.caption, block.props.url).joinToString(" ")
            else -> block.text
        }
    }

    companion object {
        fun empty(newId: String) = EditorDoc(listOf(Block(id = newId)))
    }
}

/** Bounded undo/redo stack. Consecutive typing in one block is coalesced into a single step. */
class EditorHistory(private val limit: Int = 200) {
    private val undo = ArrayDeque<EditorDoc>()
    private val redo = ArrayDeque<EditorDoc>()
    private var lastTypingBlock: String? = null
    private var lastTypingAt = 0L

    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()

    /** Call with the document as it was *before* a change. [typingBlockId] marks plain typing. */
    fun record(before: EditorDoc, typingBlockId: String? = null, now: Long = System.currentTimeMillis()) {
        val coalesce = typingBlockId != null && typingBlockId == lastTypingBlock && now - lastTypingAt < 1200
        lastTypingBlock = typingBlockId
        lastTypingAt = now
        redo.clear()
        if (coalesce) return
        undo.addLast(before)
        if (undo.size > limit) undo.removeFirst()
    }

    fun undo(current: EditorDoc): EditorDoc? {
        val previous = undo.removeLastOrNull() ?: return null
        redo.addLast(current)
        lastTypingBlock = null
        return previous
    }

    fun redo(current: EditorDoc): EditorDoc? {
        val next = redo.removeLastOrNull() ?: return null
        undo.addLast(current)
        lastTypingBlock = null
        return next
    }
}
