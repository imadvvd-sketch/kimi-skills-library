package app.imadnote.domain.editor

/**
 * Renders a practical subset of LaTeX math as Unicode text.
 *
 * Supported: Greek letters and common operators (`\alpha`, `\times`, `\leq`, `\infty`, …),
 * `\frac{a}{b}`, `\sqrt{x}`, and single-level superscripts/subscripts (`x^2`, `x^{10}`, `a_n`)
 * for digits, signs and the letters that exist in Unicode. Anything else is left as typed.
 * This is not a TeX layout engine: stacked fractions, matrices and large operators are not drawn.
 */
object Equation {
    private val symbols = mapOf(
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "epsilon" to "ε", "theta" to "θ",
        "lambda" to "λ", "mu" to "μ", "pi" to "π", "rho" to "ρ", "sigma" to "σ", "tau" to "τ", "phi" to "φ",
        "omega" to "ω", "Delta" to "Δ", "Sigma" to "Σ", "Omega" to "Ω", "Pi" to "Π", "Gamma" to "Γ",
        "times" to "×", "cdot" to "·", "div" to "÷", "pm" to "±", "leq" to "≤", "geq" to "≥", "neq" to "≠",
        "approx" to "≈", "infty" to "∞", "sum" to "∑", "prod" to "∏", "int" to "∫", "partial" to "∂",
        "to" to "→", "rightarrow" to "→", "leftarrow" to "←", "Rightarrow" to "⇒", "in" to "∈", "forall" to "∀",
        "exists" to "∃", "nabla" to "∇", "degree" to "°", "ldots" to "…", "cdots" to "⋯",
    )
    private val superscripts = "0123456789+-=()ni".zip("⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾ⁿⁱ").toMap()
    private val subscripts = "0123456789+-=()aeoxhklmnpst".zip("₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎ₐₑₒₓₕₖₗₘₙₚₛₜ").toMap()

    private val frac = Regex("""\\frac\{([^{}]*)\}\{([^{}]*)\}""")
    private val sqrt = Regex("""\\sqrt\{([^{}]*)\}""")
    private val command = Regex("""\\([A-Za-z]+)""")
    private val script = Regex("""([\^_])(\{([^{}]*)\}|(.))""")

    fun render(source: String): String {
        var out = source
        // Innermost groups first, so nested \frac and \sqrt resolve from the inside out.
        repeat(4) {
            out = frac.replace(out) { "(${it.groupValues[1]})⁄(${it.groupValues[2]})" }
            out = sqrt.replace(out) { "√(${it.groupValues[1]})" }
        }
        out = command.replace(out) { symbols[it.groupValues[1]] ?: it.value }
        out = script.replace(out) { match ->
            val table = if (match.groupValues[1] == "^") superscripts else subscripts
            val body = match.groupValues[3].ifEmpty { match.groupValues[4] }
            if (body.isNotEmpty() && body.all { it in table }) body.map { table.getValue(it) }.joinToString("") else match.value
        }
        return out
    }
}
