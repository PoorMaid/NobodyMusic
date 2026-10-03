package com.nobodymusic.tyxypoor.ui.theme

import android.app.WallpaperManager
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import android.app.Activity

data class ThemePrefs(
    val useDynamicColor: Boolean = true,
    val seedColor: Long = 0xFF6650a4,
    val darkMode: Int = 0,
    val pureBlack: Boolean = false,
    val background: Int = 0,
    val bgImagePath: String = ""
)

val LocalThemePrefs = staticCompositionLocalOf { ThemePrefs() }

private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFFDFBFF),
    surfaceVariant = Color(0xFFE7E0EC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val PureBlackColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF121212)
)

@Composable
fun NobodyTheme(
    prefs: ThemePrefs = ThemePrefs(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val sysDark = isSystemInDarkTheme()
    val dark = when (prefs.darkMode) {
        1 -> true
        2 -> false
        else -> sysDark
    }
    val scheme = when {
        prefs.pureBlack && dark -> PureBlackColors
        prefs.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    SideEffect {
        val activity = context as? Activity ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            .isAppearanceLightStatusBars = !dark
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content
    )
}
