package com.nobodymusic.tyxypoor.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    const val VERSION = 1

    private fun songToJson(s: Song) = JSONObject().apply {
        put("title", s.title)
        put("artist", s.artist)
        put("album", s.album)
        put("durationMs", s.durationMs)
        put("uri", s.uri)
        put("path", s.path)
        put("size", s.size)
        put("isLocal", s.isLocal)
        put("sourceId", s.sourceId)
        put("songmid", s.songmid)
        put("addedAt", s.addedAt)
    }

    private fun songFromJson(o: JSONObject) = Song(
        title = o.optString("title"),
        artist = o.optString("artist"),
        album = o.optString("album"),
        durationMs = o.optLong("durationMs"),
        uri = o.optString("uri"),
        path = o.optString("path"),
        size = o.optLong("size"),
        isLocal = o.optBoolean("isLocal"),
        sourceId = o.optString("sourceId"),
        songmid = o.optString("songmid"),
        addedAt = o.optLong("addedAt", System.currentTimeMillis())
    )

    fun exportJson(repo: MusicRepository): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val songsArr = JSONArray()
        runCatching {
            kotlinx.coroutines.runBlocking { repo.songsOnce() }
        }.getOrDefault(emptyList()).forEach { songsArr.put(songToJson(it)) }
        root.put("songs", songsArr)

        val plArr = JSONArray()
        val pairs = mutableListOf<Pair<Long, List<Long>>>()
        kotlinx.coroutines.runBlocking {
            val pls = repo.playlistsOnce()
            pls.forEach { p ->
                pairs.add(p.id to repo.songIdsOf(p.id))
            }
        }
        kotlinx.coroutines.runBlocking {
            repo.playlistsOnce().forEach { p ->
                plArr.put(JSONObject().apply {
                    put("name", p.name)
                    put("createdAt", p.createdAt)
                })
            }
        }
        root.put("playlists", plArr)

        val psArr = JSONArray()
        pairs.forEach { (_, ids) ->
            ids.forEach { psArr.put(it) }
        }
        root.put("playlistSongIds", psArr)

        val srcArr = JSONArray()
        kotlinx.coroutines.runBlocking { repo.sourcesOnce() }.forEach { s ->
            srcArr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("jsUrl", s.jsUrl)
                put("script", s.script)
                put("enabled", s.enabled)
            })
        }
        root.put("sources", srcArr)

        return root.toString(2)
    }

    suspend fun importJson(repo: MusicRepository, json: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = JSONObject(json)
                var songCount = 0
                var plCount = 0

                val songs = root.optJSONArray("songs") ?: JSONArray()
                for (i in 0 until songs.length()) {
                    val s = songFromJson(songs.getJSONObject(i))
                    repo.insertSong(s)
                    songCount++
                }

                val pls = root.optJSONArray("playlists") ?: JSONArray()
                for (i in 0 until pls.length()) {
                    val p = pls.getJSONObject(i)
                    repo.createPlaylist(
                        p.optString("name"),
                        p.optLong("createdAt", System.currentTimeMillis())
                    )
                    plCount++
                }

                val ps = root.optJSONArray("playlistSongIds") ?: JSONArray()
                if (ps.length() > 0) {
                    var pid = repo.createPlaylist("导入的歌单", System.currentTimeMillis())
                    repo.addSongsToPlaylist(pid, (0 until ps.length()).map { ps.optLong(it) })
                }

                val srcs = root.optJSONArray("sources") ?: JSONArray()
                for (i in 0 until srcs.length()) {
                    val s = srcs.getJSONObject(i)
                    repo.upsertSource(
                        s.optString("id"),
                        s.optString("name"),
                        s.optString("jsUrl"),
                        s.optBoolean("enabled", true),
                        s.optString("script")
                    )
                }
                "导入完成：歌曲 $songCount 首，歌单 $plCount 个"
            }
        }

    fun writeToUri(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
    }

    fun readFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: ""
    }
}
