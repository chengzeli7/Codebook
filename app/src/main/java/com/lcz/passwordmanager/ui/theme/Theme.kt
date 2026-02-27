package com.lcz.passwordmanager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 应用主题配置 - 极简风格
 * 
 * 纯黑白灰配色，简约不简单
 * 
 * @author lcz
 * @since 1.0.0
 */

private val LightColorScheme = lightColorScheme(
    primary = Gray900,
    onPrimary = Color.White,
    primaryContainer = Gray100,
    onPrimaryContainer = Gray900,
    secondary = AccentBlue,
    onSecondary = Color.White,
    secondaryContainer = Gray100,
    onSecondaryContainer = Gray900,
    tertiary = Gray700,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    error = ErrorRed,
    onError = Color.White,
    outline = Gray300
)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Gray900,
    primaryContainer = Gray800,
    onPrimaryContainer = Color.White,
    secondary = AccentBlue,
    onSecondary = Color.White,
    secondaryContainer = Gray700,
    onSecondaryContainer = Color.White,
    tertiary = Gray300,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray400,
    error = ErrorRed,
    onError = Color.White,
    outline = Gray600
)

@Composable
fun 小梨密码Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}