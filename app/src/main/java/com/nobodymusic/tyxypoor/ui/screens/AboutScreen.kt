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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val VERSION = "1.1.0"
private const val WEBSITE = "https://nobady.bbroot.com"
private const val EMAIL = "tyxypoor@outlook.com"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val openWebsite: () -> Unit = {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    val sendMail: () -> Unit = {
        runCatching {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$EMAIL"))
                .putExtra(Intent.EXTRA_SUBJECT, "Nobody Music 反馈")
            context.startActivity(Intent.createChooser(i, "发送邮件").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$EMAIL")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            Text("关于", style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Text("Nobody Music", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "版本 $VERSION",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "开源、免费、无广告的音乐播放器。本地优先，支持自定义音源插件，不依赖任何自营服务器。",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            InfoLine("作者", "Nobody 团队")
            Spacer(Modifier.height(12.dp))
            InfoLine("贡献成员", "田园蜥蜴")
            Spacer(Modifier.height(12.dp))
            InfoLine("开源协议", "GPL-3.0")
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = openWebsite,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("访问官网  nobady.bbroot.com") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = sendMail,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("联系邮箱  tyxypoor@outlook.com") }
            Spacer(Modifier.height(24.dp))
            Text(
                "本项目不提供任何音乐内容，所有音源均由用户自行导入。请支持正版音乐。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
