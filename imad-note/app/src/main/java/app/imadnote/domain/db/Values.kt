package app.imadnote.domain.db

import app.imadnote.data.db.PropertyType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Serializable
data class SelectOption(val id: String, val name: String, val color: String = "gray")

/** Per-property settings, stored as JSON in `properties.config`. */
@Serializable
data class PropertyConfig(
    val options: List<SelectOption> = emptyList(),
    /** RELATION: id of the database the records link to. */
    val relationTarget: String? = null,
    /** FORMULA: expression, see [Formula]. */
    val formula: String? = null,
)

enum class FilterOp { CONTAINS, NOT_CONTAINS, IS, IS_NOT, GREATER, LESS, BEFORE, AFTER, EMPTY, NOT_EMPTY, CHECKED, UNCHECKED }

@Serializable
data class ViewFilter(val propertyId: String, val op: FilterOp, val value: String = "")

@Serializable
data class ViewSort(val propertyId: String, val descending: Boolean = false)

/** Per-view settings, stored as JSON in `collection_views.config`. */
@Serializable
data class ViewConfig(
    val filters: List<ViewFilter> = emptyList(),
    val sorts: List<ViewSort> = emptyList(),
    val groupBy: String? = null,
    val hidden: Set<String> = emptySet(),
    /** CALENDAR: the DATE property records are placed by. */
    val dateProperty: String? = null,
)

object DbJson {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    fun property(raw: String) = runCatching { json.decodeFromString<PropertyConfig>(raw) }.getOrDefault(PropertyConfig())
    fun view(raw: String) = runCatching { json.decodeFromString<ViewConfig>(raw) }.getOrDefault(ViewConfig())
    fun encode(config: PropertyConfig) = json.encodeToString(config)
    fun encode(config: ViewConfig) = json.encodeToString(config)
    fun ids(raw: String): List<String> = runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    fun encodeIds(ids: List<String>) = json.encodeToString(ids)
}

/** A typed cell value, used by formulas, filters and sorting. */
sealed interface Value : Comparable<Value> {
    data object Empty : Value
    data class Num(val v: Double) : Value
    data class Str(val v: String) : Value
    data class Bool(val v: Boolean) : Value
    data class Date(val v: LocalDate) : Value
    data class Many(val v: List<String>) : Value

    val isEmpty: Boolean get() = this == Empty || (this is Str && v.isEmpty()) || (this is Many && v.isEmpty())

    fun asText(): String = when (this) {
        Empty -> ""
        is Num -> if (v % 1.0 == 0.0 && kotlin.math.abs(v) < 1e15) v.toLong().toString() else "%.4f".format(java.util.Locale.ROOT, v).trimEnd('0').trimEnd('.')
        is Str -> v
        is Bool -> v.toString()
        is Date -> v.toString()
        is Many -> v.joinToString(", ")
    }

    /** Empty values sort last; mixed kinds fall back to text. */
    override fun compareTo(other: Value): Int = when {
        isEmpty && other.isEmpty -> 0
        isEmpty -> 1
        other.isEmpty -> -1
        this is Num && other is Num -> v.compareTo(other.v)
        this is Date && other is Date -> v.compareTo(other.v)
        this is Bool && other is Bool -> v.compareTo(other.v)
        else -> asText().compareTo(other.asText(), ignoreCase = true)
    }
}

/** Converts between stored strings and [Value]s. Select values are stored as option ids. */
object Cells {
    fun decode(type: PropertyType, raw: String?, config: PropertyConfig): Value {
        if (raw == null || raw.isEmpty()) return if (type == PropertyType.CHECKBOX) Value.Bool(false) else Value.Empty
        return when (type) {
            PropertyType.NUMBER -> raw.toDoubleOrNull()?.let(Value::Num) ?: Value.Empty
            PropertyType.CHECKBOX -> Value.Bool(raw == "true")
            PropertyType.DATE -> runCatching { Value.Date(LocalDate.parse(raw)) }.getOrDefault(Value.Empty)
            PropertyType.SELECT, PropertyType.STATUS -> config.options.firstOrNull { it.id == raw }?.let { Value.Str(it.name) } ?: Value.Empty
            PropertyType.MULTI_SELECT -> Value.Many(DbJson.ids(raw).mapNotNull { id -> config.options.firstOrNull { it.id == id }?.name })
            else -> Value.Str(raw)
        }
    }

    fun timestamp(millis: Long, zone: ZoneId): Value = Value.Date(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())

    /** Default options for a new STATUS property. */
    val defaultStatus = listOf(SelectOption("todo", "Not started", "gray"), SelectOption("doing", "In progress", "blue"), SelectOption("done", "Done", "green"))

    fun opsFor(type: PropertyType): List<FilterOp> = when (type) {
        PropertyType.NUMBER, PropertyType.FORMULA -> listOf(FilterOp.IS, FilterOp.IS_NOT, FilterOp.GREATER, FilterOp.LESS, FilterOp.EMPTY, FilterOp.NOT_EMPTY)
        PropertyType.SELECT, PropertyType.STATUS -> listOf(FilterOp.IS, FilterOp.IS_NOT, FilterOp.EMPTY, FilterOp.NOT_EMPTY)
        PropertyType.MULTI_SELECT, PropertyType.RELATION -> listOf(FilterOp.CONTAINS, FilterOp.NOT_CONTAINS, FilterOp.EMPTY, FilterOp.NOT_EMPTY)
        PropertyType.DATE, PropertyType.CREATED_TIME, PropertyType.LAST_EDITED_TIME -> listOf(FilterOp.IS, FilterOp.BEFORE, FilterOp.AFTER, FilterOp.EMPTY, FilterOp.NOT_EMPTY)
        PropertyType.CHECKBOX -> listOf(FilterOp.CHECKED, FilterOp.UNCHECKED)
        else -> listOf(FilterOp.CONTAINS, FilterOp.NOT_CONTAINS, FilterOp.IS, FilterOp.IS_NOT, FilterOp.EMPTY, FilterOp.NOT_EMPTY)
    }
}
