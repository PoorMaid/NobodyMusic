package com.nobodymusic.tyxypoor.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.nobodymusic.tyxypoor.ui.Fmt
import com.nobodymusic.tyxypoor.ui.LocalAppState
@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val app = LocalAppState.current
    val song by app.player.current.collectAsStateWithLifecycle()
    val playing by app.player.isPlaying.collectAsStateWithLifecycle()
    val pos by app.player.position.collectAsStateWithLifecycle()
    val dur by app.player.duration.collectAsStateWithLifecycle()
    val shuffle by app.player.shuffle.collectAsStateWithLifecycle()
    val repeatMode by app.player.repeatMode.collectAsStateWithLifecycle()
    val lyric by app.lyric.collectAsStateWithLifecycle()
    var dragging by remember { mutableStateOf<Float?>(null) }
    var fav by remember(song?.uid) { mutableStateOf(false) }
    LaunchedEffect(song?.uid) { song?.let { fav = app.isFavoriteSong(it) } }
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Spacer(Modifier.weight(1f))
            Text("正在播放", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            song?.let { sg ->
                IconButton(onClick = { app.downloadSong(sg) }) {
                    Icon(Icons.Default.Download, "下载", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { fav = !fav; app.toggleFavorite(sg) }) {
                    Icon(
                        if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "收藏", modifier = Modifier.size(22.dp),
                        tint = if (fav) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } ?: Spacer(Modifier.size(96.dp))
        }
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(240.dp).clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val cover = song?.cover ?: ""
            if (cover.isNotBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(song?.title?.ifBlank { "未知歌曲" } ?: "未在播放",
            style = MaterialTheme.typography.titleLarge, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(song?.artist ?: "", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        LyricView(lyric, pos, Modifier.weight(1f).fillMaxWidth(), onSeek = { app.player.seekTo(it) })
        Spacer(Modifier.height(8.dp))
        val total = if (dur > 0) dur.toFloat() else 1f
        val cur = dragging ?: pos.toFloat().coerceIn(0f, total)
        Slider(
            value = cur,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { app.player.seekTo(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..total,
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Fmt.dur(cur.toLong()), style = MaterialTheme.typography.labelSmall)
            Text(Fmt.dur(dur), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { app.player.toggleShuffle() }) {
                Icon(
                    Icons.Default.Shuffle, "随机播放",
                    tint = if (shuffle) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { app.player.previous() }) {
                Icon(Icons.Default.SkipPrevious, "上一首", modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { app.player.toggle() }) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "播放暂停", modifier = Modifier.size(64.dp)
                )
            }
            IconButton(onClick = { app.player.next() }) {
                Icon(Icons.Default.SkipNext, "下一首", modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { app.player.cycleRepeat() }) {
                val active = repeatMode != Player.REPEAT_MODE_OFF
                Icon(
                    if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne
                    else Icons.Default.Repeat,
                    "循环模式",
                    tint = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
@Composable
private fun LyricView(
    lines: List<com.nobodymusic.tyxypoor.data.LyricLine>,
    posMs: Long,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit = {}
) {
    if (lines.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("暂无歌词", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val idx = com.nobodymusic.tyxypoor.data.Lyrics.indexAt(lines, posMs)
    val state = rememberLazyListState()
    LaunchedEffect(idx) {
        if (idx >= 0) state.animateScrollToItem(idx.coerceAtLeast(0))
    }
    LazyColumn(modifier, state = state, horizontalAlignment = Alignment.CenterHorizontally) {
        itemsIndexed(lines) { i, line ->
            val activeLine = i == idx
            Text(
                line.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (activeLine) FontWeight.Bold else FontWeight.Normal,
                color = if (activeLine) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .clickable { onSeek(line.timeMs) }
                    .padding(vertical = 6.dp)
            )
        }
    }
}