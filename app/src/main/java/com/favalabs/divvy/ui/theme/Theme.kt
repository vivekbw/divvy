package com.favalabs.divvy.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

data class ThemeController(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val setMode: (ThemeMode) -> Unit = {}
)

val LocalThemeController = compositionLocalOf { ThemeController() }

private val LightColors = lightColorScheme(
    primary = Amber,
    onPrimary = Charcoal,
    primaryContainer = AmberSubtle,
    onPrimaryContainer = AmberDark,
    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF6FF),
    onSecondaryContainer = Color(0xFF3B82F6),
    tertiary = PositiveGreen,
    onTertiary = Color.White,
    error = NegativeRed,
    onError = Color.White,
    errorContainer = NegativeRedLight,
    onErrorContainer = NegativeRedDark,
    background = BackgroundWhite,
    onBackground = Charcoal,
    surface = BackgroundWhite,
    onSurface = Charcoal,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = BorderLight,
)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Charcoal,
    primaryContainer = Color(0xFF3D2E00),
    onPrimaryContainer = AmberLight,
    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    error = NegativeRed,
    onError = Color.White,
    errorContainer = NegativeRedDark,
    onErrorContainer = NegativeRedLight,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
)

@Composable
fun DivvyTheme(
    context: Context = LocalContext.current,
    content: @Composable () -> Unit
) {
    val themeMode by ThemePreferences.themeModeFlow(context)
        .collectAsState(initial = ThemeMode.SYSTEM)
    val scope = rememberCoroutineScope()

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val controller = ThemeController(
        mode = themeMode,
        setMode = { newMode ->
            scope.launch { ThemePreferences.setThemeMode(context, newMode) }
        }
    )

    val colors = if (isDark) DarkColors else LightColors

    CompositionLocalProvider(LocalThemeController provides controller) {
        MaterialTheme(
            colorScheme = colors,
            typography = DivvyTypography,
            content = content
        )
    }
}
