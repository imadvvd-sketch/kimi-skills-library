package app.imadnote.domain.db

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Formula language for FORMULA properties.
 *
 * - Literals: numbers `3.5`, strings `"text"`, `true`, `false`.
 * - Properties: `prop("Name")` returns the value of another property of the same record.
 * - Operators (by precedence): `not`, unary `-`; `* / %`; `+ -` (`+` joins text when either side is text);
 *   `== != > < >= <=`; `and`; `or`. Parentheses group.
 * - Functions: `if(cond, a, b)`, `concat(a, b, …)`, `length(t)`, `lower(t)`, `upper(t)`, `contains(t, part)`,
 *   `empty(v)`, `round(n[, digits])`, `floor(n)`, `ceil(n)`, `abs(n)`, `min(a, b, …)`, `max(a, b, …)`,
 *   `toNumber(v)`, `format(v)`, `today()`, `dateAdd(date, n, "days"|"weeks"|"months"|"years")`,
 *   `dateBetween(a, b, "days"|"weeks"|"months"|"years")` (a − b).
 *
 * Errors (unknown function, bad arguments, syntax) evaluate to [Result.Error] with a short message.
 */
object Formula {
    sealed interface Result {
        data class Ok(val value: Value) : Result
        data class Error(val message: String) : Result
    }

    class FormulaException(message: String) : Exception(message)

    fun evaluate(source: String, today: LocalDate, prop: (String) -> Value?): Result = try {
        val parser = Parser(tokenize(source))
        val node = parser.expression()
        parser.expectEnd()
        Result.Ok(node.eval(Env(today, prop)))
    } catch (e: FormulaException) {
        Result.Error(e.message ?: "Invalid formula")
    } catch (e: ArithmeticException) {
        Result.Error("Arithmetic error")
    }

    /** Property names referenced with prop("…"), used to detect cycles between formulas. */
    fun references(source: String): Set<String> = Regex("""prop\(\s*"([^"]*)"\s*\)""").findAll(source).map { it.groupValues[1] }.toSet()

    // ---- tokenizer ----

    private sealed interface Tok {
        data class Num(val v: Double) : Tok
        data class Str(val v: String) : Tok
        data class Id(val v: String) : Tok
        data class Op(val v: String) : Tok
    }

