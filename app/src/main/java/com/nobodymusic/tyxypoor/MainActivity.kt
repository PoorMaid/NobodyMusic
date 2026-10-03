package com.nobodymusic.tyxypoor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nobodymusic.tyxypoor.ui.AppState
import com.nobodymusic.tyxypoor.ui.LocalAppState
import com.nobodymusic.tyxypoor.ui.MiniPlayer
import com.nobodymusic.tyxypoor.ui.screens.AboutScreen
import com.nobodymusic.tyxypoor.ui.screens.HomeScreen
import com.nobodymusic.tyxypoor.ui.screens.PermissionScreen
import com.nobodymusic.tyxypoor.ui.screens.PlayerScreen
import com.nobodymusic.tyxypoor.ui.screens.PlaylistImportScreen
import com.nobodymusic.tyxypoor.ui.screens.SettingsScreen
import com.nobodymusic.tyxypoor.ui.screens.SourceScreen
import com.nobodymusic.tyxypoor.ui.theme.NobodyTheme
import kotlinx.coroutines.delay

private enum class Route { Home, Player, Settings, About, Sources, PlaylistImport, Permissions }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = AppState(this)
        setContent {
            val prefs by app.theme.collectAsStateWithLifecycle()
            NobodyTheme(prefs) {
                androidx.compose.runtime.CompositionLocalProvider(LocalAppState provides app) {
                    AppRoot()
                }
            }
        }
    }

    @Composable
    private fun AppRoot() {
        val app = LocalAppState.current
        val context = LocalContext.current
        val prefs by app.theme.collectAsStateWithLifecycle()
        var route by remember { mutableStateOf(Route.Home) }
        var homeSection by remember { mutableIntStateOf(0) }
        var homeTab by remember { mutableIntStateOf(0) }
        val current by app.player.current.collectAsStateWithLifecycle()
        val dlMsg by app.dlMsg.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }
        var showWelcome by remember {
            mutableStateOf(!context.getSharedPreferences("nobody_prefs", 0).getBoolean("welcome_off", false))
        }
        LaunchedEffect(Unit) {
            while (true) {
                app.player.tick()
                delay(500)
            }
        }
        LaunchedEffect(dlMsg) {
            if (dlMsg.isNotBlank()) snackbar.showSnackbar(dlMsg)
        }
        DisposableEffect(Unit) {
            onDispose { app.player.detachUi() }
        }
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        Box(Modifier.fillMaxSize()) {
            when (prefs.background) {
                0 -> {
                    Image(
                        painterResource(R.drawable.app_background),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isDark) 0.55f else 0.25f)))
                }
                2 -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                3 -> {
                    if (prefs.bgImagePath.isNotBlank()) {
                        AsyncImage(
                            model = java.io.File(prefs.bgImagePath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isDark) 0.5f else 0.2f)))
                    } else {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                    }
                }
                else -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            }
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    if (route == Route.Home) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Nobody Music",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { route = Route.Settings }) {
                                Icon(Icons.Default.Settings, "设置")
                            }
                        }
                    }
                },
                bottomBar = {
                    if (route == Route.Home && current != null) {
                        MiniPlayer { route = Route.Player }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (route) {
                        Route.Home -> HomeScreen(
                            onOpenPlayer = { route = Route.Player },
                            section = homeSection,
                            onSection = { homeSection = it },
                            homeTab = homeTab,
                            onHomeTab = { homeTab = it }
                        )
                        Route.Player -> PlayerScreen(onBack = { route = Route.Home })
                        Route.Settings -> SettingsScreen(
                            onBack = { route = Route.Home },
                            onOpenAbout = { route = Route.About },
                            onOpenSources = { route = Route.Sources },
                            onOpenPlaylistImport = { route = Route.PlaylistImport },
                            onOpenPermissions = { route = Route.Permissions }
                        )
                        Route.About -> AboutScreen(onBack = { route = Route.Settings })
                        Route.Sources -> SourceScreen(onBack = { route = Route.Settings })
                        Route.PlaylistImport -> PlaylistImportScreen(onBack = { route = Route.Settings })
                        Route.Permissions -> PermissionScreen(onBack = { route = Route.Settings })
                    }
                }
            }
        }
        if (showWelcome) {
            WelcomeDialog(
                onClose = { showWelcome = false },
                onNever = {
                    context.getSharedPreferences("nobody_prefs", 0).edit()
                        .putBoolean("welcome_off", true).apply()
                    showWelcome = false
                },
                onOpenPermissions = {
                    showWelcome = false
                    route = Route.Permissions
                }
            )
        }
    }
}

@Composable
private fun WelcomeDialog(
    onClose: () -> Unit,
    onNever: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("欢迎使用 Nobody Music") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("本软件开源、免费、无广告，仅供学习交流使用。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("请在 24 小时内删除本软件。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("官网：nodady.bbroot.com", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("反馈邮箱：tyxypoor@outlook.com", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("作者：田园蜥蜴团队 · Nobody", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("知道了") } },
        dismissButton = {
            Row {
                TextButton(onClick = onOpenPermissions) { Text("权限设置") }
                TextButton(onClick = onNever) { Text("不再显示") }
            }
        }
    )
}
