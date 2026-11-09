package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF002228),
    primaryContainer = Color(0xFF003844),
    onPrimaryContainer = NeonCyan,

    secondary = NeonEmerald,
    onSecondary = Color(0xFF002A1A),
    secondaryContainer = Color(0xFF00442B),
    onSecondaryContainer = NeonEmerald,

    tertiary = NeonViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF381E72),
    onTertiaryContainer = Color(0xFFEADDFF),

    background = CyberBackground,
    onBackground = TextPrimary,

    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = CyberBorder,
    outlineVariant = CyberDivider,

    error = NeonCoral,
    onError = Color.White
)

@Composable
fun FreeDownloadTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = Typography,
        content = content
    )
}
