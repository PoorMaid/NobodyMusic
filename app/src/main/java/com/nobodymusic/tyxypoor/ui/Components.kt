package com.nobodymusic.tyxypoor.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nobodymusic.tyxypoor.data.Song
object Fmt {
    fun dur(ms: Long): String {
        val total = ms / 1000
        val m = total / 60
        val s = total % 60
        return "%d:%02d".format(m, s)
    }
}
@Composable
fun CoverBox(cover: String?, size: Int = 44) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!cover.isNullOrBlank()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(cover)
                    .crossfade(true)
                    .memoryCacheKey(cover)
                    .diskCacheKey(cover)
                    .size(size * 3)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Default.MusicNote, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}
@Composable
fun SongRow(song: Song, index: Int = -1, onClick: () -> Unit) {
    val app = LocalAppState.current
    val favIds by app.favoriteIds.collectAsStateWithLifecycle()
    val favKeys by app.favoriteKeys.collectAsStateWithLifecycle()
    val fav = app.isFav(song, favIds, favKeys)
    var favLocal by remember(song.uid, fav) { mutableStateOf(fav) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index >= 0) {
            Text(
                "$index",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )
        }
        CoverBox(song.cover, 44)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title.ifBlank { "未知歌曲" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOf(song.artist, song.album).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (song.durationMs > 0) {
            Text(Fmt.dur(song.durationMs), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { app.downloadSong(song) }) {
            Icon(Icons.Default.Download, contentDescription = "下载",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = {
            favLocal = !favLocal
            app.toggleFavorite(song)
        }) {
            Icon(
                if (favLocal) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "收藏",
                tint = if (favLocal) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
fun MiniPlayer(onClick: () -> Unit) {
    val app = LocalAppState.current
    val current by app.player.current.collectAsStateWithLifecycle()
    val playing by app.player.isPlaying.collectAsStateWithLifecycle()
    val pos by app.player.position.collectAsStateWithLifecycle()
    val dur by app.player.duration.collectAsStateWithLifecycle()
    val s = current ?: return
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column {
            LinearProgressIndicator(
                progress = { if (dur > 0) pos.toFloat() / dur else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverBox(s.cover, 40)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title.ifBlank { "未知歌曲" }, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text(s.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.SkipPrevious, null,
                    modifier = Modifier.size(28.dp).clickable { app.player.previous() })
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    modifier = Modifier.size(32.dp).clickable { app.player.toggle() }
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.SkipNext, null,
                    modifier = Modifier.size(28.dp).clickable { app.player.next() })
            }
        }
    }
}