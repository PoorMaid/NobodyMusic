package com.nobodymusic.tyxypoor.data.dao

import androidx.room.*
import com.nobodymusic.tyxypoor.data.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isLocal = 1 ORDER BY title COLLATE NOCASE")
    fun observeLocal(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): Song?

    @Query("SELECT * FROM songs WHERE path = :path LIMIT 1")
    suspend fun byPath(path: String): Song?

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :kw || '%' OR artist LIKE '%' || :kw || '%' OR album LIKE '%' || :kw || '%'")
    fun search(kw: String): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: Song): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(songs: List<Song>): List<Long>

    @Update
    suspend fun update(song: Song)

    @Delete
    suspend fun delete(song: Song)

    @Query("DELETE FROM songs WHERE isLocal = 1")
    suspend fun clearLocal()

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    suspend fun allOnce(): List<Song>
    @Query("SELECT * FROM songs WHERE isLocal = 1 ORDER BY title COLLATE NOCASE")
    suspend fun localOnce(): List<Song>
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(h: com.nobodymusic.tyxypoor.data.PlayHistory)

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<com.nobodymusic.tyxypoor.data.PlayHistory>

    @Query("DELETE FROM play_history")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM play_history WHERE songId = :sid")
    suspend fun countOf(sid: Long): Int

    @Query("UPDATE play_history SET playedAt = :t, playCount = playCount + 1 WHERE songId = :sid")
    suspend fun touch(sid: Long, t: Long)
}