package app.ghars.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** docs/design/tokens.json is the source of truth; the Kotlin theme must match it exactly. */
class TokensTest {

    private val tokens: JsonObject =
        Json.parseToJsonElement(File("../docs/design/tokens.json").readText()).jsonObject

    private fun hex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

    private fun colorRoles(c: GharsColors) = mapOf(
        "bg" to c.bg, "surface" to c.surface, "surface-sunk" to c.surfaceSunk, "line" to c.line,
        "ink" to c.ink, "ink-muted" to c.inkMuted, "brand" to c.brand, "on-brand" to c.onBrand,
        "brand-soft" to c.brandSoft, "leaf" to c.leaf, "mint" to c.mint, "coin" to c.coin,
        "clay" to c.clay, "sand" to c.sand, "night" to c.night, "withered" to c.withered,
        "danger" to c.danger,
    )

    @Test
    fun `every colour token matches in both themes`() {
        val light = colorRoles(GharsLightColors)
        val dark = colorRoles(GharsFocusColors)
        val list = tokens["color"]!!.jsonObject["tokens"]!!.jsonArray
        assertEquals("a token was added or removed", list.size, light.size)
        for (token in list) {
            val name = token.jsonObject["name"]!!.jsonPrimitive.content
            val value = token.jsonObject["value"]!!
            val (expectedLight, expectedDark) = if (value is JsonPrimitive) {
                value.content to value.content
            } else {
                value.jsonObject["light"]!!.jsonPrimitive.content to
                    value.jsonObject["dark"]!!.jsonPrimitive.content
            }
            assertEquals("$name (light)", expectedLight, hex(light.getValue(name)))
            assertEquals("$name (dark)", expectedDark, hex(dark.getValue(name)))
        }
    }

    private fun dpTokens(group: String): Map<String, Float> =
        tokens[group]!!.jsonObject["tokens"]!!.jsonArray.associate {
            it.jsonObject["name"]!!.jsonPrimitive.content to
                it.jsonObject["value"]!!.jsonPrimitive.content.removeSuffix("px").toFloat()
        }

    private fun assertDp(expected: Map<String, Float>, actual: Map<String, Dp>) =
        assertEquals(expected, actual.mapValues { it.value.value })

    @Test
    fun `spacing matches`() {
        val s = GharsSpacing()
        assertDp(
            dpTokens("spacing"),
            mapOf(
                "space-1" to s.space1, "space-2" to s.space2, "space-3" to s.space3, "space-4" to s.space4,
                "space-6" to s.space6, "space-8" to s.space8, "space-12" to s.space12,
            ),
        )
    }

    @Test
    fun `radius matches`() {
        val r = GharsRadius()
        assertDp(
            dpTokens("radius"),
            mapOf("radius-sm" to r.sm, "radius-md" to r.md, "radius-lg" to r.lg, "radius-pill" to r.pill),
        )
    }

    @Test
    fun `type styles match`() {
        val t = GharsTypography()
        val actual: Map<String, TextStyle> = mapOf(
            "timer" to t.timer, "display" to t.display, "title" to t.title, "heading" to t.heading,
            "body" to t.body, "label" to t.label, "caption" to t.caption,
        )
        val families = mapOf("display" to GharsDisplayFamily, "text" to GharsTextFamily)
        var seen = 0
        for (group in tokens["type"]!!.jsonObject["groups"]!!.jsonArray) {
            val family = families.getValue(group.jsonObject["family"]!!.jsonPrimitive.content)
            for (style in group.jsonObject["styles"]!!.jsonArray) {
                val o = style.jsonObject
                val name = o["name"]!!.jsonPrimitive.content
                val s = actual.getValue(name)
                assertEquals("$name size", o["fontSize"]!!.jsonPrimitive.content, "${s.fontSize.value.toInt()}px")
                assertEquals("$name line", o["lineHeight"]!!.jsonPrimitive.content, "${s.lineHeight.value.toInt()}px")
                assertEquals("$name weight", o["fontWeight"]!!.jsonPrimitive.content.toInt(), s.fontWeight!!.weight)
                assertEquals("$name family", family, s.fontFamily)
                seen++
            }
        }
        assertEquals(actual.size, seen)
        assertEquals("tnum", t.timer.fontFeatureSettings)
    }

    @Test
    fun `shadows match`() {
        val shadows = GharsShadows()
        val byName = tokens["shadow"]!!.jsonObject["tokens"]!!.jsonArray.associate {
            it.jsonObject["name"]!!.jsonPrimitive.content to it.jsonObject["value"]!!.jsonPrimitive.content
        }
        fun css(s: GharsShadow): String {
            val c = s.color
            val rgb = listOf(c.red, c.green, c.blue).joinToString(",") { Math.round(it * 255).toString() }
            val px = { d: Dp -> if (d.value == 0f) "0" else "${d.value.toInt()}px" }
            return "0 ${px(s.offsetY)} ${px(s.blur)} rgba($rgb,${"%.2f".format(c.alpha).trimEnd('0')})"
        }
        assertEquals(byName["shadow-soft"], css(shadows.soft))
        assertEquals(byName["shadow-glow"], css(shadows.glow))
    }

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    @Test
    fun `text pairs reach 4_5 to 1 in both themes`() {
        for (c in listOf(GharsLightColors, GharsFocusColors)) {
            val theme = if (c.isFocus) "dark" else "light"
            mapOf(
                "ink on bg" to (c.ink to c.bg),
                "ink on surface" to (c.ink to c.surface),
                "ink-muted on bg" to (c.inkMuted to c.bg),
                "ink-muted on surface" to (c.inkMuted to c.surface),
                "on-brand on brand" to (c.onBrand to c.brand),
                "ink on brand-soft" to (c.ink to c.brandSoft),
                "danger on bg" to (c.danger to c.bg),
            ).forEach { (pair, colors) ->
                val ratio = contrast(colors.first, colors.second)
                assertTrue("$pair ($theme) is $ratio", ratio >= 4.5f)
            }
        }
    }

    @Test
    fun `system colours in colors_xml mirror the tokens`() {
        val xml = File("src/main/res/values/colors.xml").readText()
        val c = GharsFocusColors
        mapOf(
            "ghars_sand" to c.sand, "ghars_night" to c.night, "ghars_brand_soft_dark" to c.brandSoft,
            "ghars_mint" to c.mint, "ghars_coin" to c.coin,
        ).forEach { (name, color) ->
            assertTrue(name, xml.contains("<color name=\"$name\">${hex(color)}</color>"))
        }
    }
}
