package app.ghars.data

import app.ghars.data.db.GharsDatabase
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Room refuses to open a migrated database whose tables differ from what it expects.
 * This compares the hand-written migration SQL with the schema Room exported.
 */
class MigrationSqlTest {
    private fun entities(version: Int) =
        Json.parseToJsonElement(File("schemas/app.ghars.data.db.GharsDatabase/$version.json").readText())
            .jsonObject["database"]!!.jsonObject["entities"]!!.jsonArray.map { it.jsonObject }

    @Test
    fun `migration 1 to 2 creates coin_txn exactly as Room expects`() {
        val table = entities(2).single { it["tableName"]!!.jsonPrimitive.content == "coin_txn" }
        val create = table["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", "coin_txn")
        val index = table["indices"]!!.jsonArray.single().jsonObject["createSql"]!!.jsonPrimitive.content
            .replace("\${TABLE_NAME}", "coin_txn")
        assertEquals(create, GharsDatabase.CREATE_COIN_TXN)
        assertEquals(index, GharsDatabase.CREATE_COIN_TXN_INDEX)
    }

    @Test
    fun `migration 1 to 2 leaves the existing tables untouched`() {
        val before = entities(1).associate { it["tableName"]!!.jsonPrimitive.content to it.toString() }
        val after = entities(2).associate { it["tableName"]!!.jsonPrimitive.content to it.toString() }
        assertEquals(before.keys + "coin_txn", after.keys)
        for ((name, schema) in before) assertEquals(name, schema, after[name])
        assertTrue(GharsDatabase.MIGRATION_1_2.startVersion == 1 && GharsDatabase.MIGRATION_1_2.endVersion == 2)
    }
}