    private fun tokenize(src: String): List<Tok> {
        val out = ArrayList<Tok>()
        var i = 0
        while (i < src.length) {
            val c = src[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || (c == '.' && i + 1 < src.length && src[i + 1].isDigit()) -> {
                    val start = i
                    while (i < src.length && (src[i].isDigit() || src[i] == '.')) i++
                    out += Tok.Num(src.substring(start, i).toDoubleOrNull() ?: throw FormulaException("Bad number"))
                }
                c == '"' -> {
                    val sb = StringBuilder()
                    i++
                    while (i < src.length && src[i] != '"') {
                        if (src[i] == '\\' && i + 1 < src.length) i++
                        sb.append(src[i++])
                    }
                    if (i >= src.length) throw FormulaException("Unclosed text")
                    i++
                    out += Tok.Str(sb.toString())
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < src.length && (src[i].isLetterOrDigit() || src[i] == '_')) i++
                    out += Tok.Id(src.substring(start, i))
                }
                else -> {
                    val two = src.substring(i, minOf(i + 2, src.length))
                    when {
                        two in setOf("==", "!=", ">=", "<=", "&&", "||") -> { out += Tok.Op(two); i += 2 }
                        c in "+-*/%()<>,!" -> { out += Tok.Op(c.toString()); i++ }
                        else -> throw FormulaException("Unexpected '$c'")
                    }
                }
            }
        }
        return out
    }

    // ---- parser ----

    private class Env(val today: LocalDate, val prop: (String) -> Value?)

    private fun interface Node { fun eval(env: Env): Value }

    private class Parser(private val toks: List<Tok>) {
        private var pos = 0
        private fun peek() = toks.getOrNull(pos)
        private fun isOp(v: String) = (peek() as? Tok.Op)?.v == v
        private fun isWord(v: String) = (peek() as? Tok.Id)?.v.equals(v, ignoreCase = true)
        private fun expect(v: String) { if (!isOp(v)) throw FormulaException("Expected '$v'"); pos++ }
        fun expectEnd() { if (pos < toks.size) throw FormulaException("Unexpected text at the end") }

        fun expression(): Node = or()

        private fun or(): Node {
            var left = and()
            while (isWord("or") || isOp("||")) { pos++; val l = left; val r = and(); left = Node { Value.Bool(truthy(l.eval(it)) || truthy(r.eval(it))) } }
            return left
        }

        private fun and(): Node {
            var left = comparison()
            while (isWord("and") || isOp("&&")) { pos++; val l = left; val r = comparison(); left = Node { Value.Bool(truthy(l.eval(it)) && truthy(r.eval(it))) } }
            return left
        }

        private fun comparison(): Node {
            var left = additive()
            while (true) {
                val op = (peek() as? Tok.Op)?.v?.takeIf { it in setOf("==", "!=", ">", "<", ">=", "<=") } ?: return left
                pos++
                val l = left
                val r = additive()
                left = Node { env ->
                    val a = l.eval(env); val b = r.eval(env)
                    val equal = a.asText() == b.asText() || (a is Value.Num && b is Value.Num && a.v == b.v)
                    Value.Bool(
                        when (op) {
                            "==" -> equal
                            "!=" -> !equal
                            ">" -> a > b
                            "<" -> a < b
                            ">=" -> a >= b
                            else -> a <= b
                        }
                    )
                }
            }
        }

        private fun additive(): Node {
            var left = term()
            while (isOp("+") || isOp("-")) {
                val op = (peek() as Tok.Op).v
                pos++
                val l = left; val r = term()
                left = Node { env ->
                    val a = l.eval(env); val b = r.eval(env)
                    if (op == "+" && (a is Value.Str || b is Value.Str)) Value.Str(a.asText() + b.asText())
                    else {
                        val x = num(a); val y = num(b)
                        Value.Num(if (op == "+") x + y else x - y)
                    }
                }
            }
            return left
        }

        private fun term(): Node {
            var left = unary()
            while (isOp("*") || isOp("/") || isOp("%")) {
                val op = (peek() as Tok.Op).v
                pos++
                val l = left; val r = unary()
                left = Node { env ->
                    val x = num(l.eval(env)); val y = num(r.eval(env))
                    when (op) {
                        "*" -> Value.Num(x * y)
                        else -> if (y == 0.0) throw FormulaException("Division by zero") else Value.Num(if (op == "/") x / y else x % y)
                    }
                }
            }
            return left
        }

        private fun unary(): Node = when {
            isOp("-") -> { pos++; val n = unary(); Node { Value.Num(-num(n.eval(it))) } }
            isOp("!") || isWord("not") -> { pos++; val n = unary(); Node { Value.Bool(!truthy(n.eval(it))) } }
            else -> primary()
        }

        private fun primary(): Node {
            val tok = peek() ?: throw FormulaException("Formula is incomplete")
            pos++
            return when (tok) {
                is Tok.Num -> Node { Value.Num(tok.v) }
                is Tok.Str -> Node { Value.Str(tok.v) }
                is Tok.Op -> if (tok.v == "(") expression().also { expect(")") } else throw FormulaException("Unexpected '${tok.v}'")
                is Tok.Id -> when {
                    tok.v.equals("true", true) -> Node { Value.Bool(true) }
                    tok.v.equals("false", true) -> Node { Value.Bool(false) }
                    isOp("(") -> call(tok.v.lowercase())
                    else -> throw FormulaException("Unknown name '${tok.v}'")
                }
            }
        }

        private fun call(name: String): Node {
            expect("(")
            val args = ArrayList<Node>()
            if (!isOp(")")) {
                args += expression()
                while (isOp(",")) { pos++; args += expression() }
            }
            expect(")")
            fun need(n: IntRange) { if (args.size !in n) throw FormulaException("$name() takes ${if (n.first == n.last) n.first else "${n.first}–${n.last}"} arguments") }
            return when (name) {
                "prop" -> {
                    need(1..1)
                    Node { env ->
                        val key = args[0].eval(env).asText()
                        env.prop(key) ?: throw FormulaException("No property named \"$key\"")
                    }
                }
                "if" -> { need(3..3); Node { if (truthy(args[0].eval(it))) args[1].eval(it) else args[2].eval(it) } }
                "concat" -> Node { env -> Value.Str(args.joinToString("") { it.eval(env).asText() }) }
                "length" -> { need(1..1); Node { Value.Num(args[0].eval(it).let { v -> if (v is Value.Many) v.v.size else v.asText().length }.toDouble()) } }
                "lower" -> { need(1..1); Node { Value.Str(args[0].eval(it).asText().lowercase()) } }
                "upper" -> { need(1..1); Node { Value.Str(args[0].eval(it).asText().uppercase()) } }
                "contains" -> { need(2..2); Node { env -> Value.Bool(args[0].eval(env).let { v -> val part = args[1].eval(env).asText(); if (v is Value.Many) part in v.v else v.asText().contains(part, true) }) } }
                "empty" -> { need(1..1); Node { Value.Bool(args[0].eval(it).isEmpty) } }
                "round" -> {
                    need(1..2)
                    Node { env ->
                        val digits = args.getOrNull(1)?.let { num(it.eval(env)).toInt() } ?: 0
                        val factor = Math.pow(10.0, digits.toDouble())
                        Value.Num((num(args[0].eval(env)) * factor).roundToLong() / factor)
                    }
                }
                "floor" -> { need(1..1); Node { Value.Num(floor(num(args[0].eval(it)))) } }
                "ceil" -> { need(1..1); Node { Value.Num(ceil(num(args[0].eval(it)))) } }
                "abs" -> { need(1..1); Node { Value.Num(abs(num(args[0].eval(it)))) } }
                "min", "max" -> {
                    if (args.isEmpty()) throw FormulaException("$name() needs a value")
                    Node { env -> args.map { it.eval(env) }.filterNot { it.isEmpty }.let { v -> (if (name == "min") v.minOrNull() else v.maxOrNull()) ?: Value.Empty } }
                }
                "tonumber" -> { need(1..1); Node { args[0].eval(it).let { v -> if (v.isEmpty) Value.Empty else Value.Num(num(v)) } } }
                "format" -> { need(1..1); Node { Value.Str(args[0].eval(it).asText()) } }
                "today", "now" -> { need(0..0); Node { Value.Date(it.today) } }
                "dateadd" -> {
                    need(3..3)
                    Node { env ->
                        val date = args[0].eval(env) as? Value.Date ?: return@Node Value.Empty
                        val n = num(args[1].eval(env)).toLong()
                        Value.Date(date.v.plus(n, unit(args[2].eval(env).asText())))
                    }
                }
                "datebetween" -> {
                    need(3..3)
                    Node { env ->
                        val a = args[0].eval(env) as? Value.Date ?: return@Node Value.Empty
                        val b = args[1].eval(env) as? Value.Date ?: return@Node Value.Empty
                        Value.Num(unit(args[2].eval(env).asText()).between(b.v, a.v).toDouble())
                    }
                }
                else -> throw FormulaException("Unknown function $name()")
            }
        }
    }

    private fun unit(name: String) = when (name.lowercase()) {
        "day", "days" -> ChronoUnit.DAYS
        "week", "weeks" -> ChronoUnit.WEEKS
        "month", "months" -> ChronoUnit.MONTHS
        "year", "years" -> ChronoUnit.YEARS
        else -> throw FormulaException("Unknown date unit \"$name\"")
    }

    private fun num(v: Value): Double = when (v) {
        Value.Empty -> 0.0
        is Value.Num -> v.v
        is Value.Bool -> if (v.v) 1.0 else 0.0
        is Value.Str -> v.v.trim().toDoubleOrNull() ?: throw FormulaException("\"${v.v}\" is not a number")
        is Value.Date -> v.v.toEpochDay().toDouble()
        is Value.Many -> v.v.size.toDouble()
    }

    private fun truthy(v: Value): Boolean = when (v) {
        Value.Empty -> false
        is Value.Bool -> v.v
        is Value.Num -> v.v != 0.0
        else -> !v.isEmpty
    }
}
