package com.nobodymusic.tyxypoor.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private data class PermItem(
    val title: String,
    val desc: String,
    val granted: (Context) -> Boolean,
    val manifest: String?,
    val special: ((Context) -> Unit)? = null
)

@Composable
fun PermissionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    val items = buildList {
        add(PermItem("存储 / 音乐文件", "扫描本地音乐、下载歌曲",
            { ctx ->
                if (Build.VERSION.SDK_INT >= 33)
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                else
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            },
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        ))
        if (Build.VERSION.SDK_INT >= 33) add(PermItem("通知", "后台播放控制、下载提示",
            { ctx -> ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED },
            Manifest.permission.POST_NOTIFICATIONS))
        else add(PermItem("通知", "后台播放控制、下载提示",
            { ctx -> androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled() },
            null,
            { ctx -> openAppDetail(ctx) }
        ))
        add(PermItem("悬浮窗", "桌面歌词 / 灵动岛",
            { ctx -> Settings.canDrawOverlays(ctx) },
            null,
            { ctx ->
                runCatching {
                    ctx.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        ))
        add(PermItem("忽略电池优化", "后台长时间播放不被杀",
            { ctx -> runCatching {
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                pm.isIgnoringBatteryOptimizations(ctx.packageName)
            }.getOrDefault(false) },
            null,
            { ctx ->
                runCatching {
                    ctx.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        ))
    }
    val perms = items.mapNotNull { it.manifest }.toTypedArray()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh++ }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Text("权限设置", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("以下为软件运行所需权限，可逐项申请。", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            for (item in items) {
                val ok = item.granted(context)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge)
                        Text(item.desc, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (ok) {
                        Text("已授予", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge)
                    } else {
                        OutlinedButton(onClick = {
                            if (item.special != null) item.special.invoke(context)
                            else if (perms.isNotEmpty()) launcher.launch(perms)
                            refresh++
                        }) { Text("去授权") }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) {
                Text("刷新状态")
            }
            Spacer(Modifier.height(20.dp))
            Text("提示：部分权限（如忽略电池优化）需在系统设置中手动确认，返回后点“刷新状态”即可。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun openAppDetail(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
