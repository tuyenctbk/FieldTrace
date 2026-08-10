package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FieldTraceColorScheme = darkColorScheme(
    primary = GlowCyan,
    onPrimary = Color.Black,
    secondary = GlowAmber,
    onSecondary = Color.Black,
    tertiary = GlowRed,
    onTertiary = Color.White,
    background = BackgroundBlack,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceMedium,
    onSurfaceVariant = Color.White
)

private val FieldTraceLightColorScheme = lightColorScheme(
    primary = Color(0xFF0F172A), // Dark slate
    onPrimary = Color.White,
    secondary = Color(0xFF0288D1), // Cyan/Blue
    onSecondary = Color.White,
    tertiary = Color(0xFFD32F2F), // Warning red
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC), // Slate light background
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) FieldTraceColorScheme else FieldTraceLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
