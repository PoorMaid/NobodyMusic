package com.nobodymusic.tyxypoor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nobodymusic.tyxypoor.data.dao.PlaylistDao
import com.nobodymusic.tyxypoor.data.dao.SongDao
import com.nobodymusic.tyxypoor.data.dao.SourceDao
import com.nobodymusic.tyxypoor.data.dao.HistoryDao

@Database(
    entities = [Song::class, Playlist::class, PlaylistSong::class, SourceEntity::class, PlayHistory::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun sourceDao(): SourceDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nobody_music.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}