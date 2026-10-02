package com.nobodymusic.tyxypoor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nobodymusic.tyxypoor.data.SourceEntity
import com.nobodymusic.tyxypoor.di.ServiceLocator
import com.nobodymusic.tyxypoor.source.SourceResult
import kotlinx.coroutines.launch

private val STORE_PRESETS = listOf(
    Triple("聚合", "https://raw.githubusercontent.com/pdone/lx-music-source/main/juhe/latest.js", "聚合"),
    Triple("Grass", "https://raw.githubusercontent.com/pdone/lx-music-source/main/grass/latest.js", "全平台"),
    Triple("Ikun", "https://raw.githubusercontent.com/pdone/lx-music-source/main/ikun/latest.js", "全平台"),
    Triple("LX", "https://raw.githubusercontent.com/pdone/lx-music-source/main/lx/latest.js", "聚合"),
    Triple("Flower", "https://raw.githubusercontent.com/pdone/lx-music-source/main/flower/latest.js", "流行"),
    Triple("Huibq", "https://raw.githubusercontent.com/pdone/lx-music-source/main/huibq/latest.js", "全平台"),
    Triple("Sixyin", "https://raw.githubusercontent.com/pdone/lx-music-source/main/sixyin/latest.js", "全平台")
)

@Composable
fun SourceScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<SourceEntity>>(emptyList()) }
    var mode by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var scriptText by remember { mutableStateOf("") }
    var batchText by remember { mutableStateOf("") }
    var jsonText by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    suspend fun reload() {
        list = ServiceLocator.repository.sourcesOnce()
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Text("音源管理", style = MaterialTheme.typography.titleLarge)
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            listOf("音源商店", "网络添加", "批量导入").forEachIndexed { i, t ->
                TextButton(onClick = { mode = i; msg = "" }, modifier = Modifier.weight(1f)) {
                    Text(
                        t,
                        color = if (mode == i) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Divider()

        Column(
            Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (mode) {
                0 -> {
                    Text(
                        "点击即从社区仓库下载并导入音源脚本。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            busy = true
                            msg = "正在自动选择最佳音源"
                            scope.launch {
                                val r = ServiceLocator.sourceManager.autoSelectBestSource()
                                msg = when (r) {
                                    is SourceResult.Ok -> "已选用最佳音源: " + r.value
                                    is SourceResult.Err -> "自动选择失败: " + r.message
                                }
                                busy = false
                                reload()
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("一键自动选择最佳音源") }
                    Spacer(Modifier.height(8.dp))
                    STORE_PRESETS.forEach { (n, u, tag) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(n, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                enabled = !busy,
                                onClick = {
                                    busy = true
                                    msg = "正在导入 $n …"
                                    scope.launch {
                                        val r = runCatching {
                                            ServiceLocator.sourceManager
                                                .addSource(id = n, name = n, jsUrl = u)
                                        }
                                        msg = r.fold(
                                            { "已导入 $n" },
                                            { "导入失败: ${it.message}" }
                                        )
                                        reload()
                                        busy = false
                                    }
                                }
                            ) { Text("导入") }
                        }
                    }
                }

                1 -> {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("音源名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("JS 脚本直链 (http/https)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        enabled = !busy && name.isNotBlank() && url.isNotBlank(),
                        onClick = {
                            busy = true
                            msg = ""
                            scope.launch {
                                val r = runCatching {
                                    ServiceLocator.sourceManager.addSource(
                                        id = name.trim(),
                                        name = name.trim(),
                                        jsUrl = url.trim()
                                    )
                                }
                                msg = r.fold({ "已添加" }, { "添加失败: ${it.message}" })
                                name = ""
                                url = ""
                                reload()
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "处理中…" else "添加并下载脚本") }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = scriptText,
                        onValueChange = { scriptText = it },
                        label = { Text("或粘贴本地 JS 脚本内容") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        enabled = !busy && scriptText.isNotBlank(),
                        onClick = {
                            val n = name.ifBlank { "本地音源" + (list.size + 1) }
                            busy = true
                            msg = ""
                            scope.launch {
                                val r = runCatching {
                                    ServiceLocator.sourceManager
                                        .addSourceRaw(id = n.trim(), name = n.trim(), script = scriptText)
                                }
                                msg = r.fold({ "已导入本地脚本" }, { "导入失败: ${it.message}" })
                                scriptText = ""
                                reload()
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("导入本地脚本") }
                }

                else -> {
                    Text(
                        "每行一条，格式：名称|脚本直链",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = batchText,
                        onValueChange = { batchText = it },
                        label = { Text("文本批量导入") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        enabled = !busy && batchText.isNotBlank(),
                        onClick = {
                            val items = batchText.lines()
                                .map { it.trim() }
                                .filter { it.contains("|") }
                                .map { it.substringBefore("|").trim() to it.substringAfter("|").trim() }
                                .filter { it.first.isNotBlank() && it.second.isNotBlank() }
                            if (items.isEmpty()) {
                                msg = "没有可导入的条目"
                                return@Button
                            }
                            busy = true
                            msg = ""
                            scope.launch {
                                val errs = ServiceLocator.sourceManager.importBatch(items) { i, n, nm ->
                                    msg = "导入中 $i/$n · $nm"
                                }
                                msg = if (errs.isEmpty()) "全部导入成功 (${items.size})"
                                else "部分失败: ${errs.joinToString("; ").take(120)}"
                                batchText = ""
                                reload()
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "处理中…" else "批量导入") }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "JSON 数组，支持 name/url 字段",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        label = { Text("JSON 配置导入") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        enabled = !busy && jsonText.isNotBlank(),
                        onClick = {
                            busy = true
                            msg = ""
                            scope.launch {
                                val errs = ServiceLocator.sourceManager.importJsonConfig(jsonText)
                                msg = if (errs.isEmpty()) "JSON 导入成功"
                                else "部分失败: ${errs.joinToString("; ").take(120)}"
                                jsonText = ""
                                reload()
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("导入 JSON 配置") }
                }
            }

            if (msg.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Divider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("测试关键词") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        if (list.isEmpty()) {
            Text(
                "暂无音源。可在「音源商店」一键导入。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(list, key = { i2, _ -> i2 }) { idx, s ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${idx + 1}. ${s.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                when {
                                    s.script.isBlank() -> "脚本未下载"
                                    s.jsUrl.startsWith("local://") -> "本地脚本"
                                    else -> "脚本已就绪"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "↑",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (idx > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .clickable(enabled = idx > 0) {
                                    scope.launch {
                                        ServiceLocator.repository.setSourcePriority(s.id, s.priority - 1)
                                        ServiceLocator.repository.setSourcePriority(list[idx - 1].id, list[idx - 1].priority + 1)
                                        reload()
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        Text(
                            "↓",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (idx < list.size - 1) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .clickable(enabled = idx < list.size - 1) {
                                    scope.launch {
                                        ServiceLocator.repository.setSourcePriority(s.id, s.priority + 1)
                                        ServiceLocator.repository.setSourcePriority(list[idx + 1].id, list[idx + 1].priority - 1)
                                        reload()
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        Text(
                            "测试",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(enabled = !busy && keyword.isNotBlank()) {
                                    busy = true
                                    msg = "测试 ${s.name} …"
                                    scope.launch {
                                        val r = ServiceLocator.sourceManager
                                            .testSource(s.id, keyword.trim())
                                        msg = when (r) {
                                            is SourceResult.Ok -> "${s.name} 命中 ${r.value} 条"
                                            is SourceResult.Err -> "${s.name} 失败: ${r.message}"
                                        }
                                        busy = false
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        if (!s.jsUrl.startsWith("local://")) {
                            Text(
                                "刷新",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable(enabled = !busy) {
                                        busy = true
                                        scope.launch {
                                            val r = runCatching {
                                                ServiceLocator.sourceManager.refreshSource(s.id)
                                            }
                                            msg = r.fold(
                                                { "已刷新 ${s.name}" },
                                                { "刷新失败: ${it.message}" }
                                            )
                                            reload()
                                            busy = false
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Switch(
                            checked = s.enabled,
                            onCheckedChange = { on ->
                                scope.launch {
                                    ServiceLocator.repository.setSourceEnabled(s.id, on)
                                    reload()
                                }
                            }
                        )
                        IconButton(onClick = {
                            scope.launch {
                                ServiceLocator.repository.deleteSource(s.id)
                                reload()
                            }
                        }) { Icon(Icons.Default.Delete, "删除") }
                    }
                }
            }
        }
    }
}