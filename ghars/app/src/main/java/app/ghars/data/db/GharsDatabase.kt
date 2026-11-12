package app.ghars.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT COUNT(*) FROM session")
    suspend fun count(): Int
}

@Dao
interface SpeciesDao {
    @Query(
        "SELECT s.* FROM species s JOIN user_species u ON u.species_id = s.id ORDER BY s.sort_order",
    )
    fun observeOwned(): Flow<List<SpeciesEntity>>
}

@Database(
    entities = [SessionEntity::class, SpeciesEntity::class, UserSpeciesEntity::class, TagEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class GharsDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun speciesDao(): SpeciesDao

    /** First-run content: the three free species, already owned, and the built-in tags. */
    object Seed : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            listOf("olive", "lemon", "cactus").forEachIndexed { index, id ->
                db.execSQL(
                    "INSERT INTO species (id, price_coins, is_premium, is_event, sort_order) VALUES (?, 0, 0, 0, ?)",
                    arrayOf<Any>(id, index),
                )
                db.execSQL("INSERT INTO user_species (species_id, acquired_at) VALUES (?, 0)", arrayOf<Any>(id))
            }
            // Colours are the fixed illustration palette from the design tokens: leaf, clay, mint, night.
            listOf(
                "study" to 0xFF1E7A52.toInt(),
                "work" to 0xFFB8532F.toInt(),
                "reading" to 0xFF3DDC97.toInt(),
                "sport" to 0xFF0B1A15.toInt(),
            ).forEach { (name, color) ->
                db.execSQL("INSERT INTO tag (name, color, is_builtin) VALUES (?, ?, 1)", arrayOf<Any>(name, color))
            }
        }
    }
}
