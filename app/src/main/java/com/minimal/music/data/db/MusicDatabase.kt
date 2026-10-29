package com.minimal.music.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        AddedFolderEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun addedFolderDao(): AddedFolderDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN folder_name TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE songs ADD COLUMN folder_path TEXT DEFAULT NULL")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS added_folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        path TEXT NOT NULL,
                        song_count INTEGER NOT NULL DEFAULT 0,
                        date_added INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE added_folders ADD COLUMN artwork_uri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE added_folders ADD COLUMN is_artwork_confirmed INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE added_folders ADD COLUMN candidate_urls TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE added_folders ADD COLUMN candidate_index INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "minimal_music_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
