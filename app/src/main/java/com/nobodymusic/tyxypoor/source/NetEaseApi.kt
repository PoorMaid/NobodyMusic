package com.nobodymusic.tyxypoor.source

import android.util.Log
import com.nobodymusic.tyxypoor.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetEaseApi {

    private const val TAG = "NetEaseApi"

    private const val UA =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 " +
            "(KHTML, like Gecko) Mobile/15E148"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun get(url: String, referer: String = "https://music.163.com/"): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", referer)
            .header("Accept", "application/json, text/plain, */*")
            .build()
        val holder = java.util.concurrent.atomic.AtomicReference<Result<String>>()
        val done = java.util.concurrent.CountDownLatch(1)
        Thread {
            val r = runCatching {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                    resp.body?.string() ?: throw IOException("空响应")
                }
            }
            holder.set(r)
            done.countDown()
        }.start()
        if (!done.await(30, java.util.concurrent.TimeUnit.SECONDS)) throw IOException("请求超时")
        return holder.get().getOrThrow()
    }

    private fun songFromNetEase(t: JSONObject, platform: String): Song? {
        val title = t.optString("name", "")
        if (title.isBlank()) return null
        val artist = runCatching {
            val ar = t.optJSONArray("ar") ?: t.optJSONArray("artists")
            val sb = StringBuilder()
            if (ar != null) {
                for (i in 0 until ar.length()) {
                    val o = ar.optJSONObject(i) ?: continue
                    if (sb.isNotEmpty()) sb.append("/")
                    sb.append(o.optString("name"))
                }
            }
            sb.toString()
        }.getOrDefault("")
        val album = runCatching {
            (t.optJSONObject("al") ?: t.optJSONObject("album"))?.optString("name", "") ?: ""
        }.getOrDefault("")
        val cover = runCatching {
            (t.optJSONObject("al") ?: t.optJSONObject("album"))?.optString("picUrl", "") ?: ""
        }.getOrDefault("")
        val dur = t.optLong("dt", 0L).let { if (it <= 0) t.optLong("duration", 0L) else it }
        val mid = t.optLong("id", 0L).toString()
        return Song(
            title = title,
            artist = artist,
            album = album,
            durationMs = if (dur in 1..3_600_000L) dur else 0L,
            uri = "",
            path = "",
            size = 0L,
            isLocal = false,
            sourceId = platform,
            songmid = mid,
            cover = cover
        )
    }

    suspend fun search(keyword: String): List<Song> {
        if (keyword.isBlank()) return emptyList()
        return try {
            val kw = java.net.URLEncoder.encode(keyword, "UTF-8")
            val url = "https://music.163.com/api/search/get/web?csrf_token=" +
                "&s=$kw&type=1&offset=0&total=true&limit=30"
            val json = JSONObject(get(url))
            val songs = json.optJSONObject("result")?.optJSONArray("songs")
            val out = mutableListOf<Song>()
            if (songs != null) {
                for (i in 0 until songs.length()) {
                    val o = songs.optJSONObject(i) ?: continue
                    songFromNetEase(o, "netease")?.let { out += it }
                }
            }
            Log.i(TAG, "search('$keyword') -> ${out.size} songs")
            out
        } catch (e: Exception) {
            Log.e(TAG, "search('$keyword') failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun playlistDetail(id: String): ResolvedPlaylist? {
        return try {
            val url = "https://music.163.com/api/v6/playlist/detail?id=$id"
            val json = JSONObject(get(url))
            if (json.optInt("code", -1) != 200) {
                Log.w(TAG, "playlistDetail($id) code=${json.optInt("code", -1)}")
                return null
            }
            val pl = json.optJSONObject("playlist") ?: return null
            val name = pl.optString("name", "网易云歌单")
            val cover = pl.optString("coverImgUrl", "")
            val trackTotal = pl.optInt("trackCount", 0)
            val songs = mutableListOf<Song>()
            val firstTracks = pl.optJSONArray("tracks")
            if (firstTracks != null) {
                for (i in 0 until firstTracks.length()) {
                    val o = firstTracks.optJSONObject(i) ?: continue
                    songFromNetEase(o, "netease")?.let { songs += it }
                }
            }
            if (songs.size < trackTotal || songs.isEmpty()) {
                val ids = ArrayList<String>()
                pl.optJSONArray("trackIds")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        ids.add(o.optLong("id", 0L).toString())
                    }
                }
                if (ids.isNotEmpty()) {
                    val all = fetchAllTracks(ids)
                    if (all.isNotEmpty()) {
                        songs.clear()
                        songs.addAll(all)
                    }
                }
            }
            Log.i(TAG, "playlistDetail($id) '$name' -> ${songs.size}/$trackTotal songs")
            ResolvedPlaylist(name, cover, songs, "netease")
        } catch (e: Exception) {
            Log.e(TAG, "playlistDetail($id) failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun fetchAllTracks(songIds: List<String>): List<Song> {
        val out = mutableListOf<Song>()
        var i = 0
        while (i < songIds.size) {
            val end = minOf(i + 500, songIds.size)
            val chunk = songIds.subList(i, end)
            val idsJson = "[" + chunk.joinToString(",") + "]"
            val url = "https://music.163.com/api/song/detail/?ids=" +
                java.net.URLEncoder.encode(idsJson, "UTF-8")
            val body = runCatching { get(url) }.getOrNull()
            if (body != null) {
                val arr = runCatching { JSONObject(body).optJSONArray("songs") }.getOrNull()
                if (arr != null) {
                    for (j in 0 until arr.length()) {
                        val o = arr.optJSONObject(j) ?: continue
                        songFromNetEase(o, "netease")?.let { out += it }
                    }
                }
            }
            i = end
        }
        Log.i(TAG, "fetchAllTracks(${songIds.size}) -> ${out.size}")
        return out
    }

    suspend fun songUrl(songId: String, br: Int = 320000): String? {
        if (songId.isBlank()) return null
        return try {
            val url = "https://music.163.com/api/song/enhance/player/url" +
                "?id=$songId&ids=%5B$songId%5D&br=$br"
            val json = JSONObject(get(url))
            val arr = json.optJSONArray("data")
            val o = arr?.optJSONObject(0)
            val u = o?.optString("url", "") ?: ""
            var ok = (u.startsWith("http://") || u.startsWith("https://")) && u != "null"
            if (ok && o != null) {
                val trial = o.optJSONObject("freeTrialInfo")
                val fee = o.optInt("fee", 0)
                val dl = o.optLong("size", 0L)
                if (trial != null && trial.length() > 0) ok = false
                if (fee == 1 && dl in 1..2_000_000L) ok = false
            }
            Log.i(TAG, "songUrl($songId) br=$br -> ${if (ok) "OK ${u.take(60)}" else "TRIAL/EMPTY"}")
            if (ok) u else null
        } catch (e: Exception) {
            Log.e(TAG, "songUrl($songId) failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun lyric(songId: String): Pair<String, String> {
        if (songId.isBlank()) return "" to ""
        return try {
            val url = "https://music.163.com/api/song/lyric" +
                "?id=$songId&lv=-1&kv=-1&tv=-1"
            val json = JSONObject(get(url))
            val lrc = json.optJSONObject("lrc")?.optString("lyric", "") ?: ""
            val tlrc = json.optJSONObject("tlyric")?.optString("lyric", "") ?: ""
            lrc to tlrc
        } catch (e: Exception) {
            Log.e(TAG, "lyric($songId) failed: ${e.message}")
            "" to ""
        }
    }

    suspend fun lyricByKeyword(title: String, artist: String): Pair<String, String> {
        if (title.isBlank()) return "" to ""
        val kw = (title + " " + artist).trim()
        val list = runCatching { search(kw) }.getOrDefault(emptyList())
        val hit = list.firstOrNull { it.sourceId == "netease" && it.songmid.isNotBlank() }
            ?: return "" to ""
        return lyric(hit.songmid)
    }
    data class TopList(val id: String, val name: String, val cover: String)

    suspend fun topLists(): List<TopList> {
        return try {
            val json = JSONObject(get("https://music.163.com/api/toplist"))
            val arr = json.optJSONArray("list")
            val out = mutableListOf<TopList>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    out += TopList(
                        id = o.optLong("id", 0L).toString(),
                        name = o.optString("name", ""),
                        cover = o.optString("coverImgUrl", "")
                    )
                }
            }
            Log.i(TAG, "topLists -> ${out.size}")
            out
        } catch (e: Exception) {
            Log.e(TAG, "topLists failed: ${e.message}", e)
            throw e
        }
    }

    data class HotPlaylist(val id: String, val name: String, val cover: String, val playCount: Long)

    suspend fun hotPlaylists(limit: Int = 30): List<HotPlaylist> {
        return try {
            val json = JSONObject(get("https://music.163.com/api/playlist/list?cat=全部&order=hot&limit=$limit&offset=0"))
            val arr = json.optJSONArray("playlists")
            val out = mutableListOf<HotPlaylist>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    out += HotPlaylist(
                        id = o.optLong("id", 0L).toString(),
                        name = o.optString("name", ""),
                        cover = o.optString("coverImgUrl", ""),
                        playCount = o.optLong("playCount", 0L)
                    )
                }
            }
            Log.i(TAG, "hotPlaylists($limit) -> ${out.size}")
            out
        } catch (e: Exception) {
            Log.e(TAG, "hotPlaylists failed: ${e.message}", e)
            throw e
        }
    }
}