package com.nobodymusic.tyxypoor.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nobodymusic.tyxypoor.data.Playlist
import com.nobodymusic.tyxypoor.data.Song
import com.nobodymusic.tyxypoor.ui.EmptyHint
import com.nobodymusic.tyxypoor.ui.LocalAppState
import com.nobodymusic.tyxypoor.ui.SongRow
import kotlinx.coroutines.launch
@Composable
fun HomeScreen(
    onOpenPlayer: () -> Unit,
    section: Int,
    onSection: (Int) -> Unit,
    homeTab: Int,
    onHomeTab: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            if (section == 0) HomeSection(onOpenPlayer, homeTab, onHomeTab)
            else MineSection(onOpenPlayer)
        }
        NavigationBar {
            NavigationBarItem(
                selected = section == 0,
                onClick = { onSection(0) },
                icon = { Icon(Icons.Default.Home, null) },
                label = { Text("首页") }
            )
            NavigationBarItem(
                selected = section == 1,
                onClick = { onSection(1) },
                icon = { Icon(Icons.Default.Person, null) },
                label = { Text("我的") }
            )
        }
    }
}
@Composable
private fun SegmentedChips(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        titles.forEachIndexed { i, t ->
            val sel = i == selected
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (sel) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { onSelect(i) }
            ) {
                Text(
                    t,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (sel) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
private fun HomeSection(onOpenPlayer: () -> Unit, tab: Int, onTab: (Int) -> Unit) {
    val titles = listOf("排行榜", "热门歌单", "搜索")
    Column(Modifier.fillMaxSize()) {
        SegmentedChips(titles, tab) { onTab(it) }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> RankTab(onOpenPlayer)
                1 -> HotTab(onOpenPlayer)
                else -> SearchTab(onOpenPlayer)
            }
        }
    }
}
@Composable
private fun MineSection(onOpenPlayer: () -> Unit) {
    val app = LocalAppState.current
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("本地", "收藏", "歌单", "最近")
    LaunchedEffect(Unit) {
        app.refreshLocal()
        app.refreshFavorites()
        app.refreshFavPlaylists()
        app.refreshHistory()
    }
    Column(Modifier.fillMaxSize()) {
        SegmentedChips(titles, tab) { tab = it }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> LocalTab(onOpenPlayer)
                1 -> FavoritesTab(onOpenPlayer)
                2 -> FavPlaylistsTab(onOpenPlayer)
                else -> HistoryTab(onOpenPlayer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavPlaylistsTab(onOpenPlayer: () -> Unit) {
    val app = LocalAppState.current
    val lists by app.favPlaylists.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf<Playlist?>(null) }
    if (lists.isEmpty()) {
        EmptyHint("还没有收藏的歌单，去热门歌单里收藏吧")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(lists, key = { it.id }) { p ->
            Row(
                Modifier.fillMaxWidth().clickable { open = p }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        (if (p.songCount > 0) "${p.songCount} 首" else "已收藏"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { app.deleteFavPlaylist(p.id) }) { Text("删除") }
            }
            HorizontalDivider()
        }
    }
    open?.let { p ->
        FavPlaylistSheet(p, onOpenPlayer) { open = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavPlaylistSheet(p: Playlist, onOpenPlayer: () -> Unit, onDismiss: () -> Unit) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()
    val songs by app.favoritePlaylistSongs(p.id).collectAsStateWithLifecycle(initialValue = emptyList<Song>())
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(p.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = {
                    if (songs.isNotEmpty()) {
                        app.playNow(songs.first(), songs)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        onOpenPlayer()
                    }
                }) { Text("播放全部") }
            }
            if (songs.isEmpty()) {
                EmptyHint("歌单为空")
            } else {
                LazyColumn(Modifier.fillMaxWidth()) {
                    itemsIndexed(songs, key = { _, s -> s.uid }) { i, s ->
                        SongRow(s, i + 1) {
                            app.playNow(s, songs)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            onOpenPlayer()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalTab(onOpenPlayer: () -> Unit) {
    val app = LocalAppState.current
    val songs by app.songs.collectAsStateWithLifecycle()
    if (songs.isEmpty()) {
        EmptyHint("暂无本地音乐，去设置里扫描本地文件")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, s -> s.uid }) { i, s ->
            SongRow(s, i + 1) {
                app.playNow(s, songs)
                onOpenPlayer()
            }
        }
    }
}
@Composable
private fun FavoritesTab(onOpenPlayer: () -> Unit) {
    val app = LocalAppState.current
    val fav by app.favorites.collectAsStateWithLifecycle()
    if (fav.isEmpty()) {
        EmptyHint("还没有收藏的歌曲")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(fav, key = { _, s -> s.uid }) { i, s ->
            SongRow(s, i + 1) {
                app.playNow(s, fav)
                onOpenPlayer()
            }
        }
    }
}
@Composable
private fun HistoryTab(onOpenPlayer: () -> Unit) {
    val app = LocalAppState.current
    val hist by app.history.collectAsStateWithLifecycle()
    if (hist.isEmpty()) {
        EmptyHint("暂无播放历史")
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("共 ${hist.size} 首", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = { app.clearHistory() }) {
                Icon(Icons.Default.Delete, contentDescription = "清空历史")
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(hist, key = { _, s -> s.uid }) { i, s ->
                SongRow(s, i + 1) {
                    app.playNow(s, hist)
                    onOpenPlayer()
                }
            }
        }
    }
}
@Composable
private fun SearchTab(onOpenPlayer: () -> Unit) {
    val app = LocalAppState.current
    val q by app.searchQuery.collectAsStateWithLifecycle()
    val results by app.searchResults.collectAsStateWithLifecycle()
    val searching by app.searching.collectAsStateWithLifecycle()
    val searchError by app.searchError.collectAsStateWithLifecycle()
    val history by app.searchHistory.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = q,
            onValueChange = { app.searchQuery.value = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("搜索歌曲、歌手") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { app.search(q) }),
            trailingIcon = {
                IconButton(onClick = { app.search(q) }) {
                    Icon(Icons.Default.Search, null)
                }
            }
        )
        Box(Modifier.weight(1f)) {
            when {
                searching -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                results.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(results, key = { _, s -> s.uid }) { i, s ->
                        SongRow(s, i + 1) {
                            app.playNow(s, results)
                            onOpenPlayer()
                        }
                    }
                }
                searchError.isNotEmpty() -> EmptyHint(searchError)
                history.isNotEmpty() -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("搜索历史", Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = { app.clearSearchHistory() }) {
                                Icon(Icons.Default.Delete, contentDescription = "清空搜索历史")
                            }
                        }
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(history, key = { it }) { h ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { app.search(h) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, null,
                                        modifier = Modifier.padding(end = 12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(h, Modifier.weight(1f),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    IconButton(onClick = { app.removeSearchHistory(h) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
                else -> EmptyHint("输入关键词，点击搜索")
            }
        }
    }
}