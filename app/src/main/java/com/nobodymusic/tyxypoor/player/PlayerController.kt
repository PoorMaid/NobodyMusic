package com.nobodymusic.tyxypoor.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nobodymusic.tyxypoor.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PlayOrder { SEQUENCE, LOOP_ALL, LOOP_ONE, SHUFFLE }

class PlayerController(context: Context) {
    var onSongChanged: ((Song?) -> Unit)? = null
    var onSessionIdReady: ((Int) -> Unit)? = null
    var onNeedResolve: ((Song) -> Unit)? = null
    var onIndexChanged: ((Int) -> Unit)? = null

    val exo: ExoPlayer = PlayerHolder.get(context)

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

    private val _order = MutableStateFlow(PlayOrder.SEQUENCE)
    val order: StateFlow<PlayOrder> = _order

    private var resolvedUid: String = ""

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
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                if (_order.value == PlayOrder.LOOP_ONE) return
            }
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                val playable = sg.isLocal || sg.uri.startsWith("http") || sg.uri.startsWith("content")
                if (!playable && sg.uid != resolvedUid) onNeedResolve?.invoke(sg)
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
        resolvedUid = song.uid
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
        if (playable) {
            exo.playWhenReady = true
        } else {
            exo.pause()
            onNeedResolve?.invoke(sg)
        }
    }

    fun playSingle(song: Song) {
        _current.value = song
        _queue.value = listOf(song)
        resolvedUid = song.uid
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
        val target = when (_order.value) {
            PlayOrder.SHUFFLE -> {
                if (q.size <= 1) cur
                else {
                    var n = cur
                    while (n == cur) n = (0 until q.size).random()
                    n
                }
            }
            PlayOrder.LOOP_ONE -> if (userTriggered) (cur + 1) % q.size else cur
            else -> {
                val n = cur + 1
                if (n < q.size) n
                else if (_order.value == PlayOrder.SEQUENCE && !userTriggered) return
                else 0
            }
        }
        if (target == cur && _order.value == PlayOrder.LOOP_ONE && !userTriggered) {
            exo.seekTo(0L)
            exo.play()
            return
        }
        goTo(target)
    }

    fun previous(userTriggered: Boolean = true) {
        val q = _queue.value
        if (q.isEmpty()) return
        val cur = _index.value
        val target = when (_order.value) {
            PlayOrder.SHUFFLE -> if (q.size <= 1) cur else (0 until q.size).random()
            else -> {
                val n = cur - 1
                if (n >= 0) n
                else if (_order.value == PlayOrder.SEQUENCE && !userTriggered) return
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
        if (!playable && sg.uid != resolvedUid) {
            exo.pause()
            onNeedResolve?.invoke(sg)
        } else {
            exo.playWhenReady = true
            exo.play()
        }
    }

    fun seekTo(ms: Long) { exo.seekTo(ms); _position.value = ms }

    fun cycleOrder() {
        val next = when (_order.value) {
            PlayOrder.SEQUENCE -> PlayOrder.LOOP_ALL
            PlayOrder.LOOP_ALL -> PlayOrder.LOOP_ONE
            PlayOrder.LOOP_ONE -> PlayOrder.SHUFFLE
            PlayOrder.SHUFFLE -> PlayOrder.SEQUENCE
        }
        applyOrder(next)
    }

    fun applyOrder(o: PlayOrder) {
        _order.value = o
        when (o) {
            PlayOrder.SHUFFLE -> {
                exo.shuffleModeEnabled = true
                exo.repeatMode = Player.REPEAT_MODE_ALL
            }
            PlayOrder.LOOP_ALL -> {
                exo.shuffleModeEnabled = false
                exo.repeatMode = Player.REPEAT_MODE_ALL
            }
            PlayOrder.LOOP_ONE -> {
                exo.shuffleModeEnabled = false
                exo.repeatMode = Player.REPEAT_MODE_ONE
            }
            PlayOrder.SEQUENCE -> {
                exo.shuffleModeEnabled = false
                exo.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    fun tick() {
        _position.value = exo.currentPosition
        _duration.value = exo.duration.coerceAtLeast(0)
    }

    fun release() {
        exo.removeListener(listener)
    }

    fun detachUi() {
        exo.removeListener(listener)
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