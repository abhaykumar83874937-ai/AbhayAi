package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueDark,
    onPrimary = Color(0xFF0A192F),
    primaryContainer = DeepIndigoDark,
    onPrimaryContainer = IceSapphire,
    secondary = LavenderDark,
    onSecondary = Color(0xFF1E1042),
    secondaryContainer = PlumDark,
    onSecondaryContainer = SoftLavender,
    tertiary = CyanDark,
    onTertiary = Color(0xFF082F49),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = IceSapphire,
    onPrimaryContainer = DeepSapphire,
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = SoftLavender,
    onSecondaryContainer = DeepPlum,
    tertiary = SkyCyan,
    onTertiary = Color.White,
    tertiaryContainer = IceCyan,
    onTertiaryContainer = Color(0xFF0C4A6E),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = ErrorRed
)

val SmartStudyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartStudyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SmartStudyShapes,
        content = content
    )
}
