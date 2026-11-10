package app.imadnote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkspaceEntity::class, PageEntity::class, BlockEntity::class, ProjectEntity::class,
        TaskEntity::class, LabelEntity::class, TaskLabelCrossRef::class, EventEntity::class,
        CollectionEntity::class, PropertyEntity::class, RecordEntity::class, CellEntity::class,
        RecordRelationEntity::class, ViewEntity::class, AttachmentEntity::class,
        SearchHistoryEntity::class, SearchIndexEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workspaces(): WorkspaceDao
    abstract fun pages(): PageDao
    abstract fun blocks(): BlockDao
    abstract fun projects(): ProjectDao
    abstract fun tasks(): TaskDao
    abstract fun labels(): LabelDao
    abstract fun events(): EventDao
    abstract fun collections(): CollectionDao
    abstract fun attachments(): AttachmentDao
    abstract fun search(): SearchDao

    companion object {
        const val NAME = "imadnote.db"

        /**
         * Schema changes must add a Migration here and bump [Database.version]; the exported JSON
         * schemas in app/schemas are the reference for MigrationTestHelper. Destructive fallback is
         * deliberately not enabled: user data must never be dropped silently.
         */
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()

        fun inMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }
}
