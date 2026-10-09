package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = Slate950,
    primaryContainer = CyanContainer,
    onPrimaryContainer = OnCyanContainer,
    secondary = EmeraldSecondaryLight,
    onSecondary = Slate950,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmeraldContainer,
    tertiary = AmberTertiaryLight,
    onTertiary = Slate950,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = Color(0xFF090D16),
    onBackground = Slate50,
    surface = DarkSurface,
    onSurface = Slate50,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Slate400,
    outline = Slate700
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = EmeraldSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF047857),
    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFFB45309),
    background = Color(0xFFF8FAFC),
    onBackground = Slate900,
    surface = LightSurface,
    onSurface = Slate900,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Slate600,
    outline = Slate200
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set default to false to showcase our polished custom palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
