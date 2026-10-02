package com.nobodymusic.tyxypoor.ui.screens
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.nobodymusic.tyxypoor.data.BackupManager
import com.nobodymusic.tyxypoor.di.ServiceLocator
import com.nobodymusic.tyxypoor.ui.LocalAppState
import kotlinx.coroutines.launch
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenPlaylistImport: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val app = LocalAppState.current
    val theme by app.theme.collectAsStateWithLifecycle()
    val sleepRemain by app.sleepRemain.collectAsStateWithLifecycle()
    val downloadDir by app.downloadDir.collectAsStateWithLifecycle()
    val dlQuality by app.dlQuality.collectAsStateWithLifecycle()
    val eqOn by app.eq.enabled.collectAsStateWithLifecycle()
    val eqReady by app.eq.ready.collectAsStateWithLifecycle()
    val eqLevels by app.eq.levels.collectAsStateWithLifecycle()
    val eqPreset by app.eq.preset.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var scanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf("") }
    var backupResult by remember { mutableStateOf("") }
    var customMinutes by remember { mutableStateOf("") }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val json = BackupManager.exportJson(ServiceLocator.repository)
            BackupManager.writeToUri(context, uri, json)
            backupResult = "已导出 backup.json"
        }.onFailure { backupResult = "导出失败：${it.message}" }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching { BackupManager.readFromUri(context, uri) }
                .getOrDefault("")
            if (text.isBlank()) {
                backupResult = "读取文件失败"
            } else {
                val r = BackupManager.importJson(ServiceLocator.repository, text)
                backupResult = r.getOrElse { "导入失败：${it.message}" }
                app.refreshLocal()
            }
        }
    }
    val dirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) app.setDownloadDir(uri.toString())
    }

    val bgPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ctx = context
        runCatching {
            val f = java.io.File(ctx.filesDir, "bg_custom.jpg")
            val ins = ctx.contentResolver.openInputStream(uri) ?: return@runCatching
            ins.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            app.saveTheme(app.theme.value.copy(background = 3, bgImagePath = f.absolutePath))
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Text("设置", style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SectionTitle("外观")
            SwitchRow("动态取色", theme.useDynamicColor) {
                app.saveTheme(theme.copy(useDynamicColor = it))
            }
            SwitchRow("纯黑模式", theme.pureBlack) {
                app.saveTheme(theme.copy(pureBlack = it))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("背景")
                Text(
                    listOf("默认", "纯色", "跟随主题", "自定义图片")[theme.background.coerceIn(0, 3)],
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    app.saveTheme(theme.copy(background = (theme.background + 1) % 3))
                }) { Text("切换") }
                Spacer(Modifier.padding(start = 8.dp))
                OutlinedButton(onClick = { bgPicker.launch("image/*") }) {
                    Text("选择图片")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("深色模式")
                Text(
                    listOf("跟随系统", "浅色", "深色")[theme.darkMode.coerceIn(0, 2)],
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    app.saveTheme(theme.copy(darkMode = (theme.darkMode + 1) % 3))
                }) { Text("切换") }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("播放")
            Text("定时停止播放（夜间睡眠用）", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { app.startSleepTimer(15) }) { Text("15 分") }
                OutlinedButton(onClick = { app.startSleepTimer(30) }) { Text("30 分") }
                OutlinedButton(onClick = { app.startSleepTimer(60) }) { Text("60 分") }
                OutlinedButton(onClick = { app.startSleepTimer(90) }) { Text("90 分") }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = customMinutes,
                    onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ) { inner ->
                    if (customMinutes.isEmpty()) {
                        Text("自定义分钟", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    inner()
                }
                Button(onClick = {
                    val m = customMinutes.toIntOrNull() ?: 0
                    if (m > 0) app.startSleepTimer(m)
                }) { Text("开始") }
            }
            Spacer(Modifier.height(8.dp))
            if (sleepRemain > 0) {
                val mm = sleepRemain / 60
                val ss = sleepRemain % 60
                Text("剩余 %d 分 %02d 秒后停止".format(mm, ss),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Button(onClick = { app.cancelSleepTimer() }) { Text("取消定时") }
            } else {
                Text("未设定定时", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("均衡器")
            Text("播放中开始歌曲后自动挂载到当前音频会话",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SwitchRow("启用均衡器", eqOn) { app.eq.setEnabled(it) }
            if (!eqReady) {
                Text("尚未就绪：请先播放一首歌，或点下方按钮手动挂载", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { app.eq.attachGlobal() }) { Text("手动挂载均衡器") }
            } else {
                val presetNames = remember(eqReady) { app.eq.presetNames() }
                if (presetNames.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("预设", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = {
                            val p = app.eq.preset.value
                            app.eq.usePreset(if (p <= 0) presetNames.size - 1 else p - 1)
                        }) { Text("◀") }
                        Text(
                            presetNames.getOrElse(eqPreset) { "自定义" },
                            Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(onClick = {
                            app.eq.usePreset((app.eq.preset.value + 1) % presetNames.size)
                        }) { Text("▶") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val bands = app.eq.bandLabels
                bands.forEachIndexed { i, label ->
                    val cur = eqLevels.getOrElse(i) { 0f }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, Modifier.padding(end = 8.dp),
                            style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = cur,
                            onValueChange = { app.eq.setBand(i, it) },
                            modifier = Modifier.weight(1f)
                        )
                        Text("${cur.toInt()}", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (bands.isEmpty()) {
                    Text("该设备不支持均衡器频段", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("悬浮歌词 / 灵动岛")
            Text("桌面歌词：屏幕底部显示；灵动岛：顶部胶囊显示。可拖动。需悬浮窗权限。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (!app.canFloat()) {
                Button(onClick = {
                    val i = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + app.context.packageName))
                    app.context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("授予悬浮窗权限") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { app.showFloating("desktop") }) { Text("桌面歌词") }
                    OutlinedButton(onClick = { app.showFloating("island") }) { Text("灵动岛") }
                    OutlinedButton(onClick = { app.hideFloating() }) { Text("关闭") }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            SectionTitle("下载")
            Text("下载音质（越高文件越大；部分音源可能不支持）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                app.qualityOptions.forEach { q ->
                    FilterChip(
                        selected = dlQuality == q,
                        onClick = { app.setDlQuality(q) },
                        label = { Text(q) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("下载目录（用于保存下载的音乐）", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                if (downloadDir.isBlank()) "未设置（将提示先选择目录）" else downloadDir,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { dirLauncher.launch(null) }) { Text("选择目录") }
                if (downloadDir.isNotBlank()) {
                    OutlinedButton(onClick = { app.setDownloadDir("") }) { Text("清除") }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("媒体库")
            Text("扫描本地音乐文件", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !scanning,
                onClick = {
                    scanning = true
                    scope.launch {
                        val found = runCatching {
                            ServiceLocator.localScanner.scan()
                        }.getOrDefault(emptyList())
                        runCatching {
                            ServiceLocator.repository.insertSongs(found)
                        }
                        app.refreshLocal()
                        scanResult = "扫描到 ${found.size} 首，已入库"
                        scanning = false
                    }
                }
            ) { Text(if (scanning) "扫描中…" else "开始扫描") }
            if (scanResult.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(scanResult, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            SectionTitle("音乐源")
            ClickRow("权限设置") { onOpenPermissions() }
            ClickRow("管理音源插件") { onOpenSources() }
            ClickRow("导入歌单") { onOpenPlaylistImport() }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            SectionTitle("数据管理")
            Text("导出/导入 backup.json（不含任何密码、Token）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportLauncher.launch("nobody-backup.json") }) {
                    Text("导出备份")
                }
                Button(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                    Text("导入备份")
                }
            }
            if (backupResult.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(backupResult, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            SectionTitle("其他")
            ClickRow("关于 Nobody Music") { onOpenAbout() }
            Spacer(Modifier.height(40.dp))
        }
    }
}
@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}
@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
@Composable
private fun ClickRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Button(onClick = onClick) { Text("打开") }
    }
}