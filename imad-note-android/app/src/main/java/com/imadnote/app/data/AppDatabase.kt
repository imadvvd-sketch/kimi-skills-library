package com.imadnote.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.imadnote.app.domain.Recurrence

class Converters {
    @TypeConverter
    fun recurrenceToString(value: Recurrence): String = value.name

    @TypeConverter
    fun stringToRecurrence(value: String): Recurrence =
        Recurrence.entries.firstOrNull { it.name == value } ?: Recurrence.NONE
}

@Database(
    entities = [
        ProjectEntity::class,
        NoteEntity::class,
        ChecklistItemEntity::class,
        AttachmentEntity::class,
        TaskEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao

    companion object {
        // No destructive fallback: schema changes must ship a Migration so user data survives upgrades.
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "imad_note.db").build()
    }
}
