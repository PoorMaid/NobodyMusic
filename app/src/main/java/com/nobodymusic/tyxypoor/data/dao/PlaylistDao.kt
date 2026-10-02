package com.nobodymusic.tyxypoor.data.dao

import androidx.room.*
import com.nobodymusic.tyxypoor.data.Playlist
import com.nobodymusic.tyxypoor.data.PlaylistSong
import com.nobodymusic.tyxypoor.data.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSong(item: PlaylistSong)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongs(items: List<PlaylistSong>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid AND songId = :sid")
    suspend fun removeSong(pid: Long, sid: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid")
    suspend fun clearSongs(pid: Long)

    @Query("SELECT s.* FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.songId WHERE ps.playlistId = :pid ORDER BY ps.orderIndex")
    fun observeSongs(pid: Long): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :pid")
    suspend fun count(pid: Long): Int

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun allOnce(): List<Playlist>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :pid ORDER BY orderIndex")
    suspend fun songIds(pid: Long): List<Long>

    @Query("SELECT * FROM playlists WHERE source != 'local' ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<Playlist>>
    @Query("SELECT * FROM playlists WHERE source != 'local' ORDER BY createdAt DESC")
    suspend fun favoritePlaylistsOnce(): List<Playlist>

    @Query("SELECT s.* FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.songId WHERE ps.playlistId = :pid ORDER BY ps.orderIndex")
    suspend fun songsOf(pid: Long): List<Song>

    @Query("SELECT ps.songId FROM playlist_songs ps INNER JOIN playlists p ON p.id = ps.playlistId WHERE p.name = :name")
    fun observeSongIds(name: String): Flow<List<Long>>

    @Query("SELECT ps.songId FROM playlist_songs ps INNER JOIN playlists p ON p.id = ps.playlistId WHERE p.name = :name")
    suspend fun songIdsOfName(name: String): List<Long>
}
