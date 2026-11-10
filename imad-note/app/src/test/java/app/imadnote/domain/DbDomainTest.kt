package app.imadnote.domain

import app.imadnote.data.db.PropertyEntity
import app.imadnote.data.db.PropertyType
import app.imadnote.domain.db.Cells
import app.imadnote.domain.db.DbJson
import app.imadnote.domain.db.FilterOp
import app.imadnote.domain.db.Formula
import app.imadnote.domain.db.PropertyConfig
import app.imadnote.domain.db.Query
import app.imadnote.domain.db.Row
import app.imadnote.domain.db.SelectOption
import app.imadnote.domain.db.Value
import app.imadnote.domain.db.ViewConfig
import app.imadnote.domain.db.ViewFilter
import app.imadnote.domain.db.ViewSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormulaTest {
    private val today = LocalDate.of(2026, 5, 10)
    private val props = mapOf(
        "Price" to Value.Num(12.5), "Qty" to Value.Num(4.0), "Name" to Value.Str("Pen"),
        "Due" to Value.Date(LocalDate.of(2026, 5, 20)), "Done" to Value.Bool(false), "Tags" to Value.Many(listOf("a", "b")),
    )

    private fun eval(src: String) = Formula.evaluate(src, today) { props[it] }
    private fun ok(src: String) = (eval(src) as Formula.Result.Ok).value

    @Test fun arithmetic_and_precedence() {
        assertEquals(Value.Num(50.0), ok("""prop("Price") * prop("Qty")"""))
        assertEquals(Value.Num(14.0), ok("2 + 3 * 4"))
        assertEquals(Value.Num(20.0), ok("(2 + 3) * 4"))
        assertEquals(Value.Num(-1.0), ok("-1"))
        assertEquals(Value.Num(1.0), ok("7 % 3"))
    }

    @Test fun text_functions() {
        assertEquals(Value.Str("Pen x4"), ok("""prop("Name") + " x" + prop("Qty")"""))
        assertEquals(Value.Str("PEN"), ok("""upper(prop("Name"))"""))
        assertEquals(Value.Num(3.0), ok("""length("abc")"""))
        assertEquals(Value.Num(2.0), ok("""length(prop("Tags"))"""))
        assertEquals(Value.Bool(true), ok("""contains(prop("Tags"), "b")"""))
        assertEquals(Value.Str("a-b"), ok("""concat("a", "-", "b")"""))
    }

    @Test fun logic_and_dates() {
        assertEquals(Value.Str("open"), ok("""if(prop("Done"), "closed", "open")"""))
        assertEquals(Value.Bool(true), ok("""prop("Qty") > 3 and not prop("Done")"""))
        assertEquals(Value.Num(10.0), ok("""dateBetween(prop("Due"), today(), "days")"""))
        assertEquals(Value.Date(LocalDate.of(2026, 6, 20)), ok("""dateAdd(prop("Due"), 1, "months")"""))
        assertEquals(Value.Num(3.14), ok("round(3.14159, 2)"))
        assertEquals(Value.Num(2.0), ok("min(5, 2, 9)"))
    }

    @Test fun errors_are_reported_not_thrown() {
        assertTrue(eval("""prop("Missing")""") is Formula.Result.Error)
        assertTrue(eval("1 / 0") is Formula.Result.Error)
        assertTrue(eval("nope(1)") is Formula.Result.Error)
        assertTrue(eval("1 +") is Formula.Result.Error)
        assertTrue(eval("\"open") is Formula.Result.Error)
        assertTrue(eval("if(true, 1)") is Formula.Result.Error)
        assertEquals(setOf("Price", "Qty"), Formula.references("""prop("Price") * prop( "Qty" )"""))
    }
}

class QueryTest {
    private val status = PropertyEntity("s", "c", "Status", PropertyType.STATUS, DbJson.encode(PropertyConfig(options = Cells.defaultStatus)), 1)
    private val num = PropertyEntity("n", "c", "Points", PropertyType.NUMBER, sortIndex = 2)
    private val title = PropertyEntity("t", "c", "Name", PropertyType.TITLE, sortIndex = 0)
    private val props = listOf(title, status, num)

    private fun row(id: String, name: String, statusId: String?, points: Double?) = Row(
        id, id.hashCode(),
        mapOf("t" to Value.Str(name), "s" to Cells.decode(PropertyType.STATUS, statusId, DbJson.property(status.config)), "n" to (points?.let(Value::Num) ?: Value.Empty)),
        listOfNotNull("t" to name, statusId?.let { "s" to it }).toMap(), emptyMap(),
    )

    private val rows = listOf(row("a", "Alpha", "todo", 3.0), row("b", "Beta", "done", 8.0), row("c", "Gamma", null, null), row("d", "Delta", "doing", 5.0))

    @Test fun filters() {
        fun ids(vararg f: ViewFilter) = Query.apply(rows, props, ViewConfig(filters = f.toList())).map { it.id }
        assertEquals(listOf("b", "d"), ids(ViewFilter("n", FilterOp.GREATER, "4")))
        assertEquals(listOf("b"), ids(ViewFilter("s", FilterOp.IS, "done")))
        assertEquals(listOf("c"), ids(ViewFilter("s", FilterOp.EMPTY)))
        assertEquals(listOf("a"), ids(ViewFilter("t", FilterOp.CONTAINS, "a"), ViewFilter("n", FilterOp.LESS, "4")))
        assertEquals(listOf("c"), ids(ViewFilter("t", FilterOp.IS, "gamma")))
    }

    @Test fun sorting_keeps_empty_values_last() {
        assertEquals(listOf("a", "d", "b", "c"), Query.apply(rows, props, ViewConfig(sorts = listOf(ViewSort("n")))).map { it.id })
        assertEquals(listOf("b", "d", "a", "c"), Query.apply(rows, props, ViewConfig(sorts = listOf(ViewSort("n", descending = true)))).map { it.id })
    }

    @Test fun search_and_grouping() {
        assertEquals(listOf("b"), Query.apply(rows, props, ViewConfig(), "bet").map { it.id })
        val groups = Query.group(rows, status)
        assertEquals(listOf("todo", "doing", "done", null), groups.map { it.key })
        assertEquals(listOf(listOf("a"), listOf("d"), listOf("b"), listOf("c")), groups.map { g -> g.rows.map { it.id } })
    }

    @Test fun cell_decoding() {
        val config = PropertyConfig(options = listOf(SelectOption("x", "Red"), SelectOption("y", "Blue")))
        assertEquals(Value.Many(listOf("Red", "Blue")), Cells.decode(PropertyType.MULTI_SELECT, DbJson.encodeIds(listOf("x", "y", "gone")), config))
        assertEquals(Value.Bool(false), Cells.decode(PropertyType.CHECKBOX, null, config))
        assertEquals(Value.Date(LocalDate.of(2026, 1, 2)), Cells.decode(PropertyType.DATE, "2026-01-02", config))
        assertEquals(Value.Empty, Cells.decode(PropertyType.NUMBER, "abc", config))
        assertEquals("2.5", Value.Num(2.5).asText())
        assertEquals("3", Value.Num(3.0).asText())
    }
}
