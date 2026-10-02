package com.nobodymusic.tyxypoor.data

import android.content.Context
import com.nobodymusic.tyxypoor.data.dao.PlaylistDao
import com.nobodymusic.tyxypoor.data.dao.SongDao
import com.nobodymusic.tyxypoor.data.dao.SourceDao
import kotlinx.coroutines.flow.Flow

class MusicRepository(
    val songDao: SongDao,
    val playlistDao: PlaylistDao,
    val sourceDao: SourceDao,
    val historyDao: com.nobodymusic.tyxypoor.data.dao.HistoryDao
) {
    companion object { const val FAV_NAME = "\u2764 我喜欢" }

    suspend fun favoritePlaylistId(): Long {
        val existing = playlistDao.allOnce().firstOrNull { it.name == FAV_NAME }
        if (existing != null) return existing.id
        return playlistDao.insert(Playlist(name = FAV_NAME))
    }

    suspend fun toggleFavorite(sid: Long): Boolean {
        val pid = favoritePlaylistId()
        val ids = playlistDao.songIds(pid)
        return if (ids.contains(sid)) {
            playlistDao.removeSong(pid, sid); false
        } else {
            val idx = playlistDao.count(pid)
            playlistDao.addSong(PlaylistSong(pid, sid, idx)); true
        }
    }

    suspend fun isFavorite(sid: Long): Boolean {
        val pid = playlistDao.allOnce().firstOrNull { it.name == FAV_NAME }?.id ?: return false
        return playlistDao.songIds(pid).contains(sid)
    }

    suspend fun favoritesOnce(): List<Song> {
        val pid = playlistDao.allOnce().firstOrNull { it.name == FAV_NAME }?.id
            ?: return emptyList()
        return playlistDao.songsOf(pid)
    }

    fun favoriteSongIds(): Flow<List<Long>> = playlistDao.observeSongIds(FAV_NAME)

    suspend fun favoriteSongIdsOnce(): Set<Long> =
        playlistDao.songIdsOfName(FAV_NAME).toHashSet()

    suspend fun recordPlay(sid: Long) {
        val t = System.currentTimeMillis()
        if (historyDao.countOf(sid) > 0) historyDao.touch(sid, t)
        else historyDao.insert(PlayHistory(songId = sid, playedAt = t))
    }

    suspend fun historyOnce(limit: Int = 100): List<Song> =
        historyDao.recent(limit).mapNotNull { songDao.byId(it.songId) }

    suspend fun clearHistory() = historyDao.clear()

    fun songs(): Flow<List<Song>> = songDao.observeAll()
    fun localSongs(): Flow<List<Song>> = songDao.observeLocal()
    fun search(kw: String): Flow<List<Song>> = songDao.search(kw)
    suspend fun song(id: Long) = songDao.byId(id)
    suspend fun insertSong(song: Song) = songDao.insert(song)
    suspend fun insertSongs(list: List<Song>) = songDao.insertAll(list)
    suspend fun deleteSong(song: Song) = songDao.delete(song)

    fun playlists(): Flow<List<Playlist>> = playlistDao.observeAll()
    suspend fun createPlaylist(name: String) = playlistDao.insert(Playlist(name = name))
    suspend fun deletePlaylist(id: Long) = playlistDao.deletePlaylist(id)
    suspend fun renamePlaylist(id: Long, name: String) = playlistDao.rename(id, name)
    suspend fun addToPlaylist(pid: Long, sid: Long) {
        val idx = playlistDao.count(pid)
        playlistDao.addSong(PlaylistSong(pid, sid, idx))
    }
    suspend fun removeFromPlaylist(pid: Long, sid: Long) = playlistDao.removeSong(pid, sid)
    fun playlistSongs(pid: Long): Flow<List<Song>> = playlistDao.observeSongs(pid)

    fun sources(): Flow<List<SourceEntity>> = sourceDao.observeAll()
    suspend fun enabledSources() = sourceDao.enabled()
    suspend fun upsertSource(e: SourceEntity) = sourceDao.upsert(e)
    suspend fun setSourceEnabled(id: String, on: Boolean) = sourceDao.setEnabled(id, on)
    suspend fun setSourcePriority(id: String, p: Int) = sourceDao.setPriority(id, p)
    suspend fun deleteSource(id: String) = sourceDao.delete(id)

    suspend fun songsOnce(): List<Song> = songDao.allOnce()
    suspend fun localSongsOnce(): List<Song> = songDao.allOnce().filter { it.isLocal }
    suspend fun playlistsOnce(): List<Playlist> = playlistDao.allOnce()
    suspend fun sourcesOnce(): List<SourceEntity> = sourceDao.allOnce()
    suspend fun songIdsOf(pid: Long): List<Long> = playlistDao.songIds(pid)
    suspend fun createPlaylist(name: String, createdAt: Long): Long =
        playlistDao.insert(Playlist(name = name, createdAt = createdAt))
    suspend fun addSongsToPlaylist(pid: Long, ids: List<Long>) {
        var idx = playlistDao.count(pid)
        ids.forEach { sid ->
            playlistDao.addSong(PlaylistSong(pid, sid, idx))
            idx++
        }
    }
    suspend fun upsertSource(
        id: String,
        name: String,
        jsUrl: String,
        enabled: Boolean,
        script: String
    ) {
        sourceDao.upsert(
            SourceEntity(
                id = id,
                name = name,
                jsUrl = jsUrl,
                enabled = enabled,
                script = script
            )
        )
    }

    suspend fun saveSongIfNeeded(song: Song): Song {
        if (song.id > 0) return song
        val exist = songDao.allOnce().firstOrNull {
            it.sourceId == song.sourceId && it.songmid == song.songmid && song.songmid.isNotBlank()
        }
        if (exist != null) return exist
        val nid = songDao.insert(song.copy(id = 0, addedAt = System.currentTimeMillis()))
        return if (nid > 0) song.copy(id = nid) else song
    }

    suspend fun toggleFavoriteSong(song: Song): Boolean {
        val s = saveSongIfNeeded(song)
        val pid = favoritePlaylistId()
        val ids = playlistDao.songIds(pid)
        return if (ids.contains(s.id)) {
            playlistDao.removeSong(pid, s.id); false
        } else {
            val idx = playlistDao.count(pid)
            playlistDao.addSong(PlaylistSong(pid, s.id, idx)); true
        }
    }

    suspend fun isFavoriteSong(song: Song): Boolean {
        if (song.id > 0) return isFavorite(song.id)
        val pid = playlistDao.allOnce().firstOrNull { it.name == FAV_NAME }?.id ?: return false
        val s = songDao.allOnce().firstOrNull {
            it.sourceId == song.sourceId && it.songmid == song.songmid && song.songmid.isNotBlank()
        } ?: return false
        return playlistDao.songIds(pid).contains(s.id)
    }

    fun favoritePlaylists(): Flow<List<Playlist>> = playlistDao.observeFavorites()

    suspend fun favoritePlaylistsOnce(): List<Playlist> = playlistDao.favoritePlaylistsOnce()

    suspend fun isPlaylistFavorite(key: String): Boolean =
        playlistDao.favoritePlaylistsOnce().any { it.linkId == key && key.isNotBlank() }

    suspend fun togglePlaylistFavorite(
        name: String,
        linkId: String,
        cover: String,
        songCount: Int,
        songs: List<Song>
    ): Boolean {
        val favs = playlistDao.favoritePlaylistsOnce()
        val exist = favs.firstOrNull { it.linkId == linkId && linkId.isNotBlank() }
            ?: favs.firstOrNull { it.source != "local" && it.name == name && it.linkId.isBlank() }
        if (exist != null) {
            playlistDao.clearSongs(exist.id)
            playlistDao.deletePlaylist(exist.id)
            return false
        }
        val pid = playlistDao.insert(
            Playlist(name = name, source = "netease", linkId = linkId, cover = cover, songCount = songCount)
        )
        var idx = 0
        for (s in songs) {
            val saved = saveSongIfNeeded(s)
            playlistDao.addSong(PlaylistSong(pid, saved.id, idx))
            idx++
        }
        return true
    }

    suspend fun importSongsAsFavoritePlaylist(
        name: String,
        source: String,
        linkId: String,
        cover: String,
        songs: List<Song>,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): Long {
        if (songs.isEmpty()) return -1L
        val exist = playlistDao.favoritePlaylistsOnce().firstOrNull {
            (linkId.isNotBlank() && it.linkId == linkId) || (linkId.isBlank() && it.name == name)
        }
        if (exist != null) {
            playlistDao.clearSongs(exist.id)
            playlistDao.deletePlaylist(exist.id)
        }
        val pid = playlistDao.insert(
            Playlist(
                name = name,
                source = source.ifBlank { "local" },
                linkId = linkId,
                cover = cover,
                songCount = songs.size
            )
        )
        val existing = songDao.allOnce()
        val byKey = HashMap<String, Long>()
        for (s in existing) {
            if (s.songmid.isNotBlank()) byKey[s.sourceId + "|" + s.songmid] = s.id
            else if (s.path.isNotBlank()) byKey["path|" + s.path] = s.id
        }
        val total = songs.size
        val batchSize = 200
        val newSongs = ArrayList<Song>(total)
        val orderKeys = ArrayList<String>(total)
        var done = 0
        var i = 0
        while (i < total) {
            val end = minOf(i + batchSize, total)
            for (j in i until end) {
                val s = songs[j]
                val key = if (s.songmid.isNotBlank()) s.sourceId + "|" + s.songmid
                else if (s.path.isNotBlank()) "path|" + s.path else "t|" + s.title + "|" + s.artist
                if (key in byKey) {
                    orderKeys.add("id:" + byKey[key])
                } else {
                    byKey[key] = -1L
                    orderKeys.add("pending:" + key)
                    newSongs.add(s.copy(id = 0, addedAt = System.currentTimeMillis()))
                }
            }
            done = end
            onProgress(done, total)
            i = end
        }
        val insertedIds = if (newSongs.isEmpty()) emptyList() else songDao.insertAll(newSongs)
        var ni = 0
        val pendingId = HashMap<String, Long>()
        for (s in newSongs) {
            val key = if (s.songmid.isNotBlank()) s.sourceId + "|" + s.songmid
            else if (s.path.isNotBlank()) "path|" + s.path else "t|" + s.title + "|" + s.artist
            val rid = if (ni in insertedIds.indices) insertedIds[ni] else -1L
            if (rid > 0) pendingId[key] = rid
            ni++
        }
        val links = ArrayList<PlaylistSong>(total)
        var idx = 0
        for (k in orderKeys) {
            val sid = when {
                k.startsWith("id:") -> k.removePrefix("id:").toLongOrNull() ?: -1L
                k.startsWith("pending:") -> pendingId[k.removePrefix("pending:")] ?: -1L
                else -> -1L
            }
            if (sid > 0) { links.add(PlaylistSong(pid, sid, idx)); idx++ }
        }
        if (links.isNotEmpty()) playlistDao.addSongs(links)
        playlistDao.rename(pid, name)
        return pid
    }

    suspend fun deleteFavoritePlaylist(pid: Long) {
        playlistDao.clearSongs(pid)
        playlistDao.deletePlaylist(pid)
    }

    suspend fun savePlaylistAsFavoriteLocal(pid: Long): Long {
        val p = playlistDao.allOnce().firstOrNull { it.id == pid } ?: return -1
        if (p.source == "local" && p.linkId.isBlank()) {
            playlistDao.rename(pid, "⭐ " + p.name)
            return pid
        }
        return pid
    }
}
