package com.nobodymusic.tyxypoor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val path: String,
    val size: Long,
    val isLocal: Boolean,
    val sourceId: String = "",
    val songmid: String = "",
    val cover: String = "",
    val addedAt: Long = System.currentTimeMillis()
) {
    val uid: String
        get() = if (id > 0) "db:$id" else "$sourceId:$songmid:${title.hashCode()}"
}

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "local",
    val linkId: String = "",
    val cover: String = "",
    val songCount: Int = 0
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSong(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int
)

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val jsUrl: String,
    val script: String,
    val enabled: Boolean,
    val priority: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history")
data class PlayHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)
