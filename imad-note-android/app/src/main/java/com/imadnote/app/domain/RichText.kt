package com.imadnote.app.domain

/**
 * Lightweight rich text stored as plain text with markdown-style markers, so notes stay
 * readable, searchable and future-proof. The editor styles markers live; previews hide them.
 *
 * Block markers (line start): "# " heading, "## " subheading, "> " quote, "- " bullet, "1. " numbered.
 * Inline markers: **bold**, _italic_.
 */
object RichText {
    enum class Kind { HEADING, SUBHEADING, QUOTE, BULLET, NUMBER, BOLD, ITALIC, MARKER }

    data class Span(val start: Int, val end: Int, val kind: Kind)

    /** A run of visible text for previews, with markers removed. */
    data class Run(val text: String, val kinds: Set<Kind>)

    data class Edit(val text: String, val selectionStart: Int, val selectionEnd: Int = selectionStart)

    private val numberPrefix = Regex("^(\\d{1,3})\\. ")
    private val bold = Regex("\\*\\*(\\S(?:.*?\\S)?)\\*\\*")
    private val italic = Regex("(?U)(?<![\\w*])_(\\S(?:.*?\\S)?)_(?![\\w])")

    enum class Block(val prefix: String) { HEADING("# "), SUBHEADING("## "), QUOTE("> "), BULLET("- "), NUMBER("1. ") }

    fun blockOf(line: String): Block? = when {
        line.startsWith("## ") -> Block.SUBHEADING
        line.startsWith("# ") -> Block.HEADING
        line.startsWith("> ") -> Block.QUOTE
        line.startsWith("- ") -> Block.BULLET
        numberPrefix.containsMatchIn(line) -> Block.NUMBER
        else -> null
    }

    private fun prefixLength(line: String): Int = when (blockOf(line)) {
        Block.SUBHEADING -> 3
        Block.HEADING, Block.QUOTE, Block.BULLET -> 2
        Block.NUMBER -> numberPrefix.find(line)!!.value.length
        null -> 0
    }

    fun parse(text: String): List<Span> {
        val spans = mutableListOf<Span>()
        var lineStart = 0
        for (line in text.split('\n')) {
            val lineEnd = lineStart + line.length
            val p = prefixLength(line)
            when (blockOf(line)) {
                Block.HEADING -> { spans += Span(lineStart, lineStart + p, Kind.MARKER); spans += Span(lineStart + p, lineEnd, Kind.HEADING) }
                Block.SUBHEADING -> { spans += Span(lineStart, lineStart + p, Kind.MARKER); spans += Span(lineStart + p, lineEnd, Kind.SUBHEADING) }
                Block.QUOTE -> { spans += Span(lineStart, lineStart + p, Kind.MARKER); spans += Span(lineStart + p, lineEnd, Kind.QUOTE) }
                Block.BULLET -> spans += Span(lineStart, lineStart + 1, Kind.BULLET)
                Block.NUMBER -> spans += Span(lineStart, lineStart + p - 1, Kind.NUMBER)
                null -> Unit
            }
            val body = line.substring(p)
            val offset = lineStart + p
            bold.findAll(body).forEach { m ->
                val s = offset + m.range.first
                val e = offset + m.range.last + 1
                spans += Span(s, s + 2, Kind.MARKER)
                spans += Span(s + 2, e - 2, Kind.BOLD)
                spans += Span(e - 2, e, Kind.MARKER)
            }
            italic.findAll(body).forEach { m ->
                val s = offset + m.range.first
                val e = offset + m.range.last + 1
                spans += Span(s, s + 1, Kind.MARKER)
                spans += Span(s + 1, e - 1, Kind.ITALIC)
                spans += Span(e - 1, e, Kind.MARKER)
            }
            lineStart = lineEnd + 1
        }
        return spans
    }

    /** Visible text for previews: markers dropped, bullets shown as "•". */
    fun visibleRuns(text: String): List<Run> {
        if (text.isEmpty()) return emptyList()
        val spans = parse(text)
        val kindsAt = Array(text.length) { mutableSetOf<Kind>() }
        spans.forEach { s -> for (i in s.start until s.end.coerceAtMost(text.length)) kindsAt[i] += s.kind }
        val runs = mutableListOf<Run>()
        val sb = StringBuilder()
        var current: Set<Kind>? = null
        fun flush() {
            if (sb.isNotEmpty()) runs += Run(sb.toString(), current.orEmpty())
            sb.clear()
        }
        for (i in text.indices) {
            val kinds = kindsAt[i]
            if (Kind.MARKER in kinds) continue
            val ch = if (Kind.BULLET in kinds) '•' else text[i]
            val visibleKinds = kinds - Kind.BULLET
            if (visibleKinds != current) {
                flush()
                current = visibleKinds
            }
            sb.append(ch)
        }
        flush()
        return runs
    }

