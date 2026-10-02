package com.nobodymusic.tyxypoor.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nobodymusic.tyxypoor.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerController(context: Context) {
    var onSongChanged: ((Song?) -> Unit)? = null
    var onSessionIdReady: ((Int) -> Unit)? = null
    var onNeedResolve: ((Song) -> Unit)? = null
    var onIndexChanged: ((Int) -> Unit)? = null

    val exo: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _current = MutableStateFlow<Song?>(null)
    val current: StateFlow<Song?> = _current

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue
    private val _index = MutableStateFlow(-1)
    val index: StateFlow<Int> = _index

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = exo.currentMediaItemIndex
            val sg = _queue.value.getOrNull(idx) ?: return
            _index.value = idx
            _current.value = sg
            onIndexChanged?.invoke(idx)
            onSongChanged?.invoke(sg)
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                val playable = sg.isLocal || sg.uri.startsWith("http") || sg.uri.startsWith("content")
                if (!playable) onNeedResolve?.invoke(sg)
            }
        }
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            onSessionIdReady?.invoke(audioSessionId)
        }
    }

    init {
        exo.addListener(listener)
    }

    fun setQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        _queue.value = songs
        _index.value = startIndex
        exo.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
        _current.value = songs.getOrNull(startIndex)
        onSongChanged?.invoke(_current.value)
        exo.prepare()
    }

    fun playSong(song: Song, url: String? = null) {
        val q = _queue.value.toMutableList()
        var i = q.indexOfFirst { it.uid == song.uid }
        if (i < 0) {
            q.clear(); q.add(song); i = 0
        } else {
            q[i] = song
        }
        _queue.value = q
        _index.value = i
        _current.value = song
        val item = if (url != null) MediaItem.Builder()
            .setUri(url)
            .setMediaId(song.uid)
            .setMediaMetadata(song.toMetadata())
            .build() else song.toMediaItem()
        exo.setMediaItems(q.mapIndexed { ki, s -> if (ki == i) item else s.toMediaItem() }, i, 0L)
        onSongChanged?.invoke(song)
        exo.prepare()
        exo.playWhenReady = true
    }

    fun setQueueAndPlay(songs: List<Song>, startIndex: Int) {
        setQueue(songs, startIndex)
        val sg = songs.getOrNull(startIndex) ?: return
        val playable = sg.isLocal || sg.uri.startsWith("http") || sg.uri.startsWith("content")
        if (!playable) {
            exo.pause()
            onNeedResolve?.invoke(sg)
        } else {
            exo.playWhenReady = true
        }
    }

    fun playSingle(song: Song) {
        _current.value = song
        _queue.value = listOf(song)
        exo.setMediaItem(song.toMediaItem())
        onSongChanged?.invoke(song)
        exo.prepare()
        exo.playWhenReady = true
    }

    fun play() { exo.play() }
    fun pause() { exo.pause() }
    fun toggle() { if (exo.isPlaying) pause() else play() }

    fun next(userTriggered: Boolean = true) {
        val q = _queue.value
        if (q.isEmpty()) return
        val cur = _index.value
        val target = when {
            _shuffle.value -> {
                if (q.size <= 1) cur
                else {
                    var n = cur
                    while (n == cur) n = (0 until q.size).random()
                    n
                }
            }
            else -> {
                val n = cur + 1
                if (n < q.size) n
                else if (_repeatMode.value == Player.REPEAT_MODE_OFF && !userTriggered) return
                else 0
            }
        }
        goTo(target)
    }

    fun previous(userTriggered: Boolean = true) {
        val q = _queue.value
        if (q.isEmpty()) return
        val cur = _index.value
        val target = when {
            _shuffle.value -> {
                if (q.size <= 1) cur else (0 until q.size).random()
            }
            else -> {
                val n = cur - 1
                if (n >= 0) n
                else if (_repeatMode.value == Player.REPEAT_MODE_OFF && !userTriggered) return
                else q.size - 1
            }
        }
        goTo(target)
    }

    private fun goTo(index: Int) {
        val q = _queue.value
        val sg = q.getOrNull(index) ?: return
        _index.value = index
        _current.value = sg
        exo.seekTo(index, 0L)
        val playable = sg.isLocal || sg.uri.startsWith("http") || sg.uri.startsWith("content")
        if (!playable) {
            exo.pause()
            onNeedResolve?.invoke(sg)
        } else {
            exo.playWhenReady = true
            exo.play()
        }
    }
    fun seekTo(ms: Long) { exo.seekTo(ms); _position.value = ms }

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode
    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
        exo.shuffleModeEnabled = _shuffle.value
    }
    fun cycleRepeat() {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = next
        exo.repeatMode = next
    }
    fun tick() {
        _position.value = exo.currentPosition
        _duration.value = exo.duration.coerceAtLeast(0)
    }

    fun release() {
        exo.removeListener(listener)
        exo.release()
    }
}

fun Song.toMetadata(): MediaMetadata = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(artist)
    .setAlbumTitle(album)
    .setArtworkUri(if (cover.isNotBlank()) android.net.Uri.parse(cover) else null)
    .build()

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(if (uri.isNotBlank()) uri else uid)
    .setMediaId(uid)
    .setMediaMetadata(toMetadata())
    .build()