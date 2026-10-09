package com.example.foodhubapp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = BrandDark,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = Warning,
    tertiary = SoftGreen,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color(0xFFF4F5FA),
    onSurface = Color(0xFFF4F5FA),
    onSurfaceVariant = Color(0xFFD8C4BD),
    outline = Color(0xFF9C8279),
    outlineVariant = Color(0xFF494D55),
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = Secondary,
    onSecondary = WarningDark,
    secondaryContainer = WarmAccent,
    onSecondaryContainer = WarningDark,
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = SoftGreen,
    onTertiaryContainer = SuccessDark,
    background = AppBackground,
    surface = CeramicSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = CaptionBrown,
    outlineVariant = OutlineVariant,
    onBackground = Neutral,
    onSurface = Neutral,
    error = Color(0xFFB3261E),
    surfaceTint = Color.Transparent,
)

private val FoodHubShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun FoodHubAppTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // FoodHub has a deliberate brand palette; dynamic colors would replace it
    // with wallpaper colors on Android 12+.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FoodHubShapes,
        content = content,
    )
}
