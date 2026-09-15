package ru.mgsu.molotkova.schedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF183153),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E5F5),
    onPrimaryContainer = Color(0xFF10243E),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EDF2),
    onSurface = Color(0xFF1A1D21),
    onSurfaceVariant = Color(0xFF5B6470)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7F0),
    onPrimary = Color(0xFF0C2038),
    primaryContainer = Color(0xFF183153),
    background = Color(0xFF111418),
    surface = Color(0xFF191D22),
    surfaceVariant = Color(0xFF262C33),
    onSurface = Color(0xFFF0F2F4),
    onSurfaceVariant = Color(0xFFBCC4CE)
)

@Composable
fun MolotkovaScheduleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