    fun plain(text: String): String = visibleRuns(text).joinToString("") { it.text }

    private fun lineStartAt(text: String, index: Int): Int = text.lastIndexOf('\n', (index - 1).coerceAtLeast(-1)).let { if (index == 0) 0 else it + 1 }

    private fun lineEndAt(text: String, index: Int): Int = text.indexOf('\n', index).let { if (it < 0) text.length else it }

    /**
     * Applies [block] to every line touched by the selection. If all those lines already
     * have it, it is removed instead. Other block prefixes are replaced.
     */
    fun toggleBlock(text: String, selStart: Int, selEnd: Int, block: Block): Edit {
        val from = lineStartAt(text, minOf(selStart, selEnd))
        val to = lineEndAt(text, maxOf(selStart, selEnd))
        val lines = text.substring(from, to).split('\n')
        val allHave = lines.all { blockOf(it) == block }
        var n = 0
        val newLines = lines.map { line ->
            val body = line.substring(prefixLength(line))
            when {
                allHave -> body
                block == Block.NUMBER -> { n++; "$n. $body" }
                else -> block.prefix + body
            }
        }
        val replaced = newLines.joinToString("\n")
        val newText = text.substring(0, from) + replaced + text.substring(to)
        val delta = replaced.length - (to - from)
        val cursor = (maxOf(selStart, selEnd) + delta).coerceIn(from, from + replaced.length)
        return if (selStart == selEnd) Edit(newText, cursor) else Edit(newText, from, from + replaced.length)
    }

    /** Cycles a line through none → heading → subheading → none. */
    fun cycleHeading(text: String, selStart: Int, selEnd: Int): Edit {
        val from = lineStartAt(text, minOf(selStart, selEnd))
        val line = text.substring(from, lineEndAt(text, from))
        val next = when (blockOf(line)) {
            Block.HEADING -> Block.SUBHEADING
            Block.SUBHEADING -> null
            else -> Block.HEADING
        }
        return if (next == null) toggleBlock(text, selStart, selEnd, Block.SUBHEADING) else toggleBlock(text, selStart, selEnd, next)
    }

    /** Wraps the selection in [marker] ("**" or "_"), unwraps it if already wrapped, or inserts a pair at the caret. */
    fun toggleInline(text: String, selStart: Int, selEnd: Int, marker: String): Edit {
        val s = minOf(selStart, selEnd)
        val e = maxOf(selStart, selEnd)
        val m = marker.length
        if (s >= m && e + m <= text.length && text.substring(s - m, s) == marker && text.substring(e, e + m) == marker) {
            val newText = text.substring(0, s - m) + text.substring(s, e) + text.substring(e + m)
            return Edit(newText, s - m, e - m)
        }
        if (s == e) {
            return Edit(text.substring(0, s) + marker + marker + text.substring(s), s + m)
        }
        val newText = text.substring(0, s) + marker + text.substring(s, e) + marker + text.substring(e)
        return Edit(newText, s + m, e + m)
    }

    /**
     * Called when the user typed a newline at [cursor]-1. Continues bullet/numbered/quote lists,
     * or ends the list when Enter is pressed on an empty item. Returns null when nothing changes.
     */
    fun continueList(old: String, new: String, cursor: Int): Edit? {
        if (new.length != old.length + 1 || cursor < 1 || cursor > new.length || new[cursor - 1] != '\n') return null
        if (new.removeRange(cursor - 1, cursor) != old) return null
        val prevStart = lineStartAt(new, cursor - 1)
        val prevLine = new.substring(prevStart, cursor - 1)
        val block = blockOf(prevLine) ?: return null
        if (block == Block.HEADING || block == Block.SUBHEADING) return null
        val prefixLen = prefixLength(prevLine)
        if (prevLine.substring(prefixLen).isBlank()) {
            // Empty item: remove its marker and the new line, ending the list.
            val newText = new.substring(0, prevStart) + new.substring(cursor)
            return Edit(newText, prevStart)
        }
        val prefix = when (block) {
            Block.NUMBER -> "${numberPrefix.find(prevLine)!!.groupValues[1].toInt() + 1}. "
            else -> block.prefix
        }
        val newText = new.substring(0, cursor) + prefix + new.substring(cursor)
        return Edit(newText, cursor + prefix.length)
    }
}
