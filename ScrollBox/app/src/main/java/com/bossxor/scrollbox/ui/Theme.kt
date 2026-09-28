package com.bossxor.scrollbox.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF0F6B6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EDED),
    onPrimaryContainer = Color(0xFF0A3D3D),
    secondary = Color(0xFFC17F59),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5E6DC),
    onSecondaryContainer = Color(0xFF5C3A28),
    background = Color(0xFFF4F0E8),
    onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1C1B19),
    surfaceVariant = Color(0xFFECE6DC),
    onSurfaceVariant = Color(0xFF4A453E),
    outline = Color(0xFFC9C2B6),
    outlineVariant = Color(0xFFE0D9CE)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF5EC8C5),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF1A4A4A),
    onPrimaryContainer = Color(0xFFB8EDEC),
    secondary = Color(0xFFE0A882),
    onSecondary = Color(0xFF422010),
    secondaryContainer = Color(0xFF5C3A28),
    onSecondaryContainer = Color(0xFFF5E6DC),
    background = Color(0xFF0E1416),
    onBackground = Color(0xFFE4E2DE),
    surface = Color(0xFF172022),
    onSurface = Color(0xFFE4E2DE),
    surfaceVariant = Color(0xFF2A3438),
    onSurfaceVariant = Color(0xFFC4C0B8),
    outline = Color(0xFF8A857C),
    outlineVariant = Color(0xFF3A4246)
)

private val ScrollBoxTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)

private val ScrollBoxShapes = Shapes(
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun ScrollBoxTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = ScrollBoxTypography,
        shapes = ScrollBoxShapes,
        content = content
    )
}
