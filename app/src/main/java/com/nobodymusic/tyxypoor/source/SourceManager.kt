package com.nobodymusic.tyxypoor.source

import com.nobodymusic.tyxypoor.data.MusicRepository
import com.nobodymusic.tyxypoor.data.Song
import com.nobodymusic.tyxypoor.data.SourceEntity
import com.nobodymusic.tyxypoor.source.js.JsSourceEngine
import com.nobodymusic.tyxypoor.source.js.JsNetwork
import com.nobodymusic.tyxypoor.source.js.OkHttpJsNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

val DEFAULT_SUBSCRIPTIONS: List<Pair<String, String>> = listOf(
    "聚合" to "https://raw.githubusercontent.com/pdone/lx-music-source/main/juhe/latest.js",
    "Grass" to "https://raw.githubusercontent.com/pdone/lx-music-source/main/grass/latest.js",
    "Ikun" to "https://raw.githubusercontent.com/pdone/lx-music-source/main/ikun/latest.js",
    "LX" to "https://raw.githubusercontent.com/pdone/lx-music-source/main/lx/latest.js",
    "Flower" to "https://raw.githubusercontent.com/pdone/lx-music-source/main/flower/latest.js",
    "Huibq" to "https://raw.githubusercontent.com/pdone/lx-music-source/main/huibq/latest.js",
    "Sixyin" to "https://raw.githubusercontent.com/pdone/lx-music-source/main/sixyin/latest.js"
)
class SourceManager(
    private val repo: MusicRepository,
    private val net: JsNetwork = OkHttpJsNetwork(),
    private val http: OkHttpDownloader = OkHttpDownloader()
) {
    private val engines = mutableMapOf<String, JsSourceEngine>()

    suspend fun downloadScript(url: String): String = withContext(Dispatchers.IO) {
        val mirrors = mutableListOf(url)
        if (url.startsWith("https://raw.githubusercontent.com/")) {
            val path = url.removePrefix("https://raw.githubusercontent.com/")
            val parts = path.split("/", limit = 3)
            if (parts.size == 3) {
                val (owner, repo, rest) = parts
                mirrors.add("https://cdn.jsdelivr.net/gh/$owner/$repo@main/$rest")
                mirrors.add("https://ghproxy.net/https://raw.githubusercontent.com/$owner/$repo/main/$rest")
                mirrors.add("https://gh-proxy.com/https://raw.githubusercontent.com/$owner/$repo/main/$rest")
            }
        }
        var last: Throwable? = null
        for (m in mirrors) {
            try {
                val text = http.getText(m)
                if (text.isNotBlank()) return@withContext text
            } catch (t: Throwable) {
                last = t
            }
        }
        throw (last ?: IllegalStateException("下载失败"))
    }

    suspend fun addSource(
        id: String,
        name: String,
        jsUrl: String,
        autoDownload: Boolean = true
    ): Result<SourceEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val script = if (autoDownload) downloadScript(jsUrl) else ""
            require(script.isNotBlank()) { "脚本下载失败或为空" }
            val entity = SourceEntity(
                id = id, name = name, jsUrl = jsUrl, script = script, enabled = true
            )
            repo.upsertSource(entity)
            engineFor(entity)
            entity
        }
    }

    suspend fun addSourceRaw(id: String, name: String, script: String): Result<SourceEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(script.isNotBlank()) { "脚本内容为空" }
                val entity = SourceEntity(
                    id = id, name = name, jsUrl = "local://$id", script = script, enabled = true
                )
                repo.upsertSource(entity)
                engineFor(entity)
                entity
            }
        }

    suspend fun importBatch(
        items: List<Pair<String, String>>,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): List<String> = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        items.forEachIndexed { i, (n, u) ->
            onProgress(i + 1, items.size, n)
            addSource(id = n.trim(), name = n.trim(), jsUrl = u.trim())
                .onFailure { errors += "$n: ${it.message}" }
        }
        errors
    }

    suspend fun importJsonConfig(json: String): List<String> = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        runCatching {
            val arr = JSONArray(json)
            val items = (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val n = o.optString("name")
                val u = o.optString("url").ifBlank { o.optString("jsUrl") }
                if (n.isBlank() || u.isBlank()) null else n to u
            }
            items.forEach { (n, u) ->
                addSource(id = n, name = n, jsUrl = u).onFailure { errors += "$n: ${it.message}" }
            }
        }.onFailure { errors += "JSON 解析失败: ${it.message}" }
        errors
    }

    suspend fun testSource(id: String, keyword: String): SourceResult<Int> =
        withContext(Dispatchers.IO) {
            try {
                val entity = repo.sourceDao.byId(id) ?: return@withContext SourceResult.Err("音源不存在")
                val engine = engines[id] ?: engineFor(entity)
                val info = JSONObject().apply {
                    put("musicInfo", JSONObject().put("keyword", keyword))
                    put("type", SourceConstants.DEFAULT_QUALITY)
                }
                var count = 0
                var lastMsg = "无结果"
                SourceConstants.ALL_SOURCES
                    .filter { it != "local" }
                    .forEach { src ->
                        if (count > 0) return@forEach
                        runCatching {
                            val raw = engine.requestUrl(src, "search", info.toString())
                            parseSearchResult(raw, entity.id).size
                        }.onSuccess { n ->
                            if (n > count) {
                                count = n
                            }
                        }.onFailure { lastMsg = it.message ?: "测试失败" }
                    }
                if (count > 0) SourceResult.Ok(count) else SourceResult.Err(lastMsg)
            } catch (t: Throwable) {
                SourceResult.Err(t.message ?: "测试失败")
            }
        }

    suspend fun searchSingle(sourceId: String, source: String, keyword: String): List<Song> =
        withContext(Dispatchers.IO) {
            val entity = repo.sourceDao.byId(sourceId) ?: return@withContext emptyList()
            val engine = engines[sourceId] ?: engineFor(entity)
            val info = JSONObject().apply {
                put("musicInfo", JSONObject().put("keyword", keyword))
                put("type", SourceConstants.DEFAULT_QUALITY)
            }
            runCatching {
                parseSearchResult(engine.requestUrl(source, "search", info.toString()), sourceId)
            }.getOrDefault(emptyList())
        }

    suspend fun searchAll(keyword: String): List<Song> = aggregateSearch(keyword)

    suspend fun aggregateSearch(keyword: String): List<Song> = withContext(Dispatchers.IO) {
        val sources = repo.sourcesOnce().filter { it.enabled && it.script.isNotBlank() }
        if (sources.isEmpty()) return@withContext emptyList()
        val tasks = sources.map { e ->
            async {
                var out = emptyList<Song>()
                for (src in SourceConstants.ALL_SOURCES.filter { it != "local" }) {
                    val r = runCatching { searchSingle(e.id, src, keyword) }.getOrDefault(emptyList())
                    if (r.isNotEmpty()) {
                        out = r
                        break
                    }
                }
                out
            }
        }
        tasks.awaitAll().flatten().distinctBy { (it.title + "|" + it.artist).lowercase() }
    }

    fun parseSearchResult(raw: String, sourceId: String): List<Song> {
        val text = raw.trim().removeSurrounding("\"")
        if (text.isBlank() || text == "null") return emptyList()
        val arr = runCatching { JSONArray(text) }.getOrNull() ?: runCatching {
            JSONObject(text).optJSONArray("list")
        }.getOrNull() ?: return emptyList()
        val out = mutableListOf<Song>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("name").ifBlank { o.optString("title") }
            if (title.isBlank()) continue
            out += Song(
                title = title,
                artist = o.optString("singer").ifBlank { o.optString("artist") },
                album = o.optString("albumName").ifBlank { o.optString("album") },
                durationMs = o.optLong("interval") * 1000L,
                uri = "",
                path = "",
                size = 0L,
                isLocal = false,
                sourceId = sourceId,
                songmid = o.optString("songmid").ifBlank { o.optString("songId") },
                cover = o.optString("picUrl")
                    .ifBlank { o.optString("cover") }
                    .ifBlank { o.optString("img") }
                    .ifBlank { o.optString("al_pic") }
                    .ifBlank { o.optString("pic") }
                    .ifBlank { o.optString("avatar") }
                    .ifBlank { o.optString("artwork") }
                    .ifBlank { o.optString("albumPic") }
                    .ifBlank { o.optString("alPicUrl") }
            )
        }
        return out
    }

    suspend fun refreshSource(id: String): Result<SourceEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val old = repo.sourceDao.byId(id) ?: error("音源不存在")
            require(!old.jsUrl.startsWith("local://")) { "本地脚本无法刷新" }
            val script = downloadScript(old.jsUrl)
            require(script.isNotBlank()) { "刷新失败" }
            val entity = old.copy(script = script, updatedAt = System.currentTimeMillis())
            repo.upsertSource(entity)
            engines.remove(id)?.destroy()
            engineFor(entity)
            entity
        }
    }

    suspend fun resolve(
        sourceId: String,
        source: String,
        musicInfo: Map<String, Any?>,
        quality: String = SourceConstants.DEFAULT_QUALITY
    ): SourceResult<String> = withContext(Dispatchers.IO) {
        try {
            val entity = repo.sourceDao.byId(sourceId) ?: return@withContext SourceResult.Err("音源未加载")
            val engine = engines[sourceId] ?: engineFor(entity)
            val info = JSONObject().apply {
                put("musicInfo", JSONObject(musicInfo))
                put("type", quality)
            }
            val out = engine.requestUrl(source, "musicUrl", info.toString())
            val url = out.trim().removeSurrounding("\"")
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@withContext SourceResult.Err("返回非法地址: $url")
            }
            if (url.length > 2048) return@withContext SourceResult.Err("地址过长")
            SourceResult.Ok(url)
        } catch (t: Throwable) {
            SourceResult.Err(t.message ?: "解析失败")
        }
    }

    fun engineFor(entity: SourceEntity): JsSourceEngine {
        engines[entity.id]?.let { return it }
        val engine = JsSourceEngine(
            scriptId = entity.id,
            script = entity.script,
            net = net,
            logger = { }
        )
        engine.start()
        engines[entity.id] = engine
        return engine
    }

    suspend fun autoSelectBestSource(
        presets: List<Pair<String, String>> = DEFAULT_SUBSCRIPTIONS,
        keyword: String = "测试"
    ): SourceResult<String> = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        var winner: String? = null
        var winnerName = ""
        for ((name, url) in presets) {
            val id = "auto_" + name.lowercase().replace(" ", "_")
            val r = runCatching { addSource(id, name, url) }
            val entity = r.getOrNull()?.getOrNull()
            if (entity == null) {
                errors.add(name + ": " + (r.exceptionOrNull()?.message ?: "下载失败"))
                continue
            }
            var hit = emptyList<Song>()
            for (src in SourceConstants.ALL_SOURCES.filter { it != "local" }) {
                val h = runCatching { searchSingle(entity.id, src, keyword) }.getOrDefault(emptyList())
                if (h.isNotEmpty()) { hit = h; break }
            }
            if (hit.isNotEmpty()) {
                winner = entity.id
                winnerName = name
                break
            }
            errors.add(name + ": 解析无结果")
            runCatching { repo.deleteSource(entity.id) }
            engines.remove(entity.id)?.destroy()
        }
        if (winner == null) {
            return@withContext SourceResult.Err("全部音源失败: " + errors.joinToString("; "))
        }
        repo.sourcesOnce().forEach { e ->
            runCatching { repo.setSourceEnabled(e.id, e.id == winner) }
        }
        SourceResult.Ok(winnerName)
    }

    fun destroy() {
        engines.values.forEach { it.destroy() }
        engines.clear()
    }
}
