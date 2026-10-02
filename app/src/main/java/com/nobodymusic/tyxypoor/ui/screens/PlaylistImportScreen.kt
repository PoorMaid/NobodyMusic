package com.nobodymusic.tyxypoor.ui.screens

import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nobodymusic.tyxypoor.data.Song
import com.nobodymusic.tyxypoor.di.ServiceLocator
import com.nobodymusic.tyxypoor.source.PlaylistLinkResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val EXTS = setOf("mp3", "flac", "m4a", "wav", "ogg", "aac", "ape")

@Composable
fun PlaylistImportScreen(onBack: () -> Unit) {
    val repo = ServiceLocator.repository
    val scope = rememberCoroutineScope()
    var mode by remember { mutableIntStateOf(0) }
    var dir by remember { mutableStateOf(Environment.getExternalStorageDirectory().absolutePath) }
    var text by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var found by remember { mutableStateOf(listOf<Song>()) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    fun keyOf(s: Song) = if (mode == 0) s.path else s.title + "|" + s.artist + "|" + s.songmid

    fun scan() {
        busy = true
        msg = "正在扫描..."
        scope.launch {
            val list = withContext(Dispatchers.IO) { walk(File(dir), 0) }
            found = list
            picked = list.map { keyOf(it) }.toSet()
            msg = "扫描到 ${list.size} 首"
            busy = false
        }
    }

    fun recognize() {
        val url = PlaylistLinkResolver.extractUrl(text)
        if (url != null) {
            busy = true
            msg = "正在联网解析歌单..."
            scope.launch {
                val r = runCatching { PlaylistLinkResolver.resolve(url) }
                r.fold(
                    onSuccess = { rp ->
                        found = rp.songs
                        picked = rp.songs.map { it.title + "|" + it.artist + "|" + it.songmid }.toSet()
                        if (name.isBlank()) name = rp.name
                        msg = "【${rp.platform}】${rp.name}：解析出 ${rp.songs.size} 首"
                    },
                    onFailure = {
                        msg = "解析失败：" + (it.message ?: "未知错误")
                        found = emptyList()
                        picked = emptySet()
                    }
                )
                busy = false
            }
        } else {
            val list = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
                val parts = line.split("|", "\u2014").map { it.trim() }.filter { it.isNotEmpty() }
                when {
                    parts.size >= 3 -> Song(title = parts[0], artist = parts[1], album = parts[2], durationMs = 0, uri = "", path = "", size = 0, isLocal = false, songmid = "txt")
                    parts.size == 2 -> Song(title = parts[0], artist = parts[1], album = "", durationMs = 0, uri = "", path = "", size = 0, isLocal = false, songmid = "txt")
                    parts.size == 1 -> Song(title = parts[0], artist = "未知", album = "", durationMs = 0, uri = "", path = "", size = 0, isLocal = false, songmid = "txt")
                    else -> null
                }
            }.filterNotNull()
            found = list
            picked = list.map { it.title + "|" + it.artist + "|" + it.songmid }.toSet()
            msg = "按文本解析出 ${list.size} 首"
        }
    }

    fun doImport() {
        if (name.isBlank()) { msg = "请填写歌单名称"; return }
        if (found.isEmpty()) { msg = "没有可导入的歌曲"; return }
        busy = true
        msg = "正在导入..."
        scope.launch {
            val list = found.filter { keyOf(it) in picked }
            val r = runCatching {
                repo.importSongsAsFavoritePlaylist(
                    name = name,
                    source = if (list.any { !it.isLocal } && list.all { it.sourceId == "netease" }) "netease" else "imported",
                    linkId = "",
                    cover = "",
                    songs = list,
                    onProgress = { done, total ->
                        msg = "正在导入 " + done + "/" + total + " ..."
                    }
                )
                list.size
            }
            msg = r.fold(
                onSuccess = { n -> "已收藏歌单「" + name + "」，导入 " + n + " 首（在\"歌单\"里查看）" },
                onFailure = { "导入失败: " + it.message }
            )
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("<", fontSize = 20.sp) }
            Text("导入歌单", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("歌单名称") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("本地目录") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("链接/文本") })
        }
        Spacer(Modifier.height(12.dp))
        if (mode == 0) {
            OutlinedTextField(value = dir, onValueChange = { dir = it }, label = { Text("目录路径") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = { scan() }, enabled = !busy) { Text("扫描") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { picked = if (picked.size == found.size) emptySet() else found.map { keyOf(it) }.toSet() }) {
                    Text(if (picked.size == found.size) "取消全选" else "全选")
                }
            }
        } else {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("歌单链接，或每行一首：歌名|歌手|专辑") },
                placeholder = { Text("https://music.163.com/playlist?id=...") },
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { recognize() }, enabled = !busy) { Text(if (PlaylistLinkResolver.isUrl(text)) "联网解析歌单" else "解析文本") }
        }
        Spacer(Modifier.height(8.dp))
        if (msg.isNotBlank()) Text(msg, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(found, key = { it.uid }) { s ->
                val key = keyOf(s)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = key in picked, onCheckedChange = { c ->
                        picked = if (c) picked + key else picked - key
                    })
                    Column {
                        Text(s.title, fontSize = 14.sp)
                        if (s.artist.isNotBlank()) Text(s.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { doImport() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "处理中..." else "导入所选 (" + picked.size + ")")
        }
    }
}

private fun walk(dir: File, depth: Int): List<Song> {
    if (depth > 6 || !dir.isDirectory) return emptyList()
    val out = mutableListOf<Song>()
    val files = dir.listFiles() ?: return emptyList()
    for (f in files) {
        if (f.isDirectory) {
            out.addAll(walk(f, depth + 1))
        } else {
            val ext = f.extension.lowercase()
            if (ext in EXTS && f.length() > 0) {
                out.add(Song(title = f.nameWithoutExtension, artist = "未知", album = f.parentFile?.name ?: "", durationMs = 0, uri = f.absolutePath, path = f.absolutePath, size = f.length(), isLocal = true))
            }
        }
    }
    return out
}