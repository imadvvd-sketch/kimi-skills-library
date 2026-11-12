package app.ghars.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.ghars.domain.economy.SpeciesSeeds
import kotlinx.coroutines.flow.Flow

data class DayTotalsRow(val sessions: Int, val minutes: Int)

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    /** Successful sessions that ended in [from, to). */
    @Query(
        "SELECT COUNT(*) AS sessions, COALESCE(SUM(planned_minutes), 0) AS minutes FROM session " +
            "WHERE status = 'success' AND ended_at >= :from AND ended_at < :to",
    )
    fun observeTotals(from: Long, to: Long): Flow<DayTotalsRow>

    /** Sessions planted in [from, to), oldest first. */
    @Query("SELECT * FROM session WHERE started_at >= :from AND started_at < :to ORDER BY started_at")
    fun observeStartedBetween(from: Long, to: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session ORDER BY started_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT started_at FROM session WHERE status = 'success'")
    fun observeSuccessStarts(): Flow<List<Long>>

    @Query(
        "SELECT COUNT(*) AS sessions, COALESCE(SUM(planned_minutes), 0) AS minutes FROM session " +
            "WHERE status = 'success' AND ended_at >= :from AND ended_at < :to",
    )
    suspend fun totals(from: Long, to: Long): DayTotalsRow
}

data class SpeciesWithOwnership(
    @Embedded val species: SpeciesEntity,
    @ColumnInfo(name = "owned") val owned: Boolean,
)

@Dao
interface SpeciesDao {
    @Query(
        "SELECT s.*, (u.species_id IS NOT NULL) AS owned FROM species s " +
            "LEFT JOIN user_species u ON u.species_id = s.id ORDER BY s.sort_order",
    )
    fun observeAll(): Flow<List<SpeciesWithOwnership>>

    @Query("SELECT * FROM species WHERE id = :id")
    suspend fun find(id: String): SpeciesEntity?

    @Query("SELECT COUNT(*) FROM user_species WHERE species_id = :id")
    suspend fun ownedCount(id: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addOwned(row: UserSpeciesEntity): Long
}

@Dao
interface TagDao {
    /** Built-in tags first, then custom ones in the order they were made. */
    @Query("SELECT * FROM tag ORDER BY is_builtin DESC, id")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT COUNT(*) FROM tag WHERE id = :id")
    suspend fun count(id: Long): Int

    @Insert
    suspend fun insert(tag: TagEntity): Long

    // Built-in tags cannot be changed or removed.
    @Query("UPDATE tag SET name = :name, color = :color WHERE id = :id AND is_builtin = 0")
    suspend fun update(id: Long, name: String, color: Int)

    @Query("DELETE FROM tag WHERE id = :id AND is_builtin = 0")
    suspend fun delete(id: Long)
}

@Dao
interface CoinDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_txn")
    fun observeBalance(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_txn")
    suspend fun balance(): Int

    /** Returns -1 if this reason and ref was already recorded. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(txn: CoinTxnEntity): Long

    @Query("SELECT ref_id FROM coin_txn WHERE reason = :reason AND ref_id LIKE :pattern")
    fun observeRefs(reason: String, pattern: String): Flow<List<String>>
}

@Database(
    entities = [
        SessionEntity::class, SpeciesEntity::class, UserSpeciesEntity::class, TagEntity::class,
        CoinTxnEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class GharsDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun speciesDao(): SpeciesDao
    abstract fun coinDao(): CoinDao
    abstract fun tagDao(): TagDao

    /** First-run content: every species (the free ones already owned) and the built-in tags. */
    object Seed : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            seedSpecies(db)
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

    companion object {
        // Must stay identical to what Room generates for CoinTxnEntity; MigrationSqlTest checks it
        // against the exported schema.
        const val CREATE_COIN_TXN =
            "CREATE TABLE IF NOT EXISTS `coin_txn` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`amount` INTEGER NOT NULL, `reason` TEXT NOT NULL, `ref_id` TEXT NOT NULL, `at` INTEGER NOT NULL)"
        const val CREATE_COIN_TXN_INDEX =
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_coin_txn_reason_ref_id` ON `coin_txn` (`reason`, `ref_id`)"

        /** Phase 2: the coin ledger and the seven species sold in the store. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_COIN_TXN)
                db.execSQL(CREATE_COIN_TXN_INDEX)
                seedSpecies(db)
            }
        }

        /** Safe to run on a database that already has some of the species. */
        fun seedSpecies(db: SupportSQLiteDatabase) {
            SpeciesSeeds.all.forEachIndexed { index, seed ->
                db.execSQL(
                    "INSERT OR IGNORE INTO species (id, price_coins, is_premium, is_event, sort_order) VALUES (?, ?, 0, 0, ?)",
                    arrayOf<Any>(seed.id, seed.priceCoins, index),
                )
                if (seed.priceCoins == 0) {
                    db.execSQL(
                        "INSERT OR IGNORE INTO user_species (species_id, acquired_at) VALUES (?, 0)",
                        arrayOf<Any>(seed.id),
                    )
                }
            }
        }
    }
}
