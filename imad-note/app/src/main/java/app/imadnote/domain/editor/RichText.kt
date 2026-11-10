package app.imadnote.domain.editor

import kotlinx.serialization.Serializable

enum class InlineStyle { BOLD, ITALIC, UNDERLINE, STRIKE, CODE, COLOR, HIGHLIGHT, LINK }

/** A formatted range `[start, end)` of a block's text. [value] carries the colour key or link URL. */
@Serializable
data class Span(val start: Int, val end: Int, val style: InlineStyle, val value: String? = null)

/**
 * Pure functions that keep inline formatting consistent while text is edited.
 * Everything here is independent of Android so it is covered by plain JVM tests.
 */
object RichText {

    /** Re-anchors [spans] after the text changed from [old] to [new] by a single contiguous edit. */
    fun adjustForEdit(old: String, new: String, spans: List<Span>): List<Span> {
        if (spans.isEmpty() || old == new) return spans
        val maxCommon = minOf(old.length, new.length)
        var prefix = 0
        while (prefix < maxCommon && old[prefix] == new[prefix]) prefix++
        var suffix = 0
        while (suffix < maxCommon - prefix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        val removedEnd = old.length - suffix
        val delta = new.length - old.length
        val replaced = removedEnd > prefix

        fun mapStart(x: Int) = when {
            x < prefix -> x
            x == prefix && replaced -> x
            x >= removedEnd -> x + delta
            else -> removedEnd + delta
        }

        // Text typed exactly at the end of a span continues that span.
        fun mapEnd(x: Int) = when {
            x < prefix -> x
            x == prefix && replaced -> x
            x >= removedEnd -> x + delta
            else -> prefix
        }
        return normalize(spans.map { it.copy(start = mapStart(it.start), end = mapEnd(it.end)) })
    }

    fun isActive(spans: List<Span>, start: Int, end: Int, style: InlineStyle): Boolean {
        if (start >= end) return spans.any { it.style == style && it.start < start && start <= it.end }
        var covered = start
        for (span in spans.filter { it.style == style }.sortedBy { it.start }) {
            if (span.start > covered) break
            covered = maxOf(covered, span.end)
            if (covered >= end) return true
        }
        return false
    }

    fun valueAt(spans: List<Span>, position: Int, style: InlineStyle): String? =
        spans.lastOrNull { it.style == style && it.start <= position && position < it.end.coerceAtLeast(it.start + 1) }?.value

    fun remove(spans: List<Span>, start: Int, end: Int, style: InlineStyle): List<Span> {
        if (start >= end) return spans
        val out = ArrayList<Span>(spans.size + 1)
        for (span in spans) {
            if (span.style != style || span.end <= start || span.start >= end) {
                out += span
                continue
            }
            if (span.start < start) out += span.copy(end = start)
            if (span.end > end) out += span.copy(start = end)
        }
        return normalize(out)
    }

    fun add(spans: List<Span>, start: Int, end: Int, style: InlineStyle, value: String? = null): List<Span> {
        if (start >= end) return spans
        return normalize(remove(spans, start, end, style) + Span(start, end, style, value))
    }

    /** Applies [style] to the range, or clears it when the whole range already has it. */
    fun toggle(spans: List<Span>, start: Int, end: Int, style: InlineStyle): List<Span> =
        if (isActive(spans, start, end, style)) remove(spans, start, end, style) else add(spans, start, end, style)

    /** Splits spans at [at]; the second list is rebased to start at zero. */
    fun split(spans: List<Span>, at: Int): Pair<List<Span>, List<Span>> {
        val head = ArrayList<Span>()
        val tail = ArrayList<Span>()
        for (span in spans) {
            if (span.start < at) head += span.copy(end = minOf(span.end, at))
            if (span.end > at) tail += span.copy(start = maxOf(span.start, at) - at, end = span.end - at)
        }
        return normalize(head) to normalize(tail)
    }

    /** Concatenates the spans of two texts, where the first text has [firstLength] characters. */
    fun join(first: List<Span>, firstLength: Int, second: List<Span>): List<Span> =
        normalize(first + second.map { it.copy(start = it.start + firstLength, end = it.end + firstLength) })

    fun clamp(spans: List<Span>, length: Int): List<Span> =
        normalize(spans.map { it.copy(start = it.start.coerceIn(0, length), end = it.end.coerceIn(0, length)) })

    /** Drops empty spans and merges touching spans with the same style and value. */
    fun normalize(spans: List<Span>): List<Span> {
        val out = ArrayList<Span>(spans.size)
        val sorted = spans.filter { it.end > it.start }
            .sortedWith(compareBy<Span> { it.style }.thenBy { it.value.orEmpty() }.thenBy { it.start })
        for (span in sorted) {
            val last = out.lastOrNull()
            if (last != null && last.style == span.style && last.value == span.value && span.start <= last.end) {
                out[out.lastIndex] = last.copy(end = maxOf(last.end, span.end))
            } else {
                out += span
            }
        }
        return out.sortedWith(compareBy<Span> { it.start }.thenBy { it.style })
    }
}
