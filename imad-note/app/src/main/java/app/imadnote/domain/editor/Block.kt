package app.imadnote.domain.editor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** [key] is persisted; never rename an existing key. */
enum class BlockType(val key: String, val hasText: Boolean = true) {
    PARAGRAPH("paragraph"),
    HEADING_1("h1"),
    HEADING_2("h2"),
    HEADING_3("h3"),
    BULLET("bullet"),
    NUMBERED("numbered"),
    TODO("todo"),
    TOGGLE("toggle"),
    QUOTE("quote"),
    CALLOUT("callout"),
    DIVIDER("divider", hasText = false),
    CODE("code"),
    TABLE("table", hasText = false),
    IMAGE("image", hasText = false),
    VIDEO("video", hasText = false),
    AUDIO("audio", hasText = false),
    FILE("file", hasText = false),
    BOOKMARK("bookmark", hasText = false),
    EQUATION("equation"),
    TABLE_OF_CONTENTS("toc", hasText = false);

    val isList get() = this == BULLET || this == NUMBERED || this == TODO || this == TOGGLE
    val isHeading get() = this == HEADING_1 || this == HEADING_2 || this == HEADING_3

    /** Enter inside these blocks inserts a line break instead of creating a new block. */
    val isMultiline get() = this == CODE || this == EQUATION

    companion object {
        fun fromKey(key: String): BlockType = entries.firstOrNull { it.key == key } ?: PARAGRAPH
    }
}

enum class TextAlignment { START, CENTER, END }

@Serializable
data class BlockProps(
    val checked: Boolean = false,
    val collapsed: Boolean = false,
    val align: TextAlignment = TextAlignment.START,
    /** Code language label. */
    val language: String? = null,
    /** Callout emoji. */
    val emoji: String? = null,
    /** Bookmark / media source URL. */
    val url: String? = null,
    /** Bookmark title or media caption. */
    val caption: String? = null,
    /** Id of an [app.imadnote.data.db.AttachmentEntity] for media and file blocks. */
    val attachmentId: String? = null,
    /** Table cells, row-major. */
    val cells: List<List<String>> = emptyList(),
    val headerRow: Boolean = true,
)

data class Block(
    val id: String,
    val type: BlockType = BlockType.PARAGRAPH,
    val text: String = "",
    val spans: List<Span> = emptyList(),
    val props: BlockProps = BlockProps(),
    val indent: Int = 0,
)

object BlockJson {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun encodeSpans(spans: List<Span>): String = json.encodeToString(spans)
    fun decodeSpans(raw: String): List<Span> = runCatching { json.decodeFromString<List<Span>>(raw) }.getOrDefault(emptyList())
    fun encodeProps(props: BlockProps): String = json.encodeToString(props)
    fun decodeProps(raw: String): BlockProps = runCatching { json.decodeFromString<BlockProps>(raw) }.getOrDefault(BlockProps())
}
