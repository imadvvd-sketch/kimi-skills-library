package app.imadnote.domain.editor

/** Typing shortcuts and plain-text interchange (copy, paste, share). */
object Markdown {

    data class Shortcut(val type: BlockType, val remaining: String, val checked: Boolean = false)

    private val prefixes = listOf(
        "### " to BlockType.HEADING_3,
        "## " to BlockType.HEADING_2,
        "# " to BlockType.HEADING_1,
        "- [ ] " to BlockType.TODO,
        "[] " to BlockType.TODO,
        "[ ] " to BlockType.TODO,
        "- " to BlockType.BULLET,
        "* " to BlockType.BULLET,
        "1. " to BlockType.NUMBERED,
        "> " to BlockType.QUOTE,
        "``` " to BlockType.CODE,
        "--- " to BlockType.DIVIDER,
    )

    /** Recognises a block prefix typed at the start of a paragraph, e.g. "# " for a heading. */
    fun shortcutFor(text: String): Shortcut? {
        for ((prefix, type) in prefixes) {
            if (text.startsWith(prefix)) return Shortcut(type, text.removePrefix(prefix))
        }
        if (text == "---") return Shortcut(BlockType.DIVIDER, "")
        if (text == "```") return Shortcut(BlockType.CODE, "")
        return null
    }

    /** Converts pasted or imported plain text into blocks. Fenced code is kept as one block. */
    fun parse(text: String, newId: () -> String): List<Block> {
        val out = ArrayList<Block>()
        val lines = text.replace("\r\n", "\n").split('\n')
        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val trimmed = raw.trimStart()
            val indent = ((raw.length - trimmed.length) / 2).coerceIn(0, MAX_INDENT)
            when {
                trimmed.startsWith("```") -> {
                    val language = trimmed.removePrefix("```").trim().ifEmpty { null }
                    val code = ArrayList<String>()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) code += lines[i++]
                    out += Block(newId(), BlockType.CODE, code.joinToString("\n"), props = BlockProps(language = language))
                }
                trimmed == "---" || trimmed == "***" -> out += Block(newId(), BlockType.DIVIDER)
                trimmed.startsWith("- [x] ", ignoreCase = true) ->
                    out += Block(newId(), BlockType.TODO, trimmed.substring(6), props = BlockProps(checked = true), indent = indent)
                trimmed.startsWith("- [ ] ") -> out += Block(newId(), BlockType.TODO, trimmed.substring(6), indent = indent)
                trimmed.startsWith("### ") -> out += Block(newId(), BlockType.HEADING_3, trimmed.substring(4))
                trimmed.startsWith("## ") -> out += Block(newId(), BlockType.HEADING_2, trimmed.substring(3))
                trimmed.startsWith("# ") -> out += Block(newId(), BlockType.HEADING_1, trimmed.substring(2))
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") ->
                    out += Block(newId(), BlockType.BULLET, trimmed.substring(2), indent = indent)
                numbered.matchEntire(trimmed) != null ->
                    out += Block(newId(), BlockType.NUMBERED, numbered.matchEntire(trimmed)!!.groupValues[1], indent = indent)
                trimmed.startsWith("> ") -> out += Block(newId(), BlockType.QUOTE, trimmed.substring(2))
                else -> out += Block(newId(), BlockType.PARAGRAPH, raw)
            }
            i++
        }
        return EditorDoc(out).fixIndents().blocks
    }

    private val numbered = Regex("""\d+[.)] (.*)""")

    fun export(doc: EditorDoc): String {
        val numbers = doc.numbering()
        return doc.blocks.joinToString("\n") { block ->
            val pad = "  ".repeat(block.indent)
            when (block.type) {
                BlockType.HEADING_1 -> "# ${block.text}"
                BlockType.HEADING_2 -> "## ${block.text}"
                BlockType.HEADING_3 -> "### ${block.text}"
                BlockType.BULLET, BlockType.TOGGLE -> "$pad- ${block.text}"
                BlockType.NUMBERED -> "$pad${numbers[block.id] ?: 1}. ${block.text}"
                BlockType.TODO -> "$pad- [${if (block.props.checked) "x" else " "}] ${block.text}"
                BlockType.QUOTE, BlockType.CALLOUT -> "> ${block.text}"
                BlockType.DIVIDER -> "---"
                BlockType.CODE -> "```${block.props.language.orEmpty()}\n${block.text}\n```"
                BlockType.EQUATION -> "$$${block.text}$$"
                BlockType.TABLE -> block.props.cells.joinToString("\n") { row -> row.joinToString(" | ", "| ", " |") }
                BlockType.BOOKMARK -> "[${block.props.caption ?: block.props.url.orEmpty()}](${block.props.url.orEmpty()})"
                BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.FILE -> "[${block.props.caption ?: block.type.key}]"
                BlockType.TABLE_OF_CONTENTS -> ""
                BlockType.PARAGRAPH -> pad + block.text
            }
        }
    }
}
