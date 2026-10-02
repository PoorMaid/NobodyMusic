package com.nobodymusic.tyxypoor.ui
import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import com.nobodymusic.tyxypoor.data.MusicRepository
import com.nobodymusic.tyxypoor.data.Playlist
import com.nobodymusic.tyxypoor.data.Song
import com.nobodymusic.tyxypoor.di.ServiceLocator
import com.nobodymusic.tyxypoor.data.LyricLine
import com.nobodymusic.tyxypoor.data.Lyrics
import com.nobodymusic.tyxypoor.player.EqController
import com.nobodymusic.tyxypoor.player.PlayerController
import com.nobodymusic.tyxypoor.source.Downloader
import com.nobodymusic.tyxypoor.source.NetEaseApi
import com.nobodymusic.tyxypoor.source.SourceConstants
import com.nobodymusic.tyxypoor.source.SourceResult
import com.nobodymusic.tyxypoor.ui.theme.ThemePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppState(val context: Context) {
    companion object {
        @Volatile var instance: AppState? = null
    }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val repo: MusicRepository = ServiceLocator.repository
    val player = PlayerController(context)
    val eq = EqController()
    private val _lyric = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyric: StateFlow<List<LyricLine>> = _lyric
    private val _lyricLoading = MutableStateFlow(false)
    val lyricLoading: StateFlow<Boolean> = _lyricLoading
    private var lyricJob: Job? = null

    private fun resolveAndPlay(song: Song) {
        scope.launch {
            _resolving.value = song.title
            val url = try { resolveOnlineUrl(song) } catch (e: Exception) {
                android.util.Log.e("AppState", "resolve failed: ${e.message}", e); null
            }
            _resolving.value = ""
            if (url.isNullOrBlank()) {
                _searchError.value = "无法解析播放地址：${song.title}"
                return@launch
            }
            recordPlaySong(song)
            player.playSong(song.copy(uri = url), url)
        }
    }
    fun loadLyric(song: Song?) {
        lyricJob?.cancel()
        _lyric.value = emptyList()
        if (song == null) return
        if (song.sourceId != "netease" && song.title.isBlank()) return
        lyricJob = scope.launch {
            _lyricLoading.value = true
            val pair = runCatching {
                if (song.sourceId == "netease" && song.songmid.isNotBlank())
                    NetEaseApi.lyric(song.songmid)
                else
                    NetEaseApi.lyricByKeyword(song.title, song.artist)
            }.getOrDefault("" to "")
            val main = Lyrics.parse(pair.first)
            val tr = Lyrics.parse(pair.second)
            _lyric.value = if (tr.isEmpty()) main else {
                val merged = (main + tr).sortedBy { it.timeMs }
                merged
            }
            _lyricLoading.value = false
        }
    }

    private val prefs = context.getSharedPreferences("nobody_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        ThemePrefs(
            useDynamicColor = prefs.getBoolean("dyn", true),
            seedColor = prefs.getLong("seed", 0xFF6650a4),
            darkMode = prefs.getInt("dark", 0),
            pureBlack = prefs.getBoolean("black", false),
            background = prefs.getInt("bg", 0),
            bgImagePath = prefs.getString("bg_img", "") ?: ""
        )
    )
    val theme: StateFlow<ThemePrefs> = _theme

    fun saveTheme(t: ThemePrefs) {
        _theme.value = t
        prefs.edit()
            .putBoolean("dyn", t.useDynamicColor)
            .putLong("seed", t.seedColor)
            .putInt("dark", t.darkMode)
            .putBoolean("black", t.pureBlack)
            .putInt("bg", t.background)
            .putString("bg_img", t.bgImagePath)
            .apply()
    }

    private val _dlQuality = MutableStateFlow(prefs.getString("dl_quality", SourceConstants.DEFAULT_QUALITY) ?: SourceConstants.DEFAULT_QUALITY)
    val dlQuality: StateFlow<String> = _dlQuality
    fun setDlQuality(v: String) {
        _dlQuality.value = v
        prefs.edit().putString("dl_quality", v).apply()
    }
    val qualityOptions: List<String> get() = SourceConstants.QUALITY_ORDER
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _favorites = MutableStateFlow<List<Song>>(emptyList())
    val favorites: StateFlow<List<Song>> = _favorites

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds

    private val _favoriteKeys = MutableStateFlow<Set<String>>(emptySet())
    val favoriteKeys: StateFlow<Set<String>> = _favoriteKeys

    private val _history = MutableStateFlow<List<Song>>(emptyList())
    val history: StateFlow<List<Song>> = _history

    private val _favPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val favPlaylists: StateFlow<List<Playlist>> = _favPlaylists
    val searchQuery = MutableStateFlow("")
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory

    init {
        _searchHistory.value = (prefs.getString("search_history", "") ?: "")
            .split("\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun addSearchHistory(q: String) {
        val key = q.trim()
        if (key.isBlank()) return
        val next = (listOf(key) + _searchHistory.value.filter { !it.equals(key, true) }).take(30)
        _searchHistory.value = next
        prefs.edit().putString("search_history", next.joinToString("\n")).apply()
    }

    fun removeSearchHistory(q: String) {
        val next = _searchHistory.value.filter { it != q }
        _searchHistory.value = next
        prefs.edit().putString("search_history", next.joinToString("\n")).apply()
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        prefs.edit().putString("search_history", "").apply()
    }

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching

    private val _searchError = MutableStateFlow("")
    val searchError: StateFlow<String> = _searchError

    private val _resolving = MutableStateFlow("")
    val resolving: StateFlow<String> = _resolving

    fun refreshLocal() {
        scope.launch { _songs.value = repo.localSongsOnce() }
    }

    fun refreshFavorites() {
        scope.launch {
            val list = repo.favoritesOnce()
            _favorites.value = list
            _favoriteIds.value = list.map { it.id }.toHashSet()
            _favoriteKeys.value = list.mapNotNull { s ->
                when {
                    s.songmid.isNotBlank() -> s.sourceId + "|" + s.songmid
                    s.path.isNotBlank() -> "path|" + s.path
                    else -> null
                }
            }.toHashSet()
        }
    }

    fun refreshHistory() {
        scope.launch { _history.value = repo.historyOnce() }
    }

    fun refreshFavPlaylists() {
        scope.launch { _favPlaylists.value = repo.favoritePlaylistsOnce() }
    }

    suspend fun isFavoriteSong(song: Song): Boolean = repo.isFavoriteSong(song)

    fun isFav(song: Song, ids: Set<Long>, keys: Set<String>): Boolean {
        if (song.id > 0 && ids.contains(song.id)) return true
        val k = when {
            song.songmid.isNotBlank() -> song.sourceId + "|" + song.songmid
            song.path.isNotBlank() -> "path|" + song.path
            else -> return song.id > 0 && ids.contains(song.id)
        }
        return keys.contains(k)
    }

    fun toggleFavorite(song: Song) {
        scope.launch {
            repo.toggleFavoriteSong(song)
            refreshFavorites()
        }
    }

    fun clearHistory() {
        scope.launch {
            repo.clearHistory()
            refreshHistory()
        }
    }

    fun search(q: String) {
        searchQuery.value = q
        if (q.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        addSearchHistory(q)
        scope.launch {
            _searching.value = true
            _searchError.value = ""
            var list = runCatching { ServiceLocator.sourceManager.aggregateSearch(q) }
                .getOrDefault(emptyList())
            if (list.isEmpty()) {
                list = runCatching { NetEaseApi.search(q) }.getOrDefault(emptyList())
            }
            if (list.any { it.cover.isBlank() }) {
                val ne = runCatching { NetEaseApi.search(q) }.getOrDefault(emptyList())
                if (ne.isNotEmpty()) {
                    list = list.map { s ->
                        if (s.cover.isNotBlank()) s else {
                            val hit = ne.firstOrNull {
                                it.title.equals(s.title, true) &&
                                    (s.artist.isBlank() || it.artist.contains(s.artist) || s.artist.contains(it.artist))
                            } ?: ne.firstOrNull { it.title.equals(s.title, true) }
                            if (hit != null) s.copy(cover = hit.cover) else s
                        }
                    }
                }
            }
            _searchResults.value = list
            if (list.isEmpty()) _searchError.value = "没有找到结果"
            _searching.value = false
        }
    }

    private fun recordPlaySong(song: Song) {
        scope.launch {
            try {
                var sid = song.id
                if (sid <= 0) {
                    val key = if (song.path.isNotBlank()) song.path else song.uid
                    val exist = repo.songDao.byPath(key)
                    sid = if (exist != null) exist.id
                    else repo.songDao.insert(song.copy(path = key))
                }
                if (sid > 0) { repo.recordPlay(sid); refreshHistory() }
            } catch (_: Exception) {}
        }
    }
    fun playNow(song: Song, list: List<Song> = emptyList()) {
        val q = if (list.isEmpty()) listOf(song) else list
        val idx = q.indexOfFirst { it.uid == song.uid }.coerceAtLeast(0)
        recordPlaySong(song)

        val playable = song.isLocal || song.uri.startsWith("http") || song.uri.startsWith("content")
        if (playable) {
            player.setQueueAndPlay(q, idx)
            return
        }
        player.setQueue(q, idx)
        resolveAndPlay(song)
    }

    suspend fun resolveOnlineUrl(song: Song): String? {
        if (song.uri.startsWith("http")) return song.uri
        val info = mapOf(
            "name" to song.title,
            "title" to song.title,
            "singer" to song.artist,
            "artist" to song.artist,
            "songmid" to song.songmid,
            "songId" to song.songmid,
            "album" to song.album,
            "interval" to (song.durationMs / 1000)
        )
        val all = ServiceLocator.repository.sourcesOnce().filter { it.enabled && it.script.isNotBlank() }
        val ordered = mutableListOf<String>()
        // 1) 用户所选音源最高优先
        if (song.sourceId.isNotBlank())
            all.firstOrNull { it.id == song.sourceId }?.let { ordered += it.id }
        // 2) 野草 / 野花
        listOf("Grass", "Flower").forEach { id ->
            val hit = all.firstOrNull { it.id == id || it.name.contains(if (id == "Grass") "野草" else "野花") }
            if (hit != null && hit.id !in ordered) ordered += hit.id
        }
        // 3) 其余已启用音源，按用户设定的 priority 升序（越小越优先）
        all.sortedBy { it.priority }.forEach { if (it.id !in ordered) ordered += it.id }
        for (srcId in ordered) {
            val hit = runCatching {
                var got: String? = null
                var hitSrc = ""
                for (src in listOf("wy", "tx", "kw", "kg", "mg", "local")) {
                    val rr = ServiceLocator.sourceManager.resolve(srcId, src, info)
                    if (rr is SourceResult.Ok) { got = rr.value; hitSrc = src; break }
                }
                if (got.isNullOrBlank()) null else got to hitSrc
            }.getOrNull()
            if (hit != null) {
                android.util.Log.i("AppState", "resolve '${song.title}' via 音源[$srcId]/平台[${hit.second}] -> ${hit.first}")
                return hit.first
            }
        }
        // 仅当所有所选音源均失败时，才回落到平台官方试听流（可能非完整音质/非 MP3-FLAC）
        var mid = song.songmid
        if (mid.isBlank()) {
            mid = runCatching {
                NetEaseApi.search((song.title + " " + song.artist).trim())
                    .firstOrNull { it.songmid.isNotBlank() }?.songmid ?: ""
            }.getOrDefault("")
        }
        if (mid.isNotBlank()) {
            for (br in listOf(320000, 192000, 128000)) {
                val u = runCatching { NetEaseApi.songUrl(mid, br) }.getOrNull()
                if (!u.isNullOrBlank()) {
                    android.util.Log.w("AppState", "resolve '${song.title}' 音源全部失败，回落平台官方试听流(br=$br): $u")
                    return u
                }
            }
        }
        android.util.Log.e("AppState", "resolve '${song.title}' 失败：无可用音源地址")
        return null
    }

    // ---------- 收藏歌单 ----------
    fun favoritePlaylistSongs(pid: Long): Flow<List<Song>> = repo.playlistSongs(pid)

    fun togglePlaylistFavorite(
        name: String, linkId: String, cover: String,
        songCount: Int, songs: List<Song>, onDone: (Boolean) -> Unit = {}
    ) {
        scope.launch {
            val now = repo.togglePlaylistFavorite(name, linkId, cover, songCount, songs)
            refreshFavPlaylists()
            refreshFavorites()
            onDone(now)
        }
    }

    fun deleteFavPlaylist(pid: Long) {
        scope.launch {
            repo.deleteFavoritePlaylist(pid)
            refreshFavPlaylists()
        }
    }

    // ---------- 定时停止播放 ----------
    private val _sleepRemain = MutableStateFlow(0L)
    val sleepRemain: StateFlow<Long> = _sleepRemain
    private var sleepJob: Job? = null

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        sleepJob = scope.launch {
            var left = minutes * 60L
            _sleepRemain.value = left
            while (left > 0) {
                delay(1000)
                left--
                _sleepRemain.value = left
            }
            player.pause()
            _sleepRemain.value = 0
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepRemain.value = 0
    }

    // ---------- 下载 ----------
    private val _downloadDir = MutableStateFlow(prefs.getString("dl_dir", "") ?: "")
    val downloadDir: StateFlow<String> = _downloadDir

    fun setDownloadDir(uri: String) {
        _downloadDir.value = uri
        prefs.edit().putString("dl_dir", uri).apply()
    }

    private val _floatMode = MutableStateFlow("")
    val floatMode: StateFlow<String> = _floatMode
    fun canFloat(): Boolean = android.provider.Settings.canDrawOverlays(context)
    fun showFloating(mode: String) {
        com.nobodymusic.tyxypoor.ui.floating.FloatingLyricService.start(context, mode)
        _floatMode.value = mode
      }
    fun hideFloating() {
        com.nobodymusic.tyxypoor.ui.floating.FloatingLyricService.stop(context)
        _floatMode.value = ""
      }
    private val _dlMsg = MutableStateFlow("")
    val dlMsg: StateFlow<String> = _dlMsg

    fun downloadSong(song: Song) {
        scope.launch {
            _dlMsg.value = "正在解析 ${song.title} ..."
            val url = try { resolveOnlineUrl(song) } catch (e: Exception) { null }
            if (url.isNullOrBlank()) { _dlMsg.value = "解析失败：${song.title}"; return@launch }
            _dlMsg.value = "下载中 ${song.title} ..."
            val name = song.artist + " - " + song.title
            val q = _dlQuality.value
            var saved: String? = null
            val dir = _downloadDir.value
            if (dir.isNotBlank()) {
                if (Downloader.downloadTo(context, dir, name, url, q)) saved = name
            }
            if (saved == null) {
                saved = Downloader.downloadToPublic(context, name, url, q, song.title, song.artist, song.album)
            }
            _dlMsg.value = if (saved != null) "已保存：$saved" else "下载失败：${song.title}"
        }
    }

    init {
        instance = this
        player.onSessionIdReady = { sid -> eq.attach(sid) }
        player.onSongChanged = { s -> loadLyric(s) }
        player.onNeedResolve = { s -> resolveAndPlay(s) }
        runCatching { eq.attachGlobal() }
        refreshFavorites()
        refreshFavPlaylists()
    }
}

val LocalAppState = compositionLocalOf<AppState> { error("AppState not provided") }
