package com.nobodymusic.tyxypoor.source

import com.nobodymusic.tyxypoor.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ResolvedPlaylist(
    val name: String,
    val cover: String,
    val songs: List<Song>,
    val platform: String
)

object PlaylistLinkResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val UA =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1"

    private fun httpGet(url: String, referer: String? = null): String {
        val b = Request.Builder().url(url).header("User-Agent", UA)
        if (referer != null) b.header("Referer", referer)
        client.newCall(b.build()).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            return resp.body?.string() ?: ""
        }
    }

    fun isUrl(text: String): Boolean = extractUrl(text) != null

    fun extractUrl(text: String): String? {
        val m = Regex("""https?://[^\s"'<>）)]+""").find(text.trim()) ?: return null
        return m.value.trimEnd('.', ',', '。', '，')
    }

    suspend fun resolve(rawInput: String): ResolvedPlaylist = withContext(Dispatchers.IO) {
        val url = extractUrl(rawInput) ?: error("未找到链接")
        when {
            url.contains("163.com") -> resolveNetEase(url)
            url.contains("y.qq.com") || url.contains("c.y.qq.com") || url.contains("qq.com") -> resolveQQ(url)
            else -> error("暂不支持该平台链接")
        }
    }

    private fun neteaseId(url: String): String? {
        Regex("""[?&#]id=(\d+)""").find(url)?.let { return it.groupValues[1] }
        Regex("""/playlist/(\d+)""").find(url)?.let { return it.groupValues[1] }
        Regex("""[?&]playlistId=(\d+)""").find(url)?.let { return it.groupValues[1] }
        return null
    }

    private fun resolveNetEase(url: String): ResolvedPlaylist {
        val id = neteaseId(url) ?: error("无法识别网易云歌单 ID")
        val body = httpGet(
            "https://music.163.com/api/v6/playlist/detail?id=$id",
            "https://music.163.com/"
        )
        val root = JSONObject(body)
        if (root.optInt("code", -1) != 200) error("网易云接口返回错误 code=${root.optInt("code")}")
        val pl = root.optJSONObject("playlist") ?: error("歌单数据为空")
        val name = pl.optString("name", "导入的歌单")
        val cover = pl.optString("coverImgUrl", "")
        val trackTotal = pl.optInt("trackCount", 0)
        val tracks = pl.optJSONArray("tracks")
        val out = ArrayList<Song>()
        if (trackTotal > (tracks?.length() ?: 0)) {
            val ids = ArrayList<String>()
            pl.optJSONArray("trackIds")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    ids.add(o.optLong("id", 0L).toString())
                }
            }
            var i = 0
            while (i < ids.size) {
                val end = minOf(i + 500, ids.size)
                val chunk = ids.subList(i, end)
                val idsJson = "[" + chunk.joinToString(",") + "]"
                val api = "https://music.163.com/api/song/detail/?ids=" +
                    java.net.URLEncoder.encode(idsJson, "UTF-8")
                val b = runCatching { httpGet(api, "https://music.163.com/") }.getOrNull()
                if (b != null) {
                    val arr = runCatching { JSONObject(b).optJSONArray("songs") }.getOrNull()
                    if (arr != null) {
                        for (j in 0 until arr.length()) {
                            val t = arr.optJSONObject(j) ?: continue
                            out.add(songFromTrack(t))
                        }
                    }
                }
                i = end
            }
        }
        if (out.isEmpty() && tracks != null) {
            for (i in 0 until tracks.length()) {
                val t = tracks.optJSONObject(i) ?: continue
                out.add(songFromTrack(t))
            }
        }
        if (out.isEmpty()) error("歌单没有可导入的歌曲")
        return ResolvedPlaylist(name, cover, out, "网易云")
    }
    private fun songFromTrack(t: JSONObject): Song {
        val title = t.optString("name", "")
        val artists = StringBuilder()
        t.optJSONArray("ar")?.let { arr ->
            for (j in 0 until arr.length()) {
                if (j > 0) artists.append("/")
                artists.append(arr.optJSONObject(j)?.optString("name", "") ?: "")
            }
        }
        if (artists.isEmpty()) t.optJSONArray("artists")?.let { arr ->
            for (j in 0 until arr.length()) {
                if (j > 0) artists.append("/")
                artists.append(arr.optJSONObject(j)?.optString("name", "") ?: "")
            }
        }
        val album = t.optJSONObject("al")?.optString("name", "")
            ?: t.optJSONObject("album")?.optString("name", "") ?: ""
        val dur = t.optLong("dt", 0L).let { if (it > 0) it else t.optJSONObject("h")?.optLong("br", 0L) ?: 0L }
        return Song(
            title = title,
            artist = artists.toString().ifBlank { "未知" },
            album = album,
            durationMs = if (dur > 0 && dur < 10000000) dur else t.optLong("duration", 0L),
            uri = "",
            path = "",
            size = 0,
            isLocal = false,
            sourceId = "netease",
            songmid = t.optString("id", "")
        )
    }

    private fun resolveQQ(url: String): ResolvedPlaylist {
        val id = Regex("""[?&#]id=(\w+)""").find(url)?.groupValues?.get(1)
            ?: Regex("""/playlist/(\w+)""").find(url)?.groupValues?.get(1)
            ?: error("无法识别 QQ 音乐歌单 ID")
        val api = "https://c.y.qq.com/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg" +
                "?type=1&json=1&utf8=1&onlysong=0&disstid=$id&format=json"
        val body = httpGet(api, "https://y.qq.com/")
        var raw = body.trim()
        if (raw.startsWith("callback(")) raw = raw.removePrefix("callback(").substringBeforeLast(")")
        val root = JSONObject(raw)
        val cdArr = root.optJSONArray("cdlist") ?: error("QQ 音乐歌单解析失败")
        val cd = cdArr.optJSONObject(0) ?: error("QQ 音乐歌单解析失败")
        val list = cd.optJSONArray("songlist") ?: error("QQ 音乐歌单解析失败")
        val name = cd.optString("dissname", "").ifBlank { "导入的QQ歌单" }
        val out = ArrayList<Song>()
        for (i in 0 until list.length()) {
            val t = list.optJSONObject(i) ?: continue
            val title = t.optString("songname", "")
            if (title.isBlank()) continue
            val artists = StringBuilder()
            t.optJSONArray("singer")?.let { arr ->
                for (j in 0 until arr.length()) {
                    if (j > 0) artists.append("/")
                    artists.append(arr.optJSONObject(j)?.optString("name", "") ?: "")
                }
            }
            out.add(
                Song(
                    title = title,
                    artist = artists.toString().ifBlank { "未知" },
                    album = t.optString("albumname", ""),
                    durationMs = t.optLong("interval", 0L) * 1000,
                    uri = "",
                    path = "",
                    size = 0,
                    isLocal = false,
                    sourceId = "qq",
                    songmid = t.optString("songmid", "")
                )
            )
        }
        if (out.isEmpty()) error("QQ 歌单没有可导入的歌曲")
        return ResolvedPlaylist(name, "", out, "QQ音乐")
    }
}
