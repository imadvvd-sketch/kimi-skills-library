package app.ghars.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A tree species. Names and artwork live in the app; the table holds what can change (price, flags). */
@Entity(tableName = "species")
data class SpeciesEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "price_coins") val priceCoins: Int,
    @ColumnInfo(name = "is_premium") val isPremium: Boolean,
    @ColumnInfo(name = "is_event") val isEvent: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "user_species",
    foreignKeys = [ForeignKey(SpeciesEntity::class, ["id"], ["species_id"], onDelete = ForeignKey.CASCADE)],
)
data class UserSpeciesEntity(
    @PrimaryKey @ColumnInfo(name = "species_id") val speciesId: String,
    @ColumnInfo(name = "acquired_at") val acquiredAt: Long,
)

/** Built-in tags keep a stable key in [name]; their display text comes from string resources. */
@Entity(tableName = "tag")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    @ColumnInfo(name = "is_builtin") val isBuiltin: Boolean,
)

/** One finished session. Withered and given-up trees stay here for good. */
@Entity(
    tableName = "session",
    foreignKeys = [
        ForeignKey(SpeciesEntity::class, ["id"], ["species_id"]),
        ForeignKey(TagEntity::class, ["id"], ["tag_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("species_id"), Index("tag_id"), Index("started_at")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "species_id") val speciesId: String,
    @ColumnInfo(name = "tag_id") val tagId: Long? = null,
    @ColumnInfo(name = "planned_minutes") val plannedMinutes: Int,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    /** success | withered | given_up */
    val status: String,
    @ColumnInfo(name = "deep_focus") val deepFocus: Boolean = false,
    @ColumnInfo(name = "paused_seconds") val pausedSeconds: Int = 0,
    @ColumnInfo(name = "coins_earned") val coinsEarned: Int = 0,
)

/**
 * Every change to the coin balance; the balance is the sum of [amount].
 * [refId] names what caused it (session id, species id, or a challenge claim),
 * and is unique per reason so nothing is paid or charged twice.
 */
@Entity(tableName = "coin_txn", indices = [Index(value = ["reason", "ref_id"], unique = true)])
data class CoinTxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Int,
    /** session | purchase | challenge */
    val reason: String,
    @ColumnInfo(name = "ref_id") val refId: String,
    val at: Long,
)
