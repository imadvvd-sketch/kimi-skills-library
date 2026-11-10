package app.imadnote.domain.db

import app.imadnote.data.db.PropertyEntity
import app.imadnote.data.db.PropertyType
import java.time.LocalDate

/** A record with every property resolved to a [Value]. */
data class Row(
    val id: String,
    val sortIndex: Int,
    val values: Map<String, Value>,
    /** Stored strings, used by editors (select ids, ISO dates). */
    val raw: Map<String, String>,
    /** RELATION property id -> linked record ids. */
    val links: Map<String, List<String>>,
    val formulaErrors: Map<String, String> = emptyMap(),
) {
    fun value(propertyId: String): Value = values[propertyId] ?: Value.Empty
}

data class Group(val key: String?, val label: String, val color: String?, val rows: List<Row>)

/** Pure filtering, sorting and grouping of database rows. */
object Query {
    fun apply(rows: List<Row>, properties: List<PropertyEntity>, config: ViewConfig, search: String = ""): List<Row> {
        val byId = properties.associateBy { it.id }
        var out = rows.filter { row -> config.filters.all { f -> byId[f.propertyId]?.let { matches(row.value(f.propertyId), it, f) } ?: true } }
        val needle = search.trim()
        if (needle.isNotEmpty()) out = out.filter { row -> row.values.values.any { it.asText().contains(needle, ignoreCase = true) } }
        val sorts = config.sorts.filter { it.propertyId in byId }
        if (sorts.isNotEmpty()) {
            out = out.sortedWith { a, b ->
                for (s in sorts) {
                    val va = a.value(s.propertyId); val vb = b.value(s.propertyId)
                    // Empty values stay at the bottom in both directions.
                    val c = when {
                        va.isEmpty || vb.isEmpty -> va.compareTo(vb)
                        s.descending -> vb.compareTo(va)
                        else -> va.compareTo(vb)
                    }
                    if (c != 0) return@sortedWith c
                }
                a.sortIndex.compareTo(b.sortIndex)
            }
        }
        return out
    }

    fun matches(value: Value, property: PropertyEntity, filter: ViewFilter): Boolean {
        val target = filter.value.trim()
        return when (filter.op) {
            FilterOp.EMPTY -> value.isEmpty
            FilterOp.NOT_EMPTY -> !value.isEmpty
            FilterOp.CHECKED -> value == Value.Bool(true)
            FilterOp.UNCHECKED -> value != Value.Bool(true)
            FilterOp.CONTAINS -> if (value is Value.Many) value.v.any { it.equals(target, true) } else value.asText().contains(target, true)
            FilterOp.NOT_CONTAINS -> !(if (value is Value.Many) value.v.any { it.equals(target, true) } else value.asText().contains(target, true))
            FilterOp.IS -> equalsTarget(value, target, property)
            FilterOp.IS_NOT -> !equalsTarget(value, target, property)
            FilterOp.GREATER, FilterOp.AFTER -> !value.isEmpty && value > parse(target, value)
            FilterOp.LESS, FilterOp.BEFORE -> !value.isEmpty && value < parse(target, value)
        }
    }

    private fun equalsTarget(value: Value, target: String, property: PropertyEntity): Boolean = when (value) {
        is Value.Num -> target.toDoubleOrNull() == value.v
        is Value.Date -> value.v.toString() == target
        else -> {
            // Select filters store the option id; compare by the option's name.
            val name = DbJson.property(property.config).options.firstOrNull { it.id == target }?.name ?: target
            value.asText().equals(name, ignoreCase = true)
        }
    }

    private fun parse(target: String, like: Value): Value = when (like) {
        is Value.Num -> target.toDoubleOrNull()?.let(Value::Num) ?: Value.Empty
        is Value.Date -> runCatching { Value.Date(LocalDate.parse(target)) }.getOrDefault(Value.Empty)
        else -> Value.Str(target)
    }

    /** Groups rows by a select/status/checkbox/multi-select property; options keep their defined order. */
    fun group(rows: List<Row>, property: PropertyEntity?): List<Group> {
        if (property == null) return listOf(Group(null, "", null, rows))
        val config = DbJson.property(property.config)
        return when (property.type) {
            PropertyType.SELECT, PropertyType.STATUS -> {
                val groups = config.options.map { option -> Group(option.id, option.name, option.color, rows.filter { it.raw[property.id] == option.id }) }
                val known = config.options.mapTo(HashSet()) { it.id }
                groups.plusElement(Group(null, "", null, rows.filter { it.raw[property.id] !in known }))
            }
            PropertyType.MULTI_SELECT -> config.options.map { option ->
                Group(option.id, option.name, option.color, rows.filter { option.id in DbJson.ids(it.raw[property.id].orEmpty()) })
            }.plusElement(Group(null, "", null, rows.filter { DbJson.ids(it.raw[property.id].orEmpty()).isEmpty() }))
            PropertyType.CHECKBOX -> listOf(
                Group("true", "✓", "green", rows.filter { it.value(property.id) == Value.Bool(true) }),
                Group("false", "—", "gray", rows.filter { it.value(property.id) != Value.Bool(true) }),
            )
            else -> rows.groupBy { it.value(property.id).asText() }.map { (k, v) -> Group(k.ifEmpty { null }, k, null, v) }
        }
    }

    fun groupable(type: PropertyType) = type in setOf(PropertyType.SELECT, PropertyType.STATUS, PropertyType.MULTI_SELECT, PropertyType.CHECKBOX)
}
