package com.nobodymusic.tyxypoor.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nobodymusic.tyxypoor.data.Song
import com.nobodymusic.tyxypoor.source.NetEaseApi
import com.nobodymusic.tyxypoor.ui.EmptyHint
import com.nobodymusic.tyxypoor.ui.LocalAppState
import com.nobodymusic.tyxypoor.ui.SongRow
import com.nobodymusic.tyxypoor.ui.CoverBox
import kotlinx.coroutines.launch
private data class Card(val id: String, val name: String, val sub: String, val cover: String = "")
private fun fmt(n: Long): String = when {
    n >= 100000000L -> String.format("%.1f亿", n / 100000000.0)
    n >= 10000L -> String.format("%.1f万", n / 10000.0)
    else -> n.toString()
}
@Composable
internal fun RankTab(onOpenPlayer: () -> Unit) {
    var cards by remember { mutableStateOf<List<Card>?>(null) }
    var err by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            val list = NetEaseApi.topLists()
            cards = list.map { Card(it.id, it.name, "排行榜", it.cover) }
        } catch (e: Exception) {
            err = "加载失败：${e.message}"
            cards = emptyList()
        }
    }
    CardList(cards, err, onOpenPlayer)
}
@Composable
internal fun HotTab(onOpenPlayer: () -> Unit) {
    var cards by remember { mutableStateOf<List<Card>?>(null) }
    var err by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            val list = NetEaseApi.hotPlaylists(40)
            cards = list.map { Card(it.id, it.name, "播放 ${fmt(it.playCount)}", it.cover) }
        } catch (e: Exception) {
            err = "加载失败：${e.message}"
            cards = emptyList()
        }
    }
    CardList(cards, err, onOpenPlayer)
}
@Composable
private fun CardList(cards: List<Card>?, err: String, onOpenPlayer: () -> Unit) {
    var openId by remember { mutableStateOf<String?>(null) }
    var openName by remember { mutableStateOf("") }
    when {
        cards == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        err.isNotEmpty() -> EmptyHint(err)
        cards.isEmpty() -> EmptyHint("暂无数据，请检查网络")
        else -> {
            LazyColumn(Modifier.fillMaxSize()) {
                items(cards, key = { it.id }) { c ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { openId = c.id; openName = c.name }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoverBox(c.cover, 48)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(c.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                }
            }
            openId?.let { id ->
                PlaylistDetailSheet(id, openName, onOpenPlayer) { openId = null }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailSheet(id: String, name: String, onOpenPlayer: () -> Unit, onDismiss: () -> Unit) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()
    var songs by remember { mutableStateOf<List<Song>?>(null) }
    var err by remember { mutableStateOf("") }
    var fav by remember { mutableStateOf(false) }
    var favMsg by remember { mutableStateOf("") }
    var cover by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(id) {
        try {
            val pl = NetEaseApi.playlistDetail(id)
            songs = pl?.songs ?: emptyList()
            cover = pl?.cover ?: ""
            fav = app.repo.isPlaylistFavorite(id)
        } catch (e: Exception) {
            err = "加载失败：${e.message}"
            songs = emptyList()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val s = songs
                TextButton(onClick = {
                    if (!s.isNullOrEmpty()) {
                        app.togglePlaylistFavorite(
                            name = name, linkId = id, cover = cover,
                            songCount = s.size, songs = s
                        ) { now -> fav = now; favMsg = if (now) "已收藏歌单" else "已取消收藏" }
                    }
                }) { Text(if (fav) "已收藏" else "收藏歌单") }
                TextButton(onClick = {
                    if (!s.isNullOrEmpty()) {
                        app.playNow(s.first(), s)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        onOpenPlayer()
                    }
                }) { Text("播放全部") }
            }
            if (favMsg.isNotBlank()) {
                Text(favMsg, Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            when {
                songs == null -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                err.isNotEmpty() -> EmptyHint(err)
                songs!!.isEmpty() -> EmptyHint("歌单为空")
                else -> LazyColumn(Modifier.fillMaxWidth()) {
                    itemsIndexed(songs!!, key = { _, s -> s.uid }) { i, s ->
                        SongRow(s, i + 1) {
                            app.playNow(s, songs!!)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            onOpenPlayer()
                        }
                    }
                }
            }
        }
    }
}